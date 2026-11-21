# 《怪医杜立德》A2 书籍包编辑手册

本目录是这本书的**编辑源**。`content/books/doctor-dolittle/book.json` 由
`tools/build_dolittle_book.py` 生成，**不要手改产物**。正文、段落、单元边界、句子切分都由脚本
从底本生成；编辑只写 `chapter-NN.json` 里的学习内容。

目标：按 [内容格式 v3](../../docs/content-format.md) 把 21 章 103 个单元
全部做成「完整单元」——每个单元都有标题、双语导读、每句句译、1–3 道测验、一则语法解析，
并在需要的地方写点词提示。

## 一、工作流程

```sh
# 1. 读本章工作表：单元、段落、冻结切分后的句子（U# 就是单元内 1 起的句序）
python3 tools/author/dolittle_worksheet.py 7

# 2. 写 content-src/doctor-dolittle/chapter-07.json

# 3. 校验（有 error 必须修；warning 逐条看）
python3 tools/author/lint_dolittle_overlay.py 7 --warnings

# 4. 若提示 sentence 指错句，可自动指向 target 所在句，再校验一次
python3 tools/author/fix_hint_sentences.py 7
python3 tools/author/lint_dolittle_overlay.py 7 --warnings
```

只写自己那一章的文件。不要运行 `build_dolittle_book.py`（它会写共享产物），不要改
`chapter-01.json`、`meta.json`、`book.json`、语法库、词典或其他章。

## 二、文件格式

```jsonc
{
  "chapter": 7,
  "units": [
    {
      "number": 1,
      "title": { "zh": "…", "en": "…" },
      "introduction": { "zh": "…", "en": "…" },
      "paragraphs": {
        "chapter-07-p001": {
          "sentences": ["第 1 句的中文", "第 2 句的中文"],
          "hints": [
            { "id": "chee-chee", "kind": "word", "target": "Chee-Chee",
              "sentence": 3, "zh": "奇奇，那只猴子。", "en": "Chee-Chee, the monkey." }
          ]
        }
      },
      "quiz": [ /* 1–3 题 */ ],
      "grammar": { /* 见 §六 */ }
    }
  ]
}
```

- `units` 必须按章节顺序覆盖本章**全部**单元，`number` 与工作表一致。
- `paragraphs` 的键是**段落 id**（如 `chapter-07-p001`），必须覆盖该单元全部段落，不多不少。
- `sentences` 与工作表的切分**逐句一一对应**：不合并、不拆分、不重排、不增删。工作表里有
  几句就写几条。
- 没有要写的提示就写 `"hints": []`；`analysis` 可省略。
- 文件用 UTF-8、2 空格缩进，不加注释。

## 三、句译（`sentences`）

- 忠实原意，中文自然，保留引号与语气；不解释、不评价、不剧透、不加 HTML。
- 人名地名按 §八 译名表；没列到的次要名字音译，并在一章内保持一致。
- 切分规则会把 `Mr. Smith`、`“Oh dear!` 一类切成独立句，照切分结果逐句译，不为了通顺改句。
- 一句中文对应一句英文；不要把两句合成一句，也不要漏掉感叹词、插入语。
- 方言、古语（`thou`、`sayest`、`ain’t`）用略文雅但可读的中文，不逐字硬译。
- 引语里的 `said the Doctor` 仍保留「医生说」。

## 四、导读（`introduction`）

只做两件事：**不剧透**，**让读者想往下读**。

- 2–3 句；中文 40–90 字（本章首单元可到 110 字），英文 25–50 词（首单元可到 60 词）。
- 结构：情境（谁、在哪、什么状况）→ 钩子（反常、麻烦、决定、具体问题）→ 可选提示。
- 不写本单元的结果、转折、结局、人物最终决定、主题寓意、作者评价；不写「本单元」「接下来」
  这类元话语；中文用「你」不用「您」。
- 英文独立成立，不用逐字对照中文；用 A2 能读的短句。
- 双语各自有钩子，不要英文只是直译。
- 前情需要交代时，用第一句半句带过，不单独开段（没有 `recap`）。

