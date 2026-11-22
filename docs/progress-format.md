# 进度与状态格式 v2（已冻结）

状态：**冻结于 2026-11-19**。`schemaVersion` 为 2。Kotlin 实现在
`com.pagelence.core.progress`、`com.pagelence.core.wordcards`、`com.pagelence.core.prefs`。

2026-11-20 在 v2 上追加两个向后兼容的可选字段：单本进度的 `reader`（单元续读点）和
偏好的 `readerLayoutChosen`（是否明确选过阅读方式）。2026-11-22（T139）再追加单本进度的
`finishedAt`（读完时间），T144 追加顶层 `annotations`（书签/划线/笔记）。同日 T158/T159
在偏好里追加 `readingFont`、`lineSpacing`、`pageMargin`、`showAllTranslations`，T161 追加
顶层 `lookupHistory`（查词历史）。
旧文档没有这些字段时按默认解码；`readingState` 的版本号不变。

一份 `state.json` 同时承担：本地保存、导出/导入、WebDAV 同步。三处共用同一个序列化器
（`ReadingStateCodec`），不另写格式。

```jsonc
{
  "schemaVersion": 2,
  "deviceId": "3f6c…",
  "updatedAt": "2026-11-19T01:00:00Z",
  "books": {
    "doctor-dolittle-1920": {
      "bookId": "doctor-dolittle-1920",
      "paragraphId": "chapter-01-p005",
      "readUnitIds": ["chapter-01-u01", "chapter-01-u02"],
      "lastReadAt": "2026-11-19T01:00:00Z",
      "finishedAt": null,
      "reader": {
        "unitId": "chapter-01-u02",
        "phase": "text",
        "textPage": 1,
        "textProgress": 0.5,
        "quizAnswers": { "0": 1, "1": 2 },
        "quizSubmitted": false,
        "grammarReached": false
      }
    }
  },
  "wordCards": [ /* 见第五节 */ ],
  "annotations": [ /* 见第五·一节 */ ],
  "lookupHistory": [ /* 见第五·二节 */ ],
  "preferences": { /* 见第六节 */ },
  "selfLevel": "B1",
  "levelSource": "self",
  "levelScore": null,
  "levelUpdatedAt": "2026-11-19T00:55:00Z",
  "dailyActivity": {
    "2026-11-18": { "completedUnits": 1 },
    "2026-11-19": { "completedUnits": 2 }
  }
}
```

## 一、信封

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `schemaVersion` | 是 | 当前为 `2`；读取端按 `ReadingStateCodec` 迁移或尽力读取，不因版本不符清空 |
| `deviceId` | 是 | 设备标识，首次启动生成；合并时用于取舍同字段冲突 |
| `updatedAt` | 是 | ISO-8601 UTC，写状态时更新 |
| `books` | 是 | 键为 `bookId`，值为单本进度 |
| `wordCards` | 否 | 生词卡（含墓碑） |
| `annotations` | 否 | 句子级书签/划线/笔记（含墓碑），见第五·一节 |
| `lookupHistory` | 否 | 最近查过的词，见第五·二节；最多 200 条 |
| `preferences` | 否 | 界面与词义偏好 |
| `selfLevel` | 否 | A1–C2 自评/测评等级；`null` 表示未设置 |
| `levelSource` | 是 | `self`、`quick`、`full`；未设置时仍写 `self` |
| `levelScore` | 否 | 测评得分 0–100；自评为 `null` |
| `levelUpdatedAt` | 否 | 等级写入时间，ISO-8601 UTC；合并时取较晚者 |
| `dailyActivity` | 否 | 按本地日期（`YYYY-MM-DD`）记录的日活，最多 365 天 |
| `recentAssessmentPapers` | 否 | 最近两次测评的题目 id，按试卷分组、旧卷在前；抽题先剔除 |

## 二、单本进度

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `bookId` | 是 | 与 `books` 的键一致 |
| `paragraphId` | 否 | 最近阅读段落；仅当该 id 仍在书中时有效 |
| `readUnitIds` | 是 | 已读单元集合，**只增**；顺序保留首次读完的顺序 |
| `lastReadAt` | 否 | 本书最后一次写进度的 UTC 时间；保留用于排序与同步 |
| `reader` | 否 | 最后一个打开单元的续读点，见下一节；旧状态没有此字段，按各自默认重新开始 |
| `finishedAt` | 否 | 读完全书的时间，ISO-8601 UTC；未读完或旧文档为 `null`。由 T139 追加，v2 内不升版本 |

