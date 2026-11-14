package com.example.data.remote

import android.util.Log
import android.util.Xml
import com.example.data.RahmaCatalog
import com.example.data.local.CachedChannelMediaEntity
import com.example.model.Song
import com.example.model.SongCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class ChannelFetchPage(
    val items: List<CachedChannelMediaEntity>,
    val nextContinuationToken: String?,
    val channelId: String?
)

object OfficialChannelFeedService {

    private const val TAG = "OfficialChannelFeed"
    private const val OFFICIAL_HANDLE = "@RahmaRiad"
    private const val OFFICIAL_IG_USERNAME = "rahmariadh"
    private const val OFFICIAL_IG_HANDLE = "@rahmariadh"
    private const val IG_WEB_APP_ID = "936619743392459"
    private const val CONSENT_COOKIE = "CONSENT=YES+cb.20210328-17-p0.en+FX+999; SOCS=CAI"
    private const val BROWSER_UA =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"

    @Volatile
    private var cachedOfficialChannelId: String? = null

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /**
     * Unlimited live channel fetcher for @RahmaRiad official videos & shorts.
     * Supports initial multi-tier fetch AND InnerTube continuation pagination without fixed caps.
     */
    suspend fun fetchOfficialChannelMedia(): List<CachedChannelMediaEntity> {
        return fetchOfficialChannelMediaPage(continuationToken = null).items
    }

    suspend fun fetchOfficialChannelMediaPage(
        continuationToken: String? = null
    ): ChannelFetchPage = withContext(Dispatchers.IO) {
        val collected = linkedMapOf<String, CachedChannelMediaEntity>()
        var nextToken: String? = null

        fun addAllUnique(items: List<CachedChannelMediaEntity>) {
            for (item in items) {
                if (!collected.containsKey(item.id)) {
                    collected[item.id] = item
                }
            }
        }

        val resolvedChannelId = cachedOfficialChannelId
            ?: resolveChannelIdFromHandle("https://www.youtube.com/@RahmaRiad")
            ?: resolveChannelIdFromHandle("https://www.youtube.com/@RahmaRiad/videos")
        if (!resolvedChannelId.isNullOrBlank()) {
            cachedOfficialChannelId = resolvedChannelId
        }

        if (!continuationToken.isNullOrBlank()) {
            val paged = fetchViaInnerTubeBrowse(
                browseId = resolvedChannelId,
                params = null,
                continuationToken = continuationToken,
                isShortsTab = false,
                isOfficialBrowse = true
            )
            addAllUnique(paged.items)
            nextToken = paged.nextContinuationToken
            return@withContext ChannelFetchPage(
                items = collected.values.toList(),
                nextContinuationToken = nextToken,
                channelId = resolvedChannelId
            )
        }

        // 1. Official @RahmaRiad YouTube Browse (Videos + Shorts) & Official @rahmariadh Instagram Media in parallel
        coroutineScope {
            val videosJob = if (!resolvedChannelId.isNullOrBlank()) {
                async {
                    fetchViaInnerTubeBrowse(
                        browseId = resolvedChannelId,
                        params = "EgZ2aWRlb3PyBgQKAjoA", // Videos tab
                        continuationToken = null,
                        isShortsTab = false,
                        isOfficialBrowse = true
                    )
                }
            } else null

            val shortsJob = if (!resolvedChannelId.isNullOrBlank()) {
                async {
                    fetchViaInnerTubeBrowse(
                        browseId = resolvedChannelId,
                        params = "EgZzaG9ydHPyBgUKA5oBAA%3D%3D", // Shorts tab
                        continuationToken = null,
                        isShortsTab = true,
                        isOfficialBrowse = true
                    )
                }
            } else null

            val instagramJob = async {
                fetchOfficialInstagramMedia(OFFICIAL_IG_USERNAME)
            }

            val videosRes = videosJob?.await()
            val shortsRes = shortsJob?.await()
            val igMedia = instagramJob.await()

            if (videosRes != null) addAllUnique(videosRes.items)
            if (shortsRes != null) addAllUnique(shortsRes.items)
            addAllUnique(igMedia)
            nextToken = videosRes?.nextContinuationToken ?: shortsRes?.nextContinuationToken
        }

        // 2. Atom/RSS feeds for latest official uploads
        val candidateFeeds = mutableListOf(
            "https://www.youtube.com/feeds/videos.xml?user=rahmariad"
        )
        if (!resolvedChannelId.isNullOrBlank()) {
            candidateFeeds.add(0, "https://www.youtube.com/feeds/videos.xml?channel_id=$resolvedChannelId")
        }
        for (feedUrl in candidateFeeds.distinct()) {
            try {
                val request = Request.Builder()
                    .url(feedUrl)
                    .header("User-Agent", BROWSER_UA)
                    .header("Cookie", CONSENT_COOKIE)
                    .header("Accept-Language", "ar,en;q=0.9")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val xmlBody = response.body?.string()
                        if (!xmlBody.isNullOrBlank()) {
                            addAllUnique(parseAtomFeed(xmlBody, resolvedChannelId))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Atom feed failed ($feedUrl): ${e.message}")
            }
        }

        // 3. Piped Official Channel JSON fallback if InnerTube Browse yielded sparse items
        if (collected.size < 12 && !resolvedChannelId.isNullOrBlank()) {
            addAllUnique(fetchPipedChannel(resolvedChannelId))
        }
        if (collected.size < 12) {
            addAllUnique(fetchViaInnerTubeSearch("رحمة رياض @RahmaRiad", isShortsQuery = false, officialChannelId = resolvedChannelId))
            addAllUnique(fetchViaInnerTubeSearch("رحمة رياض #shorts", isShortsQuery = true, officialChannelId = resolvedChannelId))
        }
        if (collected.isEmpty()) {
            addAllUnique(fetchViaPipedSearch("رحمة رياض", officialChannelId = resolvedChannelId))
        }
        if (collected.isEmpty()) {
            addAllUnique(fetchViaInvidiousSearch("رحمة رياض", officialChannelId = resolvedChannelId))
        }

        ChannelFetchPage(
            items = collected.values.toList(),
            nextContinuationToken = nextToken,
            channelId = resolvedChannelId
        )
    }

    /**
     * Unlimited Multi-Source Smart Search for everything about Rahma Riad,
     * preserving REAL channel names on every result and prioritizing Rahma Riad Official (@RahmaRiad).
     */
    suspend fun smartSearchRahmaRiad(query: String): List<Song> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return@withContext emptyList()

        val officialChannelId = cachedOfficialChannelId
            ?: resolveChannelIdFromHandle("https://www.youtube.com/@RahmaRiad")?.also {
                cachedOfficialChannelId = it
            }

        val containsArtist = cleanQuery.contains("رحمة") ||
            cleanQuery.contains("rahma", ignoreCase = true)

        val primaryChannelQuery = "@RahmaRiad $cleanQuery"
        val broadArtistQuery = if (containsArtist) cleanQuery else "رحمة رياض $cleanQuery"

        val collectedEntities = linkedMapOf<String, CachedChannelMediaEntity>()

        coroutineScope {
            val officialSearchJob = async {
                fetchViaInnerTubeSearch(primaryChannelQuery, isShortsQuery = false, officialChannelId = officialChannelId)
            }
            val broadSearchJob = async {
                fetchViaInnerTubeSearch(broadArtistQuery, isShortsQuery = false, officialChannelId = officialChannelId)
            }
            val shortsSearchJob = async {
                fetchViaInnerTubeSearch("رحمة رياض $cleanQuery #shorts", isShortsQuery = true, officialChannelId = officialChannelId)
            }

            val officialList = officialSearchJob.await()
            val broadList = broadSearchJob.await()
            val shortsList = shortsSearchJob.await()

            for (item in officialList + broadList + shortsList) {
                val existing = collectedEntities[item.videoId]
                if (existing == null || (!existing.isOfficialChannel && item.isOfficialChannel)) {
                    collectedEntities[item.videoId] = item
                }
            }
        }

        // Fallbacks if InnerTube yielded too few results
        if (collectedEntities.size < 6) {
            val piped = fetchViaPipedSearch(broadArtistQuery, officialChannelId)
            for (item in piped) {
                val existing = collectedEntities[item.videoId]
                if (existing == null || (!existing.isOfficialChannel && item.isOfficialChannel)) {
                    collectedEntities[item.videoId] = item
                }
            }
        }
        if (collectedEntities.size < 4) {
            val invidious = fetchViaInvidiousSearch(broadArtistQuery, officialChannelId)
            for (item in invidious) {
                val existing = collectedEntities[item.videoId]
                if (existing == null || (!existing.isOfficialChannel && item.isOfficialChannel)) {
                    collectedEntities[item.videoId] = item
                }
            }
        }

        collectedEntities.values.map { entity ->
            val cat = runCatching { SongCategory.valueOf(entity.categoryName) }.getOrDefault(SongCategory.LATEST)
            val realChannelName = when {
                entity.isOfficialChannel -> RahmaCatalog.OFFICIAL_CHANNEL_AR_NAME
                entity.channelName.isNotBlank() -> entity.channelName
                else -> "يوتيوب"
            }
            Song(
                id = entity.id,
                title = entity.title,
                arabicTitle = entity.title,
                category = cat,
                durationSeconds = 0,
                durationFormatted = if (entity.isShort) "ريلز" else "فيديو",
                viewsCount = entity.viewsCount,
                releaseYear = entity.publishedAt.take(4),
                description = entity.description,
                lyrics = entity.description,
                composer = entity.channelName.ifBlank { "رحمة رياض" },
                lyricist = if (entity.isOfficialChannel) "القناة الرسمية @RahmaRiad" else realChannelName,
                isNewRelease = false,
                isVideoAvailable = true,
                youtubeVideoId = entity.videoId,
                thumbnailUrl = entity.thumbnailUrl,
                sourcePlatform = realChannelName,
                publishDate = entity.publishedAt,
                externalUrl = entity.sourceUrl,
                isShort = entity.isShort,
                isUserImported = false,
                channelHandle = if (entity.isOfficialChannel) OFFICIAL_HANDLE else entity.channelHandle
            )
        }
    }

