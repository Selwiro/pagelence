# 《爱丽丝漫游仙境记》书籍包

B1 档第二本内置书 *Alice's Adventures in Wonderland* 的 Android 书籍包：全书 12 章、104 个
短读单元，全部达到书籍包规范的「完整单元」。包由脚本从底本与编辑源生成，不手工改
`content/` 下的产物。

- 包：`content/books/alice-1866/book.json`、`content/books-index.json`
- 编辑源：`content-src/alice/meta.json`、`content-src/alice/source/chapters.json`、
  `content-src/alice/chapter-01..12.json`
- 生成脚本：`tools/build_alice_book.py`
- 编辑手册：[`content-src/alice/AUTHORING.md`](../../content-src/alice/AUTHORING.md)；
  语法点索引：同目录 `grammar-index.tsv`
- 包元数据：`creator: Pagelence`、`version: 1.0.0`、`level: B1`、`wordCount: 26,568`、
  `contentStatus: complete`；字段口径见[内容格式](../content-format.md#编辑源元数据-metajson)
- schema：内容格式 3（`book.json`），见 [内容格式](../content-format.md)

## 底本与使用范围

英文正文取自 [Wikisource：Alice's Adventures in Wonderland (1866)](https://en.wikisource.org/wiki/Alice%27s_Adventures_in_Wonderland_(1866))，即 Macmillan and Co. 1866 年印本（John Tenniel 插图版）的维基文库转录。2026-11-19 通过 MediaWiki API 逐章取回渲染文本，整理为 `content-src/alice/source/chapters.json`（SHA-256 `be36d1320ecb54016f9ac8c66af87b32f5867f48d098dbf3f8eaa93e24d60a1a`）。

- **为什么不是 Standard Ebooks**：Standard Ebooks 的下载页需要浏览器交互，脚本只拿到跳转页；按任务约定改用 Wikisource 1866 Macmillan 版，并在此记录。
- **公版状态**：作者 Lewis Carroll 1898 年去世，正文在中国自 1949 年起、在美国自 1929 年起进入公有领域；1866 年印本本身也早已公版。Tenniel 的插图**未随包分发**，包内只有文字。
- **收录范围**：12 章故事正文；去掉每章开头的「CHAPTER X.」、章题行、`****` 分隔符与书末印刷所署名，段落按原书自然段合并。
- **文本处理**：只做空白归一（多行合并为一段；诗行之间以单个空格连接），保留 1866 年印本的拼写、标点与对话引号，不现代化、不改写。
- **补回页码断口**：2026-11-21 复核维基文库渲染文本时发现，早期抽取在 12 处页边界丢了半段到一整段正文（如第二章白兔折返、第四章小狗玩耍、第七章帽匠讲音乐会）。已按同一底本补回约 810 词；为保住 779 个段落 id 与读者进度，补回文字并入其后相邻段落，因此这几段的段落划分与原书页码不完全一致，正文顺序与用词不变。
- **许可**：正文公有领域；项目自制的导读、句译、提示、测验与语法按 CC BY-SA 4.0 授权，两份许可写在 `meta.json` 的 `licenses`，构建时进包 manifest。

## 结构

| 项 | 数值 |
| --- | --- |
| 章 | 12 |
| 短读单元 | 104 |
| 自然段 | 779 |
| 句子 | 1,359 |
| 正文词数 | 26,568（本包统计口径） |
| 提示 | 479 |
| 测验 | 208（每单元 2 题） |
| 语法卡 | 104（每单元 1 则） |

单元边界冻结在 `meta.json` 的 `unitRanges`（每章 1 起的段落区间），约 280 词一单元，只在段落
边界断开。`chapter-01-u01`…`chapter-12-u08` 的 id 不再随重建变化，读者进度不受内容修订影响。

## 学习内容

- **导读与回顾**：每个单元一段中文导读、一段英文导读，按「情境 → 钩子」写，不剧透后文；
  中文 40–90 字、英文 25–50 词（全书首单元上限 110 字 / 60 词）。v3 已无 `recap`。
- **句译**：按冻结的句子切分规则逐片段翻译；对话里被感叹号切开的碎片、谐谑诗被切开的诗行
  也逐片段译，不合并、不补写，保持与正文一一对应。被切断的引语按原文的开口与收口位置处理。
- **提示**：只标注词典解决不了的——专名与译名（柴郡猫、假海龟、红心王后等）、多词单位、
  本句语境义、文化所指（`caucus-race`、`tarts`、`croquet`）与双关（`tails`/`tales`、
  `porpoise`/`purpose`）。删掉了样章时期与词典重复的普通词（`conversation`、`hedge`、
  `remarkable` 等）；重复率报告见 `reports/alice-1866.hints.json`。
- **测验**：每单元 2 题，考大意与因果，不考细节或语法术语；题干英文、反馈中英双语，并用
  `sentence` 指回单元内支撑答案的句子。选项由构建脚本按单元 id 确定性轮转，答案位置均衡。
- **语法**：每单元一个主讲点，`pointIds` 指向共享语法库（过去进行时、过去完成时、`so…that`、
  虚拟条件、关系从句等），书内只写 `focus`；`excerpt` 逐字取自本单元原文。
- **双关与谐谑诗策略**：双关只在影响理解处用提示标注词义与文化背景，不做逐句解释；谐谑诗
  （`How doth the little crocodile`、`Father William`、`Beautiful Soup` 等）保留原诗的分行
  节奏，按冻结切分逐句翻译，注释与正文分离。`Mouse's Tale` 一类图形诗按普通段落录入，不重排。

## 缺口与报告

`reports/alice-1866.gap.json` 为 `complete`：104 个单元零缺口，1,359 句全部有句译。
长难句解析（`sentences[].analysis`）是可选字段，候选 368 句只在
`reports/alice-1866.long-sentences.json` 报告，不参与完整度；提示重复率报告为
`reports/alice-1866.hints.json`。

## 编辑与验证

```sh
# 单章工作表与 overlay 校验
python3 tools/author/alice_worksheet.py 7
python3 tools/author/lint_alice_overlay.py 7 --warnings

# 重建包与报告
python3 tools/build_alice_book.py
python3 tools/build_alice_book.py --check
python3 tools/validate/validate_content.py
```

只改 `content-src/alice/chapter-NN.json` 的学习内容；正文、段落、单元边界与句子切分由脚本
从底本生成。新增或修正内容后先跑章节 lint，再重建全书并跑内容门禁。`ContentParsingTest`
断言 12 章、104 单元、779 段、1,359 句全部有句译，且每单元都有 2 道测验与语法。
