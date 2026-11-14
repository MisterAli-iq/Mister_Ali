package com.example.data.remote

import com.example.model.AppSettings
import com.example.model.LinkSourceConfig
import com.example.model.LinkSourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import java.net.URLDecoder
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException

enum class LinkCheckStatus {
    IDLE,
    CHECKING,
    VALID,
    FIXED_AND_VALID,
    SOURCE_DISABLED,
    UNSUPPORTED,
    INVALID
}

data class LinkSourceDetection(
    val sourceType: LinkSourceType,
    val isCustomDomainMatch: Boolean = false,
    val matchedDomain: String = "",
    val sourceLabelArabic: String
)

data class LinkDiagnosticResult(
    val status: LinkCheckStatus = LinkCheckStatus.IDLE,
    val rawInput: String = "",
    val correctedUrl: String = "",
    val finalResolvedUrl: String = "",
    val detection: LinkSourceDetection = LinkSourceDetection(
        sourceType = LinkSourceType.OTHER,
        sourceLabelArabic = LinkSourceType.OTHER.titleArabic
    ),
    val isSourceEnabled: Boolean = true,
    val isSupported: Boolean = true,
    val hasMinorSyntaxErrors: Boolean = false,
    val syntaxIssues: List<String> = emptyList(),
    val canOfferCustomDomain: Boolean = false,
    val wasAutoCorrected: Boolean = false,
    val exactReason: String? = null,
    val httpCode: Int? = null
)

object LinkValidator {

    private val probeClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val zeroWidthRegex = Regex("""[\u200B-\u200F\uFEFF\r\n\t]""")
    private val trackingParams = setOf("utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content", "fbclid", "igshid", "igsh")
    private val supportedExtraHosts = setOf(
        "vimeo.com", "player.vimeo.com",
        "dailymotion.com", "dai.ly",
        "soundcloud.com", "on.soundcloud.com",
        "twitch.tv", "clips.twitch.tv",
        "streamable.com", "x.com", "twitter.com",
        "bit.ly", "t.co", "tinyurl.com"
    )
    private val directMediaExtensions = setOf(
        ".mp4", ".m3u8", ".mpd", ".mp3", ".m4a", ".mov", ".webm", ".aac", ".ogg", ".wav", ".flac"
    )
    private val knownSchemeTypoRegex = Regex(
        """^(?:ht+p+s?|ttps?|hp?tps?)[;:/\\]{1,4}""",
        RegexOption.IGNORE_CASE
    )
    private val unsupportedSchemeRegex = Regex(
        """^([a-zA-Z][a-zA-Z0-9+.-]*):(?://)?.*"""
    )

