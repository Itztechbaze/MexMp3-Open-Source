package com.mexmp3.mextv.ui.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mexmp3.mextv.data.*
import com.mexmp3.mextv.data.db.PlaylistEntity
import com.mexmp3.mextv.data.repository.MusicRepository
import com.mexmp3.mextv.data.repository.PrefsRepository
import com.mexmp3.mextv.service.MusicService
import com.mexmp3.mextv.util.Constants
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class LibraryState {
    data object Loading : LibraryState()
    data object Empty   : LibraryState()
    data class  Ready(val songs: List<Song>) : LibraryState()
    data class  Error(val message: String)   : LibraryState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val prefs = PrefsRepository(application)
    private val repo = MusicRepository(application)

    // ── Library ───────────────────────────────────────────────────────────────
    private val _libraryState = MutableStateFlow<LibraryState>(LibraryState.Loading)
    val libraryState: StateFlow<LibraryState> = _libraryState.asStateFlow()

    private val _allSongs = MutableStateFlow<List<Song>>(emptyList())
    val allSongs: StateFlow<List<Song>> = _allSongs.asStateFlow()

    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums.asStateFlow()

    private val _artists = MutableStateFlow<List<Artist>>(emptyList())
    val artists: StateFlow<List<Artist>> = _artists.asStateFlow()

    private val _folders = MutableStateFlow<List<Folder>>(emptyList())
    val folders: StateFlow<List<Folder>> = _folders.asStateFlow()

    // ── Search ────────────────────────────────────────────────────────────────
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<Song>> = combine(_allSongs, _searchQuery) { songs, q ->
        if (q.isBlank()) emptyList()
        else songs.filter {
            it.title.contains(q, ignoreCase = true)  ||
            it.artist.contains(q, ignoreCase = true) ||
            it.album.contains(q, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // ── Playlists ─────────────────────────────────────────────────────────────
    val playlists: StateFlow<List<PlaylistEntity>> = repo.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // ── Recently played song IDs (from DB, ordered most-recent-first) ─────────
    val recentSongIds: StateFlow<List<Long>> = repo.getRecentSongs()
        .map { list -> list.map { it.songId } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // ── Theme / Onboarding ────────────────────────────────────────────────────
    val themeName: StateFlow<String> = prefs.themeName
        .stateIn(viewModelScope, SharingStarted.Eagerly, Constants.THEME_MILITARY)

    val isOnboardingDone: StateFlow<Boolean> = prefs.isOnboardingDone
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // ── Player state (delegates to MusicService companion object) ─────────────
    val isPlaying:       StateFlow<Boolean>   = MusicService.isPlaying
    val currentSong:     StateFlow<Song?>     = MusicService.currentSong
    val currentPosition: StateFlow<Long>      = MusicService.currentPosition
    val duration:        StateFlow<Long>      = MusicService.duration
    val shuffleOn:       StateFlow<Boolean>   = MusicService.shuffleOn
    val repeatMode:      StateFlow<Int>       = MusicService.repeatMode

    // ── Service binding ───────────────────────────────────────────────────────
    private var musicService: MusicService? = null

    /**
     * Queue of lambda actions that arrive before the service is bound.
     * Drained once onServiceConnected fires.
     */
    private val pendingActions = ArrayDeque<(MusicService) -> Unit>()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            val svc = (binder as MusicService.MusicBinder).getService()
            musicService = svc
            // Drain any actions that arrived before binding completed
            while (pendingActions.isNotEmpty()) {
                pendingActions.removeFirst().invoke(svc)
            }
        }
        override fun onServiceDisconnected(name: ComponentName) {
            musicService = null
        }
    }

    init {
        bindMusicService()
    }

    private fun bindMusicService() {
        val ctx = getApplication<Application>()
        val intent = Intent(ctx, MusicService::class.java)
        // BIND_AUTO_CREATE — create the service if not running
        // BIND_ABOVE_CLIENT — tells the OS this service is more important than the client
        //                     activity; prevents it being killed when the activity closes
        ctx.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE or Context.BIND_ABOVE_CLIENT)
    }

    /** Run [action] on the service immediately if bound, or enqueue it. */
    private fun withService(action: (MusicService) -> Unit) {
        val svc = musicService
        if (svc != null) action(svc)
        else pendingActions.addLast(action)
    }

    // ── Library scan ──────────────────────────────────────────────────────────
    fun scanLibrary() {
        viewModelScope.launch {
            _libraryState.value = LibraryState.Loading
            runCatching {
                val songs = repo.scanSongs()
                _allSongs.value  = songs
                _albums.value    = repo.getAlbums(songs)
                _artists.value   = repo.getArtists(songs)
                _folders.value   = repo.getFolders(songs)
                _libraryState.value = if (songs.isEmpty()) LibraryState.Empty else LibraryState.Ready(songs)
            }.onFailure { e ->
                _libraryState.value = LibraryState.Error(e.message ?: "Unknown error")
            }
        }
    }

    // ── Playback ──────────────────────────────────────────────────────────────
    fun playSong(song: Song) {
        withService { it.playSong(song) }
        viewModelScope.launch { repo.markPlayed(song) }
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        withService { it.playQueue(songs, startIndex) }
        if (songs.isNotEmpty()) {
            viewModelScope.launch { repo.markPlayed(songs[startIndex.coerceIn(0, songs.lastIndex)]) }
        }
    }

    fun togglePlayPause() = withService { it.togglePlayPause() }
    fun skipNext()         = withService { it.skipToNext() }
    fun skipPrev()         = withService { it.skipToPrevious() }
    fun seekTo(pos: Long)  = withService { it.seekTo(pos) }

    /** Insert [song] immediately after the currently-playing track */
    fun playNext(song: Song) = withService { it.playNext(song) }

    /** Append [song] to the end of the current queue */
    fun addToQueue(song: Song) = withService { it.addToQueue(song) }

    fun toggleShuffle() {
        val new = !MusicService.shuffleOn.value
        MusicService.shuffleOn.value = new
        if (!new) {
            // Restore original sequential queue; keep playing the same song at the correct index
            val orig = MusicService.originalQueue.value
            if (orig.isNotEmpty()) {
                val playing = MusicService.currentSong.value
                MusicService.queue.value = orig
                MusicService.queueIndex.value = orig.indexOfFirst { it.id == playing?.id }
                    .coerceAtLeast(0)
            }
        }
        viewModelScope.launch { prefs.setShuffle(new) }
    }

    fun cycleRepeat() {
        val new = (MusicService.repeatMode.value + 1) % 3
        MusicService.repeatMode.value = new
        viewModelScope.launch { prefs.setRepeatMode(new) }
    }

    // ── Search ────────────────────────────────────────────────────────────────
    fun setSearchQuery(q: String) { _searchQuery.value = q }

    // ── Playlists ─────────────────────────────────────────────────────────────
    fun createPlaylist(name: String) = viewModelScope.launch { repo.createPlaylist(name) }
    fun deletePlaylist(p: PlaylistEntity) = viewModelScope.launch { repo.deletePlaylist(p) }
    fun addSongToPlaylist(playlistId: Long, song: Song) =
        viewModelScope.launch { repo.addSongToPlaylist(playlistId, song) }
    fun removeSongFromPlaylist(playlistId: Long, songId: Long) =
        viewModelScope.launch { repo.removeSongFromPlaylist(playlistId, songId) }
    fun getPlaylistSongs(id: Long) = repo.getPlaylistSongs(id)

    // ── Settings ──────────────────────────────────────────────────────────────
    fun setTheme(name: String) = viewModelScope.launch { prefs.setTheme(name) }
    fun completeOnboarding()   = viewModelScope.launch { prefs.setOnboardingDone(true) }

    val eqCustomBandsRaw: StateFlow<String> = prefs.eqCustomBands
        .stateIn(viewModelScope, SharingStarted.Lazily, "0,0,0,0,0")

    /** Parse the stored CSV string to a list of 5 dB floats */
    fun parseCustomBands(raw: String): List<Float> =
        raw.split(",").mapNotNull { it.trim().toFloatOrNull() }
            .let { if (it.size == 5) it else List(5) { 0f } }

    fun setEqualizerEnabled(enabled: Boolean, preset: String) {
        withService { it.setEqualizerEnabled(enabled, preset) }
        viewModelScope.launch {
            prefs.setEqEnabled(enabled)
            prefs.setEqPreset(preset)
        }
    }

    fun applyEqPreset(preset: String) {
        withService { it.applyEqPreset(preset) }
        viewModelScope.launch { prefs.setEqPreset(preset) }
    }

    /** Called when user drags a custom EQ band slider — persists bands and applies live */
    fun saveCustomEqBands(bands: List<Float>) {
        // Update the Constants.EQ_PRESETS["Custom"] in-memory so live apply works
        Constants.EQ_PRESETS["Custom"]?.also { arr ->
            bands.forEachIndexed { i, db -> if (i < arr.size) arr[i] = (db * 100).toInt() }
        }
        withService { it.applyEqPreset("Custom") }
        viewModelScope.launch {
            prefs.setEqPreset("Custom")
            prefs.setEqCustomBands(bands)
        }
    }

    /** Delete [song] from MediaStore. Returns true on success, false on failure. */
    fun deleteSong(song: Song, onResult: (success: Boolean) -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val resolver = getApplication<Application>().contentResolver
            val uri = ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.id
            )
            val deleted = runCatching { resolver.delete(uri, null, null) > 0 }.getOrElse { false }
            withContext(kotlinx.coroutines.Dispatchers.Main) {
                onResult(deleted)
            }
            if (deleted) scanLibrary()
        }
    }

    /** Write title/artist/album back to MediaStore (API 29+ uses ContentValues update) */
    fun updateSongMetadata(song: Song, title: String, artist: String, album: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val resolver = getApplication<Application>().contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Audio.Media.TITLE,  title)
                    put(MediaStore.Audio.Media.ARTIST, artist)
                    put(MediaStore.Audio.Media.ALBUM,  album)
                }
                val uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.id
                )
                resolver.update(uri, values, null, null)
                // Refresh library so new tags appear immediately
                scanLibrary()
            }
        }
    }

    fun setSleepTimer(minutes: Int) {
        withService { it.setSleepTimer(minutes) }
        viewModelScope.launch { prefs.setSleepTimerMinutes(minutes) }
    }

    fun setGapless(enabled: Boolean) {
        withService { it.setGapless(enabled) }
        viewModelScope.launch { prefs.setGapless(enabled) }
    }

    override fun onCleared() {
        super.onCleared()
        pendingActions.clear()
        // Only unbind if music is NOT playing — unbinding while playing on Vivo/Xiaomi
        // can cause the OS to kill the service since it loses its bound client.
        // The service keeps itself alive via startForegroundService() in onTaskRemoved.
        if (!MusicService.isPlaying.value) {
            runCatching { getApplication<Application>().unbindService(serviceConnection) }
        }
    }
}
