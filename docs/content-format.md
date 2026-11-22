# 内容格式 v3（已迁移）

状态：**v1 冻结于 2026-11-19**，**v2 于 2026-11-20**，**v3 于 2026-11-20**
落地。`book.json` 的 `schemaVersion` 为 **3**；内置书索引 `content/books-index.json` 与
编辑源 `content-src/<book>/meta.json` 仍是 **2**，三套版本号相互独立。
`core` 的 `com.pagelence.core.content` 是这份 schema 的可执行版本；Python 构建与校验脚本
（`tools/`）按同一份规则检查内容。

v3 与 App 内容规范、书籍包规范对齐，删除了与词典或包清单重复的字段：`units[].recap`、
`units[].review`、`paragraphs[].guide`、`sentences[].glosses`、`grammar.pointId`
/`function`/`prompt`、`hints.italic`、`book.json.license`、`source.publicDomain`；
新增 `grammar.pointIds`、`hints.sentence`、`sentences[].analysis`，并把 `quiz.feedback`
改为 `{zh, en?}`。

> 本文是内容格式的可执行说明：`core` 的模型与 `tools/` 的校验脚本都以这里为准。书籍包布局、catalog
> 字段与下载规则见 [`docs/book-sources.md`](book-sources.md)，进度文件见
> [`docs/progress-format.md`](progress-format.md)。

## 一、包布局

```
content/
  books-index.json                  内置书索引（schemaVersion 2）：id、路径、包元数据、统计
  store.json                        书城内置书架（schemaVersion 2）：order/status/策展字段
  taxonomy.json                     分类词表（schemaVersion 1）：四个分类 id 与旧名别名
  books/<dir>/book.json             一本书一个文件（schemaVersion 3），导入导出也是这一个文件
  books/<dir>/outline.json          书架用的轻量索引：标题/作者/单元/段落归属，由 book.json 生成
  grammar/grammar-library.json      语法库 schema 2，随应用；256 条，与教員版 1:1
  third-party/                      通用核心词典/词级/词频的 JSON 构建源、lexicon.bin 与许可文本
content-src/<book>/meta.json        编辑源：包元数据、章题、切分参数
content-src/<book>/units/*.json     编辑源：已编写的学习单元
content-src/shelf.json              编辑源：书架顺序、状态、封面子字与钩子
reports/<bookId>.gap.json           编辑产物：内容缺口、提示重复率、长难句候选（不进 APK）
```

`reports/` 放在仓库根、不在 `content/` 下，因此不会被打进 APK；它只给编辑和
`--check` 用。`outline.json` 随包分发，但只含书架需要的字段，App 启动时不解析
`book.json` 正文；`validate_content.py` 校验两者一致。

`content/` 由脚本生成，不手工编辑：

| 命令 | 作用 |
| --- | --- |
| `python3 tools/migrate/import_reference_data.py` | 由 `content-src/grammar/points.json` 生成语法库、复制 ECDICT 许可文本；不覆盖 ECDICT 与词级表 |
| `python3 tools/build_word_levels.py` | 五层生成词级表（wordfreq 3.0.2）、覆盖率报告与 210 条抽查样本 |
| `python3 tools/build_word_frequencies.py` | 由 wordfreq 3.0.2 生成随包词频表与四档切点 |
| `python3 tools/build_assessment_bank.py` | 合并分级测评题库，`--check` 校验是否最新 |
| `python3 tools/build_dictionaries.py --ecdict-csv <ecdict.csv>` | 从完整 ECDICT 重建通用核心词典（另需先下载固定版本 CSV） |
| `python3 tools/build_lexicon.py` | 把核心词典、词级表、词频表合成随包的 `third-party/lexicon.bin`（`--check` 校验是否最新） |
| `python3 tools/build_dolittle_book.py` | 从 Gutenberg 文本重建《怪医杜立德》 |
| `python3 tools/build_alice_book.py` | 从 Wikisource 文本重建《爱丽丝》 |
| `python3 tools/build_shelf.py` | 合并 `shelf.json` 与 `books-index.json`，生成 `store.json` |
| `python3 tools/author/dolittle_worksheet.py <章>` | 打印一章的单元/段落/冻结切句工作表，供编辑写学习内容 |
| `python3 tools/author/lint_dolittle_overlay.py <章>` | 校验一章编辑 overlay（句数、quiz、grammar、hint） |
| `python3 tools/author/fix_hint_sentences.py <章>` | 把 hint 的 `sentence` 指到 target 所在的单元句 |
| `python3 tools/author/audit_dolittle.py` | 全书内容质量嗅探（重复句译、quiz 答案分布、提示与词典重复） |
| `python3 tools/validate/validate_content.py` | 校验全部内容，任何错误退出码非零 |

