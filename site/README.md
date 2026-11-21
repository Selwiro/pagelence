# 官网与截图托管

`site/` 是 [selwiro.github.io/pagelence](https://selwiro.github.io/pagelence/) 的源文件：手写 HTML/CSS，没有前端框架、外部字体、统计脚本或 Cookie。

## 本地预览

```sh
site/build.sh
python3 -m http.server -d site/.build/pages 8000
# 打开 http://localhost:8000
```

## 发布

```sh
site/deploy.sh
```

脚本会把 `site/` 构建到 `site/.build/pages/`，再提交到 `gh-pages` 分支根目录并推送。GitHub Pages 的来源是 `gh-pages` 分支根目录。三张截图（`library.png`、`reader.png`、`word-panel.png`）在构建时从 `docs/screenshots/` 复制到站点根目录，README 用完整 URL 引用它们。

## 约定

- 页面文案与截图都以 `README.md` 和 `docs/app-phone.md` 为准，改动功能说明时两边一起改。
- 截图仍只在 `docs/screenshots/` 维护，保持三张必要图；本站不额外上传验收截图。
- 下载按钮指向 GitHub Release 的固定 tag 资产；发新版时同时更新页面上的版本、大小与 SHA-256。
- 不引入外部字体、CDN、统计或 Cookie；页面在断网和纯静态托管下都能完整显示。
