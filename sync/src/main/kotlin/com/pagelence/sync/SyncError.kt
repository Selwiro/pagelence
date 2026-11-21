package com.pagelence.sync

/**
 * Stable failure categories. The UI maps these to "one sentence + one action";
 * it must never branch on [SyncException.message], because sentences change
 * with language and wording.
 *
 * 401 and 403 are deliberately separate: one means "fix the password", the
 * other "fix the folder permission". Mixing them sends users to the wrong fix.
 */
enum class SyncErrorCategory {
    URL,
    AUTH,
    PERMISSION,
    NOT_FOUND,
    CONFLICT,
    PRECONDITION,
    LOCKED,
    TOO_LARGE,
    SERVER,
    NETWORK,
    OTHER,
}

/** A WebDAV / sync failure carrying a stable [category] and machine [code]. */
class SyncException(
    message: String,
    val category: SyncErrorCategory = SyncErrorCategory.OTHER,
    val code: String = "SYNC_ERROR",
    val status: Int = 0,
    cause: Throwable? = null,
) : Exception(message, cause)

/** Status code -> stable category. Only 5xx is a server error. */
internal fun statusError(status: Int): SyncException = when (status) {
    401 -> SyncException(
        "认证失败，请检查用户名和应用密码",
        SyncErrorCategory.AUTH,
        "AUTH_FAILED",
        status,
    )

    403 -> SyncException(
        "服务器拒绝写入：请确认目录可写、共享不是只读",
        SyncErrorCategory.PERMISSION,
        "PERMISSION_DENIED",
        status,
    )

    404, 410 -> SyncException(
        "远端文件不存在",
        SyncErrorCategory.NOT_FOUND,
        "REMOTE_NOT_FOUND",
        status,
    )

    405 -> SyncException(
        "服务器不支持该操作，请确认地址指向 WebDAV 文件",
        SyncErrorCategory.SERVER,
        "METHOD_NOT_ALLOWED",
        status,
    )

    409 -> SyncException(
        "远端目录不存在或写入冲突（HTTP 409）",
        SyncErrorCategory.CONFLICT,
        "REMOTE_CONFLICT",
        status,
    )

    412 -> SyncException(
        "远端文件已被其他设备修改，请重新同步",
        SyncErrorCategory.PRECONDITION,
        "PRECONDITION_FAILED",
        status,
    )

    413 -> SyncException(
        "文件过大，服务器拒绝接收",
        SyncErrorCategory.TOO_LARGE,
        "PAYLOAD_TOO_LARGE",
        status,
    )

    423 -> SyncException(
        "远端文件被服务器锁定，请稍后重试",
        SyncErrorCategory.LOCKED,
        "LOCKED",
        status,
    )

    507 -> SyncException(
        "远端存储空间不足",
        SyncErrorCategory.SERVER,
        "INSUFFICIENT_STORAGE",
        status,
    )

    in 300..399 -> SyncException(
        "服务器要求跳转（HTTP $status）：请检查地址、证书与认证方式",
        SyncErrorCategory.SERVER,
        "UNEXPECTED_REDIRECT",
        status,
    )

    in 500..599 -> SyncException(
        "服务器错误（HTTP $status），请稍后重试",
        SyncErrorCategory.SERVER,
        "SERVER_ERROR",
        status,
    )

    else -> SyncException(
        "服务器返回异常状态（HTTP $status）",
        SyncErrorCategory.SERVER,
        "UNEXPECTED_STATUS",
        status,
    )
}

internal fun tooLargeError(bytes: Long): SyncException = SyncException(
    "远端文件过大（约 ${bytes / 1024 / 1024} MB），已停止下载",
    SyncErrorCategory.TOO_LARGE,
    "REMOTE_TOO_LARGE",
)

internal fun networkError(message: String, cause: Throwable?): SyncException = SyncException(
    message,
    SyncErrorCategory.NETWORK,
    "NETWORK_ERROR",
    0,
    cause,
)