构建脚本支持 `--check`：产物与脚本结果不一致时退出码非零，可用于 CI；
`validate_content.py` 直接运行，发现错误时退出码非零。

### 词典：通用核心 + 书内词形

`third-party/dictionaries/ecdict.json` 是**通用核心词典**，不是按某本书裁剪的词表：

- **核心词**：ECDICT 固定版本 `39cfb96` 中 BNC/COCA 词频排名 ≤ 30000 的词，另加考试标签词、Oxford 3000、CEFR-J/Octanove 词级词；
- **书内补充**：两本内置书正文、句译与提示里出现的全部词形（含专名、连字符造词）及其原形；
- **词形关系**：原形、`form` 标记与屈折形照旧从 ECDICT `exchange` 生成，`daisies` 仍按 `daisy` 取义。

规模 38,229 条、7.9 MiB，`schemaVersion` 4；每条另存 ECDICT `tag`（考试标签，供词级第 4 层使用），
`extra.json` 只补专名识别与 note，构建时并入；`--check` 对比生成字节与磁盘文件。完整 ECDICT CSV 约 63 MB，
不在仓库内，构建前从固定版本下载：

```sh
curl -L -o /tmp/ecdict.csv \
  https://raw.githubusercontent.com/skywind3000/ECDICT/39cfb9672c0906b63569e0453c2e6abfb9ab182a/ecdict.csv
python3 tools/build_dictionaries.py --ecdict-csv /tmp/ecdict.csv
python3 tools/build_dictionaries.py --ecdict-csv /tmp/ecdict.csv --check
```

边界：词典负责「词可能是什么意思」；书内的 `hints` 只写语境义、短语习语、专名、文化所指
与本书教学点，不重复词典的通用义项。正文词形覆盖由 `DictionaryCoverageTest` 检查：内置书
全文按词次 ≥ 97%，缺口应通过书内提示或句译补齐。

### 词级与词频

`third-party/word-levels.json`（`schemaVersion` 2）保存 `levels` 与逐格来源 `sources`：

```jsonc
{
  "levels":  { "abandon": { "verb": "B1" } },
  "sources": { "abandon": { "verb": "list" } }
}
```

来源按优先级 `list` > `aligned` > `derived` > `tag` > `freq`，每层只填空位、不覆盖：

| 层 | 来源 | 做法 |
| --- | --- | --- |
| `list` | CEFR-J 1.6 + Octanove C1/C2 | 词表按词性取级；补充 `don't`、`I'm` 等 41 个缩合词，取所缩写的词 |
| `aligned` | CEFR-Annotated WordNet | 词目取最低义项级，义项类型映射词性；专名（首字母大写）跳过 |
| `derived` | 项目推算 | 后缀还原词根、继承词根级，不做级别加减；只一跳、需能反拼 |
| `tag` | ECDICT 考试标签 | gk/cet4→B1、cet6/ky→B2、toefl/ielts→C1、gre→C2，多标签取最低 |
| `freq` | wordfreq 3.0.2 | 按词性用纯 `list` 词拟合 zipf 阈值（DP、单调），`zipf = 0` 不落档 |

专名、缩写、无中文释义的词不标级；五层后仍无级的词不显示级别。
`reports/word-levels-coverage.json` 记录各层条数、覆盖率、级别分布与未覆盖原因；
`reports/levels-audit.json` 是按预测级分层、每级 35 条、种子 42 的抽查样本与判定。

`third-party/word-frequencies.json` 是全局 `lemma → zipf` 表（一位小数），`bands` 存
`high`/`common`/`normal` 三个分位切点（90/70/40 分位），面板按它显示「高频 / 较常见 / 一般 / 少见」。

书的出现次数与逐段词频不再随包：`WordQuery.countWords` 在首次查词时对全书单遍扫描，
结果缓存内存；扫描属性派生，不写回书或 `state.json`。

### 随包的二进制词表 `lexicon.bin`

