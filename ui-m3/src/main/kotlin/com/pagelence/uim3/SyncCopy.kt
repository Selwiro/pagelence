package com.pagelence.uim3

/**
 * Copy for the sync / backup screen.
 *
 * Kept out of the flat [Strings] interface for the same reason as AboutCopy:
 * this page is a self-contained flow (setup, status, export/import) and pairing
 * the two languages here keeps the whole story visible in one place.
 */
interface SyncCopy {
    val title: String
    val subtitle: String
    val sectionProvider: String
    val providerJianguoyun: String
    val providerNextcloud: String
    val providerSynology: String
    val providerInfinicloud: String
    val providerCustom: String
    val providerHint: String
    val url: String
    val urlHint: String
    val username: String
    val usernameHint: String
    val password: String
    val passwordStored: String
    val passwordMissing: String
    val passwordHint: String
    val allowHttp: String
    val allowHttpHint: String
    val saveAndTest: String
    val syncNow: String
    val disable: String
    val disableTitle: String
    val disableBody: String
    val cancel: String
    val sectionBackup: String
    val backupHint: String
    val export: String
    val import: String
    val exportDone: String
    val exportFailed: String
    val importDone: String
    val importFailed: String
    val importNothing: String
    val statusIdle: String
    val statusConfigured: String
    val statusOk: String
    val statusFailure: String
    val busy: String
    val syncUploaded: String
    val syncDownloaded: String
    val syncMerged: String
    val testOk: String
    val testEmpty: String
    val testFailed: String
    val saved: String
    val savedNoPassword: String
    val warningPrefix: String
    val errorUrl: String
    val errorAuth: String
    val errorPermission: String
    val errorNotFound: String
    val errorConflict: String
    val errorPrecondition: String
    val errorLocked: String
    val errorTooLarge: String
    val errorServer: String
    val errorNetwork: String
    val errorOther: String
    val help: String

    fun errorFor(code: String?): String = when (code) {
        "URL" -> errorUrl
        "AUTH" -> errorAuth
        "PERMISSION" -> errorPermission
        "NOT_FOUND" -> errorNotFound
        "CONFLICT" -> errorConflict
        "PRECONDITION" -> errorPrecondition
        "LOCKED" -> errorLocked
        "TOO_LARGE" -> errorTooLarge
        "SERVER" -> errorServer
        "NETWORK" -> errorNetwork
        else -> errorOther
    }
}

object ZhSyncCopy : SyncCopy {
    override val title = "同步与备份"
    override val subtitle = "用你自己的 WebDAV 在设备之间同步进度，或导出 JSON 手动迁移"
    override val sectionProvider = "WebDAV 账号"
    override val providerJianguoyun = "坚果云"
    override val providerNextcloud = "Nextcloud"
    override val providerSynology = "群晖"
    override val providerInfinicloud = "InfiniCloud"
    override val providerCustom = "自建 / 其他"
    override val providerHint = "地址必须指向一个文件（以 .json 结尾），不是文件夹。"
    override val url = "文件地址"
    override val urlHint = "https://dav.jianguoyun.com/dav/Pagelence/state.json"
    override val username = "用户名"
    override val usernameHint = "Nextcloud 用登录名；群晖用 WebDAV 账号"
    override val password = "应用密码"
    override val passwordStored = "已保存密码；留空则不修改"
    override val passwordMissing = "还没有保存密码"
    override val passwordHint = "坚果云等服务要填「应用密码」，不是登录密码。密码只加密保存在本机 Keystore。"
    override val allowHttp = "允许本机调试明文 HTTP"
    override val allowHttpHint = "仅 localhost / 127.0.0.1 / 10.0.2.2；其他地址必须 HTTPS。"
    override val saveAndTest = "保存并测试"
    override val syncNow = "立即同步"
    override val disable = "关闭同步"
    override val disableTitle = "关闭 WebDAV 同步？"
    override val disableBody = "本机进度不受影响，但会删除保存的地址、密码和同步状态。下次需重新配置。"
    override val cancel = "取消"
    override val sectionBackup = "本地备份"
    override val backupHint = "导出的就是 state.json：进度、生词卡、偏好都在里面。导入会与现有数据合并，不会覆盖。"
    override val export = "导出 JSON"
    override val import = "导入 JSON"
    override val exportDone = "已导出进度文件"
    override val exportFailed = "导出失败，请换个位置重试"
    override val importDone = "已导入并合并"
    override val importFailed = "导入失败：不是有效的进度文件"
    override val importNothing = "导入完成，没有新内容"
    override val statusIdle = "尚未配置同步"
    override val statusConfigured = "已配置，尚未同步"
    override val statusOk = "上次同步：%s"
    override val statusFailure = "上次失败（%s）：%s"
    override val busy = "正在同步…"
    override val syncUploaded = "已上传本机进度"
    override val syncDownloaded = "已下载远端进度"
    override val syncMerged = "已自动合并并上传（新增 %1\$d · 更新 %2\$d · 删除 %3\$d）"
    override val testOk = "连接成功，远端已有进度文件"
    override val testEmpty = "连接成功，远端暂无文件，会在首次同步时创建"
    override val testFailed = "连接失败"
    override val saved = "已保存"
    override val savedNoPassword = "已保存地址，但密码没有写入 Keystore，请重试"
    override val warningPrefix = "提示："
    override val errorUrl = "地址不对：请填写以文件名结尾的完整 https 地址。"
    override val errorAuth = "用户名或应用密码不对。请用「应用密码」，不是登录密码。"
    override val errorPermission = "目录不可写。请确认 WebDAV 目录可写、共享不是只读。"
    override val errorNotFound = "远端文件不存在，可能被其他设备删除。"
    override val errorConflict = "远端目录不存在或写入冲突，请检查路径。"
    override val errorPrecondition = "远端正被其他设备修改，已自动重试。请稍后再同步。"
    override val errorLocked = "远端文件被服务器锁定，请稍后重试。"
    override val errorTooLarge = "文件超过 20 MB，已停止同步。"
    override val errorServer = "服务器返回错误，请稍后重试。"
    override val errorNetwork = "连不上：请检查网络、HTTPS 证书与服务器地址。"
    override val errorOther = "同步失败，请稍后重试。"
    override val help = "各家地址形态：\n" +
        "坚果云：https://dav.jianguoyun.com/dav/<目录>/state.json（应用密码）\n" +
        "Nextcloud：https://<主机>/remote.php/dav/files/<用户名>/Pagelence/state.json\n" +
        "群晖：https://<主机>:5006/<共享文件夹>/Pagelence/state.json（需 WebDAV Server 套件）\n" +
        "InfiniCloud：https://<子域名>.infini-cloud.net/dav/Pagelence/state.json\n" +
        "同步只上传进度、生词卡与偏好，不上传书籍正文。"
}