### 续读点 `ReaderPosition`

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `unitId` | 是 | 单元 id；不在当前书中时整条丢弃 |
| `phase` | 否 | `guide`、`text`、`quiz`、`grammar`；默认 `text` |
| `textPage` | 否 | 翻页模式的 0-based 正文页；默认 `0` |
| `textProgress` | 否 | 上下滑动模式的归一化滚动位置（0–1）；默认 `0` |
| `quizAnswers` | 否 | 题目下标 → 选项下标；默认空 |
| `quizSubmitted` | 否 | 测验是否已提交；默认 `false` |
| `grammarReached` | 否 | 是否已到过语法页；默认 `false` |

读取时要清洗（`ProgressSemantics.restore`）：

- 丢弃书中不存在的 `paragraphId` 与单元 id；
- 去重，保留首次出现的顺序；
- 全书读完时保留最后位置，不清空；
- `finishedAt` 只在清洗后的单元集合仍覆盖全书时保留，否则置 `null`（手工删掉一个单元
  后，书回到「在读」）。

阅读推进（`ProgressSemantics.advance`）与 Web 版一致：

1. 把当前单元加入 `readUnitIds`（已存在则不重复）；
2. 把 `paragraphId` 移到**下一个单元的第一段**；
3. 最后一个单元读完时保持原位置。

「今天读到这里」用 `markUnitRead`：只标记已读，不移动位置。只有单元此前未读时，
调用方才记一次「完成单元」，避免重复计数。

**完成与 `finishedAt`**：`advance` 与 `markUnitRead` 都接受一个 `now` 参数。

- 单元集合覆盖全书且 `finishedAt` 为空时写入 `now`；已有非空值则保留第一次的时间，
  重读、同步都不会改写它；
- 单元集合不再覆盖全书时置 `null`；
- `now = null` 表示这次写入只是续读点保存（翻页、答题、进入语法页），不写完成时间。
  只有离开最后一个单元的语法页、或点「继续下一段 / 读完全书」这类显式推进才传 `now`。

`ProgressSemantics.justFinished(before, after)` 判断「本次写入是否从在读变为读完」，
App 用它决定是否进入读完页，不使用「是否已读完」判断，避免重读时重复弹出。

**已读单元数** = `readUnitIds` 与书本单元集合的交集大小。Web 与本工程的算法各自
独立的对照由 `core/src/test/resources/parity/progress-parity.json` 固定：该文件由
`tools/migrate/record_progress_parity.py` 按 `work/web/src/reading.mjs` 的
`advance`/`restoreProgress` 规则、对当前内置书的结构录制，Kotlin 测试逐条重放，
要求每步的已读单元数与续读段落一致。v1 与 v2 的进度都能恢复（迁移在
`ReadingStateCodec`，恢复规则本身不看版本）。fixture 只含结构与 id，不含正文。

## 三、水平与测评

| 字段 | 取值 | 说明 |
| --- | --- | --- |
| `selfLevel` | `A1`–`C2` 或 `null` | 当前等级；自评或测评结果 |
| `levelSource` | `self` / `quick` / `full` | 自评 / 快速测试 / 高级测试 |
| `levelScore` | `0`–`100` 或 `null` | 测评得分（百分比）；自评为空 |
| `levelUpdatedAt` | ISO-8601 UTC 或 `null` | 最近一次写入等级的时间 |

- 等级只影响默认值（词义语言、导读模式），不锁内容、不用于解锁。
- 快速测试只写区间建议的**单级**结果，区间文案由 UI 决定；高级测试可带分项，
  分项不写入 `state.json`，只保留最近一次总分。
- 测评结束后，「接受新等级」同时写 `selfLevel`、`levelSource`、`levelScore`、
  `levelUpdatedAt`，并套用该等级的默认阅读辅助；「保留原级」只写 `levelScore` 与
  `levelUpdatedAt`，等级与来源不变。
- 因此 `levelScore` 非空表示「最近一次测评的结果」，`levelUpdatedAt` 同时是等级写入
  时间与最近一次测评时间。自评或手动改等级时 `levelScore` 清空。
- 不设重测冷却；这两个字段只用于展示最近一次结果，不阻止再次测评，也不新增字段。
- 首次自评可跳过；跳过也写 `preferences.levelOnboardingDone = true`，不再重复弹出。
  老用户的 v1 状态迁移时自动置 `true`，可在「我的」随时设置或修改。
- 导读页是否再弹由 `preferences.seenGuideUnits` 决定；条目按 `bookId::unitId` 累积，
  不随进度清空，书更新后旧 id 不会命中。