APK 只打包 `third-party/lexicon.bin` 一个文件，三份 JSON 只留在仓库里给构建脚本、
校验脚本与单测使用（Gradle 的 `ignoreAssetsPattern` 把它们排除出 assets）。文件格式
是「排序后的键 + 偏移 + 原 JSON 条目」：App 首次点词时整体读入（约 11 MB），按键
二分查找，只把点中的那一条解释成对象，因此不再为 3.8 万条词典、3.3 万条词级和
3.8 万条词频建内存对象图（T156，模拟器实测首次点词 +12 MB 而不是 +81 MB）。

`build_lexicon.py --check` 已进 `tools/ci_check.sh`；`LexiconIndexTest` 还会抽样比对
JSON 与二进制两条路径的查词结果，防止索引与源表静默漂移。改动任何一份 JSON 后
必须重新生成 `lexicon.bin`，否则 CI 失败。

### 编辑源元数据 `meta.json`

`content-src/<book>/meta.json` 是书籍包的编辑源，不随 APK 分发；构建脚本读它生成
`content/books/<book>/book.json` 与 `content/books-index.json` 的包元数据。

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `schemaVersion` | 是 | 编辑源格式，当前为 `2`；与 `book.json` 的 3 不同 |
| `id` | 是 | 与 `book.json.id` 一致 |
| `creator` | 是 | 书籍包制作者名称 |
| `version` | 是 | 书籍包版本，`MAJOR.MINOR.PATCH` |
| `language` | 是 | 正文语言；首版只支持 `en` |
| `level` | 是 | CEFR 难度 `A1`–`C2`，随索引供书架与筛选读取 |
| `levelNote` | 否 | 难度依据，如 Lexile、ATOS、出版社标注 |
| `contentStatus` | 是 | `skeleton`、`sample` 或 `complete` |
| `wordCount` | 是 | 正文词数，口径与索引 `words` 相同 |
| `updatedAt` | 是 | 内容最后更新日期，`YYYY-MM-DD` |
| `minAppVersionCode` | 是 | 最低 App `versionCode` |
| `licenses` | 是 | 两份许可：`source` 原书、`creator` 制作者；字段见下 |

`licenses.source` 写原书权利状态：`public-domain` 必须带非空 `publicDomain`（ISO 3166-1
alpha-2，如 `["CN","US"]`）；有授权的写 `permission` 并在 `note` 说明授权方；原创书写
`original`。`licenses.creator` 写句译、导读、测验、提示、语法解析与版面的授权：内置书用
`CC-BY-SA-4.0` 并给 `url` 与 `scope`；`custom` / `all-rights-reserved` 必须带 `name`，
且至少有 `url`、`text`、`file` 之一。`book.json` 不重复许可，包 manifest 由这两个字段生成。

口径：`wordCount` 只统计 `paragraphs[].text`，按 `[A-Za-z]+(?:['’][A-Za-z]+)?` 取词；
`contentStatus` 由 gap report 复核；`level` 与书城目录同名字段一致。`book.json` 不重复
`creator`、`version`、`level`、`contentStatus` 与两份许可。

## 二、`book.json`

```jsonc
{
  "schemaVersion": 3,
  "id": "doctor-dolittle-1920",
  "title": { "zh": "怪医杜立德", "en": "The Story of Doctor Dolittle" },
  "author": "Hugh Lofting",
  "edition": "1920",
  "description": { "zh": "…", "en": "…" },
  "source": {
    "publisher": "Frederick A. Stokes Company, New York",
    "url": "https://www.gutenberg.org/ebooks/501",
    "note": "…"
  },
  "chapters": [ /* 见下 */ ]
}
```

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `schemaVersion` | 是 | 当前只能是 `3` |
| `id` | 是 | 书内稳定标识，进度、生词卡、同步都用它 |
| `title` | 是 | `{zh, en}`，至少一侧有文字 |
| `author` | 是 | 作者署名 |
| `edition` | 否 | 版本/年份，字符串 |
| `description` | 是 | 书库简介；缺失即格式错误，不再降级 |
| `source` | 是 | 底本信息：`publisher`、`url`、`note` |
| `chapters` | 是 | 至少一章 |

v3 不再有 `license` 与 `source.publicDomain`：原书许可、分发地区与制作者许可统一读包
包 `manifest.json` 的 `licenses`，`book.json` 只放阅读内容。

### 章节

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `id` | 是 | 全书写唯一 |
| `number` | 是 | 从 1 起，等于在 `chapters` 中的位置 |
| `title` | 是 | `{zh, en}` |
| `unnumbered` | 否 | `true` 表示编辑分段而非原书章节，界面不显示章号；默认 `false` |
| `units` | 是 | 至少一个单元 |

