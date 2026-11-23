# 墨水屏阅读壳（:ui-eink + :app-eink）

状态：**第四增量已落地（2026-11-23，T070 进行中）**。墨案（MOAAN）全刷已接入，只差真机验收；
主流程齐全：书架、书籍详情、阅读（点词词条、生词卡、标注创建、句译开关、书内搜索）、
复习四子页、我的（含 365 天热力图）、设置、书城、同步与备份、关于。
进度与普通版写在同一个 `state.json`。

## 一、为什么是第二个 app，而不是主题

设计理由见 [Android 方案 · 墨水屏版](../../../management/android-plan.md#一设计语言)：墨水屏要的是
翻页、按键、无动效、无跟手滚动；Material 3 的滚动、BottomSheet、ripple、转场是另一套交互，
主题覆盖不了。因此：

- `:ui-eink` 只依赖 Compose **foundation**，不引入 material3、图标库与动效库；
- `:app-eink` 是独立 APK（`applicationId com.pagelence.eink`），可单独上架、单独评价；
- 两个壳共用 `:core` / `:presentation` / `:data` / `:sync`，以及本轮抽出的 `:shell`
  （依赖装配 + 阅读会话 ViewModel + 平台适配）。

## 二、模块结构

```text
:core          内容、进度、词典、分页器、SRS（纯 JVM）
:presentation  UI 模型、Strings、ReaderFlow、分页缓存、墨水屏按键与刷新规则（纯 JVM）
:data / :sync  state.json、WebDAV 与合并（纯 JVM）
:shell         AppGraph、PagelenceViewModel、TTS/Keystore/Uri/网络/SQLite 适配
:ui-eink       墨水屏设计系统 + 书架/阅读/读完页（Compose foundation）
:app-eink      MainActivity、EinkApplication、根组件（applicationId com.pagelence.eink）
:ui-m3 / :app-phone   普通版（不变）
```

`AppGraph(context, ShellConfig)` 的 `ShellConfig` 带版本号与完整词表开关，来自各壳自己的
`BuildConfig`；`:shell` 不再读任何 app 的 `BuildConfig`。

## 三、设计规则

| 项 | 规则 | 落点 |
| --- | --- | --- |
| 颜色 | 纯白纸面 + 近黑正文 + 4 级灰；**禁止黑底**（任何面都白底黑字），无深色模式 | `theme/InkTheme.kt` |
| 排版 | 正文衬线 19sp / 行高 33sp（约 1.74）；界面无衬线；标题 20sp 粗体 | `InkTypography` |
| 组件 | 文字、描边块、1px 分隔线、底栏、输入框、选择行；主按钮用 2dp 描边 + 黑体，不用反色块 | `components/InkComponents.kt` |
| 动效 | 无 | 全部 `indication = null` |
| 阴影/圆角 | 无 | 分层用 1px 描边与留白 |
| 触控 | 按钮与可点行 ≥48dp | `InkButton` / `InkTextButton` |
| 图标 | 白底黑线、**线宽固定 2px**；五档 mipmap 都按目标尺寸直接描边，不从大图缩小 | `tools/build_eink_icon.py` |
| 翻页 | 不滚动；正文左右留白各 22dp、整页高的翻页带 | `EinkReaderScreen` |
| 输入 | 方向键/翻页键/音量键/回车/返回，见下表 | `:presentation` `EinkKeyMap` |
| 刷新 | 换单元必全刷；每 12 页或 90 秒全刷一次（设置里三档） | `:presentation` `EinkRefreshRules` |

### 按键映射（`EinkKeyMap`）

| 键 | 动作 |
| --- | --- |
| ← / ↑ / 音量+ / PageUp | 上一页 |
| → / ↓ / 音量− / PageDown | 下一页（末页再按 = 下一单元） |
| 回车 / 方向键中键 | 确认（本轮只用于按钮焦点场景，暂不拦截） |
| 返回 | 退出阅读页（测验答了一半会弹确认页） |

映射规则放在 `:presentation` 并单测（`EinkInputTest`），壳只把 `keyCode` 传进来。

### 刷新策略（`EinkRefreshRules`）

- 换单元：全刷一次，并清零计数；
- 翻页：距上次全刷满 `fullRefreshEveryPages`（默认 12）页，或满
  `fullRefreshEverySeconds`（默认 90）秒，全刷一次；
- 阈值填 0 表示关闭该项；打开单元时以当前时刻为基线，第一页不会立刻全刷。

真实全刷按厂商分流，见下面的「墨案全刷」。没有可用通道时退化为 `NoopEinkRefresher`
（只做应用层重绘），不会崩溃，也不会假装刷过。

## 四、本轮做了什么

0. **反色块下线（2026-11-22 用户要求）**：按钮、表头、选中态一律不用黑底，
   强调只靠描边粗细与字重；状态栏与导航栏也改成白底深色图标。
1. **抽出 `:shell`**：`AppGraph`、`PagelenceViewModel`、`SpeechController`、`SyncCredentialStore`
   与内容/词典适配共 10 个文件从 `:app-phone` 移到 `:shell`，包名不变；`BuildConfig` 依赖改为
   `ShellConfig`。普通版行为与产物不变（模拟器回归通过）。
2. **`:ui-eink` 设计系统**：`InkTheme`、`InkColors`、`InkTypography` 与 9 个组件。
3. **书架**：在读 / 已读完 / 可读的书三段；冷启动时用随包书单补「可读的书」，新装用户也能开书。
4. **阅读**：与普通版共用 `ReaderFlow` 页序（导读 → 正文 → 测验 → 语法）与 `:core` 的
   `SentencePaginator`；正文按固定 19sp 衬线分页；左右翻页带 + 键盘翻页；测验选项、提交、
   对错标记与得分；语法卡（讲解 / 结构 / 本句 / 易错）。
5. **状态**：同一份 `state.json`（`com.pagelence.eink` 自己的 `filesDir`）；页码、环节、已读单元、
   测验答案照常落盘，杀进程重开回到原位。
6. **读完页**：单元数、原文词数、本书收词、读完日期、再读一遍。
7. **点词与生词卡**：正文按字符偏移取词（`:presentation` 的 `WordBoundary`），点中词打开整页词条：
   音标、原形、词性/等级/本书次数、本句释义、中英释义、例句、收藏/移除；返回键先关词条。
8. **复习**：四个子页——单词（到期/新卡计数、三档评分复习轮、掌握度与下次复习日、常错筛选、删除）、
   语法（256 个语法点列表 → 详情的讲解/结构/易错/例句）、笔记（按书签/划线/笔记筛选，点开跳回原句）、
   查词（历史列表、回看、清空）。
9. **我的**：今日目标与连续、等级与来源、已读单元/读完书/阅读天数、10 项成就。
10. **设置**：同步与备份入口、界面语言、每日目标、刷新强度（轻/标准/强，只存本机）、
   词义语言、导读模式、完整词表下载与删除、关于。
11. **书城**：随包书单（含制作中）、自定义源书目、已下载管理、书源增删与启停、导入本地书籍包（SAF）。
12. **同步**：WebDAV 五个服务商预设、地址/用户名/应用密码、允许局域网 HTTP、保存并测试、
    立即同步、关闭同步、导出/导入 JSON（含导入预览与合并/覆盖）。
13. **第三增量（2026-11-23）**：
    - **书内搜索**：输入关键词检索原句与句译，结果给章 · 单元与片段，点一行翻到该句所在页
      （`jumpTarget` 由阅读页消费）。
    - **标注创建**：阅读页工具条的「标记句子」模式——开着时点句子进标注页，可加/删书签、
      划线与笔记；划线整句灰底、书签句首「▍」、笔记句末「✎」，重启后保留。
    - **全部句译**：工具条一键开关，句下跟中文；分页器把译文高度算进去，页数随之变化。
    - **书籍详情**：排版封面、等级/分类/词数/单元数、简介、图书信息、章节目录，
      可加入书架、开始阅读、按章打开。
    - **365 天热力图**：按可用宽度反算格子大小（53 列 × 7 行），0–4 级灰阶。
14. **门禁**：`tools/ci_check.sh` 增加 `:app-eink:assembleDebug` 与两个 lint，并断言 `:ui-eink`
    不得引入 Material 组件。

## 五、验证（API 34 模拟器，2026-11-22）

- 冷启动 `am start -W`：1,111ms（debug，模拟器）；PSS 约 66MB。
- 窄屏 1080×2400 上正文无横向溢出；每单元 3 页、页脚显示「正文 1 / 3」。
- DPAD_RIGHT 翻页 1/3 → 2/3 → 3/3 → 测验 1/2 → 2/2 → 语法；DPAD_LEFT 回退一致。
- 测验：选 B（错）提交 → A 标 ✓、B 标 ✗、显示句译反馈与「得分 1 / 2」；
  `state.json` 的 `readUnitIds` 增加该单元。
- 换到下一单元后 `reader` 字段为 `{"unitId":"chapter-01-u02","phase":"guide",...}`；
  `am force-stop` 重开后书架显示「已读 1 / 103 单元 · 上次读到 姐姐的抱怨」。
- 普通版回归：`:app-phone:assembleDebug`、安装、启动（1,003ms）与书架显示正常。
- 单测：`EinkInputTest` 6 条、`EinkRefreshTest` 6 条；`check_strings.py` 523 键成对。

## 六、剩下的部分

按「能读完一个单元」之外还缺什么排：

| 缺口 | 说明 |
| --- | --- |
| 逐句「译 / 析」图标 | 墨屏用「全部句译」代替逐句展开，长难句解析卡未做 |
| 章节内单元定位 | 详情页按章打开，章内单元列表未做 |
| 完整词表下载进度动画 | 只显示状态文本，无进度条 |
| 真实全刷 | 接厂商 SDK（`EinkRefresher`），并做残影实测 |
| 真机验收 | 横向溢出、按键映射、30 页全刷、48dp 与对比度都只能在真机上验 |

## 七、墨案全刷

结论：**能接，不需要 SDK、不需要 root**——墨案系统监听一条刷新广播。

| 项 | 内容 |
| --- | --- |
| 机制 | `sendBroadcast(Intent("android.eink.force.refresh"))`，固件驱动执行 `EINK_GC16_MODE` 整屏刷新 |
| 依据 | 墨案 Mini 阅 / inkPalm 5（Android 8.1）上，KOReader 用户补丁用同一广播实现了 GC16 自动全刷（[来源](https://hackintosh.club/d/10000104)）；作者注明其他墨案机型「理论上可用，未实机验证」 |
| 代码 | `app-eink` 的 `MoaanBroadcastRefresher`；`EinkRefreshers.create()` 按 `Build` 串（moaan / inkpalm / inkpad / pantone / mix7）自动选后端 |
| 设置 | 设置 → 刷新强度下显示「全刷后端」；有「强制发送墨案全刷广播（实验）」开关（`Build` 认不出的机型用）；「测试一次全刷」立即发一次 |
| 日志 | `adb logcat -s EinkRefresh`：发送成功打印「已发送 android.eink.force.refresh」 |
| 兜底 | 非墨案设备或用旧固件时后端显示「未接入（仅应用重绘）」，翻页与残影策略照常，只是不触发 GC16 |

### 真机验收步骤（墨案）

1. 打开「全刷后端」确认显示「墨案广播（GC16）」；若不是，打开「强制发送…」开关。
2. 点「测试一次全刷」，屏幕应整屏闪一次（GC16）；`logcat` 里应有对应日志。
3. 连续翻 30 页，观察是否按「刷新强度」设置全刷、残影是否清掉。
4. 若广播无效（不闪、日志无驱动响应），先确认系统版本与型号；墨案部分新机型
   把刷新策略收在系统层，可改用「长按桌面图标 → 单独设置本应用的刷新模式/全刷频率」，
   或用 Shizuku 写固件 e-ink 表（InkPalm+ 的做法，属社区方案，不进本仓库）。

### 图标与应用名

- **应用名**：`app-eink/src/main/res/values/strings.xml` 里 `app_name` 与普通版相同（`页有所成`），
  启动器上两个发行物同名，只靠图标区分——这是 2026-11-23 的明确要求。
- **图标**：`tools/build_eink_icon.py` 按 48/72/96/144/192 五档直接绘制（形状取自
  [management/brand/pagelence-eink.svg](../../management/brand/pagelence-eink.svg)，坐标 128 格），
  每档线宽都是 **2 像素**、不做超采样、不从 1024 主图缩小——缩小时 2px 线会掉成 1px 的灰边。
- 改图标后跑 `python3 tools/build_eink_icon.py`，再 `./gradlew :app-eink:assembleDebug` 即可；
  验证线宽：取任意一档 PNG，横向扫描黑色连续段，众数应为 2。

## 八、跑起来

```sh
./gradlew :app-eink:assembleDebug
adb install -r app-eink/build/outputs/apk/debug/app-eink-debug.apk
./gradlew :app-eink:lintDebug :ui-eink:lintDebug
```

模拟器上功能可验，但墨水屏特性（全刷、残影、按键手感、对比度）必须真机；接厂商 SDK 的位置
在 `app-eink/src/main/kotlin/com/pagelence/eink/MainActivity.kt` 的 `refresher` 字段。
