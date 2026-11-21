# 《爱丽丝漫游仙境记》B1 书籍包编辑手册

本目录是这本书的**编辑源**。`content/books/alice-1866/book.json` 由
`tools/build_alice_book.py` 生成，**不要手改产物**。正文、段落、单元边界、句子切分都由脚本
从底本生成；编辑只写 `chapter-NN.json` 里的学习内容。

目标：按 [内容格式 v3](../../docs/content-format.md) 把 12 章 104 个单元
全部做成「完整单元」——每个单元都有标题、双语导读、每句句译、2 道测验、一则语法解析，
并按需要写点词提示。

## 一、工作流程

```sh
# 1. 读本章工作表：单元、段落、冻结切分后的句子（U# 是单元内 1 起的句序）
python3 tools/author/alice_worksheet.py 7

# 2. 写 content-src/alice/chapter-07.json

# 3. 校验（有 error 必须修；warning 逐条看）
python3 tools/author/lint_alice_overlay.py 7 --warnings
```

只写自己那一章的文件。不要运行 `build_alice_book.py`（它会写共享产物），不要改 `meta.json`、
`source/chapters.json`、语法库、词典或其他章。

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
            { "id": "cheshire-cat", "kind": "word", "target": "Cheshire Cat",
              "sentence": 3, "zh": "柴郡猫，会隐身的猫。", "en": "The Cheshire Cat, a cat that can vanish." }
          ]
        }
      },
      "quiz": [ /* 2 题 */ ],
      "grammar": { /* 见 §六 */ }
    }
  ]
}
```

- `units` 必须按章节顺序覆盖本章**全部**单元，`number` 与工作表一致。
- `paragraphs` 的键是**段落 id**，必须覆盖该单元全部段落，不多不少。
- `sentences` 与工作表的切分**逐句一一对应**：不合并、不拆分、不重排、不增删。
- 没有要写的提示就写 `"hints": []`；`analysis` 可省略。
- 文件用 UTF-8、2 空格缩进，不加注释。

## 三、句译（`sentences`）

- 忠实原意，中文自然，保留引号与语气；不解释、不评价、不剧透、不加 HTML。
- 人名地名按 §十 译名表；没列到的次要名字音译，并在一章内保持一致。
- 切分规则会把 `“Oh dear!` 一类切成独立句，也会把谐谑诗按行/句切开；照切分结果逐句译，
  不为了通顺改句子列表。
- 诗行同样逐句译；保留原有的断行节奏感，但不要自行合并成一段。
- 双关、谐谑诗、生造词不逐句解释；影响理解的词义放在 `hints`，不在句译里加注。
- 一句中文对应一句英文；不要漏掉感叹词、插入语。

## 四、导读（`introduction`）

只做两件事：**不剧透**，**让读者想往下读**。

- 2–3 句；中文 40–90 字（全书第一单元可到 110 字），英文 25–50 词（第一单元可到 60 词）。
- 结构：情境（谁、在哪、什么状况）→ 钩子（反常、麻烦、决定、具体问题）→ 可选提示。
- 不写本单元的结果、转折、结局、人物最终决定、主题寓意、作者评价；不写「本单元」「接下来」
  这类元话语；中文用「你」不用「您」。
- 英文独立成立，不逐字对照中文；B1 能读的句子。
- 前情需要交代时，用第一句半句带过，不单独开段（没有 `recap`）。

反例：「本单元讲述爱丽丝在茶会上遇到疯帽匠，表现了奇境规则之荒唐。」（剧透 + 摘要 + 评价）

## 五、标题（`title`）

- 是钩子，不是摘要；不剧透。中文 ≤ 16 字，英文 ≤ 8 词。
- 章题已由 `meta.json` 固定，不要改章题；只写单元标题。
- 同一章内标题不要重复，也不要把章题原样搬来。

## 六、语法（`grammar`）

字段：`excerpt`、`pointIds`、`focus`；可选 `type`（`new`/`review`）。

- `excerpt` 必须**逐字**出现在本单元原文里（可跨句，但必须是连续子串）；不改写、不省略。
- 一单元一个主讲点：`pointIds[0]` 为主讲点，从 [`grammar-index.tsv`](grammar-index.tsv) 里挑
  （列：id、等级、中文名、结构）。同句确实还用到别的点，可再列 1 个互链，但不要平讲。
- 有库时**只写 `focus`**，不要写 `explain` / `form`（库提供）；`focus` 1 句、≤ 60 汉字，
  只指认本句线索：哪个词、哪个标记、从句到哪结束，最多加一句「它在本句做什么」。
- 库外点（方言、古旧用法、作者自造）才写 `explain` + `form` + `focus`，`explain` ≤ 120 字、
  `form` ≤ 30 字。
- 本书是 B1：优先 A2–B2 条目，不要把 A1 的冠词、人称代词当主讲点；同一本书里同一主讲点
  可以复现，但 `type` 用 `review`。
- `focus.en` 也写一句，英文界面要用。

```jsonc
"grammar": {
  "excerpt": "Alice was beginning to get very tired of sitting by her sister",
  "pointIds": ["cefrj-66"],
  "type": "new",
  "focus": {
    "zh": "was beginning 是过去进行时，把「开始觉得」写成一个正在展开的过程。",
    "en": "Was beginning is past continuous: it makes the feeling a process unfolding at that moment."
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

1. 专名：本书译名、文化所指、情节相关说明；
2. 多词单位：短语、习语、动词短语；
3. 单词的**本句语境义**、熟词僻义（如 `presently` = 不久）；
4. 文化所指：器物、制度、时代背景（如 `tarts`、`caucus-race`、`croquet`）；
5. 本书教学点：本单元目标词或易混词辨析。

不写：词典已收录且本句就是常用义的普通词（如 `conversation`、`hedge`、`remarkable`）；
音标/词性/等级/词频（App 给）；通用例句；语法讲解（归 `grammar`）。一条一判：删掉它，
读者点这个词会不会得到错的意思？会 → 留。

字段：`id`（小写 kebab，段内唯一）、`kind`（`word`/`phrase`/`sentence`）、`target`（必须
逐字出现在段落原文；专名照抄原拼写）、`sentence`（该 target 所在的单元句序）、`zh`、`en`；
可选 `context`、`ipa`、`pos`、`lemma`、`example`（只在示范语境义时）。

- 每单元 2–6 条，按实际需要；**不要为凑数写**，尤其不要整段标普通词。
- 同一个名字在**本章第一次出现的单元**给一条；后续单元再出现可不重复。
- 短语用 `kind: "phrase"`；整句提示只在有言外之意时用，极少。
- 同一段同一 `target` 只一条；同一词不同句意思不同，用 `sentence` 分开写。
- 名字提示的 `zh` 用「中文译名，身份/说明」，如「柴郡猫，会隐身的猫」。

## 九、长难句解析（可选）

只在句子 ≥ 25 词或结构确实绕时写，一章最多 2 条。格式见规范 §5.8。`parts[].text` 必须逐字
出现在句中；`core` 保留原词序。不写剧情、不重复 `grammar`。**不要为了每句都有而硬加。**

## 十、译名表（全书统一）

| 英文 | 中文 | 说明 |
| --- | --- | --- |
| Alice | 爱丽丝 | 主角 |
| Alice's sister | 爱丽丝的姐姐 | 不另起名 |
| Dinah | 黛娜 | 爱丽丝的猫 |
| White Rabbit | 白兔 | 揣怀表的兔子 |
| Mouse | 老鼠 | 眼泪池里的老鼠 |
| Dodo | 渡渡鸟 | 主持会议式赛跑 |
| Lory | 鹦鹉 | 池中鸟 |
| Eaglet | 小鹰 | 池中鸟 |
| Duck | 鸭子 | 池中鸟 |
| Pat | 帕特 | 白兔的园丁 |
| Bill | 比尔 | 壁虎，被派下烟囱 |
| Puppy | 小狗 | 巨大的狗崽 |
| Caterpillar | 毛毛虫 | 抽水烟的毛毛虫 |
| Pigeon | 鸽子 | 把爱丽丝当蛇的鸽子 |
| Fish-Footman | 鱼仆 | 送请柬的鱼 |
| Frog-Footman | 蛙仆 | 公爵夫人家门房 |
| Duchess | 公爵夫人 | 胡椒厨房 |
| Cook | 厨娘 | 爱扔盘子 |
| Cheshire Cat | 柴郡猫 | 会隐身的猫 |
| March Hare | 三月兔 | 茶会主人之一 |
| Mad Hatter | 疯帽匠 | 茶会主人之一 |
| Dormouse | 睡鼠 | 茶会上打瞌睡 |
| Queen of Hearts | 红心王后 | 动不动就喊砍头 |
| King of Hearts | 红心国王 | 审判时记笔记 |
| Knave of Hearts | 红心骑士 | 被控偷馅饼 |
| Gryphon | 狮鹫 | 带爱丽丝见假海龟 |
| Mock Turtle | 假海龟 | 讲自己的伤心史 |
| Executioner | 刽子手 | 王后的手下 |
| Wonderland | 奇境 | 故事发生的地方 |

新遇到的名字：音译从常见译法，首次出现时在 hint 里给中文；同一章内保持一致。

## 十一、质量自检

写完本章，逐条确认：

1. 每个单元都有 title、introduction、全部 `sentences`、quiz、grammar；
2. `sentences` 条数与工作表切分逐句一致，没有漏译、并句、英文残留；
3. 导读不剧透，读完有一个具体问题想知道；英文不是中文的逐字翻版；
4. 测验两题都能在原文找到依据，`sentence` 指对；
5. `grammar.excerpt` 在单元原文里逐字存在，`pointIds` 能在库中找到且确实是这个结构；
6. `focus` ≤ 60 汉字，没有重复库的通用讲解；
7. hint 的 `target` 在段落里逐字存在，`sentence` 指对，没有把普通词也标上；
8. `lint_alice_overlay.py N` 零 error；warning 只剩「intro 略短」这类可接受的。