### 单元

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `id` | 是 | 全书写唯一 |
| `number` | 是 | **章内序号**，从 1 起；跨章连续计数由客户端按 `chapters` 顺序自行计算 |
| `title` | 否 | `{zh, en}`。骨架书可为空，校验给 warning |
| `introduction` | 否 | 双语导读；缺省时跳过导读页 |
| `paragraphs` | 是 | 至少一段 |
| `quiz` | 否 | 读后测验；缺失计入内容缺口 |
| `grammar` | 否 | 本段语法；缺失计入内容缺口 |

v3 删除了 `recap`、`review`：续读位置由进度生成，单元回顾不再进内容，也不再显示。

### 段落、句子、提示

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `id` | 是 | 全书写唯一；进度位置用它 |
| `text` | 是 | 英文原文，纯文本，禁止 HTML |
| `sentences` | 否 | 见下。骨架书可以整段缺失，校验给 warning |
| `hints` | 否 | 点词提示 |

v3 删除了 `paragraphs[].guide`：段落带读不再进内容。

`sentences[]`：

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `text` | 是 | 必须是 `text` 按冻结规则切分的结果 |
| `zh` | 否 | 句译；缺失是内容缺口，不是格式错误 |
| `analysis` | 否 | 长难句解析，见下；不是每句都写 |

#### 长难句解析 `analysis`

只给「超过本书等级」或「结构真的绕」的句子。`level` 是编辑标注，不参与显示判断。

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `level` | 是 | `A1`–`C2` |
| `core` | 是 | 去掉从句与修饰后的英文主句，保留原词序 |
| `parts` | 是 | 1–4 条 `{text, role, zh, en?}`；`text` 逐字取自本句 |
| `order` | 否 | `{zh, en?}` 中文语序说明，1–2 句 |
| `note` | 否 | `{zh, en?}` 一句提醒 |

`parts[].role` 取 `subordinate`、`relative`、`nonfinite`、`prepositional`、`appositive`、
`parenthetical`。英文界面没有 `en` 时只显示 `core` + role 标签 + `parts[].text`，不回退中文。

`hints[]`（单词、短语、整句都在这里；v3 没有单独的 `glosses`）：

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `id` | 是 | 全书写唯一，约定 `{paragraphId}-{slug}` |
| `kind` | 是 | `word`、`phrase` 或 `sentence` |
| `target` | 是 | 必须出现在段落原文中 |
| `sentence` | 否 | **单元内 1 起的句序**，与 `quiz.sentence` 同口径；不写表示整段适用 |
| `zh` / `en` | 是 | 双语解释；单词时 `zh` 就是本句语境义 |
| `lemma` / `pos` | 否 | 只在词典还原不了或词性不同时写（override） |
| `ipa` | 否 | 只在词典没有或不对时写（override） |
| `context` | 否 | `{zh, en}`，文化、器物、制度背景 |
| `example` | 否 | `{en, zh}`，只在这条例句示范本句语境义时写 |

同一段同一 `target` 只允许一条；同一词在不同句意思不同的，用 `sentence` 分开写两条。
`italic` 与 `gloss` 在 v3 删除；斜体需要时另立字段并升版本，禁止用 HTML。

### 测验

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `question` | 是 | 题干，英文 |
| `options` | 是 | 至少 2 个，首版内容为 3–4 个 |
| `answer` | 是 | 正确选项下标，0 起 |
| `sentence` | 否 | **单元内 1 起的句序**，用于「看原句」；不是 0 起 |
| `feedback` | 是 | `{zh, en?}`；`zh` 1–2 句说明依据，`en` 可选 |

英文界面在 `feedback.en` 缺失时使用通用文案，不回退中文。

### 语法（库 + 本句）

```jsonc
{
  "excerpt": "whenever he walked down the street in his high hat everyone would say",
  "topic": "past-habit-would",
  "type": "new",
  "pointIds": ["cefrj-142"],
  "focus": { "zh": "本句线索", "en": "This sentence" }
}
```

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `excerpt` | 是 | 必须出现在单元原文中 |
| `topic` | 否 | 编辑标签，可与 `pointIds` 不同，不进界面 |
| `type` | 否 | `new` 或 `review`，编辑标签，不进界面 |
| `pointIds` | 否 | 语法库 id 数组；**第一个是主讲点**，其余只作「这一句还用到」互链 |
| `form` / `explain` | 否 | 仅当 `pointIds` 为空或首个无法解析时书内自写；有库时由库提供 |
| `focus` | 是 | 1 句（必要时 2 句）、≤ 60 汉字，只指认本句线索 |

