package com.mexmp3.mextv.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// -------- Entities --------

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_songs",
    foreignKeys = [ForeignKey(
        entity = PlaylistEntity::class,
        parentColumns = ["id"],
        childColumns = ["playlistId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("playlistId")]
)
data class PlaylistSongEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val songId: Long,
    val songPath: String,
    val addedAt: Long = System.currentTimeMillis(),
    val position: Int = 0
)

@Entity(tableName = "recent_songs")
data class RecentSongEntity(
    @PrimaryKey val songId: Long,
    val songPath: String,
    val playedAt: Long = System.currentTimeMillis()
)

// -------- DAOs --------

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)

    @Query("UPDATE playlists SET name = :name WHERE id = :id")
    suspend fun renamePlaylist(id: Long, name: String)

    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId ORDER BY position ASC")
    fun getSongsForPlaylist(playlistId: Long): Flow<List<PlaylistSongEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addSongToPlaylist(song: PlaylistSongEntity)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)

    @Query("SELECT COUNT(*) FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun getSongCount(playlistId: Long): Int
}

@Dao
interface RecentDao {
    @Query("SELECT * FROM recent_songs ORDER BY playedAt DESC LIMIT 50")
    fun getRecentSongs(): Flow<List<RecentSongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecent(song: RecentSongEntity)

    @Query("DELETE FROM recent_songs WHERE songId NOT IN (SELECT songId FROM recent_songs ORDER BY playedAt DESC LIMIT 50)")
    suspend fun pruneOldEntries()
}
