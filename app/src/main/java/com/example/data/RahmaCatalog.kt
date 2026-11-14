package com.example.data

import com.example.data.remote.LinkValidator
import com.example.model.AppSettings
import com.example.model.RahmaReel
import com.example.model.ReelFilter
import com.example.model.Song
import com.example.model.SongCategory

/**
 * Pure filtering, ranking, Arabic fuzzy matching, and smart-search utilities for Rahma Riad media.
 * Prioritizes @RahmaRiad official channel items cleanly without any mock content.
 */
object RahmaCatalog {

    const val OFFICIAL_CHANNEL_AR_NAME = "رحمة رياض الرسمية"
    const val OFFICIAL_CHANNEL_HANDLE = "@RahmaRiad"
    const val OFFICIAL_INSTAGRAM_USERNAME = "rahmariadh"
    const val OFFICIAL_INSTAGRAM_HANDLE = "@rahmariadh"
    const val OFFICIAL_INSTAGRAM_URL = "https://www.instagram.com/rahmariadh/"
    const val OFFICIAL_INSTAGRAM_AR_NAME = "رحمة رياض • إنستغرام الرسمي"

    private val ARABIC_DIACRITICS_REGEX = Regex("""[\u064B-\u065F\u0670\u0640]""")

    private val NON_OFFICIAL_INDICATORS = listOf(
        "fan", "fans", "محبي", "عشاق", "تصميم", "حالات",
        "karaoke", "كاريوكي", "cover", "ريمكس", "remix", "تغطية"
    )

    fun normalizeArabic(text: String): String {
        return ARABIC_DIACRITICS_REGEX.replace(text, "")
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ى', 'ي')
            .replace('ة', 'ه')
            .replace('ؤ', 'و')
            .replace('ئ', 'ي')
            .trim()
            .lowercase()
    }

    fun filterByCategory(songs: List<Song>, category: SongCategory): List<Song> {
        return when (category) {
            SongCategory.ALL -> songs
            SongCategory.FEATURED -> songs.filter { it.isFeatured }
            SongCategory.REELS -> songs.filter { it.isShort || it.category == SongCategory.REELS }
            SongCategory.IMPORTED -> songs.filter { it.isUserImported || it.category == SongCategory.IMPORTED }
            else -> songs.filter { it.category == category }
        }
    }

    fun matchesEnabledSources(song: Song, settings: AppSettings): Boolean {
        if (settings.allLinkSourcesEnabled) return true
        val url = song.externalUrl?.takeIf { it.isNotBlank() }
            ?: if (song.youtubeVideoId.isNotBlank()) "https://www.youtube.com/watch?v=${song.youtubeVideoId}" else ""
        if (url.isBlank() || url.startsWith("content://") || url.startsWith("file://")) return true
        val detection = LinkValidator.detectSource(url, settings.customLinkDomains)
        return LinkValidator.isSourceEnabledInSettings(detection, settings)
    }

    fun filterByEnabledSources(songs: List<Song>, settings: AppSettings): List<Song> {
        if (settings.allLinkSourcesEnabled) return songs
        return songs.filter { matchesEnabledSources(it, settings) }
    }

