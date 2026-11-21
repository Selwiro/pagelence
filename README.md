# Pagelence · 页有所成

![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.4.21-7F52FF?logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)
![License](https://img.shields.io/badge/license-Apache--2.0-blue)
![Status](https://img.shields.io/badge/status-0.1.0%20dev-orange)

**简体中文** · English（待补）

页有所成（Pagelence）是一款**本地优先的 Android 英语原著阅读器**，面向学过英语、长期不用、读原版书容易中断的成年人：以约五分钟为一个短读单元，从原文里学或复习一点词汇、短语、语法或背景知识，帮助读者一本接一本地读完英文原版书。

应用不注册账号，只在书城刷新目录与下载书籍包时联网。阅读进度、生词卡和偏好只保存在手机本地的一份 `state.json` 里，没有订阅、广告或埋点统计，也没有统计、广告或崩溃上报 SDK。

内置书只选全球公版作品：当前随包分发两本——《怪医杜立德》（*The Story of Doctor Dolittle*，1920 年版，21 章 103 单元，A2，全书完整）与《爱丽丝漫游仙境记》（*Alice's Adventures in Wonderland*，1866 Macmillan 版，12 章 104 单元，B1，全书完整）。

当前为 **0.1.0 开发版**：普通版（Material 3）已可安装运行，墨水屏版尚未开工。官网与下载页在 <https://selwiro.github.io/pagelence/>，签名 release 包随 GitHub Releases 提供；网站源文件在 [`site/`](site/README.md)。

## 应用特点

- **本地优先，无需账号：** 进度与生词卡只保存在设备上；应用只在书城刷新目录、下载书籍或导入线上书籍包时联网，没有后台同步、埋点或账号。
- **短读，而不是啃书：** 每个单元以一个自然停顿的情节片段为目标，读完再做测验、看语法，关掉应用也能原地续读。
- **读原文时随时求助：** 每句句末有小号句译图标，点按在该句下方展开中文；单词可点，看音标、语境义、A1–C2 等级、本书出现次数与两套离线词典。
- **学到的词留得下：** 点词即可收进生词卡，复习时先看词面回忆，再看释义与原文语境。
- **语法从原文里长出来：** 每个单元的语法卡以语法点为标题，下面是一句话介绍、本书原句和语法解析；不再显示语法库的其他例句。
- **水平只调默认值，不锁内容：** 首次可选 A1–C2 自评，决定默认的词义语言与导读方式；任何单元都能读。
- **双语界面：** 界面语言（中文/英文）与词义语言（中文/中英/英文）分开设置。
- **数据可迁移的底层：** 进度使用带 `schemaVersion` 的单文件状态，v1 文档启动时自动迁移，为导入导出与同步留好接口。

## 功能状态

| 范围 | 状态 |
| --- | --- |
| 书架（只放在读）、单元阅读、点词、句译图标、读后测验、语法卡、生词卡、我的、设置 | 已完成 |
| 翻页/上下滑动双模式、句级分页、单元分块进度条、导读预习与退出续读 | 已完成 |
| 语法页信息层级、顶栏移除导读入口、词义等级 chip | 已完成 |
| 标注虚线、翻译图标缩小、词义高亮与顶栏固定书籍信息 | 已完成 |
| 继续下一段先进入下一单元导读 | 已完成 |
| 单元进度条四个分块可点击跳转 | 已完成 |
| 段落连续排版、句译在句下展开 | 已完成 |
| 句末解析改图标、语法与导读跨单元滑动 | 已完成 |
| 选中左右留白、进度行优化、复习页背单词模式 | 已完成 |
| 状态 v2（等级、日活、v1→v2 迁移）、首次 A1–C2 自评、等级提示 | 已完成 |
| 屏幕分页、导读独立页、四栏导航、复习栏（单词/语法） | 已完成并验收；朗读入口按 2026-11-19 反馈暂时下线 |
| 单元目录（任意跳转/回看）、双语带读、点词即查、纯白底色、去段间线 | 已完成并验收（2026-11-19 反馈） |
| 通用核心词典（ECDICT 38,229 条：3 万词内常用词 + 考试/Oxford/CEFR 词 + 两本书词形，中英双释义，屈折形按原形取义） | 已完成，生成脚本 `tools/build_dictionaries.py` |
| 词级五层（list / aligned / derived / tag / freq）与词频表随包；面板显示 A1–C2 等级与本书次数，不再显示词频档；中文词性标签单行 | 已完成，脚本 `tools/build_word_levels.py`、`tools/build_word_frequencies.py` |
| 语法库 schema 2 与按级别浏览/详情 | 已完成，共 256 条（与教师版 1:1） |
| 我的页整合、快速/高级测评、题库（210 题，含 15 篇短文） | 已完成并验收 |
| 关于页（应用信息、检查更新、官网/GitHub、隐私与数据、开源许可） | 已完成 |
| 学习热力图（dailyActivity、365 天、灰阶、空状态） | 已完成并验收 |
| 默认排版封面（书名/作者、五本指定配色、下载书稳定散列） | 已完成并验收 |
| 书城：内置五档书架、CEFR 等级筛选、自定义源下载、书籍详情与加入书架、导入本地/线上书籍包 | 已完成；默认源托管上线前不显示、不请求 |
| 点按整条短语提示（短语、多词专名、带标点缩写） | 已完成 |
| 第二本内置书《爱丽丝》全书 104 单元的学习标注（句译、提示、测验、语法） | 已完成 |
| 书城右上角菜单：导入本地书籍包、导入线上书籍包、添加源 | 已完成 |
| 进度/生词卡导出与导入（JSON）、WebDAV 同步（可选，默认关闭） | 已完成；真机服务商联调仍待补 |
| 墨水屏版 | 未开始（M5） |
| 《怪医杜立德》全书 103 单元的学习标注（句译、提示、测验、语法） | 已完成 |

## 界面预览

截图取自 0.1.0 开发版（API 34 模拟器）。截图不随默认分支提交：只有书架、正文与点词三张必要图随官网一起放在
`gh-pages` 分支根目录，由 GitHub Pages 输出，README 用完整 URL 引用。

| 书架 | 正文分页 | 点词词义 |
|:---:|:---:|:---:|
| <img src="https://selwiro.github.io/pagelence/library.png" width="220" alt="书架"> | <img src="https://selwiro.github.io/pagelence/reader.png" width="220" alt="正文分页"> | <img src="https://selwiro.github.io/pagelence/word-panel.png" width="220" alt="点词词义"> |

## 功能介绍

### 短读单元：导读、正文与句译

一个单元依次是：导读页（开始阅读 / 预习）→ 正文 → 逐题测验 → 本段语法（没有语法就直接进入下一段）。

阅读方式有两种，默认**翻页**：翻页模式按屏幕高度切句级页，段落可以跨页，左右滑动或点两侧翻页；上下滑动模式连续滚动，末尾进入测验。设置页「阅读方式」可随时切换，旧状态里没有明确选过布局的会自动落到翻页。

正文按书里的自然段连续排布，不再按句拆成多个文本块；默认系统非衬线体（墨水屏版将默认衬线）、字号四档可调。句译入口是每个句末的小号「文A」图标，像脚注标记一样留在段落里；点按后在该句下方展开中文，再点一次收起。内容没有句译的句子不显示图标，不用机器翻译充数。

顶栏下是单元进度条，按导读 / 正文 / 测验 / 语法分成四个可点击按钮：点对应分块跳到导读、正文、第一道测验或语法页；正文回到当前/上次正文页。正文块按已读页数填充，测验块按已答题数填充，全部完成时整条走完。翻页模式底部只留一个页码胶囊，最后一页可直接「进入测验」，不再有「今天读到这里」。点返回即保存离开时的页面或环节，下次打开仍回到那里；测验已选答案也会保留。

导读页的「预习」按本书 CEFR 等级 +1 起列出本单元词汇：先只显示单词并询问是否认识；认识则展开解释后进入下一个，不认识则展开解释并加入生词卡，学完后开始阅读。

### 点词查义

点正文中的单词：正文用主题色圆角底高亮该词并直接弹出词义面板；顶栏始终显示书名/章节，不再切换成词条。关闭面板后高亮立即去掉。面板里可以看到：

- 词面、词目与音标；
- 原形标注：点 `dogs` 时标题仍是 `dogs`，下面标「原形 dog」，释义、音标、等级都按 `dog` 取；
- 等级（A1–C2，来自 CEFR-J 与 Octanove 词级表；界面只显示 A2、B1 等，不加 CEFR 前缀）；
- 本书出现次数；
- 语境义与原文句子；
- ECDICT 通用核心词典一套离线释义，中英同表；不再按单本书裁剪；
- `left`、`bit` 这类本身另有词义的屈折形，在原形释义之后加「该词形另有」，不丢「左边的」「一点」；
- 词典未收录或所选词义语言没有释义时，回退显示另一种语言或所在句子的中文；
- 「收进生词卡」。

查词顺序固定为：书内语境义 → 词形还原 → CEFR 词级 → 本书出现次数 → 离线词典。词形还原由 `tools/build_dictionaries.py` 在构建期完成：读取 ECDICT `exchange` 的原形关系，把原形条目一并收进词典，所以 `daisies` 也能查到 `daisy` 的完整释义。核心词典覆盖常用词与两本书全部词形，导入书共享同一份；点按落在虚线提示范围内时按整条提示取词（短语、多词专名），其余按单词粒度取词；整句提示只在内容层标注。

### 读后测验

每个单元 1–3 道选择题（《怪医杜立德》2 道），提交后给出得分、逐题对错与反馈。

每题作答后立即显示对错、解析与「看原句」；答题中途按返回会先确认，避免误退出丢掉已选答案。

### 语法卡：点 → 例 → 析

每个单元末尾的语法卡按下面的顺序：

- **标题**：语法点名称，右侧是等级徽标（A2、B1 等）；
- **介绍**：标题下的一句话 summary；
- **例**：只放本书中的原句，不再显示语法库的其他例句；
- **析**：结构（form）、作用（function）、易错（pitfalls）、本句（focus）；
 库的完整说明默认收起，点「展开看这个语法点」再显示。

语法库缺少字段时隐藏对应空段，仍显示书内原句与 focus，不会出现空白卡。

### 生词卡

卡片列表显示词面、音标、释义、原句与句译、所属书；屈折形会标出原形。同一本书内按原形归并：先收 `dogs` 再收 `dog` 只保留一张卡，收藏状态也按原形判断。删除采用墓碑标记（写入 `deletedAt`），为将来的同步合并保留删除信息。

复习·单词页顶部有「开始背单词」入口：卡片先只显示词面、音标和原形，点「显示答案」再展开释义、例句与所属书；用「上一个 / 下一个」翻卡，最后一张显示「这一轮完成」并退出。阅读页顶栏仍带生词卡入口；书架右上角只保留我的与设置。

### 我的：水平、统计与热力图

「我的」一页汇总：

- 英语水平：等级、来源（自评/快速测评/完整测评）、一句说明与词汇量参考、修改入口与测评入口；
- 读完本数、跨书已读单元、阅读天数；
- 学习热力图：按日完成单元数与阅读天数，最多 365 天，灰阶格子在本机展示。

阅读天数与每日完成单元数来自状态 v2 的 `dailyActivity`（保留 365 天）。热力图把最近 365 个本地日期排成「周 × 星期」的灰阶网格：按时区写入，跨日与跨年由本地日期决定；只展示、不推送、不断签惩罚，导出与同步合并按每日 max。

### 首次自评

首次启动给出一屏 A1–C2 六档自评，每档一句「大概能读什么」，并附一行词汇量参考（按认识多少词计，连续区间：1,000 以内 → 10,000 以上）。选一档后按档位给出一页推荐，读者确认后才写入：

| 等级 | 界面语言 | 词义语言 | 导读方式 |
| --- | --- | --- | --- |
| A1 / A2 | 中文 | 中文 | 双语带读 |
| B1 | 中文 | 中英 | 双语带读 |
| B2 | 中文 | 中英 | 英文带读 |
| C1 | English | 英文 | 英文带读 |
| C2 | English | 英文 | 沉浸 |

推荐不静默生效：弹窗列出会变的项（只列有变化的），可以「接受推荐」「保持原来的设定」或「现在自己调整」；下滑或返回等于取消，等级与设置都不写。三项都已是推荐值时不弹窗。等级只调整推荐值，不锁内容；也可以「稍后再说」。选择或跳过都会写入 `levelOnboardingDone`，不再重复弹出。从 v1 状态迁移来的老用户不会被自评打断，可在「我的」里补设。

### 书城与书源

- 书城首屏是**内置书架**：A2 怪医杜立德、B1 爱丽丝、B2 四签名、C1 盖茨比、C2 红字；顶部按 CEFR 等级筛选。已随包的两本可直接阅读，未制作的三本标「制作中」，不提供假下载。
- 书卡整卡可点进**书籍页**（内置书、自定义源目录、已下载列表同一页）：封面、书名（随界面语言）、作者、制作者，四格数据条（等级、分类、词汇量、来源），「加入书架 / 立即阅读」，以及进入**详细信息**的「关于此书」；下面是简介、标签与章节目录。「详细信息」按同一版式展示书籍简介与图书信息（作者、单词量、等级、分类、制作者、出版信息、授权信息、语言、章节、单元、来源）。未下载的自定义源书按钮为「下载 / 更新」，下载中就地显示进度；加入书架的书即使 0% 也出现在书架，可再点移除；章节行直接跳到该章第一个单元。
- 内置书清单在 `content/store.json`；是否有正文仍以内容清单为准，避免清单与包体不一致。
- 默认源（GitHub 直连 / 项目镜像）尚未托管，因此界面不显示、也不在打开书城时发起请求；托管上线后打开 `DefaultBookSource.AVAILABLE` 即可恢复，同一套 UI 与下载规则已就绪。
- 自定义源仍可添加；下载前校验 HTTPS 与体积，下载后校验 SHA-256、解 ZIP 做结构与路径检查、再跑书内 schema 校验；任何一步失败都不落库，并给出可读原因。
- 已下载书与内置书同权；开始阅读后会出现在书架。同一本书出现在多个源时，下载前选择来源；摘要相同视为同一本，不重复下载。
- 自定义源默认关闭，可增删启停；另有「允许局域网 HTTP（仅调试）」开关，只放行 localhost 与 10.0.2.2。
- 权限与边界见 [`docs/book-sources.md`](docs/book-sources.md)。

### 水平测评（快速 / 高级）

「我的」页的水平卡可以发起测评，两种模式共用 `content/assessment/bank.json` 一份题库：

| 模式 | 题量 / 时长 | 抽题 | 结果 |
| --- | --- | --- | --- |
| 快速测试 | 12–15 题 / 约 5 分钟 | 词汇 + 语法，覆盖当前等级与相邻两级 | 参考区间（如「A2–B1，建议 A2」） |
| 高级测试 | 30–40 题 / 15–20 分钟 | 每级约 6 题，加一篇短文阅读 | 单级 + 词汇/语法/阅读分项 |

规则在 `:core` 的 `AssessmentRules`：一页一题、作答不即时判对错；不确定的题可以点「跳过此题」，
跳过计为未答对并直接进入下一题；结果页可以「接受新等级」或「保留原级」；接受会同步该等级的
默认阅读辅助。测评不设冷却，随时可以重测，但最近两次试卷的题目不再出现；题干空白渲染为
一条固定宽度的连续下划线。自评或手动改等级仍会清空测评分数。C1/C2 题库
不足 5 题时，高级测试对 C 档也只给区间。测评只作参考评级，不作认证、不锁内容。

题库、抽题与评级细节见 [`docs/assessment.md`](docs/assessment.md)。题库按等级拆在 `content-src/assessment/*.json`，用
`python3 tools/build_assessment_bank.py` 合并成随包分发的
`content/assessment/bank.json`；校验与内容包同一套脚本。

### 设置与外观

| 设置 | 选项 |
| --- | --- |
| 正文字号 | 小 / 中 / 大 / 特大 |
| 词义语言 | 中文 / 中英 / 英文 |
| 导读模式 | 双语带读 / 英文带读 / 沉浸 |
| 标注提示词 | 正文里带书内释义的词和短语是否加浅色虚线 |
| 主题 | 跟随系统 / 浅色 / 深色 |
| 界面语言 | 简体中文 / English |
| 词典 | ECDICT |
| 关于 | 应用图标、名称、版本号与一句话简介；检查更新、官网、GitHub 仓库、隐私与数据、开源许可 |

界面语言与词义语言互相独立：切界面语言只改文案与书名/单元名，词义仍按词义语言过滤。

设置底部的「关于」进入独立页面：上方是应用图标、名称、`v0.1.0 (1)` 与一句话简介，下方依次是
检查更新、官网、GitHub 仓库、隐私与数据、开源许可。官网指向 GitHub Pages，检查更新打开
GitHub Releases，两者都走系统浏览器，不在应用内发网络请求；隐私与数据、开源许可是应用内
可读的长页，摘要与 [LICENSE-CONTENT](LICENSE-CONTENT)、[第三方说明](THIRD_PARTY_NOTICES.md) 一致。

### 本地数据与迁移

- 状态是应用私有目录下的一份 `state.json`（`schemaVersion: 2`）。
- v1 文档启动时自动迁移，保留书籍、续读位置、已读单元、生词卡、偏好与设备标识；缺失字段取默认。
- 版本高于当前只读已知字段，不自动改写，也不清空。
- 写入使用临时文件加原子改名；文件损坏时回退为空状态。
- 首次启动生成 `deviceId`，用于将来多设备合并时破平。

## 内容现状

| 书 | 规模 | 学习内容 | 缺口 |
| --- | --- | --- | --- |
| 《怪医杜立德》 | 21 章、103 单元、729 段、25,498 词 | 全书 103 个单元：1,724 句句译、563 条提示、206 道测验、103 则语法 | 无缺口（`contentStatus: complete`）；长难句解析为可选字段，候选 286 句只进报告 |
| 《爱丽丝漫游仙境记》 | 12 章、104 单元、779 段、26,568 词 | 全书 104 个单元：1,359 句句译、479 条提示、208 道测验、104 则语法 | 无缺口（`contentStatus: complete`）；长难句解析为可选字段，候选 368 句只进报告 |

逐单元明细见 [`doctor-dolittle-1920.gap.json`](content/reports/doctor-dolittle-1920.gap.json) 与
[`alice-1866.gap.json`](content/reports/alice-1866.gap.json)；底本与使用范围见
[`docs/books/doctor-dolittle.md`](docs/books/doctor-dolittle.md) 与
[`docs/books/alice-1866.md`](docs/books/alice-1866.md)。

内容校验当前为**零错误**；两本书都是 `contentStatus: complete`，没有骨架单元。

**不收录仍在版权保护期内的正文。**《老人与海》1952 年版在美国要到 2048 年才进入公有领域，公开仓库与安装包都不收录其正文；内置书只选已确认公版的作品。版权判断与逐本来源见 [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md) 与 [`docs/books/`](docs/books/)。

内容生产（M1）是首版发布的最长路径；工程侧不代写内容。

## 下载

- 官网：<https://selwiro.github.io/pagelence/>
- 最新开发版（0.1.0，签名 release）：<https://github.com/Selwiro/pagelence/releases/tag/v0.1.0>
  - 大小 13,050,047 字节（12.4 MB）
  - SHA-256 `d211466ff60e3ebf79a5256580a589fccc2d295a740d861f7c901638552177c2`
- 官网源文件只在 [`gh-pages` 分支](https://github.com/Selwiro/pagelence/tree/gh-pages) 维护，不放进 `main`；GitHub Pages 从该分支根目录输出。

这是开发版，界面与内容仍会调整。安装、覆盖升级与卸载会清掉本地进度的注意事项见下一节。

## 构建与安装

当前发布形态为**开发版 0.1.0（versionCode 1）**：签名 release 包由 GitHub Releases 提供，debug 包由本地构建。发布签名已于 2026-11-21 配好；密钥库在仓库外，口令只在本机。

### 系统要求

| 项目 | 要求 |
| --- | --- |
| Android 版本 | Android 8.0（API 26）及以上 |
| CPU 架构 | arm64-v8a / armeabi-v7a |
| APK 大小 | 约 19 MB（debug）/ 约 13 MB（release），均含两本内置书、210 题题库、通用核心词典、五层词级表与词频表 |
| 屏幕 | 竖屏手机布局；平板与横屏适配未做 |
| 安装方式 | Android Studio 或 `adb install` |

### 安装

在仓库根目录运行：

```sh
./gradlew :app-phone:assembleDebug
adb install -r app-phone/build/outputs/apk/debug/app-phone-debug.apk
adb shell am start -n com.pagelence/.MainActivity
```

**注意：** debug 包使用调试签名，与其他签名的 APK 不能互相覆盖安装。卸载应用或清除应用数据会一并删除本地进度与生词卡；当前尚未提供导出，换机前请留意。

### 发布包（release）

```sh
./gradlew :app-phone:assembleRelease
# 产物：app-phone/build/outputs/apk/release/app-phone-release.apk（v2 + v3 签名）
```

- 签名读取本机 `keystore.properties`（已 `.gitignore` 排除），密钥库在 `~/.pagelence/pagelence-release.jks`。文件缺失时 release 仍能构建，只是产物不带签名——debug 构建不受影响。
- **密钥库与口令必须异地备份**：丢失后无法为已安装的用户发布覆盖升级，只能卸载重装，而重装会清掉本地进度与生词卡。
- 从 debug 包换到 release 包要先 `adb uninstall com.pagelence`（两者签名不同，不能互相覆盖）；同一发布密钥签出的后续版本可 `adb install -r` 覆盖升级。
- 细节、证书指纹与轮换说明见[发布签名](docs/release-signing.md)。


## 数据与隐私

页有所成不提供账号体系，也不在开发者服务器上保存用户数据。

| 项目 | 处理方式 |
| --- | --- |
| 数据存储 | 应用私有目录下的单文件 `state.json`（schema v2） |
| 写入方式 | 临时文件 + 原子改名，降低写入中断造成损坏的风险 |
| 书籍内容 | 随 APK 分发的 `assets/`，首次启动复制到 `filesDir/content`；`books-index.json` 变化时重播 |
| 词典、词级与词频 | 随包分发的离线数据，运行时只读；本书次数开书时内存扫描，不落盘 |
| 账号与统计 | 无账号、无订阅、无广告、无埋点 |
| 第三方 SDK | 不引入统计、广告、推送或崩溃上报 SDK；运行时依赖只有 AndroidX、Compose、Kotlin 与 OkHttp（仅同步），测试用 JUnit 5 |
| 设备标识 | 首次启动生成随机 UUID 存进 `state.json`，用于将来多设备合并破平；不读取 Android ID、IMEI、MAC 或广告 ID，也不上传 |
| 权限 | 只申请 `INTERNET`（书城刷新与下载）；不申请存储、相机、定位等权限 |
| 网络 | 书城只在「刷新目录 / 下载 / 更新 / 导入线上书籍包」时联网；WebDAV 同步默认关闭，开启后只在启动、回前台、离开阅读器、手动与本地改动 30 秒防抖时同步，没有后台周期任务；默认禁止明文 HTTP，只放行本机调试地址 |
| 外部服务 | 书城可从用户添加的自定义静态目录拉 `catalog.json` 与 `.pagelence`；默认源上线前不联网；WebDAV 只连用户自己填写的服务器，密码用 Keystore 加密保存在本机 |

书中的正文、导读、句译、测验与语法讲解均由项目编排；第三方词典与词级数据沿用各自许可，见 [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md)。

## 已知限制

- **内容：** 两本内置书都已完整——杜立德 103 个单元、爱丽丝 104 个单元；长难句解析是可选字段，两本书都只产出候选报告。
- **近期功能：** 分页、导读独立页、系统 TTS、四栏导航、复习栏（单词/语法）、单元目录、我的页测评与书城书源已实现并验收；本地文件导入仍是占位。
- **朗读：** 2026-11-19 起阅读页与词条暂时不显示朗读入口，TTS 代码与偏好保留；恢复入口时再补真机发音矩阵。
- **点词粒度：** 按单词取词；跨词短语依赖内容里的提示标注。
- **句译展开：** 同一时间只展开一句，翻页或切换单元后自动收起。
- **导入与同步：** 本地书籍包导入、进度/生词卡 JSON 导出与导入、WebDAV 同步已实现；WebDAV 默认关闭，真实坚果云 / Nextcloud / 群晖的联调与 412 注入仍待补，见 [`docs/webdav-sync.md`](docs/webdav-sync.md)。
- **默认源托管：** `DefaultBookSource.AVAILABLE = false`，书城不显示默认源也不请求；仓库上线后改为 `true` 即可复用现有镜像与下载规则，见 [`docs/book-sources.md`](docs/book-sources.md)。
- **屏幕适配：** 以竖屏手机为主；宽度 360/572/906/1280dp 做了分页走查，平板双栏与真实 10/13 寸设备仍未验证。
- **墨水屏版：** 尚未开工（M5）。
- **语法库规模：** 共享库现有 256 个 CEFR-J 语法点（A1.1–B2.2），复习·语法页按级别列出并可查看详情；《怪医杜立德》103 个单元用到 55 个，复现按复习处理。
- **APK 签名：** 发布签名已就绪（[说明](docs/release-signing.md)），但 debug 与 release 签名不同，两者不能互相覆盖安装，切换时要先卸载。密钥库仅存本机，尚未备份与纳入 CI。

## 开发计划

里程碑与退出口径：

| 里程碑 | 内容 | 状态 |
| --- | --- | --- |
| M0 | 仓库骨架、内容格式、`core` 模型与解析 | 已完成 |
| M1 | 内容生产：其余单元补句译、测验、语法解析与提示 | 《怪医杜立德》与《爱丽丝》全书均已完成 |
| M2 | 普通版阅读壳（书库、阅读、点词、句译、测验、语法、生词卡、续读） | 已完成 |
| M3 | 书籍包导入与进度、生词卡导出/导入 | 已完成 |
| M4 | WebDAV 同步（可选，默认关闭） | 已实现；真实服务商联调待补 |
| M5 | 墨水屏壳（独立设计系统，与普通版共用 `:core`） | 未开始 |

剩余工作集中在墨水屏壳与真实 WebDAV 服务商联调两块；两本内置书的内容与阅读体验主线已完成，
没有已知的内容缺口。

发布门槛：内容生产完成前不对外发布缺章版本；前几个单元可先做内部试读。

## 开发者文档

以下内容面向希望自行构建、研究代码或参与开发的用户。

### 技术栈

| 技术 | 用途 |
| --- | --- |
| Kotlin 2.4.21 | 主要开发语言 |
| Jetpack Compose + Material 3 | 声明式 UI 与主题（Compose BOM 2026.09.00） |
| kotlinx.serialization | 内容包、状态文件、fixture 的 JSON |
| Coroutines + Flow | 异步任务与状态流 |
| OkHttp 5 | WebDAV 协议层（仅同步使用） |
| AndroidX Activity / Lifecycle / Core | 单 Activity 壳、ViewModel、边到边显示 |
| JUnit 5 | 纯 JVM 单元测试 |
| Gradle 9.6.0 + AGP 9.5.0 | 构建系统 |

架构为单 Activity + Compose，依赖装配用**手写 `AppGraph`**（两个壳共享核心，Hilt 收益不足）。
业务规则全部在纯 JVM 的 `:core`，`ui-m3` 只接 UI 模型与回调。

### 项目结构

```text
仓库根目录
├── settings.gradle.kts      包含 :core、:sync、:data、:ui-m3、:app-phone
├── build.gradle.kts        根配置
├── gradle/libs.versions.toml   版本目录（Kotlin / Compose BOM / JUnit 5）
├── gradlew, gradle/wrapper/    Gradle 9.6.0 wrapper
├── core/             纯 JVM 核心，不依赖 Android
│  └── src/main/kotlin/com/pagelence/core/
│    ├── content/        书模型、索引、句子切分、校验、内容仓库
│    ├── progress/       进度、state v2、迁移、日活、测验评分
│    ├── assessment/      测评模型、抽题与评级映射
│    ├── source/        书源目录、书包读取、校验、下载与安装规则
│    ├── reading/        阅读页词条状态机、分页
│    ├── wordcards/       生词卡与存储接口
│    ├── lexicon/        词级、词典与查词顺序
│    └── prefs/         语言、主题、字号、导读与词典偏好、等级默认值
├── sync/             纯 JVM：WebDAV 协议、字段合并、同步状态机、导出导入
├── data/             纯 JVM 持久化：state.json（实现 :sync 的 LocalState）
├── ui-m3/             Compose Material 3 页面、组件、主题与双语文案
├── app-phone/           普通版壳：MainActivity、AppGraph、ViewModel、内容落盘
├── content/            由脚本生成，不手工编辑
│  ├── books-index.json      内置书清单、大小与 SHA-256
│  ├── books/doctor-dolittle/   《怪医杜立德》21 章 103 单元
│  ├── books/alice-1866/     《爱丽丝》12 章 104 单元
│  ├── grammar/          随应用的共享语法库
│  ├── assessment/        测评题库（快速/高级共用）
│  ├── third-party/        词级表、ECDICT 与许可文本
│  └── reports/          缺口、提示重复率与长难句候选报告
├── content-src/          编辑源：包元数据、章题、冻结单元区间、章节学习内容
│  ├── doctor-dolittle/      21 章 overlay、编辑手册与语法点索引
│  └── alice/           12 章 overlay、编辑手册、语法点索引与底本
├── tools/
│  ├── lib/            切分、清单、缺口报告等共用逻辑
│  ├── migrate/          第三方表导入、进度规则对照录制
│  ├── author/          章节工作表、overlay 校验、提示句序与质量嗅探
│  ├── validate/         内容校验（本地与 CI 共用）
│  ├── build_dolittle_book.py   从 Gutenberg 文本重建《怪医杜立德》
│  ├── build_alice_book.py    从 Wikisource 文本重建《爱丽丝》
│  ├── build_assessment_bank.py  合并分级题库为 content/assessment/bank.json
├── docs/             schema、阅读壳、书源与书籍包说明
├── LICENSE            Apache-2.0 全文（代码与构建脚本）
├── LICENSE-CONTENT        CC BY-SA 4.0 全文（自制阅读内容与界面文案）
├── NOTICE             项目署名、许可范围与商标说明
├── THIRD_PARTY_NOTICES.md     第三方来源、版本、许可、署名与许可原文位置
├── CONTRIBUTING.md        贡献流程、DCO、内容与截图规范
├── local.properties.example    SDK 路径模板（复制为 local.properties）
└── README.md           本文件
```

### 模块边界

```text
app-phone (Android application, applicationId com.pagelence)
 MainActivity、AppGraph、PagelenceViewModel、ContentSeed
  ├── ui-m3 (Android library, Compose Material 3)
  │   页面、组件、主题、UI 模型、双语文案；只接数据与回调
  ├── data (纯 JVM)
  │   state.json 读写；实现 :core 的进度、生词卡、偏好、等级与日活仓库
  │   同时实现 :sync 的 LocalState（快照 + 修订号 + 条件写回）
  ├── sync (纯 JVM)
  │   WebDAV 协议、字段合并、同步状态机、本地导出/导入；不依赖 Android
  └── core (纯 JVM)
     内容解析、切分、校验、进度规则、查词顺序、卡片、偏好、等级默认值、日活、评分
```

- `ui-m3` 不依赖 `data`，不写规则，只收 `…UiState` 与回调。
- `PagelenceViewModel` 把 `:core` 结果映射成 UI 模型，把界面事件翻译成 `:core` 调用。
- `ContentSeed` 把 APK 内 `assets/`（即 `content/`）复制到 `filesDir/content`；`:core` 只认目录，不依赖 `AssetManager`。
- `:core` 不依赖 Android UI，可直接跑 JVM 单测。

### 构建环境

需要 JDK 17 或更高版本，以及 Android SDK Platform 37 与 Build-Tools 36.0.0（AGP 9.5.0 默认版本）。
Gradle 由仓库中的 Wrapper 提供，首次构建自动下载，无需安装全局 Gradle。

目前通过构建与测试的版本组合：

| 组件 | 版本 |
| --- | --- |
| Gradle | 9.6.0 |
| Android Gradle Plugin | 9.5.0 |
| Kotlin | 2.4.21 |
| Compose BOM | 2026.09.00 |
| compileSdk / targetSdk | 37 / 35 |
| minSdk | 26 |
| Build-Tools | 36.0.0 |

升级依赖前先验证构建与测试结果。受限环境里 Kotlin 编译进程内执行（`kotlin.compiler.execution.strategy=in-process`）。

### 常用命令

在仓库根目录下：

```sh
./gradlew :core:test         # 纯 JVM 单测（解析、切分、进度、查词、卡片、偏好）
./gradlew :data:test         # state.json 持久化与 v1→v2 迁移单测
./gradlew :sync:test         # WebDAV 协议、合并、同步状态机与端到端桩测试
./gradlew :app-phone:assembleDebug  # 产出 debug APK
```

在项目根目录下（内容由脚本生成，`content/` 不手工编辑）：

```sh
python3 tools/validate/validate_content.py      # 内容校验，零错误
ECDICT_CSV=/tmp/ecdict.csv tools/ci_check.sh      # 内容门禁 + 单测一键跑
python3 tools/migrate/import_reference_data.py    # 生成语法库、复制 ECDICT 许可（不覆盖 ECDICT）
python3 tools/build_word_levels.py           # 五层词级表 + 覆盖率报告 + 210 条抽查（需 wordfreq 3.0.2）
python3 tools/build_word_frequencies.py        # 词频表与四档切点（需 wordfreq 3.0.2）
python3 tools/build_dictionaries.py --ecdict-csv <ecdict.csv> # 重建通用核心词典
python3 tools/build_dolittle_book.py         # 重建《怪医杜立德》
python3 tools/author/dolittle_worksheet.py 7      # 打印第 7 章编辑工作表
python3 tools/author/lint_dolittle_overlay.py 7    # 校验第 7 章 overlay
python3 tools/author/audit_dolittle.py         # 全书内容质量嗅探
python3 tools/build_alice_book.py           # 重建《爱丽丝》
python3 tools/build_assessment_bank.py        # 重建测评题库
python3 tools/migrate/record_progress_parity.py    # 重录进度规则对照 fixture
```

生成脚本支持 `--check`，产物过期时退出码非零，可直接用于 CI；校验脚本另有 `--quiet`。

### 测试

```sh
./gradlew :core:test :data:test
```

当前共 **207 项单元测试**（`:core` 195 项、`:data` 12 项），全部通过。

其中包含句子切分的 Kotlin/Python 逐段对照，以及按 Web 阅读器规则录制的进度 fixture
（`core/src/test/resources/parity/`）；两套切分实现与对照样本都不允许静默漂移。

### 已冻结的接口

- **内容 schema：** [`docs/content-format.md`](docs/content-format.md)；可执行版本是 `content/` 下的 Kotlin 模型。
- **内容规范：** 内容格式 v3 见 [`docs/content-format.md`](docs/content-format.md)；书源与书籍包格式见 [`docs/book-sources.md`](docs/book-sources.md)。
- **进度 schema v2：** [`docs/progress-format.md`](docs/progress-format.md)；`state.json` 同时用于本地、导出与 WebDAV。
- **同步实现：** [`docs/webdav-sync.md`](docs/webdav-sync.md)；同步状态、基线与远端备份是 `filesDir` 下的旁路文件，不改变 `state.json` 格式。
- **句子切分规则：** Kotlin 与 Python 各一份实现，单测逐段比对。
- **已读单元数与续读位置：** 由 fixture 在单测中对照。

### 版本与发布

- `:app-phone` 自己维护 `versionCode` / `versionName`，当前 `1` / `0.1.0`；墨水屏壳加入后各自独立上架。
- 共享 `:core` 时用单 tag（如 `v1.0.0`）同时产出两个包；发布节奏不同时改用 `phone-v1.2.0`、`eink-v1.0.0` 双 tag。
- 提交信息带 scope：`feat(core)`、`feat(phone)`、`feat(eink)`、`fix(phone)`、`chore(content)`。

### 文档

- [内容格式 v3](docs/content-format.md) · [进度格式 v2](docs/progress-format.md)
- [普通版阅读壳](docs/app-phone.md) · [《怪医杜立德》书籍包说明](docs/books/doctor-dolittle.md)
- [发布签名](docs/release-signing.md) · [官网与截图托管](https://github.com/Selwiro/pagelence/tree/gh-pages)

## 反馈与贡献

当前为 **0.1.0 开发版**：普通版已可安装运行，墨水屏版尚未开工；签名包通过 [GitHub Releases](https://github.com/Selwiro/pagelence/releases) 分发。问题与建议走 GitHub Issues，附版本号（「设置 → 关于」）、设备与复现步骤；安全问题请走私下的安全报告渠道，不要公开提 Issue。贡献流程、内容规范与截图托管方式见 [CONTRIBUTING.md](CONTRIBUTING.md)。

### 提交代码

提交 Pull Request 前，请至少运行：

```sh
./gradlew :core:test :data:test
./gradlew :app-phone:assembleDebug
python3 tools/validate/validate_content.py
```

项目的开发约定（目录边界、验证与版本管理）见 [CONTRIBUTING.md](CONTRIBUTING.md)。

提交信息采用以下格式：

```text
type(范围): 摘要
```

外部贡献使用 **DCO** 而不是 CLA：提交需带 `git commit -s` 的 `Signed-off-by`。

所有演示与测试数据应使用合成数据。请勿提交真实数据、签名密钥、密码、构建产物，或任何在目标地区未进入公有领域的正文。

## 致谢

这个应用能跑起来，靠的是很多开源项目、公开数据与公版文本。感谢所有维护者和贡献者。

### 核心运行库

| 项目 | 许可证 | 用途 |
| --- | --- | --- |
| [Kotlin](https://kotlinlang.org/) / [kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines) / [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) | Apache-2.0 | 开发语言、协程与 JSON 序列化 |
| [AndroidX](https://developer.android.com/jetpack/androidx)（Core / Activity / Lifecycle） | Apache-2.0 | 应用基础能力、单 Activity 与 ViewModel |
| [Jetpack Compose](https://developer.android.com/jetpack/compose)（UI / Foundation / Material 3） | Apache-2.0 | 声明式界面与 Material 3 主题 |
| [Material Icons](https://fonts.google.com/icons) | Apache-2.0 | 界面图标 |

### 数据与文本

| 来源 | 许可证/性质 | 用途 |
| --- | --- | --- |
| [Project Gutenberg #501](https://www.gutenberg.org/ebooks/501)（1920 年版） | 公有领域 | 《怪医杜立德》正文底本 |
| [Wikisource：Alice (1866)](https://en.wikisource.org/wiki/Alice%27s_Adventures_in_Wonderland_(1866)) | 公有领域 | 《爱丽丝》正文底本（Macmillan 1866 年印本转录） |
| [ECDICT](https://github.com/skywind3000/ECDICT)（固定版本 `39cfb96`） | MIT | 英汉释义与音标；通用核心约 3.8 万条（BNC/COCA 3 万内、考试标签、Oxford 3000、CEFR 与两本书词形）；上游记录的第三方释义需逐条核对 |
| [Princeton WordNet 3.0](https://wordnet.princeton.edu/) | WordNet License | ECDICT 中的英文义项与义项键 |
| [CEFR-J Wordlist v1.6](https://www.cefr-j.org/download)（东京外国语大学投野研究室） | 研究、教育、商用免费，按官方格式署名 | A1–B2 词级 |
| [Octanove Vocabulary Profile C1/C2 1.0](https://github.com/openlanguageprofiles/olp-en-cefrj) | CC BY-SA 4.0 | C1–C2 词级 |
| [wordfreq](https://github.com/rspeer/wordfreq) 3.0.2 | CC BY-SA 4.0 | 词频表与四档切点 |

第三方数据的版本、位置、许可与许可原文位置见 [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md)。

### 开发与构建

| 项目 | 许可证 | 用途 |
| --- | --- | --- |
| [Gradle](https://gradle.org/) / [Android Gradle Plugin](https://developer.android.com/build) | Apache-2.0 | 构建系统 |
| Kotlin Gradle Plugin / Compose Compiler / Serialization Plugin | Apache-2.0 | 编译与代码生成 |
| [OpenJDK](https://openjdk.org/) | GPLv2 + Classpath Exception | 构建与运行 JDK |
| [JUnit 5](https://junit.org/junit5/) | Eclipse Public License 2.0 | 单元测试 |
| [Android SDK / Build Tools](https://developer.android.com/studio) | Android SDK 许可（非开源） | Android 平台与构建工具 |

### 参考与规范

- [Material Design 3](https://m3.material.io/)：普通版的界面与动效参考。
- [Android 开发者文档](https://developer.android.com/docs)：Compose、状态持久化与平台能力的使用参考。
- Web 版（2026-11-18 封存）提供了阅读模型与内容编排的参考，其代码与内容不进入本工程。

## 许可

- **代码：** [Apache License 2.0](LICENSE)，可商用、修改与闭源分发，需保留版权与许可声明；署名与范围说明见 [NOTICE](NOTICE)。
- **自制内容：** 导读、句译、测验、语法讲解、界面文案与书籍编排按 [CC BY-SA 4.0](LICENSE-CONTENT) 授权，改编内容须以同一协议共享。
- **第三方数据：** 词典、词级、词频与正文底本沿用各自许可，不能重许可；逐项来源、版本与许可原文位置见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。
- **商标：** 项目名「Pagelence · 页有所成」与图标不随上述协议授权；衍生版本需改名并更换图标。

外部贡献使用 **DCO** 而不是 CLA，提交流程见 [CONTRIBUTING.md](CONTRIBUTING.md)。

## 相关链接

- [内容格式 v3](docs/content-format.md) · [进度格式 v2](docs/progress-format.md)
- [书源与下载](docs/book-sources.md) · [普通版阅读壳](docs/app-phone.md)
- [发布签名](docs/release-signing.md) · [第三方说明与许可原文位置](THIRD_PARTY_NOTICES.md)
- [参与贡献](CONTRIBUTING.md) · [许可证全文](LICENSE)
