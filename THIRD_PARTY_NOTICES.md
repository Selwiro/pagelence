# 第三方数据与署名

本目录内的第三方数据随应用分发，各自沿用原许可，不重许可为 Apache-2.0 或
CC BY-SA 4.0。完整授权框架见 [LICENSE](LICENSE)、[LICENSE-CONTENT](LICENSE-CONTENT) 与 [NOTICE](NOTICE)。

| 数据 | 版本/来源 | 许可 | 位置 |
| --- | --- | --- | --- |
| CEFR-J Wordlist | v1.6，Yukio Tono / Tokyo University of Foreign Studies，2026-11-17 取自 cefr-j.org | 研究、教育、商用免费，按官方格式署名 | `content/third-party/word-levels.json` 的 `list` 层 |
| Octanove Vocabulary Profile C1/C2 | v1.0，Octanove Labs | CC BY-SA 4.0 | 同上（C1/C2 词级） |
| CEFR-Annotated WordNet | 2025，Masato Kikuchi、Masatsugu Ono、Toshioki Soga、Tetsu Tanabe、Tadachika Ozono；[Zenodo 17395388](https://zenodo.org/records/17395388)，`wordnet_sensekey_cefr.tsv` 取自其 `data/` 目录 | 标注 CC BY 4.0；义项键随 Princeton WordNet 3.0 许可 | 不随仓库分发（构建时另存，含 NOTICE 与两份许可原文）；只用于生成 `word-levels.json` 的 `aligned` 层，不随包分发原始表 |
| wordfreq | 3.0.2，Robyn Speer | CC BY-SA 4.0 | `content/third-party/word-frequencies.json`；由 `tools/build_word_frequencies.py` 生成 |
| ECDICT | 固定版本 [`39cfb96`](https://github.com/skywind3000/ECDICT/blob/39cfb9672c0906b63569e0453c2e6abfb9ab182a/ecdict.csv)；通用核心词典（BNC/COCA 3 万内、考试标签、Oxford 3000、CEFR 与两本书词形），释义与考试标签来自上游 | MIT | `content/third-party/dictionaries/ecdict.json` 与 `ecdict-license.txt`；由 `tools/build_dictionaries.py` 重建 |
| 词级五层衍生表 | 由上述四份数据推算（`aligned`/`derived`/`tag`/`freq` 层），项目自制 | CC BY-SA 4.0 | `content/third-party/word-levels.json` 的 `sources` 标明每格来源 |
| 本书出现次数 | 运行时以正文为输入单遍扫描，不随包存表 | CC BY-SA 4.0 | 仅内存（`WordQuery.countWords`） |

## 正文

- 《怪医杜立德》：Project Gutenberg eBook #501，1920 年版，公有领域。
- 《爱丽丝漫游仙境记》：Wikisource 转录的 Macmillan 1866 年印本（John Tenniel 插图版），
  Lewis Carroll 1898 年去世，正文公有领域；插图未随包分发。来源与处理见
  [`docs/books/alice-1866.md`](docs/books/alice-1866.md)。
- 《老人与海》1952 年版**不随 Android 分发**（美国版权至 2048 年）；Web 归档中的文本
  不进 `content/`、APK 与同步数据。

## 项目自制内容

导读、句译、测验、语法讲解与标注、单元切分按 CC BY-SA 4.0 授权。

## 许可原文位置

| 许可 | 覆盖对象 | 原文 |
| --- | --- | --- |
| Apache-2.0 | 代码与构建脚本 | [`LICENSE`](LICENSE) |
| CC BY-SA 4.0 | 项目自制的阅读内容与界面文案 | [`LICENSE-CONTENT`](LICENSE-CONTENT) |
| 项目署名与范围说明 | 全部 | [`NOTICE`](NOTICE) |
| MIT | ECDICT | [`ecdict-license.txt`](content/third-party/dictionaries/ecdict-license.txt) |
| WordNet 3.0 | Princeton WordNet 的词义与义项键 | [`wordnet-license.txt`](content/third-party/dictionaries/wordnet-license.txt) |

CEFR-J Wordlist 不提供可随包的许可全文，按官方要求的格式署名即履行业务；Octanove
Vocabulary Profile 与 wordfreq 均为 CC BY-SA 4.0，原文即 [`LICENSE-CONTENT`](LICENSE-CONTENT)。
CEFR-Annotated WordNet 的标注为 CC BY 4.0，只作构建输入，不随包分发，其 NOTICE 留在调研目录。
