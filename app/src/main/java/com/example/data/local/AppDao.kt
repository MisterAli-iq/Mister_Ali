package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Favorites
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE songId = :songId)")
    fun isFavorite(songId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE songId = :songId")
    suspend fun removeFavorite(songId: String)

    // Downloads
    @Query("SELECT * FROM downloads ORDER BY downloadedAt DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDownload(download: DownloadEntity)

    @Query("DELETE FROM downloads WHERE downloadId = :downloadId")
    suspend fun deleteDownload(downloadId: String)

    @Query("DELETE FROM downloads WHERE songId = :songId")
    suspend fun deleteDownloadsForSong(songId: String)

    @Query("DELETE FROM downloads")
    suspend fun clearAllDownloads()

    @Query("SELECT EXISTS(SELECT 1 FROM downloads WHERE songId = :songId)")
    fun isSongDownloaded(songId: String): Flow<Boolean>

    // Notifications
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markNotificationAsRead(id: String)

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    fun getUnreadNotificationCount(): Flow<Int>

    // Local Files
    @Query("SELECT * FROM local_files ORDER BY addedAt DESC")
    fun getAllLocalFiles(): Flow<List<LocalFileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocalFile(file: LocalFileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocalFiles(files: List<LocalFileEntity>)

    @Query("DELETE FROM local_files WHERE id = :id")
    suspend fun deleteLocalFile(id: String)

    @Query("DELETE FROM local_files")
    suspend fun clearLocalFiles()

    // Custom Playlists
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: String)

    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY addedAt ASC")
    fun getItemsForPlaylist(playlistId: String): Flow<List<PlaylistItemEntity>>

    @Query("SELECT * FROM playlist_items")
    fun getAllPlaylistItems(): Flow<List<PlaylistItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSongToPlaylist(item: PlaylistItemEntity)

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: String, songId: String)

    // Recent History (last 20 tracks)
    @Query("SELECT * FROM recent_played ORDER BY playedAt DESC LIMIT 20")
    fun getRecentPlayed(): Flow<List<RecentPlayedEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordPlayedSong(recent: RecentPlayedEntity)

    @Query("DELETE FROM recent_played")
    suspend fun clearRecentHistory()

    // Imported Real Media & Reels
    @Query("SELECT * FROM imported_media ORDER BY addedAt DESC")
    fun getAllImportedMedia(): Flow<List<ImportedMediaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImportedMedia(item: ImportedMediaEntity)

    @Query("DELETE FROM imported_media WHERE id = :id")
    suspend fun deleteImportedMedia(id: String)

    // Cached Official Channel Feed Media (prioritizes @RahmaRiad Official first)
    @Query("SELECT * FROM cached_channel_media ORDER BY isOfficialChannel DESC, publishedAt DESC")
    fun getAllCachedChannelMedia(): Flow<List<CachedChannelMediaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedChannelMedia(items: List<CachedChannelMediaEntity>)

    @Query("DELETE FROM cached_channel_media")
    suspend fun clearCachedChannelMedia()

    // Real User Comments on Reels & Player
    @Query("SELECT * FROM reel_comments WHERE reelId = :reelId ORDER BY createdAt DESC")
    fun getCommentsForReel(reelId: String): Flow<List<ReelCommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReelComment(comment: ReelCommentEntity)

    // Real Persistent Likes for Reels & Songs
    @Query("SELECT * FROM reel_likes ORDER BY likedAt DESC")
    fun getAllReelLikes(): Flow<List<ReelLikeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReelLike(like: ReelLikeEntity)

    @Query("DELETE FROM reel_likes WHERE reelId = :reelId")
    suspend fun removeReelLike(reelId: String)

    // Featured Songs Preferences
    @Query("SELECT * FROM featured_preferences ORDER BY updatedAt DESC")
    fun getAllFeaturedPreferences(): Flow<List<FeaturedPreferenceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFeaturedPreference(pref: FeaturedPreferenceEntity)
}
