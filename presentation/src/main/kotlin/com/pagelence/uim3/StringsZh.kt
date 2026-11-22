package com.pagelence.uim3

/** Chinese copy. Getters keep the class initialiser empty; a single wide
 * constructor call fails the ART verifier. */
object ZhStrings : Strings {
    override val uiLanguage: UiLanguage get() = UiLanguage.ZH
    override val appName: String get() = "页有所成"
    override val library: String get() = "书库"
    override val settings: String get() = "设置"
    override val wordCards: String get() = "生词卡"
    override val back: String get() = "返回"
    override val loading: String get() = "正在加载内容…"
    override val retry: String get() = "重试"
    override val recoveryFromBackupTitle: String get() = "已从备份恢复"
    override val recoveryFromBackupBody: String get() =
        "上次的进度文件读取失败，已用备份恢复。损坏的文件保留为 state.json.corrupt，方便排查。"
    override val recoveryCorruptTitle: String get() = "进度文件损坏"
    override val recoveryCorruptBody: String get() =
        "主文件和备份都无法读取，应用已用空白进度启动。损坏的文件保留为 state.json.corrupt，方便排查。"
    override val recoveryContentFailedTitle: String get() = "内容读不出来"
    override val recoveryContentFailedBody: String get() =
        "随包内容无法读取，可能是安装损坏。点「重试」重新加载；仍然失败就卸载重装，进度会随系统备份保留。"
    override val recoveryContentPartialTitle: String get() = "有本书暂时打不开"
    override val recoveryContentPartialBody: String get() =
        "部分书籍的内容文件损坏，已从书架跳过。可以在书城删掉后重新下载，其他书不受影响。"
    override val recoveryRetry: String get() = "重试"
    override val recoveryLater: String get() = "稍后"
    override val recoveryOk: String get() = "知道了"
    override val startReading: String get() = "开始阅读"
    override val continueReading: String get() = "继续阅读"
    override val readUnits: String get() = "已读 %1\$d / %2\$d 单元"
    override val unitCount: String get() = "%d 个单元"
    override val noCards: String get() = "还没有生词卡"
    override val noCardsHint: String get() = "阅读时点按单词，把想记的词收进来。"
    override val delete: String get() = "删除"
    override val cardCollected: String get() = "已在生词卡"
    override val collectCard: String get() = "收进生词卡"
    override val removeCard: String get() = "从生词卡移除"
    override val translation: String get() = "译"
    override val analysisTitle: String get() = "长难句解析"
    override val analysisParse: String get() = "解析"
    override val analysisCore: String get() = "主干"
    override val analysisParts: String get() = "成分"
    override val analysisOrder: String get() = "语序"
    override val analysisNote: String get() = "提示"
    override val analysisClose: String get() = "收起"
    override val analysisRoleSubordinate: String get() = "从句"
    override val analysisRoleRelative: String get() = "定语从句"
    override val analysisRoleNonfinite: String get() = "非谓语"
    override val analysisRolePrepositional: String get() = "介词短语"
    override val analysisRoleAppositive: String get() = "同位语"
    override val analysisRoleParenthetical: String get() = "插入语"
    override val noTranslation: String get() = "本句句译待补"
    override val readingGuide: String get() = "导读"
    override val quiz: String get() = "读后测验"
    override val quizSubmit: String get() = "看结果"
    override val quizScore: String get() = "得分 %1\$d / %2\$d"
    override val quizResult: String get() = "结果"
    override val grammar: String get() = "本段语法"
    override val grammarForm: String get() = "结构"
    override val grammarExplain: String get() = "讲解"
    override val grammarRelated: String get() = "相关条目"
    override val grammarAlsoUses: String get() = "这一句还用到"
    override val grammarPrevious: String get() = "上一条"
    override val grammarNext: String get() = "下一条"
    override val grammarFocus: String get() = "本句"
    override val nextUnit: String get() = "继续下一段"
    override val nextUnitGuide: String get() = "下一段 · 导读"
    override val previousUnitGrammar: String get() = "上一段 · 语法"
    override val finishBook: String get() = "读完全书"
    override val sectionDictionary: String get() = "词典"
    override val fullDictionary: String get() = "完整词表"
    override val dictVersionLabel: String get() = "版本 %s"
    override val dictSizeLabel: String get() = "约 %s"
    override val dictNotInstalled: String get() = "未安装，查词只用随包核心词表"
    override val dictDownloading: String get() = "正在下载 %d%%"
    override val dictPaused: String get() = "已暂停 · 已下载 %s"
    override val dictInstalled: String get() = "已安装，查词先核心后完整"
    override val dictUpdateAvailable: String get() = "有新版本 %s（已装 %s）"
    override val dictHostingPending: String get() = "下载地址尚未上线"
    override val dictNeedsNewerApp: String get() = "这个词表需要更新版本的应用"
    override val dictFailed: String get() = "下载失败：%s"
    override val dictDownload: String get() = "下载"
    override val dictPause: String get() = "暂停"
    override val dictResume: String get() = "继续下载"
    override val dictRetry: String get() = "重试"
    override val dictDelete: String get() = "删除"
    override val dictDeleteConfirm: String get() = "删除后回到随包核心词表；阅读进度与生词卡不受影响。"
    override val dictMeteredTitle: String get() = "当前非 Wi-Fi"
    override val dictMeteredBody: String get() = "完整词表约 %s，继续会消耗移动数据。"
    override val dictContinue: String get() = "继续下载"
    override val dictLicenseNote: String get() = "ECDICT（MIT）；许可与来源在关于页可查。"
    override val shelfReading: String get() = "在读"
    override val shelfFinished: String get() = "已读完"
    override val finishedDate: String get() = "读完于 %s"
    override val readAgain: String get() = "再读一遍"
    override val finishedTitle: String get() = "读完了"
    override val finishedSubtitle: String get() = "一本书读完，下一本还在等你。"
    override val finishedUnitsLabel: String get() = "个单元"
    override val finishedWordsLabel: String get() = "原文词数"
    override val finishedCardsLabel: String get() = "本书收词"
    override val finishedSummary: String get() = "读完总结"
    override val backToShelf: String get() = "返回书架"
    override val browseStore: String get() = "去书城看看"
    override val language: String get() = "界面语言"
    override val languageZh: String get() = "中文"
    override val languageEn: String get() = "English"
    override val definitionMode: String get() = "词义语言"
    override val definitionModeZh: String get() = "中文"
    override val definitionModeMixed: String get() = "中英对照"
    override val definitionModeEn: String get() = "English"
    override val guideMode: String get() = "导读模式"
    override val guideModeBilingual: String get() = "双语带读"
    override val guideModeEn: String get() = "英文带读"
    override val guideModeImmersive: String get() = "沉浸"
    override val toc: String get() = "目录"
    override val tocHint: String get() = "选一个单元开始读，已读的单元也可以回看"
    override val tocCurrent: String get() = "在读"
    override val tocRead: String get() = "已读"
    override val definitionsZh: String get() = "中文释义"
    override val definitionsEn: String get() = "英文释义"
    override val lemmaBase: String get() = "原形"
    override val otherFormSenses: String get() = "该词形另有"
    override val noDefinition: String get() = "词典暂未收录这个词，可以先看它所在句子的意思"
    override val properName: String get() = "人名"
    override val properPlace: String get() = "地名"
    override val properNoun: String get() = "专名"
    override val definitionLanguageFallback: String get() = "当前词义语言下没有这条释义，先显示可用的另一种语言。"
    override val sentenceMeaning: String get() = "本句意思"
    override val reviewWords: String get() = "单词"
    override val reviewGrammar: String get() = "语法"
    override val level: String get() = "CEFR"
    override val frequency: String get() = "词频"
    override val frequencyBandHigh: String get() = "高频"
    override val frequencyBandCommon: String get() = "较常见"
    override val frequencyBandNormal: String get() = "一般"
    override val frequencyBandLow: String get() = "少见"
    override val levelFromWordNet: String get() = "词义对齐"
    override val levelFromRoot: String get() = "由词根推算"
    override val levelFromExam: String get() = "按考试大纲"
    override val levelFromFrequency: String get() = "按词频推算"
    override val occurrences: String get() = "本书出现"
    override val definitions: String get() = "词典释义"
    override val example: String get() = "原句"
    override val collectedCount: String get() = "生词卡 %d"
    override val about: String get() = "关于"
    override val aboutCopy: AboutCopy get() = ZhAboutCopy
    override val syncCopy: SyncCopy get() = ZhSyncCopy
    override val close: String get() = "关闭"
    override val chapter: String get() = "第 %d 章"
    override val unit: String get() = "第 %d 段"
    override val emptyLibrary: String get() = "还没有可读的书"
    override val emptyShelf: String get() = "还没有在读的书"
    override val emptyShelfHint: String get() = "去书城挑一本，开始阅读后就会出现在这里。"
    override val cardRemoved: String get() = "已从生词卡移除"
    override val libraryHint: String get() = "每天读一段，把一本书读完。"
    override val lastRead: String get() = "上次读到"
    override val sectionReading: String get() = "阅读"
    override val sectionAppearance: String get() = "外观"
    override val theme: String get() = "主题"
    override val themeSystem: String get() = "跟随系统"
    override val themeLight: String get() = "浅色"
    override val themeDark: String get() = "深色"
    override val fontSize: String get() = "正文字号"
    override val fontSmall: String get() = "小"
    override val fontMedium: String get() = "标准"
    override val fontLarge: String get() = "大"
    override val fontXLarge: String get() = "特大"
    override val contentLicense: String get() = "内容许可"
    override val annotateHints: String get() = "标注提示词"
    override val annotateHintsHint: String get() = "有书内释义的词和短语加浅色虚线"
    override val profile: String get() = "我的"
    override val myLevel: String get() = "英语水平"
    override val levelUnset: String get() = "未设置"
    override val levelSelf: String get() = "自评"
    override val levelQuick: String get() = "快速测试"
    override val levelFull: String get() = "高级测试"
    override val setLevel: String get() = "设置等级"
    override val changeLevel: String get() = "修改等级"
    override val levelAffectsHint: String get() = "只调整默认界面语言、词义语言与导读方式，不锁内容。"
    override val levelUnsure: String get() = "不确定？先选 A2 或 B1，读完一本再调。"
    override val onboardingTitle: String get() = "先选一个英语水平"
    override val onboardingBody: String get() = "用来设置默认的界面语言、词义语言和导读方式，之后可随时在「我的」修改。"
    override val onboardingStart: String get() = "开始阅读"
    override val onboardingSkip: String get() = "稍后再说"
    override val levelA1Name: String get() = "A1 入门"
    override val levelA1Desc: String get() = "能看懂短句和熟悉的词；读一整段还离不开中文。"
    override val levelA2Name: String get() = "A2 基础"
    override val levelA2Desc: String get() = "能读完一页简单的故事；生词一多就接不上。"
    override val levelB1Name: String get() = "B1 中级"
    override val levelB1Desc: String get() = "能读完一章情节清楚的小说；长句要回读一遍。"
    override val levelB2Name: String get() = "B2 中高级"
    override val levelB2Desc: String get() = "能读完一本通俗小说；偶尔被一种说法绊住。"
    override val levelC1Name: String get() = "C1 高级"
    override val levelC1Desc: String get() = "能读文学原著；难在言外之意，不在单词。"
    override val levelC2Name: String get() = "C2 精通"
    override val levelC2Desc: String get() = "能读几乎任何英文；只在意写得好不好。"
    override val levelPromptTitle: String get() = "按 %s调整阅读模式"
    override val levelPromptBody: String get() = "选这个等级会改动下面这些设置。你可以接受推荐、保持原来的设定，或现在自己挑。"
    override val levelPromptAccept: String get() = "接受推荐"
    override val levelPromptKeep: String get() = "保持原来的设定"
    override val levelPromptAdjust: String get() = "现在自己调整"
    override val levelPromptApply: String get() = "应用"
    override val levelPromptBack: String get() = "返回推荐"
    override val levelPromptHint: String get() = "只调整读法，不锁内容；以后可在设置里随时改。"
    override val levelVocabLine: String get() = "词汇量参考 %s"
    override val levelA1Vocab: String get() = "1,000 以内"
    override val levelA2Vocab: String get() = "1,000–2,000"
    override val levelB1Vocab: String get() = "2,000–4,000"
    override val levelB2Vocab: String get() = "4,000–6,000"
    override val levelC1Vocab: String get() = "6,000–10,000"
    override val levelC2Vocab: String get() = "10,000 以上"
    override val booksFinished: String get() = "读完 %d 本"
    override val booksFinishedShort: String get() = "读完"
    override val unitsReadTotal: String get() = "已读 %1\$d / %2\$d 单元"
    override val unitsReadShort: String get() = "已读单元"
    override val readingDays: String get() = "阅读 %d 天"
    override val readingDaysShort: String get() = "阅读天数"
    override val grammarPoint: String get() = "点"
    override val grammarExamples: String get() = "例"
    override val grammarPitfalls: String get() = "易错"
    override val grammarBookExample: String get() = "书内原句"
    override val quizCorrect: String get() = "答对"
    override val quizWrong: String get() = "答错"
    override val quizSeeSentence: String get() = "看原句"
    override val quizHideSentence: String get() = "收起原句"
    override val quizExitTitle: String get() = "还没提交测验"
    override val quizExitBody: String get() = "现在返回会丢失已选答案。"
    override val quizExitConfirm: String get() = "退出"
    override val quizStay: String get() = "继续答题"
    override val more: String get() = "更多"
    override val wordDetails: String get() = "详情"
    override val pronounce: String get() = "朗读"
    override val pronounceComing: String get() = "朗读将在后续版本提供"
    override val tabShelf: String get() = "书架"
    override val tabStore: String get() = "书城"
    override val tabReview: String get() = "复习"
    override val tabMe: String get() = "我的"
    override val store: String get() = "书城"
    override val storeHint: String get() = "内置书直接可读；从右上角菜单导入书籍包或添加书源。"
    override val builtIn: String get() = "已内置"
    override val importBook: String get() = "导入书籍"
    override val importComing: String get() = "导入 .pagelence 包（即将支持）"
    override val viewInShelf: String get() = "在书架查看"
    override val readNow: String get() = "立即阅读"
    override val bookGrammar: String get() = "语法复习"
    override val grammarEmpty: String get() = "语法内容即将上线"
    override val grammarAll: String get() = "语法全书"
    override val grammarPointsCount: String get() = "%d 个语法点"
    override val openUnit: String get() = "回到本单元"
    override val pageGuide: String get() = "导读"
    override val pageText: String get() = "正文 %1\$d / %2\$d"
    override val pageQuiz: String get() = "测验 %1\$d / %2\$d"
    override val pageGrammar: String get() = "语法"
    override val pageReview: String get() = "回顾"
    override val nextPage: String get() = "下一页"
    override val previousPage: String get() = "上一页"
    override val scrollHint: String get() = "本页较长，向下滑动"
    override val scrollEnd: String get() = "本页结束"
    override val quizNextQuestion: String get() = "下一题"
    override val quizFeedbackGeneric: String get() = "答案已记录。"
    override val quizUnanswered: String get() = "还有 %d 题未答"
    override val speechRate: String get() = "朗读语速"
    override val speechSlow: String get() = "慢"
    override val speechFast: String get() = "快"
    override val speechEngineMissing: String get() = "没有可用的英文语音引擎，请在系统设置中安装或选择英文语音。"
    override val speechNotReady: String get() = "语音引擎正在准备，请稍后再试。"
    override val assessment: String get() = "语言水平测评"
    override val assessmentIntro: String get() = "用一组小题估计当前阅读水平，结果只作参考，不影响可读内容。"
    override val assessmentQuick: String get() = "快速测试"
    override val assessmentQuickMeta: String get() = "12–15 题 · 约 5 分钟"
    override val assessmentQuickBody: String get() = "以词汇和语法为主，覆盖当前等级和相邻两级，给出等级区间。"
    override val assessmentFull: String get() = "高级测试"
    override val assessmentFullMeta: String get() = "30–40 题 · 15–20 分钟"
    override val assessmentFullBody: String get() = "增加一篇短文阅读，按词汇、语法和阅读分项给出结果。"
    override val assessmentStart: String get() = "开始测试"
    override val assessmentQuestion: String get() = "第 %1\$d / %2\$d 题"
    override val assessmentNext: String get() = "下一题"
    override val assessmentSeeResult: String get() = "看结果"
    override val assessmentSkip: String get() = "跳过此题"
    override val assessmentSkipHint: String get() = "不确定的题可以跳过，跳过计为未答对。"
    override val assessmentSkipped: String get() = "此题已跳过，计为未答对。"
    override val assessmentResult: String get() = "测评结果"
    override val assessmentReference: String get() = "参考评级"
    override val assessmentBand: String get() = "参考区间 %s"
    override val assessmentSingle: String get() = "建议等级 %s"
    override val assessmentScore: String get() = "答对 %1\$d / %2\$d 题"
    override val assessmentAccept: String get() = "接受新等级"
    override val assessmentKeep: String get() = "保留原级"
    override val assessmentAccepted: String get() = "已更新为 %s，阅读辅助的默认值已同步。"
    override val assessmentKept: String get() = "保留原等级，可随时重测。"
    override val assessmentSkillVocab: String get() = "词汇"
    override val assessmentSkillGrammar: String get() = "语法"
    override val assessmentSkillReading: String get() = "阅读"
    override val assessmentHint: String get() = "测评只调整默认的词义语言与导读方式，不锁内容，可随时重测。"
    override val assessmentUnavailable: String get() = "题库暂不可用，请稍后再试。"
    override val storeBuiltInTitle: String get() = "内置书"
    override val storeBuiltInHint: String get() = "随应用安装，可直接阅读；未完成的书会标注制作中。"
    override val storeFilterAll: String get() = "全部"
    override val storeComingSoon: String get() = "制作中"
    override val storeComingSoonHint: String get() = "正文与学习标注仍在制作，完成后随更新提供。"
    override val storeCustomCatalogTitle: String get() = "自定义源书目"
    override val storeRefreshCustom: String get() = "刷新自定义源"
    override val storeRefresh: String get() = "刷新目录"
    override val storeStatusUpdated: String get() = "目录已更新 · %s"
    override val storeStatusCached: String get() = "使用缓存目录 · %s"
    override val storeEmptyCatalog: String get() = "暂无可下载的书"
    override val storeEmptyHint: String get() = "已安装的书在书架里；添加自定义源后可在这里看到它提供的书目。"
    override val storeCustomSources: String get() = "自定义源"
    override val storeAddSource: String get() = "添加源"
    override val storeSourceName: String get() = "名称（可选）"
    override val storeSourceUrl: String get() = "目录地址（HTTPS）"
    override val storeAdd: String get() = "添加"
    override val storeLocalHttp: String get() = "允许局域网 HTTP（仅调试）"
    override val storeLocalHttpHint: String get() = "只对本机与 10.0.2.2 生效。内容由源提供，应用不审核。"
    override val storeDownload: String get() = "下载"
    override val storeUpdate: String get() = "更新"
    override val storeInstalled: String get() = "已安装"
    override val storeRemove: String get() = "移除"
    override val storeDownloading: String get() = "下载中…"
    override val storeDownloaded: String get() = "已下载，书架里可直接读"
    override val storeSourcesCount: String get() = "%d 个来源"
    override val storeChooseSource: String get() = "选择下载来源"
    override val storeInstalledTitle: String get() = "已下载"
    override val storeSize: String get() = "约 %s"
    override val storePartialError: String get() = "部分源不可达，先显示可用目录。"
    override val storeErrorNetwork: String get() = "网络不可达或超时，换个线路或稍后重试。"
    override val storeErrorHttp: String get() = "服务器返回 HTTP %d。"
    override val storeErrorHttps: String get() = "只允许 HTTPS 地址（或在下方打开局域网 HTTP）。"
    override val storeErrorTooLarge: String get() = "文件超过大小限制，已拒绝。"
    override val storeErrorBadCatalog: String get() = "目录格式不对：%s"
    override val storeErrorBadPackage: String get() = "书籍包有问题：%s"
    override val storeErrorChecksum: String get() = "校验失败，文件可能不完整。"
    override val storeSourceInvalid: String get() = "地址必须是 HTTPS（或已打开局域网 HTTP）。"
    override val storeBadgeBuiltIn: String get() = "内置"
    override val storeMenu: String get() = "书城菜单"
    override val storeImportLocal: String get() = "导入本地书籍包"
    override val storeImportOnline: String get() = "导入线上书籍包"
    override val storeImportOnlineTitle: String get() = "导入线上书籍包"
    override val storeImportOnlineHint: String get() = "填入 .pagelence 书籍包的 HTTPS 链接。只下载这一本，不会保存为书源；内容由来源提供，应用不审核。"
    override val storeImportUrl: String get() = "书籍包链接（HTTPS）"
    override val storeImport: String get() = "导入"
    override val storeImporting: String get() = "正在导入…"
    override val storeImported: String get() = "已导入《%s》，书架里可直接读"
    override val storeImportedLocal: String get() = "本地导入"
    override val storeImportedOnline: String get() = "线上导入"
    override val storeErrorBuiltIn: String get() = "这本书已随应用内置，无需再导入。"
    override val storeErrorFile: String get() = "无法读取所选文件，请确认它是 .pagelence 书籍包后重试。"
    override val bookDetail: String get() = "书籍详情"
    override val bookWords: String get() = "约 %s 词"
    override val bookCategory: String get() = "分类"
    override val bookTags: String get() = "标签"
    override val bookCreator: String get() = "制作者"
    override val labelSeparator: String get() = "："
    override val bookIntro: String get() = "内容简介"
    override val bookChapters: String get() = "章节目录"
    override val bookChapterUnits: String get() = "%d 个单元"
    override val addToShelf: String get() = "加入书架"
    override val onShelf: String get() = "已在书架"
    override val bookPlannedHint: String get() = "这本书还在制作中，暂时不能阅读。"
    override val bookNoChapters: String get() = "章节目录制作中"
    override val bookAbout: String get() = "关于此书"
    override val bookDifficulty: String get() = "难度"
    override val bookWordsLabel: String get() = "词汇量"
    override val bookInfoTitle: String get() = "详细信息"
    override val bookInfo: String get() = "图书信息"
    override val bookAuthor: String get() = "作者"
    override val bookWordCount: String get() = "单词量"
    override val bookLevelLabel: String get() = "等级"
    override val bookLicense: String get() = "授权信息"
    override val bookLicenseSource: String get() = "原书授权"
    override val bookLicenseCreator: String get() = "制作授权"
    override val bookPublish: String get() = "出版信息"
    override val bookLanguage: String get() = "语言"
    override val bookLanguageEn: String get() = "英语"
    override val bookChaptersCount: String get() = "章节"
    override val bookUnitsCount: String get() = "单元"
    override val bookInfoEmpty: String get() = "暂无"
    override val bookSourceBuiltIn: String get() = "内置"
    override val bookSources: String get() = "来源"
    override val bookChaptersAfterDownload: String get() = "下载后可查看章节目录"
    override val heatmap: String get() = "学习热力图"
    override val heatmapEmpty: String get() = "还没有学习记录，读完一个单元就会出现在这里。"
    override val heatmapHint: String get() = "按天记录读完的单元数与阅读天数，最多保留 365 天；只在本机展示，不推送、不惩罚断签。"
    override val heatmapLess: String get() = "少"
    override val heatmapMore: String get() = "多"
    override val heatmapSummary: String get() = "阅读 %1\$d 天 · 完成 %2\$d 个单元"
    override val readingLayout: String get() = "阅读方式"
    override val layoutPaged: String get() = "翻页"
    override val layoutScroll: String get() = "上下滑动"
    override val preview: String get() = "预习"
    override val previewTitle: String get() = "预习本段词汇"
    override val previewPrompt: String get() = "认识这个词吗？"
    override val previewKnown: String get() = "认识"
    override val previewUnknown: String get() = "不认识"
    override val previewNext: String get() = "下一个"
    override val previewFinish: String get() = "开始阅读"
    override val previewEmpty: String get() = "本段没有高于本书等级的词汇。"
    override val previewProgress: String get() = "%1\$d / %2\$d"
    override val previewLevelHint: String get() = "高于本书（%1\$s）"
    override val previewInCards: String get() = "已在生词卡"
    override val previewAdded: String get() = "已加入生词卡"
    override val previewKnownTag: String get() = "认识"
    override val previewUnknownTag: String get() = "不认识"
    override val previewDone: String get() = "预习完成"
    override val previewDoneHint: String get() = "这些词会在正文里再遇到，读到时可以再确认。"
    override val enterQuiz: String get() = "进入测验"
    override val textProgressLabel: String get() = "%1\$d / %2\$d"
    override val progressGuide: String get() = "导读"
    override val progressText: String get() = "正文"
    override val progressQuiz: String get() = "测验"
    override val progressGrammar: String get() = "语法"
    override val studyWords: String get() = "背单词"
    override val studyStart: String get() = "开始背单词"
    override val studyPrompt: String get() = "先回忆词义，再显示答案"
    override val studyShowAnswer: String get() = "显示答案"
    override val studyNext: String get() = "下一个"
    override val studyPrevious: String get() = "上一个"
    override val studyExit: String get() = "退出背单词"
    override val studyDone: String get() = "这一轮完成"
    override val studyProgress: String get() = "%1\$d / %2\$d"
    override val studyCards: String get() = "%d 张卡片"
    override val annotations: String get() = "书签与笔记"
    override val annotationTitle: String get() = "标注"
    override val annotationBookmark: String get() = "书签"
    override val annotationHighlight: String get() = "划线"
    override val annotationNote: String get() = "笔记"
    override val annotationAddBookmark: String get() = "加书签"
    override val annotationRemoveBookmark: String get() = "取消书签"
    override val annotationAddHighlight: String get() = "划线"
    override val annotationRemoveHighlight: String get() = "取消划线"
    override val annotationAddNote: String get() = "写笔记"
    override val annotationEditNote: String get() = "编辑笔记"
    override val annotationDeleteNote: String get() = "删除笔记"
    override val annotationNoteHint: String get() = "写下这一句给你的想法、疑问或联想…"
    override val annotationSave: String get() = "保存"
    override val annotationCancel: String get() = "取消"
    override val annotationEmptyTitle: String get() = "还没有书签或笔记"
    override val annotationEmptyBody: String get() = "长按正文里的句子，就能加书签、划线或写笔记。"
    override val annotationEmptyHint: String get() = "长按正文里的句子开始标注。"
    override val annotationEmptyFilter: String get() = "没有符合条件的标注"
    override val annotationCount: String get() = "%d 条"
    override val annotationAnchorMoved: String get() = "原句有更新"
    override val annotationFilterAll: String get() = "全部"
    override val reviewNotes: String get() = "笔记"
}

