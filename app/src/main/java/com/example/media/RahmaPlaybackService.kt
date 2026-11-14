package com.example.media

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.MainActivity
import com.example.R

/**
 * Background playback & system media controls service backed by Jetpack Media3 [MediaSessionService].
 * Automatically publishes a MediaStyle notification with song title, artist name, and album art,
 * integrates with lock screen / Quick Settings / Bluetooth controls, and handles audio focus cleanly.
 */
@OptIn(UnstableApi::class)
class RahmaPlaybackService : MediaSessionService() {

    var mediaSession: MediaSession? = null
        private set

    override fun onCreate() {
        super.onCreate()

        val httpFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(6_000)
            .setReadTimeoutMs(8_000)

        val baseDataSourceFactory = DefaultDataSource.Factory(this, httpFactory)
        val headerAwareDataSourceFactory = ResolvingDataSource.Factory(
            baseDataSourceFactory
        ) { dataSpec: DataSpec ->
            val registeredHeaders = PlaybackHeaderRegistry.getHeaders(dataSpec.uri.toString())
            if (registeredHeaders.isEmpty()) {
                dataSpec
            } else {
                val merged = LinkedHashMap<String, String>(dataSpec.httpRequestHeaders.size + registeredHeaders.size)
                merged.putAll(registeredHeaders)
                merged.putAll(dataSpec.httpRequestHeaders)
                dataSpec.buildUpon().setHttpRequestHeaders(merged).build()
            }
        }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(headerAwareDataSourceFactory))
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityPendingIntent)
            .build()

        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelName(R.string.media_notification_channel_name)
            .build().apply {
                setSmallIcon(R.drawable.ic_music_notification)
            }
        setMediaNotificationProvider(notificationProvider)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null ||
            !player.playWhenReady ||
            player.mediaItemCount == 0 ||
            player.playbackState == Player.STATE_ENDED
        ) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