    /**
     * Flexible Arabic + English search returning close and related matches rather than strict exact match only.
     */
    fun searchSongs(songs: List<Song>, query: String): List<Song> {
        if (query.isBlank()) return songs
        val normQuery = normalizeArabic(query)
        val tokens = normQuery.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return songs

        return songs.mapNotNull { song ->
            val rawHaystack = "${song.title} ${song.arabicTitle} ${song.description} ${song.sourcePlatform} ${song.channelHandle} ${song.composer} ${song.category.displayName}"
            val haystack = normalizeArabic(rawHaystack)
            val exactPhrase = haystack.contains(normQuery)
            val matchedTokens = tokens.count { token ->
                haystack.contains(token) ||
                    (token.length >= 3 && haystack.split(Regex("\\s+")).any { word ->
                        word.startsWith(token.take(3)) || token.startsWith(word.take(3))
                    })
            }
            if (exactPhrase || matchedTokens > 0) {
                val score = (if (exactPhrase) 100 else 0) + (matchedTokens * 25) +
                    (if (matchedTokens == tokens.size) 40 else 0) +
                    (if (isOfficialRahmaChannel(song)) 15 else 0)
                song to score
            } else {
                null
            }
        }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    fun isOfficialRahmaChannelName(
        channelName: String,
        channelHandle: String = ""
    ): Boolean {
        val handle = channelHandle.trim()
        if (handle.equals(OFFICIAL_CHANNEL_HANDLE, ignoreCase = true) ||
            handle.equals(OFFICIAL_INSTAGRAM_HANDLE, ignoreCase = true)
        ) return true

        val cleanName = channelName.trim()
        if (cleanName.isBlank()) return false
        val lowerName = cleanName.lowercase()

        if (lowerName.contains("@rahmariad") || lowerName.contains("@rahmariadh") || lowerName.contains("rahmariadh")) return true
        if (cleanName.contains(OFFICIAL_CHANNEL_AR_NAME)) return true
        if (NON_OFFICIAL_INDICATORS.any { lowerName.contains(it) }) return false

        return lowerName == "rahma riad" ||
            lowerName == "rahma riad official" ||
            lowerName == "رحمة رياض" ||
            lowerName.startsWith("rahma riad |") ||
            lowerName.startsWith("رحمة رياض |") ||
            lowerName.contains("رحمة رياض الرسم") ||
            lowerName.contains("إنستغرام الرسمي") ||
            lowerName.contains("انستغرام الرسمي") ||
            lowerName.contains("القناة الرسمية")
    }

    fun isOfficialRahmaChannel(song: Song): Boolean {
        return song.channelHandle.equals(OFFICIAL_CHANNEL_HANDLE, ignoreCase = true) ||
            song.channelHandle.equals(OFFICIAL_INSTAGRAM_HANDLE, ignoreCase = true) ||
            song.externalUrl?.contains("instagram.com/rahmariadh", ignoreCase = true) == true ||
            isOfficialRahmaChannelName(song.sourcePlatform, song.channelHandle)
    }

    fun displayChannelName(song: Song): String {
        val isOfficialIg = song.channelHandle.equals(OFFICIAL_INSTAGRAM_HANDLE, ignoreCase = true) ||
            song.externalUrl?.contains("instagram.com/rahmariadh", ignoreCase = true) == true ||
            (song.externalUrl?.contains("instagram.com", ignoreCase = true) == true && isOfficialRahmaChannel(song))
        if (isOfficialIg) {
            return OFFICIAL_INSTAGRAM_AR_NAME
        }
        if (isOfficialRahmaChannel(song)) {
            val raw = song.sourcePlatform.trim()
            return if (raw.isNotBlank() &&
                raw.contains("Rahma Riad", ignoreCase = true) &&
                !raw.contains(OFFICIAL_CHANNEL_AR_NAME)
            ) {
                "$OFFICIAL_CHANNEL_AR_NAME • Rahma Riad"
            } else {
                OFFICIAL_CHANNEL_AR_NAME
            }
        }
        return song.sourcePlatform
            .takeIf { it.isNotBlank() && it != "مباشر" && it != "نتائج رحمة رياض" }
            ?: song.composer.takeIf { it.isNotBlank() && it != "رحمة رياض" }
            ?: song.channelHandle.takeIf { it.isNotBlank() }
            ?: "قناة موسيقية"
    }

    fun mergeAndRankSmartSearch(
        localSongs: List<Song>,
        remoteSongs: List<Song>,
        query: String
    ): List<Song> {
        val localMatches = searchSongs(localSongs, query)
        val combined = linkedMapOf<String, Song>()

        fun keyFor(song: Song): String {
            return song.youtubeVideoId.takeIf { it.isNotBlank() } ?: song.id
        }

        for (song in localMatches) {
            combined[keyFor(song)] = song
        }
        for (song in remoteSongs) {
            val k = keyFor(song)
            val existing = combined[k]
            if (existing == null) {
                combined[k] = song
            } else if (isOfficialRahmaChannel(song) && !isOfficialRahmaChannel(existing)) {
                combined[k] = existing.copy(
                    channelHandle = OFFICIAL_CHANNEL_HANDLE,
                    sourcePlatform = OFFICIAL_CHANNEL_AR_NAME
                )
            } else if (existing.sourcePlatform.isBlank() && song.sourcePlatform.isNotBlank()) {
                combined[k] = existing.copy(
                    sourcePlatform = song.sourcePlatform,
                    channelHandle = song.channelHandle.ifBlank { existing.channelHandle }
                )
            }
        }

        val normQuery = normalizeArabic(query)
        val tokens = normQuery.split(Regex("\\s+")).filter { it.isNotBlank() }

        return combined.values.sortedByDescending { song ->
            var score = 0
            if (isOfficialRahmaChannel(song)) score += 300
            if (song.channelHandle.equals(OFFICIAL_CHANNEL_HANDLE, ignoreCase = true)) score += 120
            val titleNorm = normalizeArabic("${song.arabicTitle} ${song.title} ${song.sourcePlatform}")
            if (normQuery.isNotBlank() && titleNorm.contains(normQuery)) score += 45
            val matchedTokens = tokens.count { t -> titleNorm.contains(t) }
            score += matchedTokens * 15
            if (song.isFeatured) score += 10
            if (song.isUserImported) score += 6
            score
        }
    }

    fun filterReels(
        reels: List<RahmaReel>,
        filter: ReelFilter,
        savedIds: Set<String> = emptySet()
    ): List<RahmaReel> {
        return when (filter) {
            ReelFilter.ALL -> reels
            ReelFilter.OFFICIAL -> reels.filter { !it.isUserImported }
            ReelFilter.INSTAGRAM -> reels.filter {
                it.platformLabel.contains("انستغرام") ||
                    it.platformLabel.contains("إنستغرام") ||
                    it.sourceUrl.contains("instagram.com", ignoreCase = true)
            }
            ReelFilter.TIKTOK -> reels.filter {
                it.platformLabel.contains("تيك توك") ||
                    it.sourceUrl.contains("tiktok.com", ignoreCase = true)
            }
            ReelFilter.IMPORTED -> reels.filter { it.isUserImported }
            ReelFilter.SAVED -> reels.filter { savedIds.contains(it.id) }
        }
    }
}