**语法解析判定（缺口报告与校验共用）**：书内写全 `form`、`explain`、`focus`；
或者 `pointIds` 首个命中语法库的 `form` + `function` + `pitfalls`，且书中给出 `focus`。
不满足即计入「缺语法解析」。库外点必须写 `explain` + `form` + `focus`，三条缺一不可。

v3 删除了 `pointId`（改数组）、`function`（改 `explain`）、`prompt` 与 `note`；
旧包带这些字段会被校验拒绝。

### 书架 `store.json`

`content-src/shelf.json` 是唯一手写的书架源，`tools/build_shelf.py` 合并
`content/books-index.json` 生成 `content/store.json`（schemaVersion 2，`--check` 可校验）。

| 字段 | available | planned | retired |
| --- | --- | --- | --- |
| `id` / `order` / `status` | 必填 | 必填 | 必填 |
| `monogram` | 可选 | 可选 | 可选 |
| `summary` | 可选；缺省回退 `book.json.description` | 必填 | 可选 |
| `title` / `author` / `level` | **不得填**，读包 manifest | 必填 | **不得填** |
| `levelSource` | 不得填 | 固定 `estimated` | 不得填 |

书架顺序：available 按 `order` 升序，planned 接在后面；retired 完全隐藏，只保留条目与旧进度。
`summary` ≤ 40 汉字，不写章数、字数、等级这类会过期的派生事实。

`category` 是 `{id, zh, en}`，`id` 固定为 `literature` / `knowledge` / `guides` / `other`；
planned 可写，available 从 `books-index.json` 读。旧包没有 `id` 时按显示名映射。

### 语法库 `grammar-library.json`

随 App 分发，跨书共用，`schemaVersion` 为 2。条目与 CEFR-J Grammar Profile 教員版一一对应，
不合并；`id` 为 `cefrj-<教員版 ID>`。字段：`name`、`levelStart`、`levelEnd?`、`freqLevel?`、
`relatedIds`、`explain`、`form`、`pitfalls`、`examples`。`explain` 承载「什么时候用 + 表示什么」，
≤120 汉字；`form` ≤30 汉字且只写结构；每条 `pitfall` ≤30 汉字、1–3 条；`examples` 1–2 条，
只在详情页显示。全库 256 条，与教員版 1:1（247 基础项 + 9 个单列变体），
`content-src/grammar/points.json` 是唯一编辑源，校验器要求恰为 256 条、ID 集合与教員版一致。

## 三、标识与稳定

- 标识只要求稳定、唯一，不要求与 Web 一致；一旦发布不要改。
- 生词卡 id 固定为 `bookId::paragraphId::hintId`，换机、同步后仍能对应。
- 进度只记 `paragraphId` 与 `readUnitIds`，不记章节号、不记像素位置。
- 提示 id 写作 `{paragraphId}-{slug}`，便于从卡片定位回原文。

## 四、句子切分规则（冻结）

规则实现在两处，必须一致：

- Kotlin：`core/.../content/SentenceSplitter.kt`
- Python：`tools/lib/content.py` 的 `split_sentences`

单测会把全部段落重新切分，并与 `book.json` 中已存的句子逐条比较；不一致即失败。

规则：

1. 句末是 `.`、`!`、`?`（可跟一个右引号），后面是空白加**大写字母或数字**，或到达文本末尾。
2. 单字母缩写（`J.`）不算句末。
3. 最后一段剩余文字作为末句。

已知且刻意保留的限制：`Mr. Smith` 这类「点号缩写 + 大写」会被切成两句。改规则会使现有
句译对不上，必须同时改两端实现、迁移脚本与全部句译，并升版本。

支持的排版假设：段落为单行、句间无换行、没有样式标记；`—`、`“”`、`‘’` 按普通字符处理。

## 五、测评题库

水平测评与书内测验是两套内容：书内测验跟单元走，测评题库跨书共用。
题库格式 2：210 题、15 篇原创短文，每级 42（非阅读 30 + 阅读 12）。每题带考点 `point`
（`cefrj-*` 或 `lex-*` / `read-*`），同级别不重复；`vocab` 不考屈折变化；选项必须能在
词典或短语表查到。题干统一 `{{blank}}`，构建期按题 id 确定性乱序选项，并在 `state.json`
记录最近两次试卷的题目 id（抽题先剔除，展示即入窗口）。

