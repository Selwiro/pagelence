package com.pagelence.uim3.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pagelence.uim3.AboutSection
import com.pagelence.uim3.R
import com.pagelence.uim3.Strings
import com.pagelence.uim3.components.PageTopBar
import com.pagelence.uim3.components.PagelenceIcons

/**
 * 官网、代码仓库与发布页地址。官网由 GitHub Pages 托管，源文件在 `site/`；
 * 检查更新只打开 Release 列表，不在后台请求网络。
 */
private object AboutLinks {
    const val WEBSITE: String = "https://selwiro.github.io/pagelence/"
    const val GITHUB: String = "https://github.com/Selwiro/pagelence"
    const val RELEASES: String = "$GITHUB/releases"
}

private data class AboutNotice(val title: String, val body: String)

/**
 * 关于页：一张应用身份卡 + 一张入口卡。
 *
 * 版本号、简介与入口都在本页完成；隐私与数据、开源许可进入应用内长页，
 * 不依赖浏览器或网络。检查更新在公开渠道上线前只说明当前开发版状态。
 */
@Composable
fun AboutScreen(
    strings: Strings,
    appVersion: String,
    appVersionCode: Int,
    onBack: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenLicenses: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val copy = strings.aboutCopy
    val context = LocalContext.current
    val version = copy.versionFormat.format(appVersion, appVersionCode)
    var notice by remember { mutableStateOf<AboutNotice?>(null) }

    fun openLink(url: String?, label: String) {
        if (url == null) {
            notice = AboutNotice(
                copy.linkUnavailableTitle,
                copy.linkUnavailableBody.format(label),
            )
            return
        }
        val opened = runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }.isSuccess
        if (!opened) {
            notice = AboutNotice(copy.openFailedTitle, copy.openFailedBody)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = { PageTopBar(title = copy.title, onBack = onBack) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "identity") {
                IdentityCard(
                    appName = strings.appName,
                    version = version,
                    tagline = copy.tagline,
                )
            }
            item(key = "links") {
                ElevatedCard(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        AboutActionRow(
                            icon = PagelenceIcons.Refresh,
                            title = copy.checkUpdate,
                            subtitle = copy.checkUpdateSubtitle.format(version),
                            onClick = {
                                val releases: String? = AboutLinks.RELEASES
                                if (releases != null) {
                                    openLink(releases, copy.checkUpdate)
                                } else {
                                    notice = AboutNotice(
                                        copy.updateTitle,
                                        copy.updateBody.format(version),
                                    )
                                }
                            },
                        )
                        RowDivider()
                        AboutActionRow(
                            icon = PagelenceIcons.Globe,
                            title = copy.website,
                            subtitle = AboutLinks.WEBSITE?.let(::shortUrl) ?: copy.pendingLink,
                            onClick = { openLink(AboutLinks.WEBSITE, copy.website) },
                        )
                        RowDivider()
                        AboutActionRow(
                            icon = PagelenceIcons.Code,
                            title = copy.github,
                            subtitle = AboutLinks.GITHUB?.let(::shortUrl) ?: copy.pendingLink,
                            onClick = { openLink(AboutLinks.GITHUB, copy.github) },
                        )
                        RowDivider()
                        AboutActionRow(
                            icon = PagelenceIcons.Lock,
                            title = copy.privacy,
                            subtitle = copy.privacySubtitle,
                            onClick = onOpenPrivacy,
                            showChevron = true,
                        )
                        RowDivider()
                        AboutActionRow(
                            icon = PagelenceIcons.Info,
                            title = copy.licenses,
                            subtitle = copy.licensesSubtitle,
                            onClick = onOpenLicenses,
                            showChevron = true,
                        )
                    }
                }
            }
        }
    }

    notice?.let { dialog ->
        AlertDialog(
            onDismissRequest = { notice = null },
            title = { Text(dialog.title) },
            text = {
                Text(
                    dialog.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                TextButton(onClick = { notice = null }) { Text(strings.close) }
            },
        )
    }
}

/** 隐私与数据、开源许可共用的长页：标题 + 若干卡片，每张卡片一段说明。 */
@Composable
fun AboutArticleScreen(
    title: String,
    sections: List<AboutSection>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { PageTopBar(title = title, onBack = onBack) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(sections) { _, section ->
                ElevatedCard(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Text(section.title, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        section.paragraphs.forEachIndexed { index, paragraph ->
                            if (index > 0) Spacer(Modifier.height(8.dp))
                            if (section.bullets) {
                                AboutBullet(paragraph)
                            } else {
                                Text(
                                    paragraph,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IdentityCard(
    appName: String,
    version: String,
    tagline: String,
) {
    ElevatedCard(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_pagelence_mark),
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        appName,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        version,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                tagline,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AboutActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showChevron: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = scheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (showChevron) {
            Icon(
                imageVector = PagelenceIcons.ChevronRight,
                contentDescription = null,
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun AboutBullet(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "·",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
    )
}

/** `https://github.com/pagelence` → `github.com/pagelence`，只用于单行摘要。 */
private fun shortUrl(url: String): String =
    url.removePrefix("https://").removePrefix("http://").trimEnd('/')