反例：「本单元讲述杜立德去非洲给猴子治病，表现了他的善良。」（剧透 + 摘要 + 评价）
正例见 `chapter-01.json` 三个单元。

## 五、标题（`title`）

- 是钩子，不是摘要；不剧透。中文 ≤ 16 字，英文 ≤ 8 词。
- 章题已由 `meta.json` 固定，不要改章题；只写单元标题。
- 同一章内标题不要重复，也不要把章题原样搬来。

## 六、语法（`grammar`）

字段：`excerpt`、`pointIds`、`focus`；可选 `type`（`new`/`review`）。

- `excerpt` 必须**逐字**出现在本单元原文里（可跨句，但必须是连续子串）；不改写、不省略。
- 一单元一个主讲点：`pointIds[0]` 为主讲点，从
  [`grammar-index.tsv`](grammar-index.tsv) 里挑（列：id、等级、中文名、结构）。
  确实同句还用到别的点，可再列 1 个互链，但不要平讲。
- 有库时**只写 `focus`**，不要写 `explain` / `form`（库提供）；`focus` 1 句、≤ 60 汉字，
  只指认本句线索：哪个词、哪个标记、从句到哪结束，最多加一句「它在本句做什么」。
- 库外点（方言、古旧用法、作者风格）才写 `explain` + `form` + `focus`，`explain` ≤ 120 字、
  `form` ≤ 30 字。
- 不给每句都写语法；挑本单元最值得学、原文确实用到的点。A2 书优先 A1–B1 条目。

```jsonc
"grammar": {
  "excerpt": "His sister used to grumble about all these animals",
  "pointIds": ["cefrj-140"],
  "type": "new",
  "focus": {
    "zh": "used to grumble 表示过去常常抱怨，现在不一定如此；used to 后面跟动词原形。",
    "en": "Used to grumble describes a habit in the past, not necessarily now; used to takes the base verb."
  }
}
```

## 七、测验（`quiz`）

每单元 **2 题**（至少 1、最多 3），考**大意理解**，不考拼写、语法术语、作者生平。

- 英文题干；3 个选项（可 4 个），干扰项要像真的、能从原文找到依据；不设「以上都对」。
- `answer` 是正确选项下标（0 起）；`sentence` 指向支撑答案的**单元内句序**（工作表 U#）。
- `feedback` 为 `{zh, en}`：中文 1–2 句说明依据，不只说「正确/错误」；英文一句，供英文界面。
- 两题分工：一题问「发生了什么/为什么」，一题问细节或人物反应；不要两题考同一句。

## 八、点词提示（`hints`）

只写词典解决不了的。**五类**：

1. 专名：本书译名、文化所指、情节相关说明（人名、地名、船名、种族名）；
2. 多词单位：短语、习语、动词短语；
3. 单词的**本句语境义**、熟词僻义；
4. 文化所指：器物、制度、旧币、时代背景；
5. 本书教学点：本单元目标词或易混词辨析。

不写：词典已收录且本句就是常用义的普通词；音标/词性/等级/词频（App 给）；通用例句；
语法讲解（归 `grammar`）。一条一判：删掉它，读者点这个词会不会得到错的意思？会 → 留。

字段：`id`（小写 kebab，段内唯一）、`kind`（`word`/`phrase`/`sentence`）、`target`（必须
逐字出现在段落原文；专名照抄原拼写，注意弯引号）、`sentence`（该 target 所在的单元句序，
不确定就省略或跑 `fix_hint_sentences.py`）、`zh`、`en`；可选 `context`、`ipa`、`pos`、
`lemma`、`example`（只在示范语境义时）。

- 每单元 3–8 条（段落多的长单元可到 11 条），按实际需要；不要为凑数写。
- 同一个名字在**本章第一次出现的单元**给一条；后续单元再出现可不重复，读者会记得。
- 短语用 `kind: "phrase"`；整句提示只在有言外之意时用，极少。
- 同一段同一 `target` 只一条；同一词不同句意思不同，用 `sentence` 分开写。
- 名字提示的 `zh` 用「中文译名，身份/说明」，如「奇奇，那只猴子」。

