# 书源与下载

状态：2026-11-19 实现，同日调整书城首屏。默认源的托管地址尚未上线，`DefaultBookSource.AVAILABLE`
为 `false`：界面不显示、打开书城也不请求；自定义源可以完整走通拉目录、下载、校验、安装与移除。
格式已于 2026-11-20 按包格式 2 / 内容格式 3 与远程书目 catalog 规范更新，字段定义以本文与
`core` 的 `SourceModels` 为准，内容格式见 [`docs/content-format.md`](content-format.md)。2026-11-20 已把目录格式与 `CatalogRules` 升到 2；书包manifest 仍是 1，等书籍包迁移。
2026-11-21 在书城右上角菜单增加「导入本地书籍包」「导入线上书籍包」与「添加源」三个入口，
本地/线上导入走同一套包校验与安装流程（见第九节）。

## 一、三层来源

| 层 | 内容 | 网络 | 状态 |
| --- | --- | --- | --- |
| 自带书籍 | 随 APK 分发，`content/books/`；书城清单在 `content/store.json` | 不需要 | 五档计划书目，已随包两本（杜立德、爱丽丝），其余三本标「制作中」 |
| 默认源 | 项目维护的静态目录 + 书包 | 直连 GitHub 或镜像 | 格式与线路逻辑已完成；托管上线前隐藏且不请求 |
| 自定义源 | 用户填写的静态目录 URL | 用户触发 | 已完成，默认关闭，需手动添加 |

默认源与自定义源使用同一套 `catalog.json` 与 `.pagelence` 格式；应用不抓取网页，也不支持
「规则源」。

## 二、`catalog.json`

```jsonc
{
  "schemaVersion": 2,
  "catalogVersion": "2026-11-20",
  "updatedAt": "2026-11-20",
  "books": [
    {
      "id": "alice-1866",
      "title": { "zh": "爱丽丝漫游仙境记", "en": "Alice's Adventures in Wonderland" },
      "author": "Lewis Carroll",
      "level": "B1",
      "language": "en",
      "description": { "zh": "…", "en": "…" },
      "sizeBytes": 245760,
      "sha256": "…64 hex…",
      "url": "https://…/alice-1866.pagelence",
      "sourceType": "public-domain",
      "publicDomain": ["CN", "US"],
      "creatorType": "CC-BY-SA-4.0",
      "creatorUrl": "https://creativecommons.org/licenses/by-sa/4.0/",
      "minAppVersionCode": 1
    }
  ]
}
```

字段定义与校验以本节与 `core` 的 `CatalogRules` / `SourceModels` 为准；包内
`manifest.licenses` 是权威，目录只做下载前预筛与展示。

校验规则（`CatalogRules.validate`）：

- `schemaVersion` 必须是 2；最多 200 本；
- `id`、`title`、`url` 非空，`id` 不重复；
- `url` 必须 HTTPS（局域网调试开关只放行 `localhost`、`127.*`、`10.0.2.2`）；
- `sha256` 必须是 64 位十六进制，`sizeBytes` 正数且不超过 8 MB；
- `minAppVersionCode` 高于当前版本的书不展示；
- 只支持英文书（`language = en`）；
- `sourceType` 取 `public-domain` / `permission` / `original` / SPDX；`public-domain` 必须带
  非空 `publicDomain`，全球公版也列出分发地区（当前 CN、US），不用空数组表示全球；
- `creatorType` 取 SPDX / `custom` / `all-rights-reserved`；`custom` / `all-rights-reserved`
  必须带 `creatorName`；
- 默认源与自定义源都按目录的 `publicDomain` 与目标地区（CN、US）预筛：`public-domain`
  必须与目标地区相交，`permission` / `original` / SPDX 按发布展示；安装后以包
  `licenses.source.publicDomain` 复核。

## 三、`.pagelence` 书包

ZIP，包格式 2 / 内容格式 3（见 [`docs/content-format.md`](content-format.md)）：

```
manifest.json          { schemaVersion: 2, bookId, title, author, creator, level, language,
                         version, wordCount, contentStatus, minAppVersionCode, updatedAt,
                         licenses: { source, creator } }
book.json              content-format v3 的一本书（与内置书同 schema）
LICENSE-SOURCE.txt     可选，原书许可全文
LICENSE-CREATOR.txt    可选，制作者许可全文
cover.jpg              可选，封面图
```

读取规则（`BookPackageReader`）：

- 只允许上述五个名字，拒绝目录项与其他条目；旧包的 `LICENSE` 只做迁移读，新包不再写；
- 拒绝绝对路径、`..`、反斜杠；条目数 ≤ 8，单条 ≤ 6 MB，解压总量 ≤ 12 MB；
- `manifest.bookId` 必须等于目录条目与 `book.json.id`；
- `cover.jpg` 若不是 JPEG 或超过 512 KB，整包拒绝；
- 解包后再跑 `ContentValidator`，任何错误都不落库；警告放行并记入缺口。