    /**
     * Identifies human-readable minor syntax issues present in [rawInput] relative to [corrected].
     */
    fun detectSyntaxIssues(rawInput: String, corrected: String): List<String> {
        if (rawInput.isBlank()) return emptyList()
        val issues = mutableListOf<String>()
        val trimmed = rawInput.trim()

        if (rawInput != trimmed || trimmed.contains(Regex("""\s""")) || zeroWidthRegex.containsMatchIn(rawInput)) {
            issues += "مسافات زائدة أو أسطر/أحرف مخفية في الرابط"
        }
        if (rawInput.contains("\\")) {
            issues += "استخدام شرطة مائلة عكسية (\\) بدلاً من (/)"
        }

        val tokenLower = trimmed
            .replace(zeroWidthRegex, " ")
            .replace("\\", "/")
            .trim(' ', '"', '\'', '<', '>', '(', ')', '[', ']', '،', '.', ';', '!')
            .lowercase()

        if (tokenLower != trimmed.lowercase() && !issues.any { it.contains("مسافات") }) {
            issues += "نصوص أو أقواس/علامات ترقيم محيطة بالرابط"
        }

        val schemeMatch = unsupportedSchemeRegex.matchEntire(tokenLower)
        val rawScheme = schemeMatch?.groupValues?.getOrNull(1).orEmpty()
        val hasUnsupportedExplicitScheme = rawScheme.isNotBlank() &&
            rawScheme !in setOf("http", "https", "content", "file", "htps", "ttps", "htp", "htpps", "htts", "httpss", "hptps")

        if (!hasUnsupportedExplicitScheme) {
            when {
                tokenLower.startsWith("ttps://") || tokenLower.startsWith("htps://") ||
                    tokenLower.startsWith("htp://") || tokenLower.startsWith("htpps://") ||
                    tokenLower.startsWith("htts://") || tokenLower.startsWith("httpss://") ||
                    tokenLower.startsWith("hptps://") ->
                    issues += "خطأ إملائي في البروتوكول (مثل htps:// أو ttps://)"
                tokenLower.startsWith("https///") || tokenLower.startsWith("http///") ||
                    tokenLower.startsWith("https//") || tokenLower.startsWith("http//") ||
                    (tokenLower.startsWith("https:/") && !tokenLower.startsWith("https://")) ||
                    (tokenLower.startsWith("http:/") && !tokenLower.startsWith("http://")) ||
                    tokenLower.startsWith("https;//") || tokenLower.startsWith("http;//") ||
                    (tokenLower.startsWith("https:") && !tokenLower.startsWith("https://")) ||
                    (tokenLower.startsWith("http:") && !tokenLower.startsWith("http://")) ||
                    tokenLower.startsWith("//") ->
                    issues += "خطأ في علامات البروتوكول (:/ أو // بدلاً من ://)"
                tokenLower.startsWith("http://") ->
                    issues += "بروتوكول http:// غير مشفّر (يفضّل الترقية إلى https://)"
                trimmed.startsWith("HTTPS://") || trimmed.startsWith("HTTP://") ->
                    issues += "أحرف كبيرة في بداية البروتوكول"
                !tokenLower.contains("://") && tokenLower.contains(".") ->
                    issues += "نقص البروتوكول https:// في بداية الرابط"
            }
        }

        if (tokenLower.contains("..") || Regex("""https?://[^/]*,""").containsMatchIn(tokenLower)) {
            issues += "أخطاء نقط أو فواصل مكررة في اسم الموقع"
        }

        val lowerRaw = rawInput.lowercase()
        if (trackingParams.any { param -> lowerRaw.contains("$param=") }) {
            issues += "معاملات تتبع زائدة (?utm/fbclid/igsh)"
        }

        if (lowerRaw.contains("m.youtube.com") || lowerRaw.contains("music.youtube.com") ||
            lowerRaw.contains("m.instagram.com") || lowerRaw.contains("instagr.am") ||
            lowerRaw.contains("m.facebook.com") || lowerRaw.contains("touch.facebook.com") ||
            lowerRaw.contains("mbasic.facebook.com")
        ) {
            issues += "صيغة جوال/مختصرة يمكن توحيدها للصيغة القياسية"
        }

        if (lowerRaw.contains("google.com/url") || lowerRaw.contains("l.facebook.com/") ||
            lowerRaw.contains("lm.facebook.com/") || lowerRaw.contains("l.instagram.com/") ||
            lowerRaw.contains("youtube.com/redirect")
        ) {
            issues += "رابط تحويل وسيط يحتوي على رابط التشغيل الحقيقي بالداخل"
        }

        if (issues.isEmpty() && corrected.isNotBlank() && corrected != rawInput) {
            issues += "تنسيق الرابط يحتاج تنظيفاً وتصحيحاً طفيفاً"
        }
        return issues.distinct()
    }

    /**
     * Unwraps redirect wrapper links (e.g. google.com/url?...&url=https://www.youtube.com/watch%3Fv%3D...,
     * l.facebook.com/l.php?u=..., l.instagram.com/?u=..., youtube.com/redirect?q=...) into the real target URL.
     */
    fun unwrapRedirectWrapperUrl(rawUrl: String): String {
        var current = rawUrl.trim()
        repeat(2) {
            val uri = runCatching { URI(current) }.getOrNull() ?: return current
            val host = uri.host?.lowercase()?.removePrefix("www.") ?: ""
            val path = uri.path?.lowercase().orEmpty()
            val query = uri.rawQuery ?: return current

            val isGoogleWrapper = (host == "google.com" || host.endsWith(".google.com")) && path.startsWith("/url")
            val isSocialWrapper = host == "l.facebook.com" || host == "lm.facebook.com" ||
                host == "l.instagram.com" ||
                ((host == "youtube.com" || host.endsWith(".youtube.com")) && path.startsWith("/redirect"))
            val hasNestedUrlParam = query.contains("url=http", ignoreCase = true) ||
                query.contains("u=http", ignoreCase = true) ||
                query.contains("q=http", ignoreCase = true)

            if (!isGoogleWrapper && !isSocialWrapper && !hasNestedUrlParam) {
                return current
            }

            val paramKeys = listOf("url", "u", "q", "target", "dest")
            var extracted: String? = null
            for (pair in query.split("&")) {
                val eqIdx = pair.indexOf('=')
                if (eqIdx <= 0) continue
                val key = pair.substring(0, eqIdx).lowercase()
                if (key in paramKeys) {
                    val rawVal = pair.substring(eqIdx + 1)
                    val decoded = runCatching { URLDecoder.decode(rawVal, "UTF-8") }.getOrDefault(rawVal).trim()
                    if (decoded.startsWith("http://", ignoreCase = true) || decoded.startsWith("https://", ignoreCase = true)) {
                        extracted = decoded
                        break
                    }
                }
            }
            if (extracted.isNullOrBlank() || extracted == current) {
                return current
            }
            current = extracted
        }
        return current
    }