## 九、长难句解析（可选）

只在句子 ≥ 25 词或结构确实绕时写，一章最多 2 条。格式见规范 §5.8 与
`chapter-01.json` 的 `chapter-01-p005`。`parts[].text` 必须逐字出现在句中；`core` 保留原词序。
不写剧情、不重复 `grammar`。**不要为了每句都有而硬加。**

## 十、译名表（全书统一）

| 英文 | 中文 | 说明 |
| --- | --- | --- |
| Doctor Dolittle / John Dolittle | 杜立德医生 / 约翰·杜立德 | 主角 |
| Sarah Dolittle | 萨拉·杜立德 | 姐姐 |
| Polynesia | 波利尼西亚 | 鹦鹉 |
| Jip | 吉普 | 狗 |
| Dab-Dab | 达布达布 | 鸭子 |
| Gub-Gub | 咕咕 | 小猪 |
| Too-Too | 图图 | 猫头鹰 |
| Chee-Chee | 奇奇 | 猴子 |
| Bumpo / Prince Bumpo | 邦波 / 邦波王子 | 乔利金基国王之子 |
| King of Jolliginki | 乔利金基国王 | 不另起名 |
| Queen Ermintrude | 埃明特鲁德王后 | 王后 |
| Jolliginki | 乔利金基 | 非洲王国 |
| Ben Ali | 本·阿里 | 海盗头子 |
| The Barbary Dragon | 巴巴里巨龙 | 本·阿里的诨名 |
| The Cat’s-meat-Man | 卖猫食的人 | 走街小贩 |
| Squire Jenkins | 詹金斯乡绅 | 本地乡绅 |
| the Parson | 教区牧师 | 不另起名 |
| Theodosia | 西奥多西娅 | 卖猫食人的妻子 |
| Oxenthorpe | 奥克森索普 | 十英里外的小镇 |
| Puddleby-on-the-Marsh | 沼泽边的帕德尔比 | 医生的家乡；简称帕德尔比 |
| West Country | 英格兰西南部 | |
| The Saucy Sally | 「俏莎莉号」 | 渔船 |
| Pushmi-Pullyu | 「推我拉你」 | 双头动物 |
| The Sleeping Beauty | 睡美人 | 童话 |
| Tripsitinka | 特里普西廷卡 | 波利尼西亚假扮的仙女皇后 |
| Black Rappee | 黑拉皮鼻烟 | 一种鼻烟 |
| Punch-and-Judy | 潘趣与朱迪 | 木偶戏 |
| Mrs. Trevelyan | 特里维廉太太 | 渔夫镇的房东 |
| Noah and the Flood | 诺亚与大洪水 | |
| Canary Islands | 加那利群岛 | |
| Isle of Wight | 怀特岛 | |
| Gibraltar | 直布罗陀 | |
| Abyssinia | 阿比西尼亚 | |
| Antwerp | 安特卫普 | |
| Kashmir | 克什米尔 | |
| Jamaica | 牙买加 | |
| Brazil | 巴西 | |

新遇到的名字：音译从常见译法，首次出现时在 hint 里给中文；同一章内保持一致，不要一会儿
「波利尼西亚」一会儿「波莉」。

## 十一、质量自检

写完本章，逐条确认：

1. 每个单元都有 title、introduction、全部 `sentences`、quiz、grammar；
2. `sentences` 条数与工作表切分逐句一致，没有漏译、并句、英文残留；
3. 导读不剧透，读完有一个具体问题想知道；英文不是中文的逐字翻版；
4. 测验两题都能在原文找到依据，`sentence` 指对；
5. `grammar.excerpt` 在单元原文里逐字存在，`pointIds` 能在库中找到且确实是这个结构；
6. `focus` ≤ 60 汉字，没有重复库的通用讲解；
7. hint 的 `target` 在段落里逐字存在，`sentence` 指对，没有把普通词也标上；
8. `lint_dolittle_overlay.py N` 零 error；warning 只剩「intro 略短」这类可接受的。
