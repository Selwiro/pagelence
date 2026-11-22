# WebDAV 进度同步与本地导出/导入

状态：**已实现**（2026-11-21，T069）。实现依据 MileNote 的 WebDAV 经验总结
（`/Users/wangwb/Workspace/100_Work/2026-10-29_milenote/work/docs/webdav/`），
按 Pagelence 的 `state.json` v2 字段落地。

同步范围只有进度、生词卡、偏好、等级、日活与自制书清单；书籍正文与词典只读数据
不进同步（同 [`progress-format.md`](progress-format.md) 第七节）。

## 一、模块与文件

新增纯 JVM 模块 `:sync`（`work/android/sync/`），不依赖 Android，可直接跑单测。

| 文件 | 内容 |
| --- | --- |
| `WebDavUrl.kt` | 文件地址校验、父目录推导 |
| `Etag.kt` | ETag 去 `W/`、去引号、条件写头还原 |
| `WebDavModels.kt` | 凭据、远端元信息、三种写入条件 |
| `OkHttpWebDavClient.kt` | HEAD/GET/PUT/MKCOL、HEAD 退化、409 建目录、双重限量、错误归类 |
| `StateMerge.kt`（`:core`） | 字段级合并（书籍、卡片、日活、等级、偏好），本地导入复用同一张规则表 |
| `SyncDecision.kt` | 决策表 |
| `SyncEngine.kt` | 同步状态机、412 自愈、互斥、退避、无 ETag 备份 |
| `SyncState.kt` | `sync-state.json`、`sync-baseline.json` 原子读写 |
| `StateTransfer.kt` | 本地导出/导入（导入走同一套合并） |
| `SyncProviders.kt` | 坚果云 / Nextcloud / 群晖 / InfiniCloud 地址模板 |

接线：

- `:data` 的 `ReadingStateStore` 实现 `LocalState`（快照 + 修订号 + `replaceIfUnchanged`），
  并在每次写入后通知监听者，供同步防抖；
- `:app-phone` 的 `AppGraph` 装配 `SyncEngine`，`SyncCredentialStore` 负责 Keystore；
- `:ui-m3` 增加「同步与备份」页与文案。

本机文件（`filesDir`）：

```
state.json            本地主文档
sync-state.json       etag、基线 hash、最近成功/失败、退避
sync-baseline.json    上次成功同步的完整文档（三向合并的 base）
sync-backups/         服务器不返回 ETag 时，覆盖前的远端副本（保留 3 份）
```

## 二、判定与合并

判定使用本地明文 hash、远端 ETag（无 ETag 时用远端 hash）与基线 hash：

| 远端 | 本地改 | 远端改 | 动作 |
| --- | --- | --- | --- |
| 无 | 否 | — | 不做事 |
| 无 | 是 | — | `If-None-Match: *` 新建 |
| 有 | 是 | 否 | `If-Match: 本次 ETag` 覆盖 |
| 有 | 否 | 是 | 下载并原子替换本地 |
| 有 | 是 | 是 | 字段合并 → 先落本机 → 条件上传 |
| 有 | 否 | 否 | 已是最新 |

补充两条长期使用才会遇到的分支：

- 本机是空白文档（无书、无卡、无日活、偏好全默认）而远端有内容 → **下载**，
  避免新设备把空文档合并后上传；
- 远端文件在成功同步后消失（被其他设备或网盘清理）→ 用 `If-None-Match: *`
  **重建**，而不是静默保持远端为空。

字段合并（`StateMerge`）：