    /**
     * Automatically cleans malformed pasted text/links:
     * - extracts first URL-like token if user pasted surrounding caption text
     * - strips spaces, invisible directional chars, backslashes, trailing brackets/punctuation
     * - repairs protocol typos (htps://, ttps://, htpps://, https//, https:/, https:, missing https://)
     * - normalizes mobile/short host variants and strips noisy tracking params
     */
    fun sanitizeAndAutoCorrect(rawInput: String): String {
        val strippedInvisible = rawInput
            .replace(zeroWidthRegex, " ")
            .replace("\\", "/")
            .trim()
        if (strippedInvisible.isBlank()) return ""

        // If user pasted spaces inside a bare URL (no caption words, e.g. "https://youtube.com /watch?v=1"),
        // collapse internal spaces when the input starts with a URL/host pattern.
        val normalizedSpacing = if (Regex("""^(?:https?|htps|ttps|htp|htpps|htts|www\.|[a-z0-9-]+\.[a-z]{2,})""", RegexOption.IGNORE_CASE)
                .containsMatchIn(strippedInvisible)
        ) {
            val words = strippedInvisible.split(Regex("""\s+""")).filter { it.isNotBlank() }
            if (words.size >= 2 && (words[1].startsWith("/") || words[1].startsWith("?") || words[1].startsWith("&") || words[1].startsWith("."))) {
                words.joinToString("")
            } else {
                strippedInvisible
            }
        } else {
            strippedInvisible
        }

        // Extract explicit scheme/broken-scheme token or domain-like token if pasted inside a sentence
        val urlToken = Regex(
            """(?:(?:https?|htps|ttps|htp|htpps|htts|httpss|hptps)(?::/{0,3}|;//|//)|[a-zA-Z][a-zA-Z0-9+.-]{1,12}:(?://|/?)|www\.)[^\s"'<>()\[\]،]+""",
            RegexOption.IGNORE_CASE
        ).find(normalizedSpacing)?.value
            ?: normalizedSpacing.split(Regex("""\s+""")).firstOrNull { it.contains(".") || it.contains(",") }
            ?: normalizedSpacing.replace(Regex("""\s+"""), "")

        var cleaned = urlToken
            .trim(' ', '"', '\'', '<', '>', '(', ')', '[', ']', '،', '.', ';', '!')

        // Preserve unsupported non-web explicit schemes (e.g. ftp://, mailto:, javascript:) so validator marks them UNSUPPORTED
        val explicitSchemeMatch = unsupportedSchemeRegex.matchEntire(cleaned.lowercase())
        val detectedScheme = explicitSchemeMatch?.groupValues?.getOrNull(1).orEmpty()
        val repairableSchemes = setOf("http", "https", "htps", "ttps", "htp", "htpps", "htts", "httpss", "hptps", "content", "file")
        if (detectedScheme.isNotBlank() && detectedScheme !in repairableSchemes && cleaned.contains(":")) {
            return cleaned
        }

        // Repair common broken web schemes
        val lower = cleaned.lowercase()
        cleaned = when {
            lower.startsWith("https:///") -> "https://" + cleaned.substring(9).trimStart('/')
            lower.startsWith("http:///") -> "https://" + cleaned.substring(8).trimStart('/')
            lower.startsWith("https://") -> "https://" + cleaned.substring(8)
            lower.startsWith("http://") -> "https://" + cleaned.substring(7)
            lower.startsWith("ttps://") -> "https://" + cleaned.substring(7)
            lower.startsWith("htps://") -> "https://" + cleaned.substring(7)
            lower.startsWith("htpps://") -> "https://" + cleaned.substring(8)
            lower.startsWith("httpss://") -> "https://" + cleaned.substring(8)
            lower.startsWith("hptps://") -> "https://" + cleaned.substring(8)
            lower.startsWith("htts://") -> "https://" + cleaned.substring(7)
            lower.startsWith("htp://") -> "https://" + cleaned.substring(6)
            lower.startsWith("https;//") -> "https://" + cleaned.substring(8)
            lower.startsWith("http;//") -> "https://" + cleaned.substring(7)
            lower.startsWith("https//") -> "https://" + cleaned.substring(7)
            lower.startsWith("http//") -> "https://" + cleaned.substring(6)
            lower.startsWith("https:/") -> "https://" + cleaned.substring(7).trimStart('/')
            lower.startsWith("http:/") -> "https://" + cleaned.substring(6).trimStart('/')
            lower.startsWith("https:") -> "https://" + cleaned.substring(6).trimStart('/')
            lower.startsWith("http:") -> "https://" + cleaned.substring(5).trimStart('/')
            knownSchemeTypoRegex.containsMatchIn(lower) -> {
                "https://" + knownSchemeTypoRegex.replaceFirst(cleaned, "").trimStart('/')
            }
            lower.startsWith("//") -> "https:$cleaned"
            lower.startsWith("content://") || lower.startsWith("file://") -> return cleaned
            (cleaned.contains(".") || cleaned.contains(",")) && !cleaned.contains("://") -> "https://$cleaned"
            else -> cleaned
        }

        // Repair accidental commas or double dots in host portion before parsing URI
        val afterSchemeIdx = cleaned.indexOf("://")
        if (afterSchemeIdx != -1) {
            val schemePart = cleaned.substring(0, afterSchemeIdx + 3)
            val rest = cleaned.substring(afterSchemeIdx + 3)
            val slashIdx = rest.indexOf('/')
            val rawHost = if (slashIdx == -1) rest else rest.substring(0, slashIdx)
            val tail = if (slashIdx == -1) "" else rest.substring(slashIdx)
            val fixedHost = rawHost
                .replace(",", ".")
                .replace(Regex("""\.{2,}"""), ".")
                .trim('.')
            cleaned = "$schemePart$fixedHost$tail"
        }

        // Unwrap Google / social redirect wrapper URLs (e.g. google.com/url?...&url=...)
        cleaned = unwrapRedirectWrapperUrl(cleaned)

        // Normalize mobile/short host aliases and strip tracking params
        cleaned = runCatching {
            val uri = URI(cleaned)
            val host = uri.host?.lowercase() ?: return@runCatching cleaned
            val normalizedHost = when (host) {
                "m.youtube.com", "music.youtube.com" -> "www.youtube.com"
                "instagr.am", "m.instagram.com" -> "www.instagram.com"
                "m.facebook.com", "touch.facebook.com", "mbasic.facebook.com" -> "www.facebook.com"
                else -> host
            }
            val cleanQuery = uri.rawQuery
                ?.split("&")
                ?.filter { pair ->
                    val key = pair.substringBefore("=").lowercase()
                    pair.isNotBlank() && key !in trackingParams
                }
                ?.joinToString("&")
                ?.takeIf { it.isNotBlank() }

            URI(
                uri.scheme ?: "https",
                uri.userInfo,
                normalizedHost,
                uri.port,
                uri.path,
                cleanQuery,
                uri.fragment
            ).toString()
        }.getOrDefault(cleaned)

        return cleaned
    }