## 四、日活（`dailyActivity`）

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| 键 | 是 | 本地日期 `YYYY-MM-DD`（设备时区，不是 UTC） |
| `completedUnits` | 是 | 当天读完的单元数，从 0 开始累加 |

- 当天打开过书就写入一个键（`completedUnits` 可为 0），**键的个数就是阅读天数**；
  没有分钟数：分钟要靠会话计时，闲置与多设备同步都会污染。
- 保留最近 **365 天**（含今天）；写入时按当天日期裁剪，非法日期键在读取时丢弃。
- 只展示、不推送、不断签惩罚；导出文件会包含活动记录，隐私说明需写清。
- 只记完成单元数与阅读天数，不记逐题、逐词或章节明细。

**合并规则（必须遵守）**：本地与远端同一天的 `completedUnits` 取 **max**，日期键取
并集；不求和、不按设备分桶。求和会把同一份阅读量重复计算；按设备分桶会让同一天在
热力图里出现多格。max 在「两端都读了同一天」时会少算一端多读的部分，但换来无冲突的
合并；热力图只表达「这天读过、读了几段」，可以接受。实现见
`DailyActivityRules.mergeDayMax`。

## 五、生词卡

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `id` | 是 | `bookId::paragraphId::hintId`，本应用内稳定 |
| `bookId` / `paragraphId` / `hintId` | 是 | 定位回原文的坐标 |
| `unitId` | 否 | 冗余保存，便于按单元筛选 |
| `term` / `lemma` | 是 | 词面与词目 |
| `pos` / `zh` / `en` / `ipa` | 否 | 点词时看到的解释与音标 |
| `example` | 否 | 原句 `{en, zh}` |
| `createdAt` / `updatedAt` | 是 | ISO-8601 UTC |
| `deletedAt` | 否 | 墓碑时间；非空即已删除 |

删除写墓碑而不是物理删除，同步时才不会把卡片又「复活」。列表默认过滤墓碑，
`includeDeleted = true` 用于导出与合并。

## 五·一、标注（书签 / 划线 / 笔记）

句子级，不做任意字范围选择。三种类型共用一条记录，`kind` 区分；同一句的同一种类型
只有一条。

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `id` | 是 | `bookId::paragraphId::sentenceNumber::kind`，跨设备稳定 |
| `bookId` / `unitId` / `paragraphId` | 是 | 定位回原文的坐标；`unitId` 便于按单元分组 |
| `sentenceNumber` | 是 | 单元内 1-based 句号，与阅读器一致（跨段落连续计数） |
| `kind` | 是 | `bookmark`、`highlight`、`note` |
| `note` | 否 | 仅 `note` 使用；读写时 `trim`，全空白视为没有笔记 |
| `quote` | 否 | 句子原文快照；内容更新导致锚点失效时用它兜底显示 |
| `createdAt` / `updatedAt` | 是 | ISO-8601 UTC；再添加同一句同一类型时保留 `createdAt` |
| `deletedAt` | 否 | 墓碑时间；非空即已删除 |

- 长按句子加/取消书签或划线；笔记输入框非空才保存，删除写墓碑。
- 同一句、同一类型再次添加时沿用原 `id` 并清空 `deletedAt`（复活），`createdAt` 不变。
- 读取时 `AnnotationRules.sanitize` 丢掉空 id / 空段落或句号 < 1 的记录，并按 `id`
  去重；合并规则与生词卡一致：**墓碑优先**，否则 `updatedAt` 较晚者胜。
- `AnnotationRules.anchor` 把记录解析回当前内容；解析失败时保留 `quote`，列表显示
  「原句有更新」，不崩溃。

## 五·二、查词历史（`lookupHistory`）

每次点词查义后追加一条；复习 → 查词按时间倒序展示，点一条回到原句并重新打开词义。
同一本书、同一原形只保留最新一条（`id` 为 `bookId::lemma`），重复查词只刷新时间与位置。

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `id` | 是 | `bookId::原形`（小写、压缩空白），跨设备稳定 |
| `bookId` / `unitId` / `paragraphId` | 是 | 定位回原书；书已移除时列表标「书籍已移除」且不可点 |
| `sentenceNumber` | 是 | 单元内 1-based 句号 |
| `term` / `lemma` | 是 | 词面与原形；短语提示按整条记录 |
| `start` / `end` | 是 | 词面在句中的字符范围，用于重新高亮 |
| `gloss` / `ipa` | 否 | 词义与音标快照，供列表显示；词典更新后仍可读 |
| `lookedUpAt` | 是 | ISO-8601 UTC；排序与合并都用解析后的时间，不按字符串比较 |

