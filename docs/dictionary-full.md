# 完整词表（可选下载）

状态：2026-11-22 实现（T123）并上线。词表托管在 GitHub Releases
（tag [`dict-39cfb96`](https://github.com/Selwiro/pagelence/releases/tag/dict-39cfb96)），
`gradle.properties` 默认打开设置页下载入口；已安装用户离线可查，升级不清包。
格式依据见 [App 内容规范](../../../management/app-content-spec.md) §3.3.7。

## 一、形态

- 随包核心表仍是 `content/third-party/dictionaries/ecdict.json`（38,292 条，7.9 MiB，
  开箱即用、离线可查）。
- 完整表是**可选下载**的单个 SQLite 文件，字段与核心表同构：

  ```sql
  CREATE TABLE entries (
    term TEXT PRIMARY KEY,  -- 小写、去所有格后的词形
    phonetic TEXT,
    senses TEXT,            -- JSON：[[词性, 中文释义], …]
    en TEXT,                -- JSON：[[词性, 英文释义], …]
    lemma TEXT,
    form TEXT
  );
  CREATE TABLE meta (key TEXT PRIMARY KEY, value TEXT);  -- schemaVersion / dictVersion / termCount
  ```

- 当前产物：`dictVersion 39cfb96`、770,575 条、85,970,944 B（82.0 MiB）。
  `content/dict-manifest.json` 编译进 APK，固定 `dictVersion`、`schemaVersion`（1）、
  `bytes`、`sha256` 与 `minAppVersionCode`（1）。

## 二、构建

上游 CSV 不入库，用固定版本：

```sh
curl -L -o /tmp/ecdict.csv \
  https://raw.githubusercontent.com/skywind3000/ECDICT/39cfb9672c0906b63569e0453c2e6abfb9ab182a/ecdict.csv
python3 tools/build_full_dictionary.py --ecdict-csv /tmp/ecdict.csv
python3 tools/build_full_dictionary.py --check   # 有产物时校验 manifest 与文件一致
```

产物（`dist/` 已在 `.gitignore`，**不入库**）：

```
dist/dict/full-dict.sqlite
dist/dict/dict-manifest.json
dist/dict/LICENSE-ECDICT.txt
content/dict-manifest.json     # 随 APK，入库
```

与核心表的差异是**有意为之**：

- 完整表保留带领域标签的释义（`[网络]`、`[医]`、`[化]`…），只去掉行首标签本身。
  核心表为常用词而丢弃这些行；完整表正是为长尾词准备，丢掉就失去下载意义。
- 没有释义、也没有音标的 36 行跳过；其余 770,575 行全部入库。
- `lemma` 与 `form` 沿用核心表的 `exchange` 规则（`0:` 与原形行列出的 `s/p/d/i/3/r/t`）
  及同样的小词表覆盖（`am/are/was/does`）。

**manifest 必须与上传的 SQLite 同批次生成**：SQLite 文件不保证跨版本字节一致，
先上传旧文件、再单独 `--check` 会得到 `stale`。上线时把三个文件一起上传，并把
`content/dict-manifest.json` 的改动一并提交。

## 三、安装与查词

- 安装目录 `filesDir/dictionaries/`：`full-dict.sqlite`、`.part` 临时文件、
  `.part.json` 与 `full-dict.json`（安装指纹）。都不在 `content/` 下，
  **App 升级重播内容不会清除用户已下载的词表**。
- 下载先写 `.part`，用 `Range` 续传；服务器忽略 Range 返回 200 时从头重下。
  暂停/失败保留 `.part`，重试从字节偏移继续。完整后校验大小与 `sha256`，
  通过才原子改名替换正式文件。
- 安装后 `WordQuery` 先查核心表，**核心表命中就不再查完整表**（即使该条只有音标），未命中再查完整表；两表都没有才走原有兜底
  （缩合词、复合词、专名、本句中文）。安装/删除后关闭 SQLite 句柄并清空
  `FileContentRepository` 的 `WordQuery` 缓存。
- SQLite 以 `OPEN_READONLY` 打开；`pagelence_dictionary` 首次点词才建立句柄，
  未安装时每次查询直接落回核心表。
- 若 App 在「原子改名」与「写入安装指纹」之间被杀，下次读取会重新校验文件并补写
  指纹（不重复下载 82 MB）。
- manifest 的 `schemaVersion` 不匹配、或 `minAppVersionCode` 高于当前版本时
  拒绝安装并提示升级 App；`sha256`/大小不符同样拒绝，不改动已在用的表。

## 四、设置页与网络

设置 → 词典 → 完整词表，显示版本、体积、状态（未安装 / 下载百分比 / 已暂停 +
已下载字节 / 已安装 / 失败原因），操作：

- 下载、暂停、继续（续传）、重试、删除（二次确认，删除后回核心表）；
- 非 Wi-Fi 时先弹「当前非 Wi-Fi」并给出体积，确认后照常下载，不静默拦截；
  判定只读当前网络传输类型，因此需要 `ACCESS_NETWORK_STATE` 权限（安装时授予，
  不读取任何网络内容）；
- 失败只提示一次，不挡阅读。

本地验收构建可指向任意地址并临时打开入口：

```sh
./gradlew :app-phone:assembleDebug \
  -Ppagelence.fullDictEnabled=true \
  -Ppagelence.fullDictUrl=http://10.0.2.2:9000/full-dict.sqlite
```

`FullDictSource.URL` 是正式地址常量；托管上线后把上面两个属性设为默认即可。
局域网 HTTP 仍受「允许局域网 HTTP（仅调试）」开关限制。

## 五、验证记录（2026-11-22，API 34 模拟器）

| 项 | 结果 |
| --- | --- |
| 未安装查长尾词 | 导入 1 单元测试书，点 `flibbertigibbet`：提示「词典暂未收录这个词」 |
| 安装后离线查词 | 同一词显示 `/ˌflɪbətiˈdʒɪbɪt/` 与 `n. 饶舌的人`，飞机/断网不影响；仅来自完整表 |
| 先核心后完整 | `:core` 单测：同词核心释义胜出；核心缺失才落完整表；两表都无仍走兜底 |
| 下载进度 | 设置页实时显示百分比与进度条 |
| 暂停/续传 | 47% 暂停（`.part` 39.1 MiB），点继续后按 `Range` 从偏移续传，校验 sha256 后安装成功 |
| 删除 | 二次确认后目录清空，状态回「未安装」，查词回核心表 |
| 非 Wi-Fi 提示 | 关 Wi-Fi 后点下载弹出「当前非 Wi-Fi / 约 82.0 MB / 关闭 / 继续下载」 |
| App 升级 | 用默认构建 `install -r` 覆盖安装后，已安装词表仍在，状态仍为「已安装」 |
| 真实托管 | 从 GitHub Release `dict-39cfb96` 下载：302 跟随后 `Range` 续传 40 MB 分片，sha256 校验通过并安装；设置页显示「已安装」 |
| B 单测 | `:core` 全绿（含 manifest schema/版本/大小/sha、续传、200 重下、取消保留、删除、崩溃补指纹）；`:data` 20 全绿 |
| CI | `tools/ci_check.sh` 通过；新增的 `build_full_dictionary.py --check` 在无产物时跳过 |

## 六、托管（GitHub Releases）

已上线：词表与 manifest 作为 GitHub Release 资产，**每个 `dictVersion` 一个 tag**，
`FullDictSource.URL` 指固定地址：

```
https://github.com/Selwiro/pagelence/releases/download/dict-39cfb96/full-dict.sqlite
```

已验证（2026-11-22，真实 Release）：该地址 302 到 `release-assets.githubusercontent.com`
的签名 URL，签名 URL 支持 `Range`（206）；签名约 1 小时过期，续传时重新打开稳定地址即可
拿到新签名。`AndroidDictTransport` 因此**手动跟随跳转**、在每一跳重发 `Range`，不依赖
`HttpURLConnection` 的自动重定向行为；模拟器上用「40 MB 本地分片 + 真实 GitHub 地址续传」
跑通到 sha256 校验通过。

**为什么标记 pre-release**：App 的 `v0.1.0` 本身是 Pre-release，词表若是普通 Release 会
被 GitHub 当成「Latest Release」，把「检查更新」的落点从 App 变成词表。因此词表 Release
保留 Pre-release 标记，资产地址不受影响。

上传（产物在 `dist/dict/`，不入库）：

```sh
cd work/android
python3 tools/build_full_dictionary.py --ecdict-csv /tmp/ecdict.csv
gh release create dict-39cfb96 \
  dist/dict/full-dict.sqlite dist/dict/dict-manifest.json dist/dict/LICENSE-ECDICT.txt \
  -R Selwiro/pagelence \
  --title "完整词表 39cfb96（ECDICT）" \
  --notes "ECDICT 完整词表，字段与核心表同构；MIT，许可全文与 manifest 一并附上。"
```

入口默认值在 `gradle.properties`：`pagelence.fullDictEnabled=true`（临时关闭用
`-Ppagelence.fullDictEnabled=false`）。新增版本时用 `gh release upload dict-<版本> …`
补资产，或对尚未发布的 tag 直接用 `gh release create`。

- 不要用 `releases/latest/download/...`：latest 随 App 发版移动，manifest 固定的版本
  可能对不上；用 tag 固定。
- 不要用 jsDelivr 之类的仓库文件 CDN：它们不代理 Release 资产，82 MB 也超出其单文件限制。
- 更新词表：新 tag `dict-<新版本>`，重新生成 manifest、改 `FullDictSource.URL`、发版。
- 上传失败或资产被删时保持入口关闭即可，已安装用户的本地表不受影响；GitHub Release
  资产上限 2 GiB，82 MiB 没有触及。

## 七、遗留

- 词表更新需要重新生成 manifest 并随 App 发版（§3.3.7 的既定取舍）；新版本用新 tag，
  并同步改 `FullDictSource.URL`。
- 未在真机验证后台被杀后的续传（模拟器验证了主动暂停、40 MB 分片续传与 GitHub 跳转）。
- GitHub Release 资产没有项目自己的镜像；GitHub 不可达时下载失败可重试，核心表不受影响。