    fun detectSource(
        url: String,
        customDomains: List<String> = emptyList()
    ): LinkSourceDetection {
        val lower = url.lowercase()
        val host = runCatching { URI(url).host?.lowercase()?.removePrefix("www.") ?: "" }.getOrDefault("")

        // Check user-defined custom domains first so custom rules override generic OTHER
        val matchedCustom = customDomains.firstOrNull { domain ->
            val d = domain.trim().lowercase().removePrefix("www.")
            d.isNotBlank() && (host == d || host.endsWith(".$d"))
        }
        if (matchedCustom != null) {
            return LinkSourceDetection(
                sourceType = LinkSourceType.OTHER,
                isCustomDomainMatch = true,
                matchedDomain = matchedCustom,
                sourceLabelArabic = "موقع مخصص ($matchedCustom)"
            )
        }

        return when {
            host.contains("youtube.com") || host == "youtu.be" || host.contains("youtube-nocookie.com") ||
                lower.contains("youtube.com") || lower.contains("youtu.be") ->
                LinkSourceDetection(
                    sourceType = LinkSourceType.YOUTUBE,
                    matchedDomain = host.ifBlank { "youtube.com" },
                    sourceLabelArabic = LinkSourceType.YOUTUBE.titleArabic
                )
            host.contains("instagram.com") || host == "instagr.am" || lower.contains("instagram.com") ->
                LinkSourceDetection(
                    sourceType = LinkSourceType.INSTAGRAM,
                    matchedDomain = host.ifBlank { "instagram.com" },
                    sourceLabelArabic = LinkSourceType.INSTAGRAM.titleArabic
                )
            host.contains("tiktok.com") || host.contains("tiktokv.com") || lower.contains("tiktok.com") ->
                LinkSourceDetection(
                    sourceType = LinkSourceType.TIKTOK,
                    matchedDomain = host.ifBlank { "tiktok.com" },
                    sourceLabelArabic = LinkSourceType.TIKTOK.titleArabic
                )
            host.contains("facebook.com") || host == "fb.watch" || host == "fb.com" || host.contains("fbcdn.net") ||
                lower.contains("facebook.com") || lower.contains("fb.watch") ->
                LinkSourceDetection(
                    sourceType = LinkSourceType.FACEBOOK,
                    matchedDomain = host.ifBlank { "facebook.com" },
                    sourceLabelArabic = LinkSourceType.FACEBOOK.titleArabic
                )
            host.contains("whatsapp.com") || host == "wa.me" ->
                LinkSourceDetection(
                    sourceType = LinkSourceType.WHATSAPP,
                    matchedDomain = host.ifBlank { "whatsapp.com" },
                    sourceLabelArabic = LinkSourceType.WHATSAPP.titleArabic
                )
            host == "t.me" || host.contains("telegram.me") || host.contains("telegram.org") ->
                LinkSourceDetection(
                    sourceType = LinkSourceType.TELEGRAM,
                    matchedDomain = host.ifBlank { "t.me" },
                    sourceLabelArabic = LinkSourceType.TELEGRAM.titleArabic
                )
            host.contains("spotify.com") || host == "spoti.fi" ->
                LinkSourceDetection(
                    sourceType = LinkSourceType.SPOTIFY,
                    matchedDomain = host.ifBlank { "spotify.com" },
                    sourceLabelArabic = LinkSourceType.SPOTIFY.titleArabic
                )
            host.contains("music.apple.com") || host.contains("itunes.apple.com") ->
                LinkSourceDetection(
                    sourceType = LinkSourceType.APPLE_MUSIC,
                    matchedDomain = host.ifBlank { "music.apple.com" },
                    sourceLabelArabic = LinkSourceType.APPLE_MUSIC.titleArabic
                )
            host.contains("soundcloud.com") || host == "on.soundcloud.com" ->
                LinkSourceDetection(
                    sourceType = LinkSourceType.SOUNDCLOUD,
                    matchedDomain = host.ifBlank { "soundcloud.com" },
                    sourceLabelArabic = LinkSourceType.SOUNDCLOUD.titleArabic
                )
            host.contains("news.google.com") ->
                LinkSourceDetection(
                    sourceType = LinkSourceType.GOOGLE_NEWS,
                    matchedDomain = host.ifBlank { "news.google.com" },
                    sourceLabelArabic = LinkSourceType.GOOGLE_NEWS.titleArabic
                )
            host.contains("chrome.google.com") || host.contains("chromewebstore.google.com") ->
                LinkSourceDetection(
                    sourceType = LinkSourceType.GOOGLE_CHROME,
                    matchedDomain = host.ifBlank { "chrome.google.com" },
                    sourceLabelArabic = LinkSourceType.GOOGLE_CHROME.titleArabic
                )
            host == "google.com" || host.endsWith(".google.com") ->
                LinkSourceDetection(
                    sourceType = LinkSourceType.GOOGLE_SEARCH,
                    matchedDomain = host.ifBlank { "google.com" },
                    sourceLabelArabic = LinkSourceType.GOOGLE_SEARCH.titleArabic
                )
            else ->
                LinkSourceDetection(
                    sourceType = LinkSourceType.OTHER,
                    matchedDomain = host,
                    sourceLabelArabic = if (host.isNotBlank()) "موقع خارجي ($host)" else LinkSourceType.OTHER.titleArabic
                )
        }
    }