private val ZhAboutCopy: AboutCopy = AboutCopy(
    title = "关于",
    tagline = "本地优先的英文原著阅读器：每天五分钟读一段，一本接一本地读完原版书；进度只留在本机。",
    settingsSubtitle = "版本 v%s · 隐私与数据 · 开源许可",
    versionFormat = "v%s (%d)",
    checkUpdate = "检查更新",
    checkUpdateSubtitle = "当前 %s · 开发版",
    website = "官网",
    github = "GitHub 仓库",
    privacy = "隐私与数据",
    privacySubtitle = "数据、权限与联网",
    licenses = "开源许可",
    licensesSubtitle = "本应用与第三方许可",
    pendingLink = "地址待公开",
    linkUnavailableTitle = "尚未公开",
    openFailedTitle = "无法打开链接",
    openFailedBody = "请稍后重试，或在浏览器中手动打开。",
    linkUnavailableBody = "%s还没有公开地址；公开后会从这里直接打开。",
    updateTitle = "当前为开发版",
    updateBody = "当前版本是 %s。新版本发布在 GitHub Releases，应用不会在后台自动检查更新。",
    privacySections = listOf(
        AboutSection(
            title = "数据保存在哪里",
            paragraphs = listOf(
                "阅读进度、生词卡与阅读偏好只保存在这台设备的应用私有目录，主体是单文件 state.json；" +
                    "你自己下载的书籍包也放在同一私有目录。应用没有账号体系，也没有开发者服务器，不会收到你的阅读记录。",
                "卸载应用或清除应用数据会删除这些内容。Android 系统备份是否包含应用数据，由你在系统设置里决定。",
            ),
        ),
        AboutSection(
            title = "应用权限",
            paragraphs = listOf(
                "应用只申请网络权限与网络状态权限：前者用于书源与可选完整词表，后者只判断当前是不是 Wi-Fi，" +
                    "下载前提示而不读取网络内容。不申请存储、相机、位置、通讯录或通知权限。",
                "不联网也能读书：两本内置书随安装包提供，查词与阅读进度都在本机完成。",
            ),
        ),
        AboutSection(
            title = "会联网的场景",
            paragraphs = listOf(
                "只有在你点「刷新目录」或「下载」时，才会向你填写的书源地址发起 HTTPS 请求。" +
                    "请求会带上该书源地址，不会上传阅读进度或生词卡。",
                "默认书源在托管上线前不显示，也不会被请求；「检查更新」目前不联网。",
            ),
        ),
        AboutSection(
            title = "不收集什么",
            paragraphs = listOf(
                "没有广告、统计埋点或崩溃上报 SDK；不收集设备标识符、位置或使用行为。",
            ),
        ),
        AboutSection(
            title = "你的控制",
            paragraphs = listOf(
                "可以在系统设置 → 应用 → 页有所成 → 存储 中清除应用数据，或在应用内删除生词卡；" +
                    "卸载应用会一并删除本机数据。",
                "本页是应用内的说明，不是正式法律文本；正式条款将在公开仓库发布。",
            ),
        ),
    ),
    licenseSections = listOf(
        AboutSection(
            title = "本应用",
            paragraphs = listOf(
                "源代码以 Apache License 2.0 授权：可自由使用、修改与再分发，需保留版权与许可声明。" +
                    "项目名「Pagelence · 页有所成」与图标不随代码授权，衍生版本请改名。",
                "项目自制的导读、句译、测验、语法讲解与界面文案以 CC BY-SA 4.0 授权；" +
                    "书籍正文与第三方数据不适用这两个许可。",
            ),
        ),
        AboutSection(
            title = "第三方组件",
            paragraphs = listOf(
                "Kotlin、kotlinx.coroutines、kotlinx.serialization、AndroidX、Jetpack Compose、" +
                    "OkHttp：" +
                    "Apache License 2.0。",
                "Material Icons：Apache License 2.0。Android SDK 与 Android 运行库按其各自条款提供。",
            ),
            bullets = true,
        ),
        AboutSection(
            title = "第三方数据",
            paragraphs = listOf(
                "ECDICT（固定版本 39cfb96）：MIT，英汉释义与音标。",
                "Princeton WordNet 3.0：WordNet License。",
                "CEFR-J Wordlist v1.6：研究、教育、商用免费，按官方格式署名。",
                "Octanove Vocabulary Profile C1/C2 1.0：CC BY-SA 4.0。",
                "CEFR-Annotated WordNet（2025）：标注 CC BY 4.0，义项键随 WordNet 3.0 许可；只作构建输入，不随包分发。",
                "wordfreq 3.0.2：CC BY-SA 4.0，词频数据。",
                "词级五层衍生表与本书出现次数：项目自制，CC BY-SA 4.0。",
            ),
            bullets = true,
        ),
        AboutSection(
            title = "书籍正文",
            paragraphs = listOf(
                "《怪医杜立德》：Project Gutenberg #501，1920 年版，公有领域。",
                "《爱丽丝漫游仙境记》：Wikisource 转录的 Macmillan 1866 年印本，正文公有领域；" +
                    "Tenniel 插图未随包分发。",
            ),
            bullets = true,
        ),
        AboutSection(
            title = "说明",
            paragraphs = listOf(
                "本页只列主要许可摘要。完整许可文本、NOTICE 与第三方数据清单，将在公开仓库发布时一并提供。",
            ),
        ),
    ),
)