    private fun resolveChannelIdFromHandle(handleUrl: String): String? {
        return try {
            val request = Request.Builder()
                .url(handleUrl)
                .header("User-Agent", BROWSER_UA)
                .header("Cookie", CONSENT_COOKIE)
                .header("Accept-Language", "ar,en;q=0.9")
                .build()
            client.newCall(request).execute().use { response ->
                val html = response.body?.string() ?: return null
                val patterns = listOf(
                    Regex("""itemprop="channelId"\s+content="(UC[a-zA-Z0-9_-]{20,})""""),
                    Regex(""""externalId"\s*:\s*"(UC[a-zA-Z0-9_-]{20,})""""),
                    Regex(""""browseId"\s*:\s*"(UC[a-zA-Z0-9_-]{20,})""""),
                    Regex("""youtube\.com/channel/(UC[a-zA-Z0-9_-]{20,})""")
                )
                for (regex in patterns) {
                    val match = regex.find(html)
                    if (match != null) return match.groupValues[1]
                }
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not resolve handle: ${e.message}")
            null
        }
    }

    private fun fetchViaInnerTubeBrowse(
        browseId: String?,
        params: String?,
        continuationToken: String?,
        isShortsTab: Boolean,
        isOfficialBrowse: Boolean = true
    ): ChannelFetchPage {
        val results = mutableListOf<CachedChannelMediaEntity>()
        var nextContinuation: String? = null
        try {
            val payload = JSONObject().apply {
                if (!continuationToken.isNullOrBlank()) {
                    put("continuation", continuationToken)
                } else {
                    put("browseId", browseId ?: return ChannelFetchPage(emptyList(), null, browseId))
                    if (!params.isNullOrBlank()) {
                        put("params", params)
                    }
                }
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB")
                        put("clientVersion", "2.20240726.00.00")
                        put("hl", "ar")
                        put("gl", "IQ")
                    })
                })
            }.toString()

            val request = Request.Builder()
                .url("https://www.youtube.com/youtubei/v1/browse?prettyPrint=false")
                .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .header("User-Agent", BROWSER_UA)
                .header("Cookie", CONSENT_COOKIE)
                .header("Origin", "https://www.youtube.com")
                .header("Referer", "https://www.youtube.com/@RahmaRiad")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return ChannelFetchPage(results, null, browseId)
                val jsonText = response.body?.string() ?: return ChannelFetchPage(results, null, browseId)
                val root = JSONObject(jsonText)
                val browseChannelTitle = root.optJSONObject("metadata")
                    ?.optJSONObject("channelMetadataRenderer")
                    ?.optString("title")
                    ?.takeIf { it.isNotBlank() }
                    ?: root.optJSONObject("header")
                        ?.optJSONObject("c4TabbedHeaderRenderer")
                        ?.optString("title")
                        ?.takeIf { it.isNotBlank() }
                    ?: if (isOfficialBrowse) "Rahma Riad | رحمة رياض" else ""

                collectInnerTubeRenderers(
                    node = root,
                    out = results,
                    isShortsHint = isShortsTab,
                    officialChannelId = browseId,
                    defaultChannelName = browseChannelTitle,
                    defaultChannelHandle = if (isOfficialBrowse) OFFICIAL_HANDLE else "",
                    defaultIsOfficial = isOfficialBrowse
                )
                nextContinuation = extractContinuationToken(root)
            }
        } catch (e: Exception) {
            Log.w(TAG, "InnerTube browse failed: ${e.message}")
        }
        return ChannelFetchPage(results.distinctBy { it.id }, nextContinuation, browseId)
    }

    private fun extractContinuationToken(node: Any?): String? {
        when (node) {
            is JSONObject -> {
                if (node.has("continuationEndpoint")) {
                    val token = node.optJSONObject("continuationEndpoint")
                        ?.optJSONObject("continuationCommand")
                        ?.optString("token")
                        ?.takeIf { it.isNotBlank() }
                    if (token != null) return token
                }
                if (node.has("continuationCommand")) {
                    val token = node.optJSONObject("continuationCommand")
                        ?.optString("token")
                        ?.takeIf { it.isNotBlank() }
                    if (token != null) return token
                }
                val keys = node.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val found = extractContinuationToken(node.opt(key))
                    if (found != null) return found
                }
            }
            is JSONArray -> {
                for (i in 0 until node.length()) {
                    val found = extractContinuationToken(node.opt(i))
                    if (found != null) return found
                }
            }
        }
        return null
    }

    private fun fetchPipedChannel(channelId: String): List<CachedChannelMediaEntity> {
        val instances = listOf(
            "https://pipedapi.kavin.rocks/channel/$channelId",
            "https://pipedapi.tokhmi.xyz/channel/$channelId",
            "https://pipedapi.adminforge.de/channel/$channelId"
        )
        for (url in instances) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", BROWSER_UA)
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    val json = JSONObject(response.body?.string() ?: return@use)
                    val channelTitle = json.optString("name").trim().ifBlank { "Rahma Riad | رحمة رياض" }
                    val items = json.optJSONArray("relatedStreams") ?: return@use
                    val parsed = mutableListOf<CachedChannelMediaEntity>()
                    for (i in 0 until items.length()) {
                        val obj = items.optJSONObject(i) ?: continue
                        val relUrl = obj.optString("url")
                        val videoId = relUrl.substringAfter("v=").substringAfter("/shorts/").substringBefore("&")
                        val title = obj.optString("title").trim()
                        if (videoId.isBlank() || title.isBlank()) continue
                        val duration = obj.optLong("duration", 0L)
                        val isShort = obj.optBoolean("isShort", false) || (duration in 1..60)
                        val thumb = obj.optString("thumbnail").takeIf { it.startsWith("http") }
                            ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                        val views = obj.optLong("views", -1L).let {
                            if (it > 0) formatViews(it) else channelTitle
                        }
                        parsed.add(
                            CachedChannelMediaEntity(
                                id = "ch_$videoId",
                                title = title,
                                videoId = videoId,
                                sourceUrl = if (isShort) "https://www.youtube.com/shorts/$videoId" else "https://www.youtube.com/watch?v=$videoId",
                                thumbnailUrl = thumb,
                                publishedAt = obj.optString("uploadedDate").ifBlank { "حديث" },
                                viewsCount = views,
                                description = obj.optString("shortDescription").ifBlank { "$title • $channelTitle" },
                                isShort = isShort,
                                categoryName = classifyCategory(title, "", isShort).name,
                                channelName = channelTitle,
                                channelHandle = OFFICIAL_HANDLE,
                                isOfficialChannel = true,
                                fetchedAt = System.currentTimeMillis()
                            )
                        )
                    }
                    if (parsed.isNotEmpty()) return parsed
                }
            } catch (e: Exception) {
                Log.w(TAG, "Piped channel fetch failed ($url): ${e.message}")
            }
        }
        return emptyList()
    }

    private fun fetchViaInnerTubeSearch(
        query: String,
        isShortsQuery: Boolean,
        officialChannelId: String? = null
    ): List<CachedChannelMediaEntity> {
        val results = mutableListOf<CachedChannelMediaEntity>()
        try {
            val payload = JSONObject().apply {
                put("query", query)
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB")
                        put("clientVersion", "2.20240726.00.00")
                        put("hl", "ar")
                        put("gl", "IQ")
                    })
                })
            }.toString()

            val request = Request.Builder()
                .url("https://www.youtube.com/youtubei/v1/search?prettyPrint=false")
                .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .header("User-Agent", BROWSER_UA)
                .header("Cookie", CONSENT_COOKIE)
                .header("Origin", "https://www.youtube.com")
                .header("Referer", "https://www.youtube.com/")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return results
                val jsonText = response.body?.string() ?: return results
                val root = JSONObject(jsonText)
                collectInnerTubeRenderers(root, results, isShortsQuery, officialChannelId)
            }
        } catch (e: Exception) {
            Log.w(TAG, "InnerTube search failed ($query): ${e.message}")
        }
        return results
    }

    private fun collectInnerTubeRenderers(
        node: Any?,
        out: MutableList<CachedChannelMediaEntity>,
        isShortsHint: Boolean,
        officialChannelId: String? = null,
        defaultChannelName: String = "",
        defaultChannelHandle: String = "",
        defaultIsOfficial: Boolean = false
    ) {
        when (node) {
            is JSONObject -> {
                if (node.has("videoRenderer")) {
                    parseVideoRenderer(
                        renderer = node.getJSONObject("videoRenderer"),
                        isShortsHint = isShortsHint,
                        officialChannelId = officialChannelId,
                        defaultChannelName = defaultChannelName,
                        defaultChannelHandle = defaultChannelHandle,
                        defaultIsOfficial = defaultIsOfficial
                    )?.let { out.add(it) }
                }
                if (node.has("gridVideoRenderer")) {
                    parseVideoRenderer(
                        renderer = node.getJSONObject("gridVideoRenderer"),
                        isShortsHint = isShortsHint,
                        officialChannelId = officialChannelId,
                        defaultChannelName = defaultChannelName,
                        defaultChannelHandle = defaultChannelHandle,
                        defaultIsOfficial = defaultIsOfficial
                    )?.let { out.add(it) }
                }
                if (node.has("reelItemRenderer")) {
                    parseReelItemRenderer(
                        renderer = node.getJSONObject("reelItemRenderer"),
                        defaultChannelName = defaultChannelName,
                        defaultChannelHandle = defaultChannelHandle,
                        defaultIsOfficial = defaultIsOfficial
                    )?.let { out.add(it) }
                }
                if (node.has("shortsLockupViewModel")) {
                    parseShortsLockupViewModel(
                        vm = node.getJSONObject("shortsLockupViewModel"),
                        defaultChannelName = defaultChannelName,
                        defaultChannelHandle = defaultChannelHandle,
                        defaultIsOfficial = defaultIsOfficial
                    )?.let { out.add(it) }
                }
                val keys = node.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    if (key != "videoRenderer" && key != "gridVideoRenderer" &&
                        key != "reelItemRenderer" && key != "shortsLockupViewModel"
                    ) {
                        collectInnerTubeRenderers(
                            node = node.opt(key),
                            out = out,
                            isShortsHint = isShortsHint,
                            officialChannelId = officialChannelId,
                            defaultChannelName = defaultChannelName,
                            defaultChannelHandle = defaultChannelHandle,
                            defaultIsOfficial = defaultIsOfficial
                        )
                    }
                }
            }
            is JSONArray -> {
                for (i in 0 until node.length()) {
                    collectInnerTubeRenderers(
                        node = node.opt(i),
                        out = out,
                        isShortsHint = isShortsHint,
                        officialChannelId = officialChannelId,
                        defaultChannelName = defaultChannelName,
                        defaultChannelHandle = defaultChannelHandle,
                        defaultIsOfficial = defaultIsOfficial
                    )
                }
            }
        }
    }

    private fun parseVideoRenderer(
        renderer: JSONObject,
        isShortsHint: Boolean,
        officialChannelId: String? = null,
        defaultChannelName: String = "",
        defaultChannelHandle: String = "",
        defaultIsOfficial: Boolean = false
    ): CachedChannelMediaEntity? {
        val videoId = renderer.optString("videoId").trim()
        if (videoId.isBlank()) return null

        val title = extractRunsOrSimpleText(renderer.optJSONObject("title"))
        if (title.isBlank()) return null

        val lengthText = extractRunsOrSimpleText(renderer.optJSONObject("lengthText"))
        val isShort = isShortsHint ||
            title.contains("#shorts", ignoreCase = true) ||
            isUnderOneMinute(lengthText)

        val ownerRun = renderer.optJSONObject("ownerText")
            ?.optJSONArray("runs")?.optJSONObject(0)
            ?: renderer.optJSONObject("longBylineText")
                ?.optJSONArray("runs")?.optJSONObject(0)
            ?: renderer.optJSONObject("shortBylineText")
                ?.optJSONArray("runs")?.optJSONObject(0)

        val rawOwnerText = extractRunsOrSimpleText(renderer.optJSONObject("ownerText"))
            .ifBlank { extractRunsOrSimpleText(renderer.optJSONObject("longBylineText")) }
            .ifBlank { extractRunsOrSimpleText(renderer.optJSONObject("shortBylineText")) }
            .ifBlank { defaultChannelName }
            .trim()

        val browseEndpoint = ownerRun
            ?.optJSONObject("navigationEndpoint")
            ?.optJSONObject("browseEndpoint")
        val ownerBrowseId = browseEndpoint?.optString("browseId").orEmpty()
        val canonicalBaseUrl = browseEndpoint?.optString("canonicalBaseUrl").orEmpty()
        val extractedHandle = extractCanonicalHandle(canonicalBaseUrl).ifBlank { defaultChannelHandle }

        val isOfficialOwner = defaultIsOfficial ||
            (!officialChannelId.isNullOrBlank() && ownerBrowseId == officialChannelId) ||
            RahmaCatalog.isOfficialRahmaChannelName(rawOwnerText, extractedHandle)

        val finalChannelName = when {
            isOfficialOwner -> rawOwnerText.ifBlank { "Rahma Riad | رحمة رياض" }
            rawOwnerText.isNotBlank() -> rawOwnerText
            else -> "قناة موسيقية"
        }
        val finalHandle = if (isOfficialOwner) OFFICIAL_HANDLE else extractedHandle

        val views = extractRunsOrSimpleText(renderer.optJSONObject("shortViewCountText"))
            .ifBlank { extractRunsOrSimpleText(renderer.optJSONObject("viewCountText")) }
            .ifBlank { if (isShort) "ريلز" else "فيديو" }

        val published = extractRunsOrSimpleText(renderer.optJSONObject("publishedTimeText"))
            .ifBlank { "حديث" }

        val thumb = renderer.optJSONObject("thumbnail")
            ?.optJSONArray("thumbnails")
            ?.let { arr -> if (arr.length() > 0) arr.optJSONObject(arr.length() - 1)?.optString("url") else null }
            ?.takeIf { it.startsWith("http") }
            ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

        val sourceUrl = if (isShort) {
            "https://www.youtube.com/shorts/$videoId"
        } else {
            "https://www.youtube.com/watch?v=$videoId"
        }

        val descTag = "$title • $finalChannelName"

        return CachedChannelMediaEntity(
            id = "ch_$videoId",
            title = title,
            videoId = videoId,
            sourceUrl = sourceUrl,
            thumbnailUrl = thumb,
            publishedAt = published,
            viewsCount = views,
            description = descTag,
            isShort = isShort,
            categoryName = classifyCategory(title, descTag, isShort).name,
            channelName = finalChannelName,
            channelHandle = finalHandle,
            isOfficialChannel = isOfficialOwner,
            fetchedAt = System.currentTimeMillis()
        )
    }

    private fun parseShortsLockupViewModel(
        vm: JSONObject,
        defaultChannelName: String = "",
        defaultChannelHandle: String = "",
        defaultIsOfficial: Boolean = false
    ): CachedChannelMediaEntity? {
        val videoId = vm.optJSONObject("onTap")
            ?.optJSONObject("innertubeCommand")
            ?.optJSONObject("reelWatchEndpoint")
            ?.optString("videoId")
            ?.trim()
            ?.takeIf { it.isNotBlank() } ?: return null

        val title = vm.optJSONObject("overlayMetadata")
            ?.optJSONObject("primaryText")
            ?.optString("content")
            ?.trim()
            ?.takeIf { it.isNotBlank() } ?: "مقطع قصير • رحمة رياض"

        val thumb = vm.optJSONObject("thumbnail")
            ?.optJSONArray("sources")
            ?.optJSONObject(0)
            ?.optString("url")
            ?.takeIf { it.startsWith("http") }
            ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

        val views = vm.optJSONObject("overlayMetadata")
            ?.optJSONObject("secondaryText")
            ?.optString("content")
            ?.takeIf { it.isNotBlank() } ?: "ريلز"

        val resolvedChannel = defaultChannelName.ifBlank {
            if (defaultIsOfficial) RahmaCatalog.OFFICIAL_CHANNEL_AR_NAME else "يوتيوب شورتس"
        }

        return CachedChannelMediaEntity(
            id = "ch_$videoId",
            title = title,
            videoId = videoId,
            sourceUrl = "https://www.youtube.com/shorts/$videoId",
            thumbnailUrl = thumb,
            publishedAt = "ريلز",
            viewsCount = views,
            description = "$title • $resolvedChannel",
            isShort = true,
            categoryName = SongCategory.REELS.name,
            channelName = resolvedChannel,
            channelHandle = if (defaultIsOfficial) OFFICIAL_HANDLE else defaultChannelHandle,
            isOfficialChannel = defaultIsOfficial,
            fetchedAt = System.currentTimeMillis()
        )
    }

    private fun parseReelItemRenderer(
        renderer: JSONObject,
        defaultChannelName: String = "",
        defaultChannelHandle: String = "",
        defaultIsOfficial: Boolean = false
    ): CachedChannelMediaEntity? {
        val videoId = renderer.optString("videoId").trim()
        if (videoId.isBlank()) return null
        val title = extractRunsOrSimpleText(renderer.optJSONObject("headline")).ifBlank { "مقطع قصير • رحمة رياض" }
        val views = extractRunsOrSimpleText(renderer.optJSONObject("viewCountText")).ifBlank { "ريلز" }
        val thumb = renderer.optJSONObject("thumbnail")
            ?.optJSONArray("thumbnails")
            ?.let { arr -> if (arr.length() > 0) arr.optJSONObject(arr.length() - 1)?.optString("url") else null }
            ?.takeIf { it.startsWith("http") }
            ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

        val resolvedChannel = defaultChannelName.ifBlank {
            if (defaultIsOfficial) RahmaCatalog.OFFICIAL_CHANNEL_AR_NAME else "يوتيوب شورتس"
        }

        return CachedChannelMediaEntity(
            id = "ch_$videoId",
            title = title,
            videoId = videoId,
            sourceUrl = "https://www.youtube.com/shorts/$videoId",
            thumbnailUrl = thumb,
            publishedAt = "ريلز",
            viewsCount = views,
            description = "$title • $resolvedChannel",
            isShort = true,
            categoryName = SongCategory.REELS.name,
            channelName = resolvedChannel,
            channelHandle = if (defaultIsOfficial) OFFICIAL_HANDLE else defaultChannelHandle,
            isOfficialChannel = defaultIsOfficial,
            fetchedAt = System.currentTimeMillis()
        )
    }

    private fun fetchViaPipedSearch(
        query: String,
        officialChannelId: String? = null
    ): List<CachedChannelMediaEntity> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val instances = listOf(
            "https://pipedapi.kavin.rocks/search?q=$encoded&filter=videos",
            "https://pipedapi.tokhmi.xyz/search?q=$encoded&filter=videos",
            "https://pipedapi.adminforge.de/search?q=$encoded&filter=videos"
        )
        for (url in instances) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", BROWSER_UA)
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    val json = JSONObject(response.body?.string() ?: return@use)
                    val items = json.optJSONArray("items") ?: return@use
                    val parsed = mutableListOf<CachedChannelMediaEntity>()
                    for (i in 0 until items.length()) {
                        val obj = items.optJSONObject(i) ?: continue
                        val relUrl = obj.optString("url")
                        val videoId = relUrl.substringAfter("v=").substringAfter("/shorts/").substringBefore("&")
                        val title = obj.optString("title").trim()
                        if (videoId.isBlank() || title.isBlank()) continue
                        val duration = obj.optLong("duration", 0L)
                        val isShort = obj.optBoolean("isShort", false) || (duration in 1..60)
                        val thumb = obj.optString("thumbnail").takeIf { it.startsWith("http") }
                            ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                        val uploaderName = obj.optString("uploaderName").trim()
                        val uploaderUrl = obj.optString("uploaderUrl").trim()
                        val uploaderHandle = extractCanonicalHandle(uploaderUrl)
                        val isOfficial = (!officialChannelId.isNullOrBlank() && uploaderUrl.contains(officialChannelId)) ||
                            RahmaCatalog.isOfficialRahmaChannelName(uploaderName, uploaderHandle)
                        val realChannel = uploaderName.ifBlank {
                            if (isOfficial) "Rahma Riad | رحمة رياض" else "يوتيوب"
                        }
                        val views = obj.optLong("views", -1L).let {
                            if (it > 0) formatViews(it) else realChannel
                        }
                        val desc = obj.optString("shortDescription").ifBlank { "$title • $realChannel" }
                        parsed.add(
                            CachedChannelMediaEntity(
                                id = "ch_$videoId",
                                title = title,
                                videoId = videoId,
                                sourceUrl = if (isShort) "https://www.youtube.com/shorts/$videoId" else "https://www.youtube.com/watch?v=$videoId",
                                thumbnailUrl = thumb,
                                publishedAt = obj.optString("uploadedDate").ifBlank { "حديث" },
                                viewsCount = views,
                                description = desc,
                                isShort = isShort,
                                categoryName = classifyCategory(title, desc, isShort).name,
                                channelName = realChannel,
                                channelHandle = if (isOfficial) OFFICIAL_HANDLE else uploaderHandle,
                                isOfficialChannel = isOfficial,
                                fetchedAt = System.currentTimeMillis()
                            )
                        )
                    }
                    if (parsed.isNotEmpty()) return parsed
                }
            } catch (e: Exception) {
                Log.w(TAG, "Piped search failed ($url): ${e.message}")
            }
        }
        return emptyList()
    }

    private fun fetchViaInvidiousSearch(
        query: String,
        officialChannelId: String? = null
    ): List<CachedChannelMediaEntity> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val instances = listOf(
            "https://inv.nadeko.net/api/v1/search?q=$encoded&type=video",
            "https://iv.datura.network/api/v1/search?q=$encoded&type=video"
        )
        for (url in instances) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", BROWSER_UA)
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    val arr = JSONArray(response.body?.string() ?: return@use)
                    val parsed = mutableListOf<CachedChannelMediaEntity>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        val videoId = obj.optString("videoId").trim()
                        val title = obj.optString("title").trim()
                        if (videoId.isBlank() || title.isBlank()) continue
                        val duration = obj.optLong("lengthSeconds", 0L)
                        val isShort = duration in 1..60 || title.contains("#shorts", ignoreCase = true)
                        val author = obj.optString("author").trim()
                        val authorId = obj.optString("authorId").trim()
                        val authorHandle = extractCanonicalHandle(obj.optString("authorUrl"))
                        val isOfficial = (!officialChannelId.isNullOrBlank() && authorId == officialChannelId) ||
                            RahmaCatalog.isOfficialRahmaChannelName(author, authorHandle)
                        val realChannel = author.ifBlank {
                            if (isOfficial) "Rahma Riad | رحمة رياض" else "يوتيوب"
                        }
                        val views = obj.optLong("viewCount", -1L).let {
                            if (it > 0) formatViews(it) else realChannel
                        }
                        val desc = obj.optString("description").ifBlank { "$title • $realChannel" }
                        parsed.add(
                            CachedChannelMediaEntity(
                                id = "ch_$videoId",
                                title = title,
                                videoId = videoId,
                                sourceUrl = if (isShort) "https://www.youtube.com/shorts/$videoId" else "https://www.youtube.com/watch?v=$videoId",
                                thumbnailUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
                                publishedAt = obj.optString("publishedText").ifBlank { "حديث" },
                                viewsCount = views,
                                description = desc,
                                isShort = isShort,
                                categoryName = classifyCategory(title, desc, isShort).name,
                                channelName = realChannel,
                                channelHandle = if (isOfficial) OFFICIAL_HANDLE else authorHandle,
                                isOfficialChannel = isOfficial,
                                fetchedAt = System.currentTimeMillis()
                            )
                        )
                    }
                    if (parsed.isNotEmpty()) return parsed
                }
            } catch (e: Exception) {
                Log.w(TAG, "Invidious search failed ($url): ${e.message}")
            }
        }
        return emptyList()
    }

    private fun extractRunsOrSimpleText(obj: JSONObject?): String {
        if (obj == null) return ""
        val simple = obj.optString("simpleText")
        if (simple.isNotBlank()) return simple
        val runs = obj.optJSONArray("runs") ?: return ""
        val sb = StringBuilder()
        for (i in 0 until runs.length()) {
            sb.append(runs.optJSONObject(i)?.optString("text") ?: "")
        }
        return sb.toString().trim()
    }

    private fun isUnderOneMinute(lengthText: String): Boolean {
        val parts = lengthText.trim().split(":")
        return when (parts.size) {
            2 -> {
                val mins = parts[0].toIntOrNull() ?: return false
                mins == 0
            }
            else -> false
        }
    }

    private fun classifyCategory(title: String, description: String, isShort: Boolean): SongCategory {
        return when {
            isShort -> SongCategory.REELS
            title.contains("حفل") || title.contains("مهرجان") || title.contains("Live", ignoreCase = true) -> SongCategory.CONCERTS
            title.contains("كليب") || title.contains("Clip", ignoreCase = true) || title.contains("Official", ignoreCase = true) || description.contains("كليب") -> SongCategory.MUSIC_VIDEO
            else -> SongCategory.LATEST
        }
    }

    private fun extractCanonicalHandle(urlOrPath: String): String {
        val raw = urlOrPath.trim()
        val idx = raw.indexOf("@")
        if (idx == -1) return ""
        return raw.substring(idx).substringBefore("/").substringBefore("?").substringBefore("#").trim()
    }

    private fun parseAtomFeed(
        xml: String,
        officialChannelId: String? = null
    ): List<CachedChannelMediaEntity> {
        val entries = mutableListOf<CachedChannelMediaEntity>()
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(StringReader(xml))

        var insideEntry = false
        var insideAuthor = false
        var currentTag = ""
        var feedAuthorName = ""
        var entryAuthorName = ""
        var entryAuthorUri = ""
        var videoId = ""
        var title = ""
        var published = ""
        var linkHref = ""
        var description = ""
        var thumbnailUrl = ""
        var viewsCount = ""

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name ?: ""
                    when (currentTag) {
                        "author" -> insideAuthor = true
                        "entry" -> {
                            insideEntry = true
                            entryAuthorName = ""
                            entryAuthorUri = ""
                            videoId = ""
                            title = ""
                            published = ""
                            linkHref = ""
                            description = ""
                            thumbnailUrl = ""
                            viewsCount = ""
                        }
                        "link" -> if (insideEntry) {
                            val rel = parser.getAttributeValue(null, "rel")
                            val href = parser.getAttributeValue(null, "href") ?: ""
                            if (rel == "alternate" || linkHref.isBlank()) {
                                linkHref = href
                            }
                        }
                        "media:thumbnail" -> if (insideEntry) {
                            thumbnailUrl = parser.getAttributeValue(null, "url") ?: thumbnailUrl
                        }
                        "media:statistics" -> if (insideEntry) {
                            val views = parser.getAttributeValue(null, "views")?.toLongOrNull()
                            if (views != null) {
                                viewsCount = formatViews(views)
                            }
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    val text = parser.text ?: ""
                    if (insideEntry) {
                        when (currentTag) {
                            "yt:videoId" -> videoId += text
                            "title" -> title += text
                            "published" -> published += text
                            "media:description" -> description += text
                            "name" -> if (insideAuthor) entryAuthorName += text
                            "uri" -> if (insideAuthor) entryAuthorUri += text
                        }
                    } else if (insideAuthor && currentTag == "name") {
                        feedAuthorName += text
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "author") {
                        insideAuthor = false
                    } else if (parser.name == "entry" && insideEntry) {
                        val cleanId = videoId.trim()
                        val cleanTitle = title.trim()
                        if (cleanId.isNotBlank() && cleanTitle.isNotBlank()) {
                            val cleanLink = linkHref.trim().ifBlank { "https://www.youtube.com/watch?v=$cleanId" }
                            val isShort = cleanLink.contains("/shorts/") ||
                                cleanTitle.contains("#shorts", ignoreCase = true) ||
                                description.contains("#shorts", ignoreCase = true)

                            val rawChannel = entryAuthorName.trim().ifBlank { feedAuthorName.trim() }
                                .ifBlank { "Rahma Riad | رحمة رياض" }
                            val isOfficial = (!officialChannelId.isNullOrBlank() && entryAuthorUri.contains(officialChannelId)) ||
                                RahmaCatalog.isOfficialRahmaChannelName(rawChannel, OFFICIAL_HANDLE)

                            val category = classifyCategory(cleanTitle, description, isShort)
                            val finalDesc = if (description.isBlank()) "$cleanTitle • $rawChannel" else "${description.trim()} • $rawChannel"

                            entries.add(
                                CachedChannelMediaEntity(
                                    id = "ch_$cleanId",
                                    title = cleanTitle,
                                    videoId = cleanId,
                                    sourceUrl = cleanLink,
                                    thumbnailUrl = thumbnailUrl.ifBlank { "https://i.ytimg.com/vi/$cleanId/hqdefault.jpg" },
                                    publishedAt = published.trim().take(10),
                                    viewsCount = viewsCount.ifBlank { rawChannel },
                                    description = finalDesc,
                                    isShort = isShort,
                                    categoryName = category.name,
                                    channelName = rawChannel,
                                    channelHandle = if (isOfficial) OFFICIAL_HANDLE else "",
                                    isOfficialChannel = isOfficial,
                                    fetchedAt = System.currentTimeMillis()
                                )
                            )
                        }
                        insideEntry = false
                    }
                    currentTag = ""
                }
            }
            eventType = parser.next()
        }
        return entries
    }

    /**
     * Fetches official Instagram Reels & Videos directly from Rahma Riad's verified Instagram account:
     * https://www.instagram.com/rahmariadh
     */
    suspend fun fetchOfficialInstagramMedia(
        username: String = OFFICIAL_IG_USERNAME
    ): List<CachedChannelMediaEntity> = withContext(Dispatchers.IO) {
        val cleanUser = username.trim().removePrefix("@").ifBlank { OFFICIAL_IG_USERNAME }
        val profileUrl = "https://www.instagram.com/$cleanUser/"
        val collected = linkedMapOf<String, CachedChannelMediaEntity>()

        // Tier 1: Official web_profile_info public JSON API
        try {
            val apiReq = Request.Builder()
                .url("https://www.instagram.com/api/v1/users/web_profile_info/?username=$cleanUser")
                .header("User-Agent", BROWSER_UA)
                .header("X-IG-App-ID", IG_WEB_APP_ID)
                .header("X-ASBD-ID", "129477")
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Referer", profileUrl)
                .header("Accept", "application/json, text/plain, */*")
                .header("Accept-Language", "ar,en;q=0.9")
                .build()
            client.newCall(apiReq).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string().orEmpty()
                    if (body.isNotBlank()) {
                        parseInstagramProfileJson(body, cleanUser).forEach { entity ->
                            collected.putIfAbsent(entity.id, entity)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Instagram web_profile_info failed: ${e.message}")
        }

        // Tier 2: Profile & Reels HTML fallback for embedded shortcodes / video_url payloads
        if (collected.isEmpty()) {
            for (targetUrl in listOf("${profileUrl}reels/", profileUrl)) {
                try {
                    val htmlReq = Request.Builder()
                        .url(targetUrl)
                        .header("User-Agent", BROWSER_UA)
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .header("Accept-Language", "ar,en;q=0.9")
                        .build()
                    client.newCall(htmlReq).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val html = resp.body?.string().orEmpty()
                            if (html.isNotBlank()) {
                                parseInstagramProfileHtml(html, cleanUser).forEach { entity ->
                                    collected.putIfAbsent(entity.id, entity)
                                }
                            }
                        }
                    }
                    if (collected.isNotEmpty()) break
                } catch (e: Exception) {
                    Log.w(TAG, "Instagram profile HTML fallback failed ($targetUrl): ${e.message}")
                }
            }
        }

        collected.values.toList()
    }

    internal fun parseInstagramProfileJson(
        jsonBody: String,
        username: String = OFFICIAL_IG_USERNAME
    ): List<CachedChannelMediaEntity> {
        val out = mutableListOf<CachedChannelMediaEntity>()
        val root = runCatching { JSONObject(jsonBody) }.getOrNull() ?: return out
        val userObj = root.optJSONObject("data")?.optJSONObject("user") ?: return out

        val timelineEdges = userObj.optJSONObject("edge_owner_to_timeline_media")?.optJSONArray("edges")
        val felixEdges = userObj.optJSONObject("edge_felix_video_timeline")?.optJSONArray("edges")

        val allNodes = mutableListOf<JSONObject>()
        if (timelineEdges != null) {
            for (i in 0 until timelineEdges.length()) {
                timelineEdges.optJSONObject(i)?.optJSONObject("node")?.let { allNodes.add(it) }
            }
        }
        if (felixEdges != null) {
            for (i in 0 until felixEdges.length()) {
                felixEdges.optJSONObject(i)?.optJSONObject("node")?.let { allNodes.add(it) }
            }
        }

        val seenShortcodes = HashSet<String>()
        for (node in allNodes) {
            val shortcode = node.optString("shortcode").trim()
            if (shortcode.isBlank() || !seenShortcodes.add(shortcode)) continue

            // Check direct video_url or sidecar carousel video child
            var isVideo = node.optBoolean("is_video", false)
            var directVideoUrl = node.optString("video_url")
                .takeIf { it.startsWith("http", ignoreCase = true) }
            var thumbUrl = node.optString("display_url")
                .takeIf { it.startsWith("http", ignoreCase = true) }
                ?: node.optString("thumbnail_src")
                    .takeIf { it.startsWith("http", ignoreCase = true) }
                ?: ""

            if (!isVideo) {
                val sidecarEdges = node.optJSONObject("edge_sidecar_to_children")?.optJSONArray("edges")
                if (sidecarEdges != null) {
                    for (j in 0 until sidecarEdges.length()) {
                        val child = sidecarEdges.optJSONObject(j)?.optJSONObject("node") ?: continue
                        if (child.optBoolean("is_video", false)) {
                            isVideo = true
                            if (directVideoUrl == null) {
                                directVideoUrl = child.optString("video_url")
                                    .takeIf { it.startsWith("http", ignoreCase = true) }
                            }
                            if (thumbUrl.isBlank()) {
                                thumbUrl = child.optString("display_url")
                                    .takeIf { it.startsWith("http", ignoreCase = true) }.orEmpty()
                            }
                            break
                        }
                    }
                }
            }

            // Skip image-only posts without playable video so Reels & Player never stall
            if (!isVideo && directVideoUrl == null) continue

            val productType = node.optString("product_type").lowercase()
            val isReel = productType == "clips" || isVideo
            val rawCaption = node.optJSONObject("edge_media_to_caption")
                ?.optJSONArray("edges")
                ?.optJSONObject(0)
                ?.optJSONObject("node")
                ?.optString("text")
                ?.trim()
                .orEmpty()
            val titleLine = rawCaption
                .lineSequence()
                .map { it.trim() }
                .firstOrNull { it.isNotBlank() && !it.startsWith("#") }
                ?.take(110)
                ?: "رحمة رياض • ريلز إنستغرام"

            val postUrl = if (isReel) {
                "https://www.instagram.com/reel/$shortcode/"
            } else {
                "https://www.instagram.com/p/$shortcode/"
            }

            if (!directVideoUrl.isNullOrBlank()) {
                MediaStreamResolver.preCacheInstagramStream(
                    pageUrl = postUrl,
                    directVideoUrl = directVideoUrl,
                    title = titleLine,
                    thumbnailUrl = thumbUrl.takeIf { it.isNotBlank() },
                    handle = "@$username"
                )
            }

            val viewCount = node.optLong("video_view_count", -1L).let { v ->
                if (v > 0L) formatViews(v) else RahmaCatalog.OFFICIAL_INSTAGRAM_AR_NAME
            }
            val takenTs = node.optLong("taken_at_timestamp", 0L)
            val publishedDate = if (takenTs > 0L) {
                java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                    .format(java.util.Date(takenTs * 1000L))
            } else {
                "إنستغرام الرسمي"
            }

            out.add(
                CachedChannelMediaEntity(
                    id = "ig_$shortcode",
                    title = titleLine,
                    videoId = "",
                    sourceUrl = postUrl,
                    thumbnailUrl = thumbUrl,
                    publishedAt = publishedDate,
                    viewsCount = viewCount,
                    description = rawCaption.ifBlank { "$titleLine • @${username}" },
                    isShort = isReel,
                    categoryName = if (isReel) SongCategory.REELS.name else SongCategory.MUSIC_VIDEO.name,
                    channelName = RahmaCatalog.OFFICIAL_INSTAGRAM_AR_NAME,
                    channelHandle = OFFICIAL_IG_HANDLE,
                    isOfficialChannel = true,
                    fetchedAt = System.currentTimeMillis()
                )
            )
        }
        return out
    }

    internal fun parseInstagramProfileHtml(
        html: String,
        username: String = OFFICIAL_IG_USERNAME
    ): List<CachedChannelMediaEntity> {
        val out = mutableListOf<CachedChannelMediaEntity>()
        val seen = HashSet<String>()
        val reelMatches = Regex("""/(?:$username/)?(reel|p)/([A-Za-z0-9_-]{8,15})""", RegexOption.IGNORE_CASE)
            .findAll(html)
        for (match in reelMatches) {
            val typeSegment = match.groupValues[1].lowercase()
            val shortcode = match.groupValues[2]
            if (!seen.add(shortcode)) continue
            val isReel = typeSegment == "reel"
            val postUrl = "https://www.instagram.com/$typeSegment/$shortcode/"
            out.add(
                CachedChannelMediaEntity(
                    id = "ig_$shortcode",
                    title = "رحمة رياض • إنستغرام الرسمي",
                    videoId = "",
                    sourceUrl = postUrl,
                    thumbnailUrl = "",
                    publishedAt = "إنستغرام الرسمي",
                    viewsCount = RahmaCatalog.OFFICIAL_INSTAGRAM_AR_NAME,
                    description = "من الحساب الرسمي على إنستغرام @$username",
                    isShort = isReel,
                    categoryName = if (isReel) SongCategory.REELS.name else SongCategory.MUSIC_VIDEO.name,
                    channelName = RahmaCatalog.OFFICIAL_INSTAGRAM_AR_NAME,
                    channelHandle = OFFICIAL_IG_HANDLE,
                    isOfficialChannel = true,
                    fetchedAt = System.currentTimeMillis()
                )
            )
            if (out.size >= 16) break
        }
        return out
    }

    private fun formatViews(views: Long): String {
        return when {
            views >= 1_000_000 -> String.format("%.1fM مشاهدة", views / 1_000_000.0)
            views >= 1_000 -> String.format("%.1fK مشاهدة", views / 1_000.0)
            else -> "$views مشاهدة"
        }
    }
}