```
content-src/assessment/*.json          编辑源，按等级一档一个文件
content/assessment/bank.json           构建产物，随 APK 分发
tools/build_assessment_bank.py         合并与校验，--check 校验是否最新
```

## 六、校验规则

`validate_content.py` 对每本书检查（错误即失败）：

- `schemaVersion == 3`、必填字段、`{zh,en}` 结构；`description` 必填；
- 旧字段残留即报错：`recap`、`review`、`guide`、`glosses`、`pointId`、`function`、
  `prompt`、`note`、`italic`、`gloss`、`book.license`、`source.publicDomain`；
- 章号从 1 连续、单元号为章内序号；
- 章、单元、段落、提示 id 在书内唯一；
- 段落实文非空、无 HTML；
- 已存句子与切分规则一致；句子非空；`zh` 存在时不得为空，也不得含 HTML；
- `analysis` 的 `level` 合法、`core` 非空、`parts` 1–4 条、`role` 可识别、`text` 在句中、`zh` 必填；
- 提示 `kind` 合法、`target` 出现在段落原文、双语解释齐备；`sentence` 在 1..单元句数且
  该句含 `target`；同一 `target` + `sentence` 只一条；
- 测验选项不少于 2 个、`answer` 在范围内、`sentence` 在 1..单元句数、`feedback.zh` 非空；
- 语法 `excerpt` 出现在单元原文、`focus` 存在、`pointIds` 无重复；未知 id 只警告；
  库外点必须 `explain` + `form` + `focus`；
- 编辑源 `meta.json` 与索引一致：`wordCount`、`level`、`contentStatus`（由缺口报告复核）；
  `licenses.source` 与 `licenses.creator` 齐全，公版必须写非空 `publicDomain`，制作者许可
  至少有 `url` / `text` / `file` 之一；
- 第三方数据存在且许可文本非空；
- `content/books-index.json`（schemaVersion 2）与 `content/books/` 派生结果完全一致
  （大小、sha256、统计、level、contentStatus）；
- `store.json`（schemaVersion 2）与 `content-src/shelf.json` 生成结果一致：order 唯一、
  status 合法；available 不带 title/author/level 且能在 books-index 找到；planned 不带包且
  title/author/level/levelSource 齐全；retired 可带包但不进书架；每本随包书都在书架上；
- `content/assessment/bank.json`：210 题、15 篇、每级 42（非阅读 30 + 阅读 12）、每篇 4 题；
  选项 4 个不重复且在词典/短语表可查、`vocab` 无同 lemma 屈折选项、同级别 `point` 不重复、
  答案位置各占 10–40%；
- `reports/<bookId>.gap.json` 与按本书重新计算的结果一致；
- `reports/<bookId>.hints.json` 是构建产出的提示重复率报告（`zh`/`en`/`ipa`/`pos` 分项），
  只报告不阻断，阈值待两本书重建后再定；
- `reports/<bookId>.long-sentences.json` 按句长与从句标记列出 `analysis` 候选句及
  `suggestedLevel`，只作编辑初值，`analysis` 不参与完整度。

允许的 warning（不计为错误）：骨架段落没有学习标注、单元标题缺失、语法解析不完整、
语法库条目缺 pitfalls/examples、未知 `pointIds`。缺口报告是这些 warning 的逐项明细。

## 七、版本演进

- 字段只增不改语义；新增可选字段不升 `schemaVersion`。
- v2（2026-11-20）：删除 `grammar.note`，内容并入 `focus`；缺口口径改为
  「语法解析 / `grammarAnalysis`」。
- v3（2026-11-20）：删除 `recap`、`review`、`guide`、`glosses`、`pointId`、`function`、
  `prompt`、`italic`、`license`、`source.publicDomain`；新增 `pointIds`、`hints.sentence`、
  `sentences[].analysis`；`quiz.feedback` 改 `{zh, en?}`；`description` 升为必填。
- 删除字段、改语义、改 id 规则、改切分规则必须升版本，并同时更新：
  Kotlin 模型、Python 迁移/校验、`book.json` 版本号、gap report 规则说明。
- 导入的自制书必须与内置书同一 `schemaVersion`；校验失败不落库。