object EnSyncCopy : SyncCopy {
    override val title = "Sync & backup"
    override val subtitle = "Sync progress between devices with your own WebDAV, or export JSON"
    override val sectionProvider = "WebDAV account"
    override val providerJianguoyun = "Jianguoyun"
    override val providerNextcloud = "Nextcloud"
    override val providerSynology = "Synology"
    override val providerInfinicloud = "InfiniCloud"
    override val providerCustom = "Self-hosted"
    override val providerHint = "The URL must point to a file (ending in .json), not a folder."
    override val url = "File URL"
    override val urlHint = "https://dav.jianguoyun.com/dav/Pagelence/state.json"
    override val username = "Username"
    override val usernameHint = "Login name for Nextcloud; a WebDAV account for Synology"
    override val password = "App password"
    override val passwordStored = "Password stored; leave blank to keep it"
    override val passwordMissing = "No password stored yet"
    override val passwordHint = "Use an app password, not your login password. It is encrypted with the local Keystore."
    override val allowHttp = "Allow cleartext HTTP for local debugging"
    override val allowHttpHint = "localhost / 127.0.0.1 / 10.0.2.2 only; everything else must use HTTPS."
    override val saveAndTest = "Save & test"
    override val syncNow = "Sync now"
    override val disable = "Turn off sync"
    override val disableTitle = "Turn off WebDAV sync?"
    override val disableBody = "Local progress stays. The saved URL, password and sync state are removed."
    override val cancel = "Cancel"
    override val sectionBackup = "Local backup"
    override val backupHint = "The export is state.json: progress, word cards and preferences. Import merges with existing data."
    override val export = "Export JSON"
    override val import = "Import JSON"
    override val exportDone = "Progress file exported"
    override val exportFailed = "Export failed; try another location"
    override val importDone = "Imported and merged"
    override val importFailed = "Import failed: not a progress file"
    override val importNothing = "Imported; nothing new"
    override val statusIdle = "Sync is not set up"
    override val statusConfigured = "Configured; not synced yet"
    override val statusOk = "Last sync: %s"
    override val statusFailure = "Last attempt (%s): %s"
    override val busy = "Syncing…"
    override val syncUploaded = "Local progress uploaded"
    override val syncDownloaded = "Remote progress downloaded"
    override val syncMerged = "Merged and uploaded (%1\$d added · %2\$d updated · %3\$d deleted)"
    override val testOk = "Connected; a progress file already exists"
    override val testEmpty = "Connected; no file yet, it will be created on first sync"
    override val testFailed = "Connection failed"
    override val saved = "Saved"
    override val savedNoPassword = "URL saved, but the password could not be stored. Try again."
    override val warningPrefix = "Note: "
    override val errorUrl = "Invalid URL: enter a full https file URL ending in a file name."
    override val errorAuth = "Wrong username or app password. Use an app password, not the login password."
    override val errorPermission = "The folder is not writable. Check the share is read-write."
    override val errorNotFound = "The remote file does not exist; another device may have deleted it."
    override val errorConflict = "Missing remote folder or write conflict. Check the path."
    override val errorPrecondition = "The remote changed during sync; it was retried. Try again later."
    override val errorLocked = "The file is locked by the server; retry later."
    override val errorTooLarge = "The file exceeds 20 MB; sync stopped."
    override val errorServer = "The server returned an error; retry later."
    override val errorNetwork = "Cannot connect: check the network, HTTPS certificate and address."
    override val errorOther = "Sync failed; try again later."
    override val help = "Address shapes:\n" +
        "Jianguoyun: https://dav.jianguoyun.com/dav/<folder>/state.json (app password)\n" +
        "Nextcloud: https://<host>/remote.php/dav/files/<username>/Pagelence/state.json\n" +
        "Synology: https://<host>:5006/<share>/Pagelence/state.json (WebDAV Server package)\n" +
        "InfiniCloud: https://<subdomain>.infini-cloud.net/dav/Pagelence/state.json\n" +
        "Only progress, cards and preferences are uploaded, never book text."
}