| 字段 | 规则 |
| --- | --- |
| `readUnitIds` | 并集，保留本机顺序，远端新增按远端顺序追加 |
| `paragraphId` / `reader` / `lastReadAt` | `lastReadAt` 较晚的一端胜出 |
| `wordCards` | 按 `id`；任一端是墓碑即取墓碑（已删除的卡不因另一端更晚的编辑复活），否则 `updatedAt` 较晚者胜 |
| `annotations` | 与 `wordCards` 相同的墓碑与 `updatedAt` 规则 |
| `lookupHistory` | 按词条 `id` 取并集，`lookedUpAt` 较晚者胜，倒序截断到 200 条；不做墓碑 |
| `dailyActivity` | 每天 `completedUnits` 取 max，日期取并集（`DailyActivityRules.mergeDayMax`） |
| 等级四字段 | `levelUpdatedAt` 较晚的一端；两端都缺该字段时回退到文档 `updatedAt` |
| `preferences` | 有基线时按「哪边改过」裁决；两边都改取文档 `updatedAt` 较晚者；本机是空文档（新装/清数据）时取文件里的偏好 |
| `recentAssessmentPapers` | 并集，保留最近两套 |
| `deviceId` / `updatedAt` | 永远用本机；只有内容真变化时才刷新 `updatedAt` |

因此**不需要「两边都改就停手」**：v2 的每个字段都有确定胜者，正常路径不产生
需要用户裁决的冲突。合并结果与本地一致时不重写文档，避免两台设备互相判成「改过」。

## 三、写协议

- `If-Match` **只使用本轮 GET/HEAD 刚拿到的 ETag**；不提供回退到上次 ETag 的路径。
- 远端不存在 → `If-None-Match: *`；用户确认覆盖才不带条件（当前无手动覆盖入口）。
- 412 → 重新 GET、重新判定、重新合并，最多 3 次尝试；不是拿新 ETag 重发旧合并结果。
- 服务器不返回 ETag → 覆盖前把远端内容写入 `sync-backups/`（保留 3 份），
  并在结果提示「服务器不支持条件写」。
- HEAD 被 405/501 拒绝 → `GET Range: bytes=0-0`，206 时从 `Content-Range` 取总长。
- PUT 409 → 逐级 MKCOL 后重试一次。
- 下载同时用 `Content-Length` 与流式计数限量（20 MB），超限立即断开。
- 401（账号/应用密码）与 403（目录只读/无权限）分别归类；404/410 视为「不存在」。

## 四、引擎行为

- 手动与自动同步共用一把 `ReentrantLock`；并发时后来者返回 `Busy`，不排队。
- 写回前比对修订号（`replaceIfUnchanged`）；同步期间用户又存了卡片或翻页，
  本轮作废重算，最多 2 次。
- 地址或用户名变化 → 清空 ETag、基线与失败状态，绝不用旧基线合并新服务器。
- 失败退避 30 s → 60 s → 120 s，上限 2 分钟；回前台、手动同步、任意成功清零。
- 最近一次失败按稳定类别持久化（类别 + code + 消息 + 时间），重启后仍可见；
  状态行按「最近成功 / 最近失败」中较晚者显示。
- 触发：启动（距上次成功/失败 ≥60 s）、回到前台、离开阅读器、手动、本地修改后
  30 s 防抖。**不做后台周期任务**，与经验总结 §10 一致。

## 五、安全

- 默认只允许 HTTPS；明文 HTTP 仅 loopback / `10.0.2.2`，且需显式打开调试开关。
  Android 的 `network_security_config.xml` 同样只放行这些地址，两层一致。
- 地址必须指向文件（不能以 `/` 结尾、不能带查询串/锚点/内嵌账号密码）。
- 密码用 Android Keystore 的 AES-256-GCM 密钥加密后存 SharedPreferences（每次随机 IV）；
  密钥不出 Keystore，Keystore 失效时解密返回 null 并要求重输。URL 与用户名是普通配置。
- 不记录密码、远端内容或 Authorization 头；无日志上报。

## 六、导出 / 导入

- 导出就是当前 `state.json`（`StateTransfer.export`），走系统文件选择器写 JSON，
  默认文件名 `pagelence-backup-YYYYMMDD-HHmm.json`。
