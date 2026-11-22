package com.pagelence.uim3

/** One titled block on the in-app privacy or licence page. */
data class AboutSection(
    val title: String,
    val paragraphs: List<String>,
    val bullets: Boolean = false,
)

/**
 * Copy for the About screen and its two long-form subpages.
 *
 * All text stays in this file rather than the flat [Strings] interface: the
 * About page has its own structure (sections with paragraphs), and pairing the
 * two languages here keeps that structure visible. [Strings.aboutCopy] exposes
 * the right language to the screen.
 */
data class AboutCopy(
    val title: String,
    val tagline: String,
    val settingsSubtitle: String,
    val versionFormat: String,
    val checkUpdate: String,
    val checkUpdateSubtitle: String,
    val website: String,
    val github: String,
    val privacy: String,
    val privacySubtitle: String,
    val licenses: String,
    val licensesSubtitle: String,
    val pendingLink: String,
    val linkUnavailableTitle: String,
    val openFailedTitle: String,
    val openFailedBody: String,
    val linkUnavailableBody: String,
    val updateTitle: String,
    val updateBody: String,
    val privacySections: List<AboutSection>,
    val licenseSections: List<AboutSection>,
)
