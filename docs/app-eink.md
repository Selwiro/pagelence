# 墨水屏阅读壳（:ui-eink + :app-eink）

状态：**第一增量已落地（2026-11-22，T070 进行中）**。可安装、可读完一个单元、进度与普通版
写在同一个 `state.json`；点词、书签、复习、书城、设置、我的仍是后续增量。

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
| 颜色 | 纯白纸面 + 近黑正文 + 4 级灰；无深色模式 | `theme/InkTheme.kt` |
| 排版 | 正文衬线 19sp / 行高 33sp（约 1.74）；界面无衬线；标题 20sp 粗体 | `InkTypography` |
| 组件 | 只有文字、描边块、1px 分隔线、反色主按钮；共 9 个 | `components/InkComponents.kt` |
| 动效 | 无 | 全部 `indication = null` |
| 阴影/圆角 | 无 | 分层用 1px 描边与留白 |
| 触控 | 按钮与可点行 ≥48dp | `InkButton` / `InkTextButton` |
| 翻页 | 不滚动；正文左右留白各 22dp、整页高的翻页带 | `EinkReaderScreen` |
| 输入 | 方向键/翻页键/音量键/回车/返回，见下表 | `:presentation` `EinkKeyMap` |
| 刷新 | 换单元必全刷；每 12 页或 90 秒全刷一次 | `:presentation` `EinkRefreshRules` |

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

真实全刷要厂商 SDK（ONYX/Boox 等）；接入点已留好：`app-eink` 的 `EinkRefresher`，
当前是 `NoopEinkRefresher`，见第七节。

## 四、本轮做了什么

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
7. **门禁**：`tools/ci_check.sh` 增加 `:app-eink:assembleDebug` 与两个 lint，并断言 `:ui-eink`
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
| 点词词条 | 正文中间不拦截点击，留给这一增量；需要词条面板、生词卡、句译展开的墨水屏版 |
| 书签/划线/笔记 | `:core` 与 `state.json` 已支持，只差墨屏交互（长按被禁用，需要按键菜单） |
| 复习 / 查词 / 笔记列表 | 复用 `:presentation` 的 presenter，做三个列表页 |
| 书城 / 书籍详情 | 下载、导入、书源设置；墨屏版目前只有随包书与已下载书 |
| 设置 / 我的 | 刷新强度、语言、关于、等级、统计与热力图 |
| 同步界面 | WebDAV 配置与手动同步 |
| 真实全刷 | 接厂商 SDK（`EinkRefresher`），并做残影实测 |
| 真机验收 | 横向溢出、按键映射、30 页全刷、48dp 与对比度都只能在真机上验 |

## 七、跑起来

```sh
./gradlew :app-eink:assembleDebug
adb install -r app-eink/build/outputs/apk/debug/app-eink-debug.apk
./gradlew :app-eink:lintDebug :ui-eink:lintDebug
```

模拟器上功能可验，但墨水屏特性（全刷、残影、按键手感、对比度）必须真机；接厂商 SDK 的位置
在 `app-eink/src/main/kotlin/com/pagelence/eink/MainActivity.kt` 的 `refresher` 字段。
