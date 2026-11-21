# 《怪医杜立德》书籍包

A2 档内置书 *The Story of Doctor Dolittle* 的 Android 书籍包，已按
[内容格式 v3](../content-format.md) 补成全书：21 章、103 个短读单元，
每个单元都有标题、双语导读、逐句句译、点词提示、读后测验与语法解析。包由脚本从底本和编辑源
生成，不手工改 `content/` 下的产物。

- 包：`content/books/doctor-dolittle/book.json`（内容格式 3）、`content/books-index.json`
- 编辑源：`content-src/doctor-dolittle/meta.json`、`chapter-01.json` … `chapter-21.json`
- 编辑手册与译名表：[`content-src/doctor-dolittle/AUTHORING.md`](../../content-src/doctor-dolittle/AUTHORING.md)
- 生成脚本：`tools/build_dolittle_book.py`；编辑辅助脚本在 `tools/author/`
- 包元数据：`creator: Pagelence`、`version: 1.0.0`、`level: A2`、`wordCount: 25,498`、
  `contentStatus: complete`，两份许可写在 `meta.licenses`
- schema：见[内容格式](../content-format.md)

## 底本与使用范围

英文正文取自 [Project Gutenberg eBook #501](https://www.gutenberg.org/ebooks/501)，1920 年
Frederick A. Stokes Company（纽约）版。取回记录在本机 `inputs/base-texts/manifest.json`：
文件 `pg501-doctor-dolittle.txt`，SHA-256
`d182620a8319837fde3e15aa662a4fc189d1d207363122f1e0cff893a7901386`，2026-11-17 经
`web.archive.org` 快照取回（当时 gutenberg.org 返回 503），快照地址
`web.archive.org/web/20241211140244id_/https://www.gutenberg.org/cache/epub/501/pg501.txt`。

- **公版状态**：作者 Hugh Lofting 1947 年去世，正文在中国自 1998 年起进入公有领域；1920 年
  出版，在美国自 2016 年起进入公有领域。1929 年前在美国出版的作品已进入公有领域，本书按此范围随包分发。
- **收录范围**：只收故事正文，从 `*** START ***` 到 `*** END ***` 之间，再切掉项目古腾堡的
  书目信息、书名页、献词、目录、插图占位和转写者注。
- **不收导读**：PG #501 正文前有 Hugh Walpole 写的 1,507 词导读，本身也已公版，但它是评论而
  不是故事，暂不进入单元；将来若做前置材料，按独立内容另行登记。
- **不收插图**：原文里的 `[Illustration: …]` 是插图说明；构建时整块丢弃，包括换行后只剩
  `”]` 的说明尾巴，避免正文里出现 `branches to greet him”]` 一类残片。Lofting 自绘插图已公版，
  但配图仍需按具体版本核对扫描件来源和署名。
- **文本处理**：保留原标点、原拼写和原分段；只把段首全大写的小型大写字母还原成正常大小写
  （如 `ONCE` → `Once`），并把四处人名的小型大写（`DOLITTLE`、`ERMINTRUDE`、`CHEE`、
  `PULLYUS`）还原成 `Dolittle`、`Ermintrude`、`Chee`、`Pullyus`；斜体标记 `_..._` 还原为
  纯文本；换行按原自然段合并为一段。
- **许可**：正文属公有领域；古腾堡对本转录文本的排版与校订按其许可使用（[PG 许可](https://www.gutenberg.org/policy/license.html)）。
  项目自制的导读、句译、提示、测验、语法解析与版面按 **CC BY-SA 4.0** 授权。两份许可写在
  `meta.json` 的 `licenses.source` / `licenses.creator`，由打包器写进包 manifest；`book.json`
  不再重复。

## 结构

| 项 | 数值 |
| --- | --- |
| 章 | 21 |
| 短读单元 | 103 |
| 自然段 | 729（丢弃 6 段插图说明残片后） |
| 正文词数 | 25,498（本包统计口径；底本清单记 25,478，差 20 词，来自章题与分隔行的计法） |
| 单元词数 | 163–319，平均 248，目标 250 |
| 句 | 1,724，全部有中文句译 |
| 点词提示 | 563 条，每单元 3–11 条 |
| 读后测验 | 206 道（每单元 2 道） |
| 语法解析 | 103 则（每单元 1 个主讲点） |
| 长难句解析 | 1 条示例；候选报告见 `content/reports/doctor-dolittle-1920.long-sentences.json` |

单元切分在 `meta.json` 的 `unitRanges` 中冻结为每章段落区间（1 起），不再由动态规划重算；
正文改版时必须同步改区间，否则 id 会移动。id 规则仍是 `chapter-01`、`chapter-01-u01`、
`chapter-01-p001`。

## 每单元的学习内容

- `title`：中文 ≤ 16 字、英文 ≤ 8 词，钩子式标题，不剧透。
- `introduction`：双语导读，2–3 句；只给情境与钩子，不说本单元的回答、决定或结局。
- `paragraphs[].sentences[]`：与冻结切分逐句对应，句译忠实、保留引号与语气。
- `paragraphs[].hints[]`：只写词典解决不了的：专名译名、多词单位、本句语境义、文化所指、
  本书教学点。与通用词典重复的只有 `Parson` 一条（重复率报告 `content/reports/doctor-dolittle-1920.hints.json`）。
- `unit.quiz`：每单元 2 题，考大意理解，3 个选项，`sentence` 指回单元句序，`feedback`
  中英各一句。构建时按单元 id 确定性地轮转选项，避免正确答案总在第一个。
- `unit.grammar`：每单元一个主讲点，`excerpt` 逐字取自本单元原文，`pointIds[0]` 命中语法库
  256 条；`focus` 只指认本句线索（≤ 60 汉字）。没有库外点，因此不写 `explain` / `form`。
- `sentences[].analysis`：可选；第一章有一条例句（过去进行时句的拆解），其余候选只进报告，
  由编辑按需补，不参与完整度。

## 编辑流程

编辑只写 `content-src/doctor-dolittle/chapter-NN.json`，不改产物：

```sh
# 读一章的单元/段落/冻结切句工作表（U# 为单元内 1 起）
python3 tools/author/dolittle_worksheet.py 7

# 校验一章 overlay：句数、句译、提示、quiz、语法
python3 tools/author/lint_dolittle_overlay.py 7 --warnings

# 把 hint 的 sentence 指到 target 所在句
python3 tools/author/fix_hint_sentences.py 7

# 全书质量嗅探：重复句译、quiz 答案分布、提示与词典重复
python3 tools/author/audit_dolittle.py
```

overlay 的 `paragraphs` 用段落 id 作键（`chapter-07-p001`），`sentences` 与工作表逐句一一
对应。语法点从[`grammar-index.tsv`](../../content-src/doctor-dolittle/grammar-index.tsv) 选。
全书译名表见编辑手册第十节。

## 生成与校验

```sh
python3 tools/build_dolittle_book.py          # 生成 book.json、索引与三份报告
python3 tools/build_dolittle_book.py --check  # 产物是否与源一致，不一致返回 1
python3 tools/validate/validate_content.py    # 全书内容门禁
```

脚本行为：

- 从底本解析 21 章、段落和句子；句切分避开 `Mr.` `Mrs.` `Dr.` `M.D.` 一类缩写。
- 丢弃整块插图说明；按 `meta.unitRanges` 冻结单元边界，不再重新分段。
- 合并 `content-src` 的元数据与章节 overlay；按段落 id 挂句译、提示、长难句解析。
- 构建前校验：章/单元/段落/提示 id 全书唯一；句译数与句数一致；测验答案在选项内、
  `sentence` 在范围内；`grammar.excerpt` 在单元原文中；提示 `target` 在段落原文中。
- 构建时轮转测验选项；计算 `wordCount`、`contentStatus`；产出缺口报告
  `doctor-dolittle-1920.gap.json`、提示重复率报告 `doctor-dolittle-1920.hints.json`、
  长难句候选报告 `doctor-dolittle-1920.long-sentences.json`。
- 输出不含时间戳，重跑字节一致；`--check` 用于构建前确认产物没有过期。

## 现状与遗留

- **已完成**：21 章 103 单元的标题、导读、1,724 句句译、563 条提示、206 道测验、103 则语法；
  缺口报告为 `complete`，`validate_content.py` 零错误。
- **长难句解析**：规范里是可选字段，不参与完整度。候选 286 句（≥ 25 词，或 ≥ 18 词且 ≥ 2 个
  从句标记），已写 1 条示例；后续可按报告逐章补，不影响本书完整性。
- **语法点分布**：103 个单元用到 55 个 CEFR-J 条目，`if` 条件、`as soon as`、`so…that`、
  `although` 等复现较多，属复习式复用；库外方言与古旧用法不单独开点。
- **包 manifest**：本书是内置书，随 APK 分发，不走 `.pagelence` 打包；包格式 2 的写入与读取
  仍待书籍包侧迁移（遗留）。`meta.licenses` 已按包规范备好，迁移时直接生成 manifest。
- **与 Web 版无关**：本包是新做的 Android 内容，不复用、也不回写 `work/web/`。