- 读取与写入都走 `LookupHistoryRules.sanitize`：丢弃空 id / 空书 / 句号 < 1 的记录，
  按 `id` 去重后取 `lookedUpAt` 较晚者，倒序保留最近 **200** 条。
- 合并：按 `id` 取并集，`lookedUpAt` 较晚者胜，再按时间倒序截断；不做墓碑
  （历史是「最近查过」，清空后两端都为空，不需要保留删除记录）。
- 清空历史写空列表；生词卡不受影响。

## 六、偏好

与 Web 版存储键一致，未知值回落到默认值（不报错）：

| 字段 | 取值 | 默认 |
| --- | --- | --- |
| `language` | `zh`、`en` | `zh` |
| `readingMode` | `guided`、`original` | `guided` |
| `readerLayout` | `paged`、`scroll`、`eink`（旧值按翻页处理） | `paged` |
| `readerLayoutChosen` | 是否在设置页明确选过阅读方式；旧状态为 `false` 时 `readerLayout` 迁移为 `paged` | `false` |
| `definitionMode` | `zh`、`mixed`、`en` | `zh` |
| `guideMode` | `zh`、`en`、`immersive` | `zh` |
| `theme` | `system`、`light`、`dark` | `system` |
| `fontSize` | `small`、`medium`、`large`、`xlarge` | `medium` |
| `readingFont` | `sans`、`serif`：正文字体（T158） | `sans` |
| `lineSpacing` | `compact`、`normal`、`relaxed`：行高倍数 1.45 / 1.62 / 1.85（T158） | `normal` |
| `pageMargin` | `narrow`、`normal`、`wide`：正文左右留白 14 / 22 / 32dp（T158） | `normal` |
| `showAllTranslations` | `true`、`false`：阅读页是否一键显示全部句译（T159） | `false` |
| `annotateHints` | `true`、`false`：是否给有书内释义的词/短语加浅色虚线 | `true` |
| `handledOffers` | 已处理的里程碑 id 列表 | 空 |
| `levelOnboardingDone` | 首次 A1–C2 自评是否已展示并处理（选择或跳过） | `false`（v1 迁移为 `true`） |
| `speechRate` | 系统 TTS 语速，`0.7`–`1.3`，超出范围读取时夹紧 | `1.0` |
| `seenGuideUnits` | 已点过「开始阅读」或滑过导读的单元，键为 `bookId::unitId` | 空（旧状态迁移为空） |
| `shelfBookIds` | 加入书架但还没开始读的书籍 id，按加入顺序；与进度无关，移除书不影响已读记录 | 空（旧状态迁移为空） |

## 七、只读数据不进同步

词典、词级表、词频、本书出现次数随包分发（`content/third-party/`），不进
`state.json`。书的正文与学习内容同理只读；同步只带进度、卡片、偏好、等级、日活与自制书清单。

## 八、查词顺序

点词面板按以下顺序取数（`WordQuery`）：

1. 句中或段落 `hints`：语境义；
2. 段落 `hints`：释义、音标、原句与句译；
3. 词级表：CEFR-J，C1/C2 记 Octanove；
4. 词频（wordfreq，按段落）；
5. 本书出现次数；
6. ECDICT，中英释义来自同一张表；查不到再走缩合/连字符/专名/未收录。

词形归一只有「小写、弯引号转直引号、去所有格 `'s`」三步；完整词形还原不在首版范围。

## 九、v1 → v2 迁移

`ReadingStateCodec.decode` 先读原始 `schemaVersion`，再用同一个序列化器补默认值：

- **v1（或缺少版本号）**：保留 `books`、`paragraphId`、`readUnitIds`、`wordCards`、
  `preferences`、`deviceId`、`updatedAt`；新增字段取默认（`selfLevel = null`、
  `levelSource = self`、`dailyActivity = {}`、`lastReadAt = null`）；把 `preferences`
  的 `levelOnboardingDone` 置 `true`，避免老用户被首次自评打断。读取后立即写回 v2。
- **缺失或未知字段**：读成默认值，不崩溃；写回时**保留未知字段**（见下）。
- **损坏 JSON**：回退为空状态（并保留原文件内容到下一次写入前）；不清空已有磁盘文件。
- **更高版本**：按已知字段尽力读取，加载时不改写文件；之后任何一次写入都把原始 JSON 的
  未知字段合并回去，并把 `schemaVersion` 保持为文档原本的更高版本，避免旧版把新版数据
  「降级抹掉」。合并只覆盖对象层级（顶层、`books.<id>`、`preferences` 等）；数组元素
  （例如某张生词卡上的新字段）仍会被新版整体替换，v3 落地时在本文补充例外。
