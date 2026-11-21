# 发布签名

2026-11-21 起，本工程有一套本机发布签名，可构建带签名的 release 包。密钥库不进仓库，口令只写在本机、被 `.gitignore` 排除的 `keystore.properties` 里。

## 密钥库

| 项目 | 值 |
| --- | --- |
| 位置 | `~/.pagelence/pagelence-release.jks`（仓库外，目录权限 700、文件 600） |
| 格式 | PKCS12，RSA 4096，`SHA256withRSA` |
| 别名 | `pagelence` |
| 主题 | `CN=Pagelence, OU=Android, O=Pagelence, L=Shanghai, ST=Shanghai, C=CN` |
| 有效期 | 10,000 天（自 2026-11-21 起，约至 2054 年） |
| 证书 SHA-256 | `71:65:93:8F:8A:BC:6A:84:EC:47:3A:FF:EE:36:41:1A:FD:FD:D8:51:F8:64:ED:CB:2B:27:91:CC:AF:A3:25:9C` |
| 签名方案 | v2 + v3；不启用 v1（minSdk 26，v1 已无必要） |

## 配置怎么接进来

`keystore.properties`（仓库根目录，已在 `.gitignore` 排除）提供四个键，`storeFile` 写本机绝对路径：

```properties
storeFile=/home/<you>/.pagelence/pagelence-release.jks
storePassword=…
keyAlias=pagelence
keyPassword=…
```

[`app-phone/build.gradle.kts`](../app-phone/build.gradle.kts) 在配置阶段读取它：

- 有密钥库 → 创建 `signingConfigs.release` 并绑到 `buildTypes.release`，构建时打印一行 `:app-phone 发布签名：release 使用 … 签名`。
- 文件或密钥库缺失 → 不创建签名配置，`assembleRelease` 照常成功，产物是 `app-phone-release-unsigned.apk`，同时打印提示。debug 构建从不读这些文件，本地开发与没有密钥的 CI 都不会因此失败。

release 暂不开混淆与资源压缩：Compose 和 kotlinx.serialization 需要额外 keep 规则，等有发布渠道和真机回归矩阵再单独评估，`app-phone/proguard-rules.pro` 目前是说明性占位。

## 构建与校验

```sh
./gradlew :app-phone:assembleRelease
"$ANDROID_HOME"/build-tools/36.0.0/apksigner verify --print-certs --verbose \
  app-phone/build/outputs/apk/release/app-phone-release.apk
```

预期输出：`Verifies`、v2 与 v3 均为 `true`、签名者 1 个且 DN 为 `CN=Pagelence`。产物约 13 MB（debug 约 19 MB），比 debug 小是因为不含调试符号与 `android:debuggable`。

## 备份

密钥库文件和口令必须各自异地备份（例如密码管理器 + 离线盘）。**丢失任一项都无法补救**：Android 不允许换密钥覆盖升级，已安装的用户只能卸载重装，而卸载会一并删除本地进度与生词卡——当前还没有导出功能。备份时不要放进仓库、网盘公开链接或任何会被打包进 APK 的位置。

## 与调试包的关系

| 场景 | 结果 |
| --- | --- |
| 同一发布密钥签出的新版本覆盖旧版本 | 可覆盖升级（`adb install -r`） |
| debug 包覆盖 release 包，或反向 | 失败（`INSTALL_FAILED_UPDATE_INCOMPATIBLE`），需先卸载 |
| 两个 debug 包互相覆盖 | 可以，本机共用同一调试证书 |

从 debug 包（`dist/` 下的调试包）换到 release 包时需要先 `adb uninstall com.pagelence`，这一步会清掉该设备上的阅读进度。

## 轮换（暂不需要）

已有 v3 签名且 `targetSdk ≥ 33` 时可以用 `apksigner rotate` 轮换密钥并保留升级能力，但目前没有发布渠道、也没有需要轮换的理由。真要换之前先补这一节的操作步骤，别直接生成新密钥库。
