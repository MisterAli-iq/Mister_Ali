package com.example.data.remote

import android.util.Log
import com.example.data.RahmaCatalog
import com.example.model.PreferredQuality
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class StreamFormatCandidate(
    val url: String,
    val heightPx: Int = 360,
    val bitrateBps: Long = 0L,
    val mimeType: String = "video/mp4"
)

data class ResolvedStreamResult(
    val streamUrl: String? = null,
    val downloadVideoUrl: String? = null,
    val downloadAudioUrl: String? = null,
    val videoMimeType: String = "video/mp4",
    val audioMimeType: String = "audio/mp4",
    val selectedQualityLabel: String = "HD",
    val pageUrl: String? = null,
    val fallbackEmbedUrl: String? = null,
    val snifferUrl: String? = null,
    val requestHeaders: Map<String, String> = emptyMap(),
    val title: String? = null,
    val thumbnailUrl: String? = null,
    val platformLabel: String = "مباشر",
    val channelName: String? = null,
    val channelHandle: String? = null,
    val isShort: Boolean = false,
    val durationSeconds: Int = 0,
    val isDirectVerified: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Multi-source direct stream extractor, verifier, and in-memory cache for Media3 ExoPlayer
 * and Real Downloads. Strictly separates webpage links/titles from verified direct playable
 * MP4/M3U8/audio URLs and enforces fast parallel resolution within a 10-second budget.
 */
object MediaStreamResolver {

    private const val TAG = "MediaStreamResolver"
    const val MAX_RESOLUTION_TIMEOUT_MS = 10_000L
    private const val INTERNAL_EXTRACTION_BUDGET_MS = 8_500L
    private const val STREAM_CACHE_TTL_MS = 12 * 60_000L
    private const val MAX_CACHE_ENTRIES = 64

    private const val MOBILE_SAFARI_UA =
        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1"
    private const val ANDROID_CHROME_UA =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"

    private data class CachedStreamEntry(
        val result: ResolvedStreamResult,
        val cachedAtMs: Long
    )

    private val verifiedStreamCache = ConcurrentHashMap<String, CachedStreamEntry>()

    private val client = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .callTimeout(6, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val probeClient = OkHttpClient.Builder()
        .connectTimeout(2500, TimeUnit.MILLISECONDS)
        .readTimeout(2500, TimeUnit.MILLISECONDS)
        .callTimeout(3000, TimeUnit.MILLISECONDS)
        .followRedirects(true)
        .build()

    fun getCachedStream(
        sourceUrl: String,
        videoIdHint: String = "",
        preferredQuality: PreferredQuality = PreferredQuality.HIGH
    ): ResolvedStreamResult? {
        val key = buildCacheKey(sourceUrl, videoIdHint, preferredQuality) ?: return null
        val entry = verifiedStreamCache[key] ?: return null
        val age = System.currentTimeMillis() - entry.cachedAtMs
        if (age > STREAM_CACHE_TTL_MS || entry.result.streamUrl.isNullOrBlank()) {
            verifiedStreamCache.remove(key)
            return null
        }
        return entry.result
    }

    fun putCachedStream(
        sourceUrl: String,
        videoIdHint: String = "",
        preferredQuality: PreferredQuality,
        result: ResolvedStreamResult
    ) {
        val url = result.streamUrl ?: return
        if (!isDirectMediaUri(url)) return
        val key = buildCacheKey(sourceUrl, videoIdHint, preferredQuality) ?: return
        if (verifiedStreamCache.size >= MAX_CACHE_ENTRIES) {
            val oldestKey = verifiedStreamCache.entries.minByOrNull { it.value.cachedAtMs }?.key
            if (oldestKey != null) verifiedStreamCache.remove(oldestKey)
        }
        verifiedStreamCache[key] = CachedStreamEntry(
            result = result.copy(isDirectVerified = true),
            cachedAtMs = System.currentTimeMillis()
        )
    }

    fun preCacheInstagramStream(
        pageUrl: String,
        directVideoUrl: String,
        title: String?,
        thumbnailUrl: String?,
        handle: String = "@rahmariadh"
    ) {
        if (pageUrl.isBlank() || !isDirectMediaUri(directVideoUrl)) return
        val headers = mapOf(
            "User-Agent" to MOBILE_SAFARI_UA,
            "Referer" to "https://www.instagram.com/"
        )
        val result = ResolvedStreamResult(
            streamUrl = directVideoUrl,
            downloadVideoUrl = directVideoUrl,
            downloadAudioUrl = directVideoUrl,
            videoMimeType = "video/mp4",
            selectedQualityLabel = "HD",
            pageUrl = pageUrl,
            fallbackEmbedUrl = "${pageUrl.trimEnd('/')}/embed/captioned/",
            snifferUrl = pageUrl,
            requestHeaders = headers,
            title = title,
            thumbnailUrl = thumbnailUrl,
            platformLabel = "انستغرام • $handle",
            channelName = "رحمة رياض • إنستغرام الرسمي",
            channelHandle = handle,
            isShort = true,
            isDirectVerified = true
        )
        PreferredQuality.entries.forEach { q ->
            putCachedStream(
                sourceUrl = pageUrl,
                videoIdHint = "",
                preferredQuality = q,
                result = result
            )
        }
    }

    fun invalidateCachedStream(sourceUrl: String, videoIdHint: String = "") {
        val ytId = extractYouTubeId(sourceUrl).ifBlank { videoIdHint.trim() }
        val normalized = sourceUrl.trim().lowercase()
        val keysToRemove = verifiedStreamCache.keys.filter { k ->
            (ytId.isNotBlank() && k.startsWith("yt:${ytId.lowercase()}|")) ||
                (normalized.isNotBlank() && k.startsWith("url:$normalized|"))
        }
        keysToRemove.forEach { verifiedStreamCache.remove(it) }
    }

    fun clearStreamCache() {
        verifiedStreamCache.clear()
    }

    private fun buildCacheKey(
        sourceUrl: String,
        videoIdHint: String,
        quality: PreferredQuality
    ): String? {
        val ytId = extractYouTubeId(sourceUrl).ifBlank { videoIdHint.trim() }
        if (ytId.isNotBlank()) return "yt:${ytId.lowercase()}|${quality.name}"
        val clean = sourceUrl.trim().lowercase()
        if (clean.isBlank()) return null
        return "url:$clean|${quality.name}"
    }

    suspend fun resolvePlayableStream(
        sourceUrl: String,
        videoIdHint: String = "",
        preferredQuality: PreferredQuality = PreferredQuality.HIGH,
        bypassCache: Boolean = false,
        verifyNetworkStream: Boolean = true,
        requireVideoTrack: Boolean = false
    ): ResolvedStreamResult = withContext(Dispatchers.IO) {
        val raw = LinkValidator.unwrapRedirectWrapperUrl(sourceUrl).trim()
        val lower = raw.lowercase()

        // 0. Check verified in-memory cache first for < 1s playback start
        if (!bypassCache) {
            getCachedStream(raw, videoIdHint, preferredQuality)?.let { cached ->
                val cachedUrl = cached.streamUrl.orEmpty().lowercase()
                if (!requireVideoTrack || !isAudioPath(cachedUrl)) {
                    return@withContext cached
                }
            }
        }

        // 1. Direct local content/file URI or pre-verified direct remote media URL
        if (isDirectMediaUri(raw)) {
            val isLocal = lower.startsWith("content://") || lower.startsWith("file://")
            val isAudio = isAudioPath(lower)
            val headers = if (isLocal) emptyMap() else mapOf("User-Agent" to ANDROID_CHROME_UA)
            if (isLocal || !verifyNetworkStream || verifyDirectPlayableStream(raw, headers)) {
                val directRes = ResolvedStreamResult(
                    streamUrl = raw,
                    downloadVideoUrl = if (!isLocal && !isHlsStreamUrl(raw) && !isAudio) raw else null,
                    downloadAudioUrl = if (!isLocal && !isHlsStreamUrl(raw)) raw else null,
                    audioMimeType = if (lower.contains(".mp3")) "audio/mpeg" else "audio/mp4",
                    pageUrl = raw,
                    requestHeaders = headers,
                    platformLabel = if (isLocal) "جهازي" else "بث مباشر",
                    isDirectVerified = true
                )
                if (!isLocal) putCachedStream(raw, videoIdHint, preferredQuality, directRes)
                return@withContext directRes
            }
        }

        // 2. Bounded extraction within strict budget (< 10s total)
        val resolved = withTimeoutOrNull(INTERNAL_EXTRACTION_BUDGET_MS) {
            val effectiveRaw = if (LinkValidator.isShortenedLink(raw) && !lower.contains("youtu.be/")) {
                resolveRedirectTarget(raw) ?: raw
            } else {
                raw
            }
            val effectiveLower = effectiveRaw.lowercase()

            // Re-check if redirect landed directly on a playable stream
            if (effectiveRaw != raw && isDirectMediaUri(effectiveRaw)) {
                val headers = mapOf("User-Agent" to ANDROID_CHROME_UA)
                if (!verifyNetworkStream || verifyDirectPlayableStream(effectiveRaw, headers)) {
                    return@withTimeoutOrNull ResolvedStreamResult(
                        streamUrl = effectiveRaw,
                        downloadVideoUrl = if (!isHlsStreamUrl(effectiveRaw)) effectiveRaw else null,
                        downloadAudioUrl = if (!isHlsStreamUrl(effectiveRaw)) effectiveRaw else null,
                        pageUrl = raw,
                        requestHeaders = headers,
                        platformLabel = "بث مباشر",
                        isDirectVerified = true
                    )
                }
            }

            // 3. YouTube Video / Short
            val ytId = extractYouTubeId(effectiveRaw).ifBlank { videoIdHint.trim() }
            if (ytId.isNotBlank() && (effectiveLower.contains("youtube.com") || effectiveLower.contains("youtu.be") || effectiveRaw.isBlank())) {
                return@withTimeoutOrNull resolveYouTubeStream(
                    videoId = ytId,
                    originalPageUrl = effectiveRaw.ifBlank { "https://www.youtube.com/watch?v=$ytId" },
                    isShortHint = effectiveLower.contains("/shorts/"),
                    preferredQuality = preferredQuality,
                    verifyNetworkStream = verifyNetworkStream,
                    requireVideoTrack = requireVideoTrack
                )
            }

            // 4. Instagram Reel / Post
            if (effectiveLower.contains("instagram.com")) {
                return@withTimeoutOrNull resolveInstagramStream(effectiveRaw, verifyNetworkStream)
            }

            // 5. TikTok Video
            if (effectiveLower.contains("tiktok.com")) {
                return@withTimeoutOrNull resolveTikTokStream(effectiveRaw, preferredQuality, verifyNetworkStream)
            }

            // 6. Facebook Video / Reel / Watch
            if (effectiveLower.contains("facebook.com") || effectiveLower.contains("fb.watch") || effectiveLower.contains("fb.com")) {
                return@withTimeoutOrNull resolveFacebookStream(effectiveRaw, preferredQuality, verifyNetworkStream)
            }

            // 7. Generic webpage OpenGraph / JSON-LD / HTML5 <video> scan + sniffer target URL
            if (effectiveLower.startsWith("http://") || effectiveLower.startsWith("https://")) {
                return@withTimeoutOrNull resolveGenericWebVideo(effectiveRaw, verifyNetworkStream)
            }

            ResolvedStreamResult(
                pageUrl = raw.ifBlank { null },
                errorMessage = "الرابط غير مدعوم للتشغيل المباشر"
            )
        } ?: ResolvedStreamResult(
            pageUrl = raw.ifBlank { null },
            fallbackEmbedUrl = buildPageSnifferUrl(raw, videoIdHint),
            snifferUrl = buildPageSnifferUrl(raw, videoIdHint),
            errorMessage = "استغرق جلب البث المباشر وقتاً أطول من المعتاد"
        )

        if (!resolved.streamUrl.isNullOrBlank() && isDirectMediaUri(resolved.streamUrl)) {
            if (!requireVideoTrack || !isAudioPath(resolved.streamUrl.lowercase())) {
                putCachedStream(raw, videoIdHint, preferredQuality, resolved)
            }
        }
        return@withContext resolved
    }

    suspend fun fetchLinkPreviewMetadata(rawUrl: String): ResolvedStreamResult = withContext(Dispatchers.IO) {
        val unwrapped = LinkValidator.unwrapRedirectWrapperUrl(rawUrl).trim()
        val ytId = extractYouTubeId(unwrapped)
        withTimeoutOrNull(6_000L) {
            resolvePlayableStream(
                sourceUrl = unwrapped,
                videoIdHint = ytId,
                preferredQuality = PreferredQuality.MEDIUM,
                bypassCache = false,
                verifyNetworkStream = false
            )
        } ?: ResolvedStreamResult(
            pageUrl = unwrapped,
            title = if (ytId.isNotBlank()) "فيديو يوتيوب ($ytId)" else unwrapped.substringAfter("://").substringBefore("/"),
            thumbnailUrl = if (ytId.isNotBlank()) "https://i.ytimg.com/vi/$ytId/hqdefault.jpg" else null,
            platformLabel = LinkValidator.detectSource(unwrapped).sourceLabelArabic
        )
    }

    suspend fun verifyAndCacheSniffedStream(
        originalPageUrl: String,
        videoIdHint: String = "",
        sniffedMediaUrl: String,
        requestHeaders: Map<String, String> = emptyMap(),
        titleHint: String? = null,
        preferredQuality: PreferredQuality = PreferredQuality.HIGH
    ): ResolvedStreamResult? = withContext(Dispatchers.IO) {
        val unwrappedPage = LinkValidator.unwrapRedirectWrapperUrl(originalPageUrl)
        val clean = sniffedMediaUrl.trim()
        if (!isDirectMediaUri(clean)) return@withContext null
        val mergedHeaders = if (requestHeaders.isEmpty()) {
            mapOf("User-Agent" to ANDROID_CHROME_UA, "Referer" to unwrappedPage)
        } else requestHeaders
        if (!verifyDirectPlayableStream(clean, mergedHeaders)) return@withContext null

        val isHls = isHlsStreamUrl(clean)
        val result = ResolvedStreamResult(
            streamUrl = clean,
            downloadVideoUrl = if (!isHls && !isAudioPath(clean.lowercase())) clean else null,
            downloadAudioUrl = if (!isHls) clean else null,
            videoMimeType = if (isHls) "application/x-mpegurl" else "video/mp4",
            selectedQualityLabel = "${preferredQuality.maxHeightPx}p",
            pageUrl = unwrappedPage,
            fallbackEmbedUrl = buildPageSnifferUrl(unwrappedPage, videoIdHint),
            snifferUrl = buildPageSnifferUrl(unwrappedPage, videoIdHint),
            requestHeaders = mergedHeaders,
            title = titleHint,
            platformLabel = "بث مباشر",
            isDirectVerified = true
        )
        putCachedStream(unwrappedPage, videoIdHint, preferredQuality, result)
        result
    }

    fun buildPageSnifferUrl(rawUrl: String, videoIdHint: String = ""): String? {
        val unwrapped = LinkValidator.unwrapRedirectWrapperUrl(rawUrl).trim()
        val ytId = extractYouTubeId(unwrapped).ifBlank { videoIdHint.trim() }
        // Use mobile watch page (never external /embed/ which blocks copyrighted music videos with Error 150/153)
        if (ytId.isNotBlank()) return "https://m.youtube.com/watch?v=$ytId"
        return unwrapped.takeIf { it.startsWith("http://", ignoreCase = true) || it.startsWith("https://", ignoreCase = true) }
    }

    /**
     * Strictly distinguishes verified direct playable media files/streams from webpage/watch links.
     */
    fun isDirectMediaUri(url: String): Boolean {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return false
        val lower = trimmed.lowercase()

        if (lower.startsWith("content://") || lower.startsWith("file://")) return true
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) return false

        val pathWithoutQuery = lower.substringBefore("?").substringBefore("#")

        // Reject explicit webpage file extensions and common HTML watch/reel routes
        if (pathWithoutQuery.endsWith(".html") ||
            pathWithoutQuery.endsWith(".htm") ||
            pathWithoutQuery.endsWith(".php") ||
            pathWithoutQuery.endsWith(".asp") ||
            pathWithoutQuery.endsWith(".aspx")
        ) {
            return false
        }
        if (lower.contains("google.com/url") ||
            lower.contains("youtube.com/redirect") ||
            lower.contains("l.facebook.com/") ||
            lower.contains("lm.facebook.com/") ||
            lower.contains("l.instagram.com/") ||
            lower.contains("youtube.com/watch") ||
            lower.contains("youtube.com/shorts/") ||
            lower.contains("youtube.com/live/") ||
            lower.contains("youtube.com/embed/") ||
            lower.contains("youtube-nocookie.com/embed/") ||
            lower.contains("youtu.be/") ||
            lower.contains("instagram.com/reel") ||
            lower.contains("instagram.com/p/") ||
            lower.contains("instagram.com/tv/") ||
            (lower.contains("tiktok.com/") && lower.contains("/video/")) ||
            lower.contains("facebook.com/plugins/")
        ) {
            return false
        }

        val directExtensions = listOf(".mp4", ".m3u8", ".webm", ".mp3", ".m4a", ".aac", ".ogg", ".wav")
        if (directExtensions.any { pathWithoutQuery.endsWith(it) || pathWithoutQuery.contains("$it/") }) {
            return true
        }

        return pathWithoutQuery.contains("googlevideo.com/videoplayback") ||
            pathWithoutQuery.contains("manifest.googlevideo.com") ||
            pathWithoutQuery.contains("hls_playlist") ||
            (pathWithoutQuery.contains("tiktokcdn") && (lower.contains("mime_type=video") || pathWithoutQuery.contains("/video/"))) ||
            (pathWithoutQuery.contains("fbcdn.net") && (lower.contains(".mp4") || lower.contains("efg="))) ||
            (pathWithoutQuery.contains("cdninstagram.com") && lower.contains(".mp4"))
    }

    private fun isAudioPath(lowerUrl: String): Boolean {
        val path = lowerUrl.substringBefore("?").substringBefore("#")
        return path.endsWith(".mp3") || path.endsWith(".m4a") || path.endsWith(".aac") || path.endsWith(".ogg") || path.endsWith(".wav")
    }

    fun isHlsStreamUrl(url: String): Boolean {
        val lower = url.lowercase()
        val path = lower.substringBefore("?").substringBefore("#")
        return path.endsWith(".m3u8") ||
            lower.contains(".m3u8") ||
            lower.contains("hls_playlist") ||
            lower.contains("application/vnd.apple.mpegurl")
    }

    fun verifyDirectPlayableStream(
        candidateUrl: String,
        headers: Map<String, String> = emptyMap()
    ): Boolean {
        if (!isDirectMediaUri(candidateUrl)) return false
        val lower = candidateUrl.lowercase()
        if (lower.startsWith("content://") || lower.startsWith("file://")) return true

        fun applyHeaders(builder: Request.Builder) {
            if (headers.isNotEmpty()) {
                headers.forEach { (k, v) -> builder.header(k, v) }
            } else {
                builder.header("User-Agent", ANDROID_CHROME_UA)
            }
        }

        // Fast HEAD probe first
        try {
            val headReq = Request.Builder().url(candidateUrl).head().apply { applyHeaders(this) }.build()
            probeClient.newCall(headReq).execute().use { resp ->
                if (resp.isSuccessful || resp.code == 206) {
                    val ct = resp.header("Content-Type")?.lowercase().orEmpty()
                    if (ct.contains("text/html") || ct.contains("application/xhtml")) return false
                    return true
                }
                if (resp.code == 403 || resp.code == 404 || resp.code == 410) return false
            }
        } catch (_: Exception) {
            // Fall through to tiny range GET check if HEAD is blocked or times out briefly
        }

        return try {
            val rangeReq = Request.Builder()
                .url(candidateUrl)
                .get()
                .header("Range", "bytes=0-1")
                .apply { applyHeaders(this) }
                .build()
            probeClient.newCall(rangeReq).execute().use { resp ->
                if (!resp.isSuccessful && resp.code != 206) return false
                val ct = resp.header("Content-Type")?.lowercase().orEmpty()
                if (ct.contains("text/html") || ct.contains("application/xhtml")) return false
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun pickFirstVerifiedCandidate(
        candidates: List<Pair<StreamFormatCandidate?, Boolean>>,
        headers: Map<String, String>,
        verifyNetworkStream: Boolean
    ): Pair<StreamFormatCandidate, Boolean>? {
        for ((candidate, isHls) in candidates) {
            if (candidate == null || !isDirectMediaUri(candidate.url)) continue
            if (!verifyNetworkStream || verifyDirectPlayableStream(candidate.url, headers)) {
                return candidate to isHls
            }
        }
        return null
    }

    fun selectBestVideoCandidate(
        candidates: List<StreamFormatCandidate>,
        maxHeightPx: Int
    ): StreamFormatCandidate? {
        if (candidates.isEmpty()) return null
        val withinCap = candidates.filter { it.heightPx in 1..maxHeightPx }
        return if (withinCap.isNotEmpty()) {
            withinCap.maxWithOrNull(compareBy<StreamFormatCandidate> { it.heightPx }.thenBy { it.bitrateBps })
        } else {
            candidates.minWithOrNull(compareBy<StreamFormatCandidate> { it.heightPx }.thenBy { it.bitrateBps })
        }
    }

    fun selectBestAudioCandidate(
        candidates: List<StreamFormatCandidate>,
        preferHighBitrate: Boolean
    ): StreamFormatCandidate? {
        if (candidates.isEmpty()) return null
        val mp4Audio = candidates.filter { it.mimeType.contains("mp4", ignoreCase = true) || it.mimeType.contains("mpeg", ignoreCase = true) }
        val pool = mp4Audio.ifEmpty { candidates }
        return if (preferHighBitrate) {
            pool.maxByOrNull { it.bitrateBps }
        } else {
            pool.sortedBy { it.bitrateBps }.firstOrNull()
        }
    }

    fun extractYouTubeId(url: String): String {
        val trimmed = LinkValidator.unwrapRedirectWrapperUrl(url).trim()
        if (trimmed.isBlank()) return ""
        val pathRegex = Regex(
            """(?:youtu\.be/|youtube(?:-nocookie)?\.com/(?:shorts|live|embed|v)/)([A-Za-z0-9_-]{6,15})""",
            RegexOption.IGNORE_CASE
        )
        pathRegex.find(trimmed)?.groupValues?.getOrNull(1)?.let { return it }

        val watchRegex = Regex("""[?&]v=([A-Za-z0-9_-]{6,15})""", RegexOption.IGNORE_CASE)
        if (trimmed.contains("youtube.com", ignoreCase = true) || trimmed.contains("youtu.be", ignoreCase = true)) {
            watchRegex.find(trimmed)?.groupValues?.getOrNull(1)?.let { return it }
        }
        return ""
    }

    fun buildSilentYouTubeEmbedUrl(videoId: String): String {
        val cleanId = videoId.trim()
        return "https://www.youtube-nocookie.com/embed/$cleanId" +
            "?autoplay=1&playsinline=1&controls=0&modestbranding=1&rel=0&iv_load_policy=3&fs=0&disablekb=1&cc_load_policy=0"
    }

    private suspend fun resolveYouTubeStream(
        videoId: String,
        originalPageUrl: String,
        isShortHint: Boolean,
        preferredQuality: PreferredQuality,
        verifyNetworkStream: Boolean,
        requireVideoTrack: Boolean = false
    ): ResolvedStreamResult = coroutineScope {
        val silentFallback = buildSilentYouTubeEmbedUrl(videoId)
        val snifferWatchUrl = buildPageSnifferUrl(originalPageUrl, videoId)
        val oEmbedDeferred = async { fetchYouTubeOEmbedMeta(videoId) }
        val lastThumb = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
        var lastTitle: String? = null
        var lastAuthor: String? = null
        var lastAuthorHandle: String? = null
        var lastDuration = 0

        fun buildVerifiedResult(
            pickedCandidate: StreamFormatCandidate,
            isHls: Boolean,
            chosenVideo: StreamFormatCandidate?,
            chosenAudio: StreamFormatCandidate?,
            matchedHeaders: Map<String, String>,
            oEmbedMeta: Triple<String?, String?, String?>?
        ): ResolvedStreamResult {
            val metaTitle = lastTitle ?: oEmbedMeta?.first
            val metaAuthor = lastAuthor ?: oEmbedMeta?.second
            val metaHandle = lastAuthorHandle ?: oEmbedMeta?.third
            val isOfficial = RahmaCatalog.isOfficialRahmaChannelName(metaAuthor.orEmpty(), metaHandle.orEmpty())
            val resolvedChannel = if (isOfficial) RahmaCatalog.OFFICIAL_CHANNEL_AR_NAME else metaAuthor
            val resolvedHandle = if (isOfficial) RahmaCatalog.OFFICIAL_CHANNEL_HANDLE else metaHandle

            return ResolvedStreamResult(
                streamUrl = pickedCandidate.url,
                downloadVideoUrl = chosenVideo?.url?.takeIf { !isHls },
                downloadAudioUrl = chosenAudio?.url ?: chosenVideo?.url,
                videoMimeType = if (isHls) "application/x-mpegurl" else pickedCandidate.mimeType,
                audioMimeType = chosenAudio?.mimeType ?: "audio/mp4",
                selectedQualityLabel = "${pickedCandidate.heightPx.coerceAtLeast(360)}p",
                pageUrl = originalPageUrl,
                fallbackEmbedUrl = silentFallback,
                snifferUrl = snifferWatchUrl,
                requestHeaders = matchedHeaders,
                title = metaTitle,
                thumbnailUrl = lastThumb,
                platformLabel = resolvedChannel ?: "يوتيوب",
                channelName = resolvedChannel,
                channelHandle = resolvedHandle,
                isShort = isShortHint,
                durationSeconds = lastDuration,
                isDirectVerified = true
            )
        }

        // Attempt 1: Fast InnerTube endpoints ordered by highest direct-stream reliability
        val clients = listOf(
            InnerTubeClient(
                clientName = "IOS",
                clientVersion = "20.10.4",
                clientHeaderId = "5",
                userAgent = "com.google.ios.youtube/20.10.4 (iPhone16,2; U; CPU iOS 18_3_2 like Mac OS X;)"
            ),
            InnerTubeClient(
                clientName = "ANDROID_VR",
                clientVersion = "1.60.19",
                clientHeaderId = "28",
                userAgent = "com.google.android.apps.youtube.vr.oculus/1.60.19 (Linux; U; Android 12L; Quest 3 Build/SQ3A.220605.009.A1) gzip"
            ),
            InnerTubeClient(
                clientName = "MWEB",
                clientVersion = "2.20250317.01.00",
                clientHeaderId = "2",
                userAgent = MOBILE_SAFARI_UA
            ),
            InnerTubeClient(
                clientName = "ANDROID_TESTSUITE",
                clientVersion = "1.9",
                clientHeaderId = "30",
                userAgent = "com.google.android.youtube/1.9 (Linux; U; Android 14) gzip"
            ),
            InnerTubeClient(
                clientName = "TVHTML5_SIMPLY_EMBEDDED_PLAYER",
                clientVersion = "2.0",
                clientHeaderId = "85",
                userAgent = "Mozilla/5.0 (ChromiumStylePlatform) Cobalt/Version",
                embedHost = "https://www.youtube.com"
            ),
            InnerTubeClient(
                clientName = "ANDROID",
                clientVersion = "20.10.38",
                clientHeaderId = "3",
                userAgent = "com.google.android.youtube/20.10.38 (Linux; U; Android 14) gzip"
            )
        )

        for (cfg in clients) {
            try {
                val clientObj = JSONObject().apply {
                    put("clientName", cfg.clientName)
                    put("clientVersion", cfg.clientVersion)
                    put("hl", "ar")
                    put("gl", "IQ")
                }
                val contextObj = JSONObject().apply {
                    put("client", clientObj)
                    if (cfg.embedHost != null) {
                        put("thirdParty", JSONObject().apply {
                            put("embedUrl", cfg.embedHost)
                        })
                    }
                }
                val payload = JSONObject().apply {
                    put("videoId", videoId)
                    put("contentCheckOk", true)
                    put("racyCheckOk", true)
                    put("context", contextObj)
                }.toString()

                val req = Request.Builder()
                    .url("https://www.youtube.com/youtubei/v1/player?prettyPrint=false")
                    .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .header("User-Agent", cfg.userAgent)
                    .header("X-YouTube-Client-Name", cfg.clientHeaderId)
                    .header("X-YouTube-Client-Version", cfg.clientVersion)
                    .header("Origin", "https://www.youtube.com")
                    .header("Referer", "https://www.youtube.com/")
                    .build()

                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use
                    val bodyStr = resp.body?.string() ?: return@use
                    val json = JSONObject(bodyStr)

                    val details = json.optJSONObject("videoDetails")
                    if (details != null) {
                        lastTitle = details.optString("title").takeIf { it.isNotBlank() } ?: lastTitle
                        lastAuthor = details.optString("author").takeIf { it.isNotBlank() } ?: lastAuthor
                        lastDuration = details.optString("lengthSeconds").toIntOrNull() ?: lastDuration
                    }

                    val playStatus = json.optJSONObject("playabilityStatus")?.optString("status").orEmpty()
                    if (playStatus.equals("UNPLAYABLE", ignoreCase = true) ||
                        playStatus.equals("LOGIN_REQUIRED", ignoreCase = true) ||
                        playStatus.equals("ERROR", ignoreCase = true)
                    ) {
                        return@use
                    }

                    val streamingData = json.optJSONObject("streamingData") ?: return@use
                    val matchedHeaders = if (cfg.embedHost != null) {
                        mapOf(
                            "User-Agent" to cfg.userAgent,
                            "Origin" to "https://www.youtube.com",
                            "Referer" to "https://www.youtube.com/"
                        )
                    } else {
                        mapOf("User-Agent" to cfg.userAgent)
                    }

                    val progressiveCandidates = extractCandidatesFromArray(
                        arr = streamingData.optJSONArray("formats"),
                        audioOnly = false
                    )
                    val audioCandidates = extractCandidatesFromArray(
                        arr = streamingData.optJSONArray("adaptiveFormats"),
                        audioOnly = true
                    )

                    val chosenVideo = selectBestVideoCandidate(progressiveCandidates, preferredQuality.maxHeightPx)
                    val chosenAudio = selectBestAudioCandidate(
                        candidates = audioCandidates,
                        preferHighBitrate = preferredQuality != PreferredQuality.DATA_SAVER
                    )
                    val hlsUrl = streamingData.optString("hlsManifestUrl").takeIf { it.startsWith("http") }
                        ?.let { StreamFormatCandidate(url = it, heightPx = preferredQuality.maxHeightPx, mimeType = "application/x-mpegurl") }

                    val candidateOrder = if (requireVideoTrack) {
                        listOf(chosenVideo to false, hlsUrl to true)
                    } else {
                        listOf(chosenVideo to false, hlsUrl to true, chosenAudio to false)
                    }
                    val verifiedPick = pickFirstVerifiedCandidate(
                        candidates = candidateOrder,
                        headers = matchedHeaders,
                        verifyNetworkStream = verifyNetworkStream
                    )

                    if (verifiedPick != null) {
                        val oEmbedMeta = runCatching { oEmbedDeferred.await() }.getOrNull()
                        val (pickedCandidate, isHls) = verifiedPick
                        return@coroutineScope buildVerifiedResult(
                            pickedCandidate = pickedCandidate,
                            isHls = isHls,
                            chosenVideo = chosenVideo,
                            chosenAudio = chosenAudio,
                            matchedHeaders = matchedHeaders,
                            oEmbedMeta = oEmbedMeta
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "InnerTube ${cfg.clientName} error: ${e.message}")
            }
        }

        // Attempt 1b: Direct mobile watch-page HTML ytInitialPlayerResponse extraction (works for copyrighted official MVs)
        try {
            val watchJson = fetchWatchPagePlayerJson(videoId)
            if (watchJson != null) {
                val details = watchJson.optJSONObject("videoDetails")
                if (details != null) {
                    lastTitle = details.optString("title").takeIf { it.isNotBlank() } ?: lastTitle
                    lastAuthor = details.optString("author").takeIf { it.isNotBlank() } ?: lastAuthor
                    lastDuration = details.optString("lengthSeconds").toIntOrNull() ?: lastDuration
                }
                val streamingData = watchJson.optJSONObject("streamingData")
                if (streamingData != null) {
                    val watchHeaders = mapOf(
                        "User-Agent" to MOBILE_SAFARI_UA,
                        "Referer" to "https://m.youtube.com/watch?v=$videoId"
                    )
                    val progressiveCandidates = extractCandidatesFromArray(
                        arr = streamingData.optJSONArray("formats"),
                        audioOnly = false
                    )
                    val audioCandidates = extractCandidatesFromArray(
                        arr = streamingData.optJSONArray("adaptiveFormats"),
                        audioOnly = true
                    )
                    val chosenVideo = selectBestVideoCandidate(progressiveCandidates, preferredQuality.maxHeightPx)
                    val chosenAudio = selectBestAudioCandidate(
                        candidates = audioCandidates,
                        preferHighBitrate = preferredQuality != PreferredQuality.DATA_SAVER
                    )
                    val hlsUrl = streamingData.optString("hlsManifestUrl").takeIf { it.startsWith("http") }
                        ?.let { StreamFormatCandidate(url = it, heightPx = preferredQuality.maxHeightPx, mimeType = "application/x-mpegurl") }

                    val candidateOrder = if (requireVideoTrack) {
                        listOf(chosenVideo to false, hlsUrl to true)
                    } else {
                        listOf(chosenVideo to false, hlsUrl to true, chosenAudio to false)
                    }
                    val verifiedPick = pickFirstVerifiedCandidate(
                        candidates = candidateOrder,
                        headers = watchHeaders,
                        verifyNetworkStream = verifyNetworkStream
                    )
                    if (verifiedPick != null) {
                        val oEmbedMeta = runCatching { oEmbedDeferred.await() }.getOrNull()
                        val (pickedCandidate, isHls) = verifiedPick
                        return@coroutineScope buildVerifiedResult(
                            pickedCandidate = pickedCandidate,
                            isHls = isHls,
                            chosenVideo = chosenVideo,
                            chosenAudio = chosenAudio,
                            matchedHeaders = watchHeaders,
                            oEmbedMeta = oEmbedMeta
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Watch page HTML player extraction failed: ${e.message}")
        }

        // Attempt 2: Race public Piped JSON stream endpoints in parallel
        val pipedInstances = listOf(
            "https://pipedapi.kavin.rocks/streams/$videoId",
            "https://pipedapi.tokhmi.xyz/streams/$videoId",
            "https://pipedapi.adminforge.de/streams/$videoId"
        )
        val pipedResults = pipedInstances.map { apiUrl ->
            async { queryPipedEndpoint(apiUrl, videoId, originalPageUrl, isShortHint, preferredQuality, silentFallback, lastThumb, verifyNetworkStream, requireVideoTrack) }
        }.mapNotNull { runCatching { it.await() }.getOrNull() }
        if (pipedResults.isNotEmpty()) {
            return@coroutineScope pipedResults.first()
        }

        // Attempt 3: Race public Invidious JSON stream endpoints in parallel
        val invidiousInstances = listOf(
            "https://inv.nadeko.net/api/v1/videos/$videoId",
            "https://iv.datura.network/api/v1/videos/$videoId"
        )
        val invidiousResults = invidiousInstances.map { apiUrl ->
            async { queryInvidiousEndpoint(apiUrl, originalPageUrl, isShortHint, preferredQuality, silentFallback, lastThumb, verifyNetworkStream, requireVideoTrack) }
        }.mapNotNull { runCatching { it.await() }.getOrNull() }
        if (invidiousResults.isNotEmpty()) {
            return@coroutineScope invidiousResults.first()
        }

        // Attempt 4: Return mobile watch URL for hidden stream sniffer backup (streamUrl remains null)
        val oEmbedMeta = runCatching { oEmbedDeferred.await() }.getOrNull()
        lastTitle = lastTitle ?: oEmbedMeta?.first
        lastAuthor = lastAuthor ?: oEmbedMeta?.second
        lastAuthorHandle = lastAuthorHandle ?: oEmbedMeta?.third
        val isOfficial = RahmaCatalog.isOfficialRahmaChannelName(lastAuthor.orEmpty(), lastAuthorHandle.orEmpty())
        val resolvedChannel = if (isOfficial) RahmaCatalog.OFFICIAL_CHANNEL_AR_NAME else lastAuthor
        val resolvedHandle = if (isOfficial) RahmaCatalog.OFFICIAL_CHANNEL_HANDLE else lastAuthorHandle
        ResolvedStreamResult(
            streamUrl = null,
            pageUrl = originalPageUrl,
            fallbackEmbedUrl = silentFallback,
            snifferUrl = snifferWatchUrl,
            title = lastTitle,
            thumbnailUrl = lastThumb,
            platformLabel = resolvedChannel ?: "يوتيوب",
            channelName = resolvedChannel,
            channelHandle = resolvedHandle,
            isShort = isShortHint,
            durationSeconds = lastDuration,
            isDirectVerified = false,
            errorMessage = "جارٍ البحث عن رابط بث مباشر بديل"
        )
    }

    private fun fetchWatchPagePlayerJson(videoId: String): JSONObject? {
        return try {
            val req = Request.Builder()
                .url("https://m.youtube.com/watch?v=$videoId&hl=ar")
                .header("User-Agent", MOBILE_SAFARI_UA)
                .header("Accept-Language", "ar,en;q=0.9")
                .header("Cookie", "CONSENT=YES+cb.20210328-17-p0.en+FX+999; SOCS=CAI")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val html = resp.body?.string() ?: return null
                val marker = "ytInitialPlayerResponse"
                val markerIdx = html.indexOf(marker)
                if (markerIdx == -1) return null
                val braceStart = html.indexOf('{', markerIdx)
                if (braceStart == -1) return null
                var depth = 0
                var inString = false
                var escaped = false
                for (i in braceStart until html.length) {
                    val ch = html[i]
                    if (escaped) {
                        escaped = false
                        continue
                    }
                    if (ch == '\\' && inString) {
                        escaped = true
                        continue
                    }
                    if (ch == '"') {
                        inString = !inString
                        continue
                    }
                    if (!inString) {
                        if (ch == '{') depth++
                        else if (ch == '}') {
                            depth--
                            if (depth == 0) {
                                val jsonStr = html.substring(braceStart, i + 1)
                                return JSONObject(jsonStr)
                            }
                        }
                    }
                }
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun queryPipedEndpoint(
        apiUrl: String,
        videoId: String,
        originalPageUrl: String,
        isShortHint: Boolean,
        preferredQuality: PreferredQuality,
        silentFallback: String,
        defaultThumb: String,
        verifyNetworkStream: Boolean,
        requireVideoTrack: Boolean = false
    ): ResolvedStreamResult? {
        return try {
            val req = Request.Builder()
                .url(apiUrl)
                .header("User-Agent", ANDROID_CHROME_UA)
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val json = JSONObject(resp.body?.string() ?: return null)
                val title = json.optString("title").takeIf { it.isNotBlank() }
                val uploader = json.optString("uploader").takeIf { it.isNotBlank() }
                val uploaderUrl = json.optString("uploaderUrl")
                val handle = extractHandleFromPath(uploaderUrl)
                val thumb = json.optString("thumbnailUrl").takeIf { it.startsWith("http") } ?: defaultThumb
                val dur = json.optInt("duration", 0)
                val headers = mapOf("User-Agent" to ANDROID_CHROME_UA)

                val videoCandidates = mutableListOf<StreamFormatCandidate>()
                val streams = json.optJSONArray("videoStreams")
                if (streams != null) {
                    for (i in 0 until streams.length()) {
                        val st = streams.optJSONObject(i) ?: continue
                        if (!st.optBoolean("videoOnly", true)) {
                            val url = st.optString("url")
                            if (url.startsWith("http")) {
                                val height = st.optInt("height", parseQualityHeight(st.optString("quality")))
                                videoCandidates.add(
                                    StreamFormatCandidate(
                                        url = url,
                                        heightPx = height,
                                        bitrateBps = st.optLong("bitrate", 0L),
                                        mimeType = st.optString("mimeType", "video/mp4")
                                    )
                                )
                            }
                        }
                    }
                }

                val audioCandidates = mutableListOf<StreamFormatCandidate>()
                val audioStreams = json.optJSONArray("audioStreams")
                if (audioStreams != null) {
                    for (i in 0 until audioStreams.length()) {
                        val st = audioStreams.optJSONObject(i) ?: continue
                        val url = st.optString("url")
                        if (url.startsWith("http")) {
                            audioCandidates.add(
                                StreamFormatCandidate(
                                    url = url,
                                    heightPx = 0,
                                    bitrateBps = st.optLong("bitrate", 128_000L),
                                    mimeType = st.optString("mimeType", "audio/mp4")
                                )
                            )
                        }
                    }
                }

                val chosenVideo = selectBestVideoCandidate(videoCandidates, preferredQuality.maxHeightPx)
                val chosenAudio = selectBestAudioCandidate(
                    audioCandidates,
                    preferHighBitrate = preferredQuality != PreferredQuality.DATA_SAVER
                )
                val hls = json.optString("hls").takeIf { it.startsWith("http") }
                    ?.let { StreamFormatCandidate(url = it, heightPx = preferredQuality.maxHeightPx, mimeType = "application/x-mpegurl") }

                val verifiedPick = pickFirstVerifiedCandidate(
                    candidates = if (requireVideoTrack) listOf(chosenVideo to false, hls to true) else listOf(chosenVideo to false, hls to true, chosenAudio to false),
                    headers = headers,
                    verifyNetworkStream = verifyNetworkStream
                ) ?: return null

                val (pickedCandidate, isHls) = verifiedPick
                val isOfficial = RahmaCatalog.isOfficialRahmaChannelName(uploader.orEmpty(), handle.orEmpty())
                val resolvedChannel = if (isOfficial) RahmaCatalog.OFFICIAL_CHANNEL_AR_NAME else uploader
                val resolvedHandle = if (isOfficial) RahmaCatalog.OFFICIAL_CHANNEL_HANDLE else handle
                ResolvedStreamResult(
                    streamUrl = pickedCandidate.url,
                    downloadVideoUrl = chosenVideo?.url?.takeIf { !isHls },
                    downloadAudioUrl = chosenAudio?.url ?: chosenVideo?.url,
                    videoMimeType = if (isHls) "application/x-mpegurl" else pickedCandidate.mimeType,
                    audioMimeType = chosenAudio?.mimeType ?: "audio/mp4",
                    selectedQualityLabel = "${pickedCandidate.heightPx.coerceAtLeast(360)}p",
                    pageUrl = originalPageUrl,
                    fallbackEmbedUrl = silentFallback,
                    requestHeaders = headers,
                    title = title,
                    thumbnailUrl = thumb,
                    platformLabel = resolvedChannel ?: "يوتيوب",
                    channelName = resolvedChannel,
                    channelHandle = resolvedHandle,
                    isShort = isShortHint,
                    durationSeconds = dur,
                    isDirectVerified = true
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Piped fallback failed ($apiUrl): ${e.message}")
            null
        }
    }

    private fun queryInvidiousEndpoint(
        apiUrl: String,
        originalPageUrl: String,
        isShortHint: Boolean,
        preferredQuality: PreferredQuality,
        silentFallback: String,
        defaultThumb: String,
        verifyNetworkStream: Boolean,
        requireVideoTrack: Boolean = false
    ): ResolvedStreamResult? {
        return try {
            val req = Request.Builder()
                .url(apiUrl)
                .header("User-Agent", ANDROID_CHROME_UA)
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val json = JSONObject(resp.body?.string() ?: return null)
                val title = json.optString("title").takeIf { it.isNotBlank() }
                val author = json.optString("author").takeIf { it.isNotBlank() }
                val authorUrl = json.optString("authorUrl")
                val handle = extractHandleFromPath(authorUrl)
                val dur = json.optInt("lengthSeconds", 0)
                val headers = mapOf("User-Agent" to ANDROID_CHROME_UA)

                val videoCandidates = mutableListOf<StreamFormatCandidate>()
                val formats = json.optJSONArray("formatStreams")
                if (formats != null) {
                    for (i in 0 until formats.length()) {
                        val fmt = formats.optJSONObject(i) ?: continue
                        val url = fmt.optString("url")
                        if (url.startsWith("http")) {
                            val height = parseQualityHeight(fmt.optString("resolution").ifBlank { fmt.optString("qualityLabel") })
                            videoCandidates.add(
                                StreamFormatCandidate(
                                    url = url,
                                    heightPx = height,
                                    mimeType = fmt.optString("type", "video/mp4").substringBefore(";")
                                )
                            )
                        }
                    }
                }

                val audioCandidates = extractCandidatesFromArray(
                    arr = json.optJSONArray("adaptiveFormats"),
                    audioOnly = true
                )

                val chosenVideo = selectBestVideoCandidate(videoCandidates, preferredQuality.maxHeightPx)
                val chosenAudio = selectBestAudioCandidate(
                    audioCandidates,
                    preferHighBitrate = preferredQuality != PreferredQuality.DATA_SAVER
                )
                val hls = json.optString("hlsUrl").takeIf { it.startsWith("http") }
                    ?.let { StreamFormatCandidate(url = it, heightPx = preferredQuality.maxHeightPx, mimeType = "application/x-mpegurl") }

                val verifiedPick = pickFirstVerifiedCandidate(
                    candidates = if (requireVideoTrack) listOf(chosenVideo to false, hls to true) else listOf(chosenVideo to false, hls to true, chosenAudio to false),
                    headers = headers,
                    verifyNetworkStream = verifyNetworkStream
                ) ?: return null

                val (pickedCandidate, isHls) = verifiedPick
                val isOfficial = RahmaCatalog.isOfficialRahmaChannelName(author.orEmpty(), handle.orEmpty())
                val resolvedChannel = if (isOfficial) RahmaCatalog.OFFICIAL_CHANNEL_AR_NAME else author
                val resolvedHandle = if (isOfficial) RahmaCatalog.OFFICIAL_CHANNEL_HANDLE else handle
                ResolvedStreamResult(
                    streamUrl = pickedCandidate.url,
                    downloadVideoUrl = chosenVideo?.url?.takeIf { !isHls },
                    downloadAudioUrl = chosenAudio?.url ?: chosenVideo?.url,
                    videoMimeType = if (isHls) "application/x-mpegurl" else pickedCandidate.mimeType,
                    audioMimeType = chosenAudio?.mimeType ?: "audio/mp4",
                    selectedQualityLabel = "${pickedCandidate.heightPx.coerceAtLeast(360)}p",
                    pageUrl = originalPageUrl,
                    fallbackEmbedUrl = silentFallback,
                    requestHeaders = headers,
                    title = title,
                    thumbnailUrl = defaultThumb,
                    platformLabel = resolvedChannel ?: "يوتيوب",
                    channelName = resolvedChannel,
                    channelHandle = resolvedHandle,
                    isShort = isShortHint,
                    durationSeconds = dur,
                    isDirectVerified = true
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Invidious fallback failed ($apiUrl): ${e.message}")
            null
        }
    }

    private fun extractCandidatesFromArray(arr: JSONArray?, audioOnly: Boolean): List<StreamFormatCandidate> {
        if (arr == null) return emptyList()
        val list = mutableListOf<StreamFormatCandidate>()
        for (i in 0 until arr.length()) {
            val fmt = arr.optJSONObject(i) ?: continue
            val url = fmt.optString("url")
            if (!url.startsWith("http")) continue
            val mime = fmt.optString("mimeType").ifBlank { fmt.optString("type") }.substringBefore(";")
            if (audioOnly) {
                if (mime.startsWith("audio/", ignoreCase = true)) {
                    list.add(
                        StreamFormatCandidate(
                            url = url,
                            heightPx = 0,
                            bitrateBps = fmt.optLong("bitrate", 128_000L),
                            mimeType = mime
                        )
                    )
                }
            } else {
                val height = fmt.optInt("height", parseQualityHeight(fmt.optString("qualityLabel")))
                list.add(
                    StreamFormatCandidate(
                        url = url,
                        heightPx = height.coerceAtLeast(360),
                        bitrateBps = fmt.optLong("bitrate", 500_000L),
                        mimeType = mime.ifBlank { "video/mp4" }
                    )
                )
            }
        }
        return list
    }

    private fun parseQualityHeight(label: String): Int {
        val digits = Regex("""(\d{3,4})""").find(label)?.groupValues?.getOrNull(1)?.toIntOrNull()
        return digits ?: 360
    }

    private fun extractHandleFromPath(pathOrUrl: String?): String? {
        val raw = pathOrUrl?.trim().orEmpty()
        val idx = raw.indexOf("@")
        if (idx == -1) return null
        return raw.substring(idx).substringBefore("/").substringBefore("?").substringBefore("#").takeIf { it.length > 1 }
    }

    private fun fetchYouTubeOEmbedMeta(videoId: String): Triple<String?, String?, String?>? {
        return try {
            val req = Request.Builder()
                .url("https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=$videoId&format=json")
                .header("User-Agent", ANDROID_CHROME_UA)
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val json = JSONObject(resp.body?.string() ?: return null)
                val title = json.optString("title").takeIf { it.isNotBlank() }
                val author = json.optString("author_name").takeIf { it.isNotBlank() }
                val authorHandle = extractHandleFromPath(json.optString("author_url"))
                Triple(title, author, authorHandle)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveInstagramStream(url: String, verifyNetworkStream: Boolean): ResolvedStreamResult {
        val cleanUrl = url.substringBefore("?").trimEnd('/') + "/"
        val embedFallback = "${cleanUrl}embed/captioned/"
        val headers = mapOf(
            "User-Agent" to MOBILE_SAFARI_UA,
            "Referer" to "https://www.instagram.com/"
        )

        var resolvedTitle: String? = null
        var resolvedThumb: String? = null
        var resolvedHandle: String? = Regex("""instagram\.com/([A-Za-z0-9._]+)/(?:reel|p|tv)/""", RegexOption.IGNORE_CASE)
            .find(cleanUrl)?.groupValues?.getOrNull(1)?.let { "@$it" }
            ?: if (cleanUrl.contains("rahmariadh", ignoreCase = true)) "@rahmariadh" else null

        val shortcode = Regex("""instagram\.com/(?:[^/]+/)?(?:reel|p|tv)/([A-Za-z0-9_-]+)""", RegexOption.IGNORE_CASE)
            .find(cleanUrl)?.groupValues?.getOrNull(1)
        val apiJsonUrl = shortcode?.let { "https://www.instagram.com/p/$it/?__a=1&__d=dis" }

        for (target in listOfNotNull(apiJsonUrl, cleanUrl, embedFallback)) {
            try {
                val reqBuilder = Request.Builder()
                    .url(target)
                    .header("User-Agent", MOBILE_SAFARI_UA)
                    .header("Referer", "https://www.instagram.com/")
                if (target.contains("__a=1")) {
                    reqBuilder
                        .header("X-IG-App-ID", "936619743392459")
                        .header("X-Requested-With", "XMLHttpRequest")
                        .header("Accept", "application/json, text/plain, */*")
                } else {
                    reqBuilder.header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                }
                client.newCall(reqBuilder.build()).execute().use { resp ->
                    val bodyStr = resp.body?.string() ?: ""
                    if (bodyStr.isBlank()) return@use

                    if (resolvedTitle == null) {
                        resolvedTitle = extractMetaContent(bodyStr, "og:title")
                    }
                    if (resolvedThumb == null) {
                        resolvedThumb = extractMetaContent(bodyStr, "og:image")
                    }
                    if (resolvedHandle == null) {
                        val handleMatch = Regex("""(@[A-Za-z0-9._]{2,})""").find(resolvedTitle.orEmpty())
                        resolvedHandle = handleMatch?.groupValues?.getOrNull(1)
                    }

                    val jsonVideoMatches = Regex(""""url"\s*:\s*"(https:[^"]+\.mp4[^"]*)"""")
                        .findAll(bodyStr)
                        .map { it.groupValues[1] }
                        .toList()
                    val candidates = (listOfNotNull(
                        extractMetaContent(bodyStr, "og:video:secure_url"),
                        extractMetaContent(bodyStr, "og:video"),
                        extractRegexGroup(bodyStr, Regex(""""video_url"\s*:\s*"(https:[^"]+)"""")),
                        extractRegexGroup(bodyStr, Regex(""""contentUrl"\s*:\s*"(https:[^"]+\.mp4[^"]*)"""")),
                        extractRegexGroup(bodyStr, Regex("""(https?:[^"'\s<>]+\.mp4[^"'\s<>]*)"""))
                    ) + jsonVideoMatches).map { rawCandidate ->
                        rawCandidate
                            .replace("\\u0026", "&")
                            .replace("\\/", "/")
                            .replace("&amp;", "&")
                    }.distinct()

                    for (candidate in candidates) {
                        if (isDirectMediaUri(candidate) && (!verifyNetworkStream || verifyDirectPlayableStream(candidate, headers))) {
                            return ResolvedStreamResult(
                                streamUrl = candidate,
                                downloadVideoUrl = candidate,
                                downloadAudioUrl = candidate,
                                pageUrl = cleanUrl,
                                fallbackEmbedUrl = embedFallback,
                                snifferUrl = cleanUrl,
                                requestHeaders = headers,
                                title = resolvedTitle ?: "ريلز إنستغرام",
                                thumbnailUrl = resolvedThumb,
                                platformLabel = resolvedHandle?.let { "انستغرام • $it" } ?: "انستغرام",
                                channelName = resolvedHandle ?: "انستغرام",
                                channelHandle = resolvedHandle,
                                isShort = true,
                                isDirectVerified = true
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Instagram resolution error ($target): ${e.message}")
            }
        }

        return ResolvedStreamResult(
            streamUrl = null,
            pageUrl = cleanUrl,
            fallbackEmbedUrl = embedFallback,
            requestHeaders = headers,
            title = resolvedTitle ?: "ريلز إنستغرام",
            thumbnailUrl = resolvedThumb,
            platformLabel = resolvedHandle?.let { "انستغرام • $it" } ?: "انستغرام",
            channelName = resolvedHandle ?: "انستغرام",
            channelHandle = resolvedHandle,
            isShort = true,
            isDirectVerified = false
        )
    }

    private suspend fun resolveTikTokStream(
        url: String,
        preferredQuality: PreferredQuality,
        verifyNetworkStream: Boolean
    ): ResolvedStreamResult = coroutineScope {
        var resolvedTitle: String? = null
        var resolvedThumb: String? = null
        var resolvedAuthor: String? = null
        var resolvedHandle: String? = extractHandleFromPath(url)
        val headers = mapOf(
            "User-Agent" to ANDROID_CHROME_UA,
            "Referer" to "https://www.tiktok.com/"
        )
        val tiktokId = Regex("""/video/(\d+)""").find(url)?.groupValues?.getOrNull(1)
        val embedFallback = if (!tiktokId.isNullOrBlank()) {
            "https://www.tiktok.com/embed/v2/$tiktokId"
        } else {
            url
        }

        val oembedDeferred = async {
            try {
                val encoded = URLEncoder.encode(url, "UTF-8")
                val oembedReq = Request.Builder()
                    .url("https://www.tiktok.com/oembed?url=$encoded")
                    .header("User-Agent", ANDROID_CHROME_UA)
                    .build()
                client.newCall(oembedReq).execute().use { resp ->
                    if (resp.isSuccessful) {
                        JSONObject(resp.body?.string() ?: "{}")
                    } else null
                }
            } catch (_: Exception) { null }
        }

        try {
            val encoded = URLEncoder.encode(url, "UTF-8")
            val req = Request.Builder()
                .url("https://www.tikwm.com/api/?url=$encoded")
                .header("User-Agent", ANDROID_CHROME_UA)
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val json = JSONObject(resp.body?.string() ?: "{}")
                    val data = json.optJSONObject("data")
                    if (data != null) {
                        val hdUrl = data.optString("hdplay").takeIf { it.startsWith("http") }
                        val playUrl = data.optString("play").takeIf { it.startsWith("http") }
                        val musicUrl = data.optString("music").takeIf { it.startsWith("http") }
                        val orderedUrls = if (preferredQuality == PreferredQuality.DATA_SAVER) {
                            listOfNotNull(playUrl, hdUrl)
                        } else {
                            listOfNotNull(hdUrl, playUrl)
                        }
                        val chosenPlay = orderedUrls.firstOrNull { candidate ->
                            isDirectMediaUri(candidate) && (!verifyNetworkStream || verifyDirectPlayableStream(candidate, headers))
                        }

                        val title = data.optString("title").takeIf { it.isNotBlank() }
                        val cover = data.optString("cover").takeIf { it.startsWith("http") }
                        val authorObj = data.optJSONObject("author")
                        val nickname = authorObj?.optString("nickname")?.takeIf { it.isNotBlank() }
                        val uniqueId = authorObj?.optString("unique_id")?.takeIf { it.isNotBlank() }
                        val handle = uniqueId?.let { "@${it.removePrefix("@")}" } ?: resolvedHandle
                        val dur = data.optInt("duration", 0)
                        if (chosenPlay != null) {
                            return@coroutineScope ResolvedStreamResult(
                                streamUrl = chosenPlay,
                                downloadVideoUrl = chosenPlay,
                                downloadAudioUrl = musicUrl ?: chosenPlay,
                                audioMimeType = if (musicUrl != null) "audio/mpeg" else "video/mp4",
                                pageUrl = url,
                                fallbackEmbedUrl = embedFallback,
                                requestHeaders = headers,
                                title = title ?: "فيديو تيك توك",
                                thumbnailUrl = cover,
                                platformLabel = nickname ?: "تيك توك",
                                channelName = nickname ?: handle ?: "تيك توك",
                                channelHandle = handle,
                                isShort = true,
                                durationSeconds = dur,
                                isDirectVerified = true
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "TikTok resolution error: ${e.message}")
        }

        val oembedJson = runCatching { oembedDeferred.await() }.getOrNull()
        if (oembedJson != null) {
            resolvedTitle = oembedJson.optString("title").takeIf { it.isNotBlank() }
            resolvedThumb = oembedJson.optString("thumbnail_url").takeIf { it.startsWith("http") }
            resolvedAuthor = oembedJson.optString("author_name").takeIf { it.isNotBlank() }
            val uniqueId = oembedJson.optString("author_unique_id").takeIf { it.isNotBlank() }
            if (uniqueId != null) resolvedHandle = "@${uniqueId.removePrefix("@")}"
        }

        ResolvedStreamResult(
            streamUrl = null,
            pageUrl = url,
            fallbackEmbedUrl = embedFallback,
            requestHeaders = headers,
            title = resolvedTitle ?: "فيديو تيك توك",
            thumbnailUrl = resolvedThumb,
            platformLabel = resolvedAuthor ?: "تيك توك",
            channelName = resolvedAuthor ?: resolvedHandle ?: "تيك توك",
            channelHandle = resolvedHandle,
            isShort = true,
            isDirectVerified = false
        )
    }

    private fun resolveRedirectTarget(url: String): String? {
        val unwrapped = LinkValidator.unwrapRedirectWrapperUrl(url)
        if (unwrapped != url.trim()) return unwrapped
        return try {
            val req = Request.Builder()
                .url(unwrapped)
                .header("User-Agent", ANDROID_CHROME_UA)
                .build()
            client.newCall(req).execute().use { resp ->
                val landed = resp.request.url.toString().takeIf { it.startsWith("http") } ?: return@use null
                val cleanLanded = LinkValidator.unwrapRedirectWrapperUrl(landed)
                if (cleanLanded != unwrapped) return@use cleanLanded
                val body = resp.body?.string().orEmpty()
                val jsOrMetaRedirect = Regex("""(?:URL=|location\.replace\(['"]|href=['"])(https?://[^"'\s>]+)""", RegexOption.IGNORE_CASE)
                    .find(body)?.groupValues?.getOrNull(1)?.replace("&amp;", "&")
                jsOrMetaRedirect?.let { LinkValidator.unwrapRedirectWrapperUrl(it) } ?: cleanLanded
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveFacebookStream(
        url: String,
        preferredQuality: PreferredQuality,
        verifyNetworkStream: Boolean
    ): ResolvedStreamResult {
        val headers = mapOf(
            "User-Agent" to ANDROID_CHROME_UA,
            "Referer" to "https://www.facebook.com/"
        )
        val encoded = runCatching { URLEncoder.encode(url, "UTF-8") }.getOrDefault(url)
        val embedFallback = "https://www.facebook.com/plugins/video.php?href=$encoded&show_text=0&autoplay=1"
        var resolvedTitle: String? = null
        var resolvedThumb: String? = null

        try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", ANDROID_CHROME_UA)
                .header("Accept-Language", "ar,en;q=0.9")
                .build()
            client.newCall(req).execute().use { resp ->
                val html = resp.body?.string().orEmpty()
                resolvedTitle = extractMetaContent(html, "og:title") ?: "فيديو فيسبوك"
                resolvedThumb = extractMetaContent(html, "og:image")
                val hd = extractRegexGroup(html, Regex(""""browser_native_hd_url"\s*:\s*"(https:[^"]+)""""))
                    ?: extractRegexGroup(html, Regex(""""playable_url_quality_hd"\s*:\s*"(https:[^"]+)""""))
                val sd = extractRegexGroup(html, Regex(""""browser_native_sd_url"\s*:\s*"(https:[^"]+)""""))
                    ?: extractRegexGroup(html, Regex(""""playable_url"\s*:\s*"(https:[^"]+)""""))
                val og = extractMetaContent(html, "og:video:secure_url")
                    ?: extractMetaContent(html, "og:video")

                val ordered = if (preferredQuality == PreferredQuality.DATA_SAVER) {
                    listOfNotNull(sd, og, hd)
                } else {
                    listOfNotNull(hd, og, sd)
                }.map {
                    it.replace("\\u0026", "&").replace("\\/", "/").replace("&amp;", "&")
                }.distinct()

                for (cleaned in ordered) {
                    if (isDirectMediaUri(cleaned) && (!verifyNetworkStream || verifyDirectPlayableStream(cleaned, headers))) {
                        return ResolvedStreamResult(
                            streamUrl = cleaned,
                            downloadVideoUrl = cleaned,
                            downloadAudioUrl = cleaned,
                            pageUrl = url,
                            fallbackEmbedUrl = embedFallback,
                            requestHeaders = headers,
                            title = resolvedTitle,
                            thumbnailUrl = resolvedThumb,
                            platformLabel = "فيسبوك",
                            channelName = "فيسبوك",
                            isShort = url.contains("/reel/", ignoreCase = true),
                            isDirectVerified = true
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Facebook resolution error: ${e.message}")
        }

        return ResolvedStreamResult(
            streamUrl = null,
            pageUrl = url,
            fallbackEmbedUrl = embedFallback,
            requestHeaders = headers,
            title = resolvedTitle ?: "فيديو فيسبوك",
            thumbnailUrl = resolvedThumb,
            platformLabel = "فيسبوك",
            channelName = "فيسبوك",
            isShort = url.contains("/reel/", ignoreCase = true),
            isDirectVerified = false
        )
    }

    private fun resolveGenericWebVideo(url: String, verifyNetworkStream: Boolean): ResolvedStreamResult {
        val headers = mapOf("User-Agent" to ANDROID_CHROME_UA, "Referer" to url)
        var resolvedTitle: String? = null
        var resolvedThumb: String? = null
        try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", ANDROID_CHROME_UA)
                .build()
            client.newCall(req).execute().use { resp ->
                val contentType = resp.header("Content-Type")?.lowercase() ?: ""
                if (contentType.startsWith("video/") || contentType.startsWith("audio/") || contentType.contains("mpegurl")) {
                    val directUrl = resp.request.url.toString()
                    val isDirectDl = !contentType.contains("mpegurl") && !directUrl.contains(".m3u8")
                    return ResolvedStreamResult(
                        streamUrl = directUrl,
                        downloadVideoUrl = if (isDirectDl) directUrl else null,
                        downloadAudioUrl = if (isDirectDl) directUrl else null,
                        videoMimeType = if (contentType.contains("mpegurl")) "application/x-mpegurl" else "video/mp4",
                        pageUrl = url,
                        requestHeaders = headers,
                        platformLabel = "بث مباشر",
                        isDirectVerified = true
                    )
                }
                val html = resp.body?.string() ?: ""
                resolvedTitle = extractMetaContent(html, "og:title")
                resolvedThumb = extractMetaContent(html, "og:image")
                val candidates = listOfNotNull(
                    extractMetaContent(html, "og:video:secure_url"),
                    extractMetaContent(html, "og:video"),
                    extractMetaContent(html, "twitter:player:stream"),
                    extractRegexGroup(html, Regex(""""contentUrl"\s*:\s*"(https?://[^"]+)"""")),
                    extractRegexGroup(html, Regex("""<(?:video|source)[^>]+src=["'](https?://[^"']+)["']""")),
                    extractRegexGroup(html, Regex("""(https?://[^\s"'<>]+\.(?:mp4|m3u8|webm)(?:\?[^\s"'<>]*)?)"""))
                ).map {
                    it.replace("\\u0026", "&").replace("\\/", "/").replace("&amp;", "&")
                }.distinct()

                for (cleanVideo in candidates) {
                    if (isDirectMediaUri(cleanVideo) && (!verifyNetworkStream || verifyDirectPlayableStream(cleanVideo, headers))) {
                        val isHls = isHlsStreamUrl(cleanVideo)
                        return ResolvedStreamResult(
                            streamUrl = cleanVideo,
                            downloadVideoUrl = if (!isHls) cleanVideo else null,
                            downloadAudioUrl = if (!isHls) cleanVideo else null,
                            videoMimeType = if (isHls) "application/x-mpegurl" else "video/mp4",
                            pageUrl = url,
                            fallbackEmbedUrl = url,
                            requestHeaders = headers,
                            title = resolvedTitle,
                            thumbnailUrl = resolvedThumb,
                            platformLabel = "فيديو ويب",
                            isDirectVerified = true
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Generic web video extraction failed: ${e.message}")
        }
        return ResolvedStreamResult(
            streamUrl = null,
            pageUrl = url,
            fallbackEmbedUrl = url,
            requestHeaders = headers,
            title = resolvedTitle,
            thumbnailUrl = resolvedThumb,
            platformLabel = "الويب",
            isDirectVerified = false
        )
    }

    private fun extractMetaContent(html: String, propertyName: String): String? {
        val pattern1 = Regex("""<meta[^>]+(?:property|name)=["']$propertyName["'][^>]+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val pattern2 = Regex("""<meta[^>]+content=["']([^"']+)["'][^>]+(?:property|name)=["']$propertyName["']""", RegexOption.IGNORE_CASE)
        return pattern1.find(html)?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
            ?: pattern2.find(html)?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
    }

    private fun extractRegexGroup(html: String, regex: Regex): String? {
        return regex.find(html)?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
    }

    private data class InnerTubeClient(
        val clientName: String,
        val clientVersion: String,
        val clientHeaderId: String,
        val userAgent: String,
        val embedHost: String? = null
    )
}