    fun isWebOnlyNonPlayableUrl(url: String): Boolean {
        val detection = detectSource(url)
        val lower = url.lowercase()
        return detection.sourceType == LinkSourceType.GOOGLE_NEWS ||
            detection.sourceType == LinkSourceType.GOOGLE_SEARCH ||
            detection.sourceType == LinkSourceType.GOOGLE_CHROME ||
            detection.sourceType == LinkSourceType.WHATSAPP ||
            detection.sourceType == LinkSourceType.SPOTIFY ||
            detection.sourceType == LinkSourceType.APPLE_MUSIC ||
            lower.contains("news.google.com")
    }

    fun isSourceEnabledInSettings(detection: LinkSourceDetection, settings: AppSettings): Boolean {
        return if (detection.isCustomDomainMatch) {
            settings.customSourceConfig.enabled
        } else {
            settings.configFor(detection.sourceType).enabled
        }
    }

    fun getSourceConfig(detection: LinkSourceDetection, settings: AppSettings): LinkSourceConfig {
        return if (detection.isCustomDomainMatch) {
            settings.customSourceConfig
        } else {
            settings.configFor(detection.sourceType)
        }
    }

    fun isSupportedMediaUrl(correctedUrl: String, detection: LinkSourceDetection): Boolean {
        if (correctedUrl.isBlank()) return false
        val lower = correctedUrl.lowercase()
        if (lower.startsWith("content://") || lower.startsWith("file://")) return true
        val parsed = runCatching { URI(correctedUrl) }.getOrNull() ?: return false
        val scheme = parsed.scheme?.lowercase() ?: ""
        if (scheme != "http" && scheme != "https") return false

        if (detection.sourceType != LinkSourceType.OTHER || detection.isCustomDomainMatch) {
            return true
        }

        val host = parsed.host?.lowercase()?.removePrefix("www.") ?: ""
        val pathWithoutQuery = parsed.path?.lowercase().orEmpty()
        if (directMediaExtensions.any { pathWithoutQuery.endsWith(it) || lower.contains("$it?") }) {
            return true
        }
        return supportedExtraHosts.any { known -> host == known || host.endsWith(".$known") }
    }