- **写入**：临时文件 + 原子改名；`schemaVersion` 为当前版本，或在更高版本文档上保持原版本。

## 九·一 阅读位置日志

翻页只改「读到哪儿」，不值得每次重写整份文档。`ReadingStateStore.saveReaderPoint` 把
`bookId / paragraphId / ReaderPosition / savedAt` 写进 `filesDir/reader-position.json`
（约 250 字节，同样是临时文件 + 原子改名），内存状态同步更新，导出与同步看到的是最新位置。

- 启动时先读 `state.json`，再把日志叠加到对应书上；日志里的书不存在、位置为空或 JSON
  损坏时直接忽略（不删用户其他数据）。
- 下一次整份写入（交卷、读完单元、改设置、同步写回等）会把日志中的位置一并写入
  `state.json` 并删除日志。退出阅读器时写日志、切单元/交卷时写整份。
- 崩溃/强杀后重启：日志仍在，续读位置不丢；日志最多比 `state.json` 多一页。

## 九·二 一次性的压实

加载时顺手清掉只增不减的数据，避免几年后状态文件与同步包无谓变大：

- `wordCards` 与 `annotations` 里 `deletedAt` 超过 180 天的墓碑删除（保留期足够所有设备看到删除）。
- `lookupHistory` 读取时清洗并截断到 200 条。
- `preferences.seenGuideUnits` 中所属书已不在 `books` 里的条目删除。
- 只有真的删了东西才写回一次；统计与卡片内容不变。

迁移 fixture：`data/src/test/resources/state-v1.json`；对照用例见
`ReadingStateCodecTest` 与 `ReadingStateStoreTest`。

## 十、导出、导入与 WebDAV 合并

- 导出/导入复用同一份 `state.json`，不另写格式；导出走 `snapshotJson()`，默认文件名
  `pagelence-backup-YYYYMMDD-HHmm.json`；导入先 `ReadingStateCodec.decode`（含迁移），
  上限 5 MB，损坏文件或更高 `schemaVersion` 在写库前拒绝。
- 合并后再写回，写回是单事务；服务端 412 表示需要重新合并。
- 两端都有同一 `bookId` 时才合并该书：`readUnitIds` 取并集（保留本地顺序，远端新增项
  按远端顺序追加）；`paragraphId` 与 `lastReadAt` 取 `lastReadAt` 较晚的一端；
  `finishedAt` 取两端**最早的非空值**（谁先读完用谁的时间）。合并本身拿不到书本单元数，
  并集若不完整，App 在下次加载时由 `ProgressSemantics.restore` 把 `finishedAt` 置空。
- `dailyActivity`：按日 max，见第四节。
- `wordCards`：按 `id` 合并；任一端是墓碑即取墓碑（不复活），否则 `updatedAt` 较晚者胜。
- `annotations`：与 `wordCards` 相同的按 `id` 合并规则；同句同类冲突时 `updatedAt` 较晚者胜。
- `lookupHistory`：按 `id` 取并集，`lookedUpAt` 较晚者胜，倒序截断到 200 条；没有墓碑。
- 等级：`levelUpdatedAt` 较晚者胜；相同则保留本机。
- 偏好：整组取文档 `updatedAt` 较晚的一端；本机是空文档（新装/清数据）时取文件里的偏好；
  `deviceId`、`updatedAt` 始终用本机值。
- 缺书的一端显示「未导入的书」，不丢进度。
- 导入先显示预览（书数、已读单元、生词卡、等级、`updatedAt`）再写库；默认合并，
  经二次确认可整份覆盖，覆盖前写 `state.json.bak` 且保留本机 `deviceId`。
- 实现见 `:core` 的 `StateMerge` 与 `docs/webdav-sync.md`：合并结果与本地内容一致时不刷新
  `updatedAt`；`deviceId` 永远保留本机值。同步的 ETag、基线与失败退避不写进
  `state.json`，而是同目录的 `sync-state.json` / `sync-baseline.json`；远端备份在
  `sync-backups/`（仅服务器不返回 ETag 时产生）。

## 十一、版本演进

- 新增**可选**字段不升版本；删除字段或改语义必须升 `schemaVersion`。
- 任何会改变读取含义的迁移都必须先于同步定稿；v2 已满足此条件。
- T144 的 `annotations` 是 v2 上的可选字段：旧文档解码为空列表，新文档对旧版本是
  「未知字段」，回退安装时会被原样保留在顶层（数组内字段仍可能被旧版本丢弃）。