## 四、下载与安装

1. 下载前检查 `sizeBytes` 与 HTTPS；
2. 边读边限长，超过 8 MB 直接判失败；
3. 对整包做 SHA-256，与目录条目比对；不一致即拒绝，不写盘；
4. 解包、校验 schema 与 book 结构；
5. 原子写入 `filesDir/downloaded/books/<bookId>/`，再生成
   `filesDir/downloaded/downloaded-books.json`，让 `FileContentRepository` 作为第二内容根读取；
6. 书架与书城立即刷新，已下载书与内置书同权，可正常点词、句译、测验与记进度。

更新：目录里的 `sha256` 与已安装记录不同时显示「更新」；换线路不重复下载——摘要相同
视为同一本。移除：删除目录并重建清单，`state.json` 里的进度保留（重装后仍可续读）。

## 五、线路与缓存

- 默认源支持 `自动 / 直连 / 镜像`；自动模式先试上次成功的线路，再试另一条；
- 目录请求带 `If-None-Match`，304 复用缓存；缓存按 URL 存于 `filesDir/source-cache/`；
- 超时 10 秒连接 / 20 秒读取，最多 3 次重定向；
- 所有失败都有可读原因：网络不可达、HTTP 状态码、非 HTTPS、体积超限、目录/书包格式错误、
  校验失败。多个源同时失败时优先显示内容类错误。

## 六、自定义源

- 只允许 HTTPS；打开「允许局域网 HTTP（仅调试）」后额外放行 `localhost`、`127.*`、`10.0.2.2`；
- 添加后默认启用，可随时停用或删除；同一 `bookId` 出现在多个源时，下载前弹出「选择下载来源」；
- 应用不审核源内容；源站会看到请求地址与时间，隐私说明写明。

## 七、权限与隐私

- 新增 `android.permission.INTERNET`；应用只在用户点「刷新目录」「下载」「更新」或
  「导入线上书籍包」时联网，没有后台同步、埋点或账号；
- `network_security_config` 默认禁止明文 HTTP，仅对 `localhost`、`127.0.0.1`、`10.0.2.2`
  放行，供本机调试；
- 下载的书只存在应用私有目录；源地址、下载记录与错误不会上传任何服务器；
- 留在 `state.json` 的进度仍按原规则本地保存，导出与 WebDAV 尚未实现。

## 八、当前托管状态与书城首屏

`DefaultBookSource.AVAILABLE = false`，书城因此不显示默认源卡片，也不会在打开书城时请求
占位地址。两个地址仍保留为项目约定：

- 直连：`https://raw.githubusercontent.com/pagelence/books/main/catalog.json`
- 镜像：`https://cdn.jsdelivr.net/gh/pagelence/books@main/catalog.json`

仓库上线后把 `AVAILABLE` 改为 `true` 即可恢复线路选择、刷新与目录错误提示；目录与书包格式
不再变。首屏的内置书架来自 `content/store.json`，按等级筛选，是否可读以
`content/books-index.json` 实际列出的包与包内 `manifest.json` 为准，避免「清单有、包体没有」。

## 九、导入书籍包

书城右上角菜单收三种入口：**导入本地书籍包**、**导入线上书籍包**、**添加源**。前两项只装一本
书，不新增书源；第三项仍是原来的自定义源对话框。

- **本地导入**：用系统文件选择器（`ActivityResultContracts.OpenDocument`）选任意文件，应用自己
  读内容，不需要存储权限；读取时只缓冲到 8 MB 上限，超出即拒绝。
- **线上导入**：填入 `.pagelence` 包的直链（HTTPS；打开「允许局域网 HTTP」后额外放行
  `localhost`、`127.*`、`10.0.2.2`），只下载这一本，不把 URL 保存为源。
- 两条路径共用 `SourceManager.importPackage`：先 `BookPackageReader` 解包并校验 manifest/book，
  再跑 `ContentValidator`，任一错误都不落盘；成功后原子安装到
  `filesDir/downloaded/books/<bookId>/`，刷新内容清单、书城与书架。
- 已安装列表的来源标签分别为「本地导入」「线上导入」；同一 `bookId` 再次导入按更新处理，
  `state.json` 里的阅读进度保留。
- 与随包内置书同 `bookId` 的包会被拒绝（`SourceError.AlreadyBuiltIn`），避免下载副本遮蔽内置书
  与它的进度；内置书列表由 `AppGraph.shippedBookIds` 从种子内容清单读取。
- 错误沿用书城已有的可读文案：网络、HTTP 状态码、非 HTTPS、体积超限、包格式/内容校验失败、
  文件不可读；导入过程中顶栏显示进度并暂时禁用菜单。
- 走查使用 1 章 1 段的临时小包（不入库）：合法包用于本地/线上导入，`builtin-alice` 验证内置
  id 冲突，非 zip 文件验证包格式错误；正式书籍包仍按 [`docs/content-format.md`](content-format.md)
  生成。