    fun validateFormat(correctedUrl: String): String? {
        if (correctedUrl.isBlank()) {
            return "الحقل فارغ، يرجى لصق رابط فيديو أو مقطع صحيح."
        }
        val lower = correctedUrl.lowercase()
        if (lower.startsWith("content://") || lower.startsWith("file://")) {
            return null
        }
        if (correctedUrl.contains(" ")) {
            return "الرابط يحتوي على مسافات داخلية غير صالحة."
        }
        val parsed = runCatching { URI(correctedUrl) }.getOrNull()
            ?: return "صيغة الرابط تالفة ولا يمكن تحليلها كعنوان ويب."
        val scheme = parsed.scheme?.lowercase() ?: ""
        if (scheme != "http" && scheme != "https") {
            return "البروتوكول غير مدعوم ($scheme)؛ يدعم المشغل روابط http و https فقط."
        }
        val host = parsed.host?.lowercase() ?: ""
        if (host.isBlank() || !host.contains(".") || host.startsWith(".") || host.endsWith(".")) {
            return "اسم الموقع أو النطاق في الرابط غير مكتمل (مثال صحيح: youtube.com)."
        }
        val tld = host.substringAfterLast('.')
        if (tld.length < 2 || !tld.all { it.isLetter() }) {
            return "نهاية النطاق في الرابط غير صالحة (.$tld)."
        }
        return null
    }

    fun isShortenedLink(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("youtu.be/") ||
            lower.contains("vm.tiktok.com") ||
            lower.contains("vt.tiktok.com") ||
            lower.contains("fb.watch") ||
            lower.contains("bit.ly/") ||
            lower.contains("t.co/") ||
            lower.contains("tinyurl.com/") ||
            lower.contains("google.com/url") ||
            lower.contains("vertexaisearch.cloud.google.com") ||
            lower.contains("/share/")
    }

