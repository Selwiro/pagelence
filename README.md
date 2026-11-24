# 页有所成官网（gh-pages）

本分支既是 GitHub Pages 的发布源，也是官网的源文件：根目录的 `index.html`、`styles.css`、`icon.svg`、`robots.txt`、`sitemap.xml` 与三张 PNG 由 Pages 原样输出。官网只在本分支维护，`main` 不保留副本。

## 维护

- 直接编辑本分支文件并提交；推送后 Pages 会自动构建。
- 截图固定三张：`library.png`（书架）、`reader.png`（正文分页）、`word-panel.png`（点词词义），用同名文件覆盖提交。其余验收截图留在本机，不入库。
- 下载按钮指向 `main` 仓库的固定 Release：普通版 `https://github.com/Selwiro/pagelence/releases/download/v0.2.0/pagelence-0.2.0-release.apk`，墨水屏版 `https://github.com/Selwiro/pagelence/releases/download/v0.2.0/pagelence-eink-0.2.0-release.apk`；发新版时同步更新页面上的版本、大小与两个 SHA-256。
- 不引入外部字体、CDN、统计脚本或 Cookie；页面需在纯静态托管下完整可用。
- 改版后至少检查 1200 / 768 / 390 三个宽度无横向溢出，并确认 `https://selwiro.github.io/pagelence/` 与三张截图都返回 200。
