package com.example.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import com.example.R
import com.example.data.RahmaCatalog
import com.example.model.Song
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe registry passing per-stream HTTP headers (Referer, User-Agent, cookies)
 * to [RahmaPlaybackService]'s ExoPlayer DataSource without Parcel/Binder loss.
 */
object PlaybackHeaderRegistry {
    private val headersByUri = ConcurrentHashMap<String, Map<String, String>>()

    fun registerHeaders(uriString: String, headers: Map<String, String>) {
        val clean = uriString.trim()
        if (clean.isBlank()) return
        if (headers.isEmpty()) {
            headersByUri.remove(clean)
        } else {
            if (headersByUri.size > 64) {
                headersByUri.clear()
            }
            headersByUri[clean] = headers.toMap()
        }
    }

    fun getHeaders(uriString: String): Map<String, String> {
        return headersByUri[uriString.trim()].orEmpty()
    }

    fun clear() {
        headersByUri.clear()
    }
}

/**
 * Builds rich Media3 [MediaMetadata] and [MediaItem] instances so the Android system
 * media notification, lock screen, and Bluetooth controls display song title, artist name,
 * and album art cleanly.
 */
object MediaSessionMetadataFactory {

    @Volatile
    private var cachedFallbackArtwork: ByteArray? = null

    fun resolveTrackTitle(song: Song): String {
        return song.arabicTitle.trim()
            .ifBlank { song.title.trim() }
            .ifBlank { "رحمة رياض" }
    }

    fun resolveArtistName(song: Song): String {
        if (RahmaCatalog.isOfficialRahmaChannel(song)) {
            return "رحمة رياض"
        }
        val channel = RahmaCatalog.displayChannelName(song)
        return when {
            channel.isBlank() || channel == "قناة موسيقية" ->
                song.composer.takeIf { it.isNotBlank() } ?: "رحمة رياض"
            else -> channel
        }
    }

    fun resolveAlbumTitle(song: Song): String {
        return if (song.category.displayName == "الكل") {
            RahmaCatalog.OFFICIAL_CHANNEL_AR_NAME
        } else {
            song.category.displayName
        }
    }

    fun loadFallbackArtworkBytes(context: Context): ByteArray? {
        cachedFallbackArtwork?.let { return it }
        return runCatching {
            val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.rahma_avatar)
                ?: return null
            ByteArrayOutputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                out.toByteArray()
            }.also { bytes ->
                cachedFallbackArtwork = bytes
            }
        }.getOrNull()
    }

    fun buildMediaMetadata(
        song: Song,
        context: Context? = null,
        fallbackArtworkBytes: ByteArray? = context?.let { loadFallbackArtworkBytes(it) }
    ): MediaMetadata {
        val title = resolveTrackTitle(song)
        val artist = resolveArtistName(song)
        val albumTitle = resolveAlbumTitle(song)
        val subtitle = song.releaseYear.takeIf { it.isNotBlank() }?.let { "$artist • $it" } ?: artist

        val builder = MediaMetadata.Builder()
            .setTitle(title)
            .setDisplayTitle(title)
            .setArtist(artist)
            .setAlbumArtist("رحمة رياض")
            .setAlbumTitle(albumTitle)
            .setSubtitle(subtitle)
            .setIsPlayable(true)
            .setMediaType(
                if (song.isVideoAvailable) {
                    MediaMetadata.MEDIA_TYPE_VIDEO
                } else {
                    MediaMetadata.MEDIA_TYPE_MUSIC
                }
            )

        val thumb = song.thumbnailUrl?.trim()
        val hasValidRemoteOrLocalThumb = !thumb.isNullOrBlank() && (
            thumb.startsWith("http://", ignoreCase = true) ||
                thumb.startsWith("https://", ignoreCase = true) ||
                thumb.startsWith("content://", ignoreCase = true) ||
                thumb.startsWith("file://", ignoreCase = true)
            )

        if (hasValidRemoteOrLocalThumb) {
            builder.setArtworkUri(Uri.parse(thumb))
        } else if (fallbackArtworkBytes != null && fallbackArtworkBytes.isNotEmpty()) {
            builder.setArtworkData(fallbackArtworkBytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
        } else if (context != null) {
            builder.setArtworkUri(
                Uri.parse("android.resource://${context.packageName}/${R.drawable.rahma_avatar}")
            )
        }

        return builder.build()
    }

    fun buildMediaItem(
        song: Song,
        streamUri: String,
        isHls: Boolean,
        context: Context? = null,
        fallbackArtworkBytes: ByteArray? = context?.let { loadFallbackArtworkBytes(it) }
    ): MediaItem {
        val builder = MediaItem.Builder()
            .setMediaId(song.id)
            .setUri(streamUri)
            .setMediaMetadata(buildMediaMetadata(song, context, fallbackArtworkBytes))

        if (isHls) {
            builder.setMimeType(MimeTypes.APPLICATION_M3U8)
        }
        return builder.build()
    }
}