- 导入先 `ReadingStateCodec.decode`（含 v1→v2 迁移），读取上限 5 MB；损坏 JSON、
  更高 `schemaVersion`、超限都在写库前拒绝并给出各自原因，原状态不变。
- 选完文件先显示预览（书数、已读单元数、生词卡数、等级、文档 `updatedAt`），
  用户确认后才写库。
- 默认「合并」：与本地进度合并，导出之后新读的进度与卡片仍在。
- 可选「覆盖」：二次确认后整份替换，保留本机 `deviceId`，替换前写 `state.json.bak`；
  备份写失败则取消覆盖。
- 没有配置同步的用户也能用这条路径换机或备份。

## 七、服务商地址

| 服务 | 地址形态 | 备注 |
| --- | --- | --- |
| 坚果云 | `https://dav.jianguoyun.com/dav/<目录>/state.json` | 必须用应用密码 |
| Nextcloud | `https://<host>/remote.php/dav/files/<用户名>/Pagelence/state.json` | 路径区分大小写 |
| 群晖 | `https://<host>:5006/<共享文件夹>/Pagelence/state.json` | 需 WebDAV Server 套件 |
| InfiniCloud | `https://<子域名>.infini-cloud.net/dav/Pagelence/state.json` | — |
| 自建 | `https://<host>/dav/Pagelence/state.json` | 注意 mod_dav 的 409 与 MKCOL 权限 |

选择「坚果云」会自动填入完整地址；其余只显示模板，主机/共享名需用户替换。

## 八、测试与验收

自动化：

- `:core` 207 项单测，含合并规则（并集、位置、墓碑优先、等级 `levelUpdatedAt`、
  日活 max、偏好基线、`updatedAt` 稳定）。
- `:sync` 43 项单测：URL/ETag、决策表、双设备收敛、412 自愈、同步期间本地修改、
  无 ETag 备份、远端删除重建、换服务器清基线、退避、本机 HTTP 桩协议矩阵
  （HEAD/GET/PUT/MKCOL、401/403/412、Range 206、超限）、真实 OkHttp 客户端端到端，
  以及导入校验（预览计数、损坏文件、未来版本、合并/覆盖、导出可解码）。
- `:data` 20 项单测，含持久化、恢复与覆盖前 `state.json.bak` 备份。

手工 QA（API 34 模拟器 ×2 + 本机 WebDAV 桩）：

1. A 配置并上传 → 远端 `state.json` 出现；
2. B 配置同一地址 → 自动合并下载 A 的较新续读点，同时保留 B 自己的书；
3. A 重新打开 → 下载 B 上传的合并结果，两边书籍集合一致；
4. A 收集生词卡 → 重启触发同步 → B 拉取后出现同一张卡；
5. 导出 JSON（2067 B）→ 再导入 → 提示「导入完成，没有新内容」，数据不变；
6. 密码文件里只有 `iv:密文`，无明文；换地址后基线清除。

未覆盖：真实坚果云/Nextcloud/群晖联调（与经验总结的风险一致，上线前需补）、
412 的真实服务商注入（只有桩与单测）、后台同步（刻意不做）。

## 九、与 `android-plan.md` 第六节的差异

| 计划 | 实际 | 原因 |
| --- | --- | --- |
| 协议层用 dav4jvm | 直接用 OkHttp 实现 | 需要的行为（条件写、HEAD 退化、限量、错误归类）都要自己控制；少一层依赖 |
| 失败进 WorkManager 重试 | 前台触发 + 30 s 防抖，失败持久化后退避 | 经验总结明确建议不做后台周期任务；避免电量/权限坑 |
| 可选端到端加密 | 未实现 | 计划列为后续项；当前同步内容不含正文，风险可接受 |
| 明文 HTTP 局域网开关 | 仅 loopback / 10.0.2.2 | 与既有 `network_security_config.xml` 一致，避免全局放开明文 |
