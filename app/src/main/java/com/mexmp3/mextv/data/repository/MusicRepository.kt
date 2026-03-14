package com.mexmp3.mextv.data.repository

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import com.mexmp3.mextv.data.*
import com.mexmp3.mextv.data.db.AppDatabase
import com.mexmp3.mextv.data.db.PlaylistEntity
import com.mexmp3.mextv.data.db.PlaylistSongEntity
import com.mexmp3.mextv.data.db.RecentSongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

class MusicRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val playlistDao = db.playlistDao()
    private val recentDao = db.recentDao()

    // ---- MediaStore scanning ----

    suspend fun scanSongs(): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.SIZE
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 10000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(uri, projection, selection, null, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)

            while (cursor.moveToNext()) {
                val path = cursor.getString(pathCol) ?: continue
                val file = File(path)
                if (!file.exists()) continue

                songs.add(Song(
                    id = cursor.getLong(idCol),
                    title = cursor.getString(titleCol) ?: file.nameWithoutExtension,
                    artist = cursor.getString(artistCol) ?: "Unknown Artist",
                    album = cursor.getString(albumCol) ?: "Unknown Album",
                    albumId = cursor.getLong(albumIdCol),
                    duration = cursor.getLong(durationCol),
                    path = path,
                    dateAdded = cursor.getLong(dateCol),
                    folderName = file.parentFile?.name ?: "Unknown",
                    size = cursor.getLong(sizeCol)
                ))
            }
        }
        songs
    }

    suspend fun getAlbums(songs: List<Song>): List<Album> = withContext(Dispatchers.Default) {
        songs.groupBy { it.albumId }.map { (albumId, albumSongs) ->
            val first = albumSongs.first()
            Album(
                id = albumId,
                name = first.album,
                artist = first.artist,
                songCount = albumSongs.size,
                albumArtUri = first.albumArtUri()
            )
        }.sortedBy { it.name }
    }

    suspend fun getArtists(songs: List<Song>): List<Artist> = withContext(Dispatchers.Default) {
        songs.groupBy { it.artist }.map { (artist, artistSongs) ->
            Artist(
                id = artistSongs.first().id,
                name = artist,
                albumCount = artistSongs.distinctBy { it.albumId }.size,
                songCount = artistSongs.size
            )
        }.sortedBy { it.name }
    }

    suspend fun getFolders(songs: List<Song>): List<Folder> = withContext(Dispatchers.Default) {
        songs.groupBy { it.folderName }.map { (folder, folderSongs) ->
            Folder(
                path = File(folderSongs.first().path).parent ?: folder,
                name = folder,
                songCount = folderSongs.size
            )
        }.sortedBy { it.name }
    }

    // ---- Playlists (Room) ----

    fun getAllPlaylists() = playlistDao.getAllPlaylists()

    suspend fun createPlaylist(name: String) = playlistDao.insertPlaylist(PlaylistEntity(name = name))

    suspend fun deletePlaylist(playlist: PlaylistEntity) = playlistDao.deletePlaylist(playlist)

    fun getPlaylistSongs(playlistId: Long) = playlistDao.getSongsForPlaylist(playlistId)

    suspend fun addSongToPlaylist(playlistId: Long, song: Song) {
        val count = playlistDao.getSongCount(playlistId)
        playlistDao.addSongToPlaylist(
            PlaylistSongEntity(
                playlistId = playlistId,
                songId = song.id,
                songPath = song.path,
                position = count
            )
        )
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) =
        playlistDao.removeSongFromPlaylist(playlistId, songId)

    // ---- Recent ----

    fun getRecentSongs(): Flow<List<RecentSongEntity>> = recentDao.getRecentSongs()

    suspend fun markPlayed(song: Song) {
        recentDao.insertRecent(RecentSongEntity(songId = song.id, songPath = song.path))
        recentDao.pruneOldEntries()
    }
}