    /**
     * Quick synchronous diagnostic used live as the user pastes or types a URL.
     */
    fun inspectLocally(rawInput: String, settings: AppSettings): LinkDiagnosticResult {
        if (rawInput.isBlank()) return LinkDiagnosticResult(status = LinkCheckStatus.IDLE)
        val corrected = sanitizeAndAutoCorrect(rawInput)
        val wasFixed = corrected != rawInput.trim()
        val syntaxIssues = detectSyntaxIssues(rawInput, corrected)
        val hasMinorSyntax = corrected.isNotBlank() && corrected != rawInput && syntaxIssues.isNotEmpty()
        val formatError = validateFormat(corrected)
        val detection = detectSource(corrected, settings.customLinkDomains)
        val enabled = isSourceEnabledInSettings(detection, settings)

        val lower = corrected.lowercase()
        val parsedScheme = runCatching { URI(corrected).scheme?.lowercase().orEmpty() }.getOrDefault("")
        val isUnsupportedScheme = parsedScheme.isNotBlank() &&
            parsedScheme !in setOf("http", "https", "content", "file")

        if (isUnsupportedScheme) {
            return LinkDiagnosticResult(
                status = LinkCheckStatus.UNSUPPORTED,
                rawInput = rawInput,
                correctedUrl = "",
                detection = detection,
                isSourceEnabled = enabled,
                isSupported = false,
                hasMinorSyntaxErrors = false,
                syntaxIssues = emptyList(),
                canOfferCustomDomain = false,
                wasAutoCorrected = false,
                exactReason = "البروتوكول ($parsedScheme) غير مدعوم؛ يدعم التطبيق روابط الفيديو والصوت (https://) فقط."
            )
        }

        if (formatError != null) {
            return LinkDiagnosticResult(
                status = LinkCheckStatus.INVALID,
                rawInput = rawInput,
                correctedUrl = corrected,
                detection = detection,
                isSourceEnabled = enabled,
                isSupported = false,
                hasMinorSyntaxErrors = hasMinorSyntax,
                syntaxIssues = syntaxIssues,
                canOfferCustomDomain = false,
                wasAutoCorrected = wasFixed,
                exactReason = formatError
            )
        }

        if (!enabled) {
            return LinkDiagnosticResult(
                status = LinkCheckStatus.SOURCE_DISABLED,
                rawInput = rawInput,
                correctedUrl = corrected,
                detection = detection,
                isSourceEnabled = false,
                isSupported = isSupportedMediaUrl(corrected, detection),
                hasMinorSyntaxErrors = hasMinorSyntax,
                syntaxIssues = syntaxIssues,
                canOfferCustomDomain = false,
                wasAutoCorrected = wasFixed,
                exactReason = "مصدر «${detection.sourceLabelArabic}» معطّل حالياً من إعدادات مصدر الروابط."
            )
        }

        val supported = isSupportedMediaUrl(corrected, detection)
        if (!supported) {
            val hostLabel = detection.matchedDomain.ifBlank { lower }
            return LinkDiagnosticResult(
                status = LinkCheckStatus.UNSUPPORTED,
                rawInput = rawInput,
                correctedUrl = corrected,
                finalResolvedUrl = corrected,
                detection = detection,
                isSourceEnabled = true,
                isSupported = false,
                hasMinorSyntaxErrors = hasMinorSyntax,
                syntaxIssues = syntaxIssues,
                canOfferCustomDomain = detection.matchedDomain.isNotBlank(),
                wasAutoCorrected = wasFixed,
                exactReason = "الرابط من ($hostLabel) لا يطابق منصة وسائط مدعومة أو ملف فيديو/صوت مباشر (.mp4 / .m3u8). يمكنك إضافته كموقع مخصص."
            )
        }

        return LinkDiagnosticResult(
            status = if (wasFixed) LinkCheckStatus.FIXED_AND_VALID else LinkCheckStatus.VALID,
            rawInput = rawInput,
            correctedUrl = corrected,
            finalResolvedUrl = corrected,
            detection = detection,
            isSourceEnabled = true,
            isSupported = true,
            hasMinorSyntaxErrors = hasMinorSyntax,
            syntaxIssues = syntaxIssues,
            canOfferCustomDomain = false,
            wasAutoCorrected = wasFixed
        )
    }

