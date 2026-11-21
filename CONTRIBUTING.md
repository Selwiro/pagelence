# 参与贡献

## 许可

- 代码与构建脚本以 [Apache-2.0](LICENSE) 授权。
- 项目自制的阅读内容（导读、句译、测验、语法讲解、界面文案、单元切分与提示标注）以 [CC BY-SA 4.0](LICENSE-CONTENT) 授权。
- 书籍正文底本与第三方数据（ECDICT、WordNet、CEFR-J、Octanove、wordfreq）沿用各自条款，见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。

提交代码即表示同意以 Apache-2.0 授权该贡献；提交内容即表示同意以 CC BY-SA 4.0 授权该贡献。

## 贡献者证明（DCO）

本项目使用 Developer Certificate of Origin，不使用 CLA。提交时加签：

```sh
git commit -s -m "feat(reader): ..."
```

签名表示你有权提交该内容，并同意上述授权。

## 提交规范

- 提交信息用 `type(scope): summary`，例如 `feat(core): 补词形还原`、`fix(phone): 修复翻页残留`、`docs(content): 校订第一段句译`。
- scope 取 `core`、`data`、`ui`、`phone`、`content`、`tools`、`docs`。
- 一次提交只做一件事；内容改动与代码改动分开。
- 改动后至少走查对应阅读流程，并在提交信息里写验证方式。

## 本地验证

提交 Pull Request 前，在仓库根目录至少运行：

```sh
./gradlew :core:test :data:test
./gradlew :app-phone:assembleDebug
python3 tools/validate/validate_content.py
```

内容脚本改动后，跑一次完整门禁：

```sh
ECDICT_CSV=/path/to/ecdict.csv tools/ci_check.sh
```

`content/` 是脚本产物，不手工编辑；改内容请改 `content-src/` 后重新生成，并让
`--check` 保持通过。

## 内容贡献

- 只提交已核对公版状态的正文；在 `source` 与 `publicDomain` 字段记录版本、来源与适用地区。
- 不提交来源不明、翻译版权未清或从其他应用抓取的文本。
- 导读、句译与语法讲解按 [`docs/content-format.md`](docs/content-format.md) 的体例撰写，保留原文语境；新增或修改后需人工校订。
- 单元区间冻结在 `content-src/<book>/meta.json`，重建不重算边界。
- 不提交个人数据、凭据、签名文件或 WebDAV 账号信息。

## 截图与文档图片

截图**不提交到默认分支**，只放少数必要的图，避免仓库被二进制文件撑大：

1. 目前只维护三张：`library.png`（书架）、`reader.png`（正文分页）、`word-panel.png`（点词词义）。
2. 图只提交到 `gh-pages` 分支根目录，文件名保持不变；本机留档可放 `docs/screenshots/`（已被 `.gitignore` 排除），不进 `main`。
3. 仓库的 GitHub Pages 来源指向 `gh-pages` 分支根目录，README 与官网用 `https://<owner>.github.io/<repo>/<name>.png` 引用。

换图时在 `gh-pages` 分支用同名文件覆盖提交，文档无需改动。截图前清掉状态栏通知与个人内容；
其余验收截图留在本地，不进仓库也不上传。

## 报告问题与安全

- 一般问题与建议走 GitHub Issues，附版本号（「设置 → 关于」）、设备与复现步骤。
- 安全问题不要公开提 Issue，请通过仓库主页的私下渠道联系维护者。
- 当前为 0.1.0 开发版，签名包通过 [GitHub Releases](https://github.com/Selwiro/pagelence/releases) 分发；从源码构建见 [README](README.md)。