    /**
     * Full pre-flight network check: expands short URLs, verifies HTTP reachability,
     * and returns exact failure diagnostics + corrected URL when unreachable.
     */
    suspend fun checkLink(rawInput: String, settings: AppSettings): LinkDiagnosticResult {
        val local = inspectLocally(rawInput, settings)
        if (local.status == LinkCheckStatus.INVALID ||
            local.status == LinkCheckStatus.UNSUPPORTED ||
            local.status == LinkCheckStatus.SOURCE_DISABLED ||
            local.status == LinkCheckStatus.IDLE
        ) {
            return local
        }
        val corrected = local.correctedUrl
        if (corrected.startsWith("content://", true) || corrected.startsWith("file://", true)) {
            return local.copy(status = LinkCheckStatus.VALID, finalResolvedUrl = corrected, isSupported = true)
        }

        return withContext(Dispatchers.IO) {
            try {
                // Use HEAD first, fallback to lightweight GET for servers returning 405
                val headReq = Request.Builder()
                    .url(corrected)
                    .head()
                    .header(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
                    )
                    .build()

                var finalUrl = corrected
                var code = 0
                probeClient.newCall(headReq).execute().use { resp ->
                    finalUrl = resp.request.url.toString()
                    code = resp.code
                }

                if (code == 405 || code == 403 || code == 400) {
                    val getReq = Request.Builder()
                        .url(corrected)
                        .get()
                        .header("Range", "bytes=0-1024")
                        .header(
                            "User-Agent",
                            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
                        )
                        .build()
                    probeClient.newCall(getReq).execute().use { resp ->
                        finalUrl = resp.request.url.toString()
                        code = resp.code
                    }
                }

                val expandedChanged = finalUrl != corrected
                val expandedDetection = detectSource(finalUrl, settings.customLinkDomains)
                val expandedEnabled = isSourceEnabledInSettings(expandedDetection, settings)
                val expandedSupported = isSupportedMediaUrl(finalUrl, expandedDetection)

                if (!expandedEnabled) {
                    return@withContext local.copy(
                        status = LinkCheckStatus.SOURCE_DISABLED,
                        finalResolvedUrl = finalUrl,
                        detection = expandedDetection,
                        isSourceEnabled = false,
                        isSupported = expandedSupported,
                        httpCode = code,
                        exactReason = "الرابط يشير إلى مصدر معطّل (${expandedDetection.sourceLabelArabic}) في الإعدادات."
                    )
                }

                if (!expandedSupported) {
                    return@withContext local.copy(
                        status = LinkCheckStatus.UNSUPPORTED,
                        finalResolvedUrl = finalUrl,
                        detection = expandedDetection,
                        isSupported = false,
                        canOfferCustomDomain = expandedDetection.matchedDomain.isNotBlank(),
                        httpCode = code,
                        exactReason = "الوجهة النهائية (${expandedDetection.matchedDomain}) غير مدعومة كمنصة وسائط؛ يمكنك إضافتها كموقع مخصص."
                    )
                }

                if (code in 200..399) {
                    local.copy(
                        status = if (local.wasAutoCorrected || expandedChanged) {
                            LinkCheckStatus.FIXED_AND_VALID
                        } else {
                            LinkCheckStatus.VALID
                        },
                        correctedUrl = if (expandedChanged && isShortenedLink(corrected)) finalUrl else corrected,
                        finalResolvedUrl = finalUrl,
                        detection = expandedDetection,
                        isSupported = true,
                        wasAutoCorrected = local.wasAutoCorrected || expandedChanged,
                        httpCode = code,
                        exactReason = null
                    )
                } else {
                    val reason = when (code) {
                        404, 410 -> "المحتوى غير موجود أو تم حذفه من المصدر (رمز الخطأ 404)."
                        401, 403 -> "المصدر يرفض الوصول المباشر أو المقطع خاص/محمي (رمز الخطأ $code)."
                        429 -> "تم إرسال طلبات كثيرة للمصدر مؤقتاً (رمز 429)، انتظر قليلاً ثم أعد المحاولة."
                        in 500..599 -> "خادم الموقع يواجه عطلاً مؤقتاً حالياً (رمز $code)."
                        else -> "استجابة غير متوقعة من الخادم (رمز HTTP $code)."
                    }
                    local.copy(
                        status = LinkCheckStatus.INVALID,
                        finalResolvedUrl = finalUrl,
                        detection = expandedDetection,
                        httpCode = code,
                        exactReason = reason
                    )
                }
            } catch (e: UnknownHostException) {
                local.copy(
                    status = LinkCheckStatus.INVALID,
                    exactReason = "تعذر الوصول للنطاق (${local.detection.matchedDomain})؛ تأكد من اتصال الإنترنت أو صحة اسم الموقع."
                )
            } catch (e: SSLException) {
                local.copy(
                    status = LinkCheckStatus.INVALID,
                    exactReason = "فشل التحقق من شهادة الأمان SSL للموقع (${e.message ?: "خطأ HTTPS"})."
                )
            } catch (e: Exception) {
                local.copy(
                    status = LinkCheckStatus.INVALID,
                    exactReason = "فشل فحص الرابط عبر الشبكة: ${e.localizedMessage ?: "انتهت مهلة الاتصال"}"
                )
            }
        }
    }
}
