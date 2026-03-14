package com.mexmp3.mextv.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mexmp3.mextv.data.Album
import com.mexmp3.mextv.data.Artist
import com.mexmp3.mextv.data.Folder
import com.mexmp3.mextv.data.Song
import com.mexmp3.mextv.data.albumArtUri
import com.mexmp3.mextv.data.db.PlaylistEntity
import com.mexmp3.mextv.ui.components.AlbumArtImage
import com.mexmp3.mextv.ui.components.SongCard
import com.mexmp3.mextv.ui.viewmodel.MainViewModel

// ── Shared detail hero header ─────────────────────────────────────────────────

@Composable
private fun DetailHeader(
    art:       @Composable BoxScope.() -> Unit,
    title:     String,
    subtitle:  String,
    songCount: Int,
    onBack:    () -> Unit,
    onPlayAll: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
    ) {
        // Background art / colour block
        art()

        // Gradient scrim so text is always legible
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.45f to Color.Black.copy(alpha = 0.25f),
                        1f to Color.Black.copy(alpha = 0.82f)
                    )
                )
        )

        // Back button
        IconButton(
            onClick  = onBack,
            modifier = Modifier
                .statusBarsPadding()
                .padding(8.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint   = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }

        // Title / subtitle / play-all button
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text  = title,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text  = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.78f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onPlayAll,
                    shape  = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Play All  •  $songCount songs", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

// ── Album Detail ──────────────────────────────────────────────────────────────

@Composable
fun AlbumDetailScreen(
    album:          Album,
    vm:             MainViewModel,
    onBack:         () -> Unit,
    onSongLongPress: (Song) -> Unit
) {
    val allSongs    by vm.allSongs.collectAsState()
    val currentSong by vm.currentSong.collectAsState()

    val songs = remember(allSongs, album.id) {
        allSongs.filter { it.albumId == album.id }
            .sortedWith(compareBy({ it.album }, { it.title }))
    }

    LazyColumn(
        modifier       = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 0.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            DetailHeader(
                art = {
                    AlbumArtImage(
                        albumArtUri = album.albumArtUri,
                        modifier    = Modifier.fillMaxSize()
                    )
                },
                title     = album.name,
                subtitle  = album.artist,
                songCount = songs.size,
                onBack    = onBack,
                onPlayAll = { if (songs.isNotEmpty()) vm.playQueue(songs, 0) }
            )
        }
        item { Spacer(Modifier.height(4.dp)) }
        itemsIndexed(songs, key = { _, s -> s.id }) { idx, song ->
            SongCard(
                song      = song,
                isPlaying = currentSong?.id == song.id,
                onClick   = { vm.playQueue(songs, idx) },
                onLongClick = { onSongLongPress(song) }
            )
        }
    }
}

// ── Artist Detail ─────────────────────────────────────────────────────────────

@Composable
fun ArtistDetailScreen(
    artist:          Artist,
    vm:              MainViewModel,
    onBack:          () -> Unit,
    onSongLongPress: (Song) -> Unit
) {
    val allSongs    by vm.allSongs.collectAsState()
    val currentSong by vm.currentSong.collectAsState()

    val songs = remember(allSongs, artist.name) {
        allSongs.filter { it.artist == artist.name }
            .sortedWith(compareBy({ it.album }, { it.title }))
    }

    LazyColumn(
        modifier       = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 0.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            DetailHeader(
                art = {
                    // Tinted avatar background — no album art available at artist level
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.background
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Person,
                            contentDescription = null,
                            tint     = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                            modifier = Modifier.size(110.dp)
                        )
                    }
                },
                title     = artist.name,
                subtitle  = "${artist.albumCount} albums",
                songCount = songs.size,
                onBack    = onBack,
                onPlayAll = { if (songs.isNotEmpty()) vm.playQueue(songs, 0) }
            )
        }
        item { Spacer(Modifier.height(4.dp)) }
        itemsIndexed(songs, key = { _, s -> s.id }) { idx, song ->
            SongCard(
                song      = song,
                isPlaying = currentSong?.id == song.id,
                onClick   = { vm.playQueue(songs, idx) },
                onLongClick = { onSongLongPress(song) }
            )
        }
    }
}

// ── Folder Detail ─────────────────────────────────────────────────────────────

@Composable
fun FolderDetailScreen(
    folder:          Folder,
    vm:              MainViewModel,
    onBack:          () -> Unit,
    onSongLongPress: (Song) -> Unit
) {
    val allSongs    by vm.allSongs.collectAsState()
    val currentSong by vm.currentSong.collectAsState()

    val songs = remember(allSongs, folder.path) {
        allSongs.filter { it.folderName == folder.name }
            .sortedBy { it.title }
    }

    LazyColumn(
        modifier       = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 0.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            DetailHeader(
                art = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.secondaryContainer,
                                        MaterialTheme.colorScheme.background
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.FolderOpen,
                            contentDescription = null,
                            tint     = MaterialTheme.colorScheme.secondary.copy(alpha = 0.65f),
                            modifier = Modifier.size(110.dp)
                        )
                    }
                },
                title     = folder.name,
                subtitle  = folder.path,
                songCount = songs.size,
                onBack    = onBack,
                onPlayAll = { if (songs.isNotEmpty()) vm.playQueue(songs, 0) }
            )
        }
        item { Spacer(Modifier.height(4.dp)) }
        itemsIndexed(songs, key = { _, s -> s.id }) { idx, song ->
            SongCard(
                song      = song,
                isPlaying = currentSong?.id == song.id,
                onClick   = { vm.playQueue(songs, idx) },
                onLongClick = { onSongLongPress(song) }
            )
        }
    }
}

// ── Playlist Detail ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlist:        PlaylistEntity,
    vm:              MainViewModel,
    onBack:          () -> Unit,
    onSongLongPress: (Song) -> Unit
) {
    val allSongs       by vm.allSongs.collectAsState()
    val currentSong    by vm.currentSong.collectAsState()
    val playlistSongsDb by vm.getPlaylistSongs(playlist.id).collectAsState(emptyList())

    val songs = remember(playlistSongsDb, allSongs) {
        playlistSongsDb.mapNotNull { ps ->
            allSongs.find { it.path == ps.songPath } ?: allSongs.find { it.id == ps.songId }
        }
    }

    var deleteTarget      by remember { mutableStateOf<Song?>(null) }
    var showAddSongsSheet by remember { mutableStateOf(false) }

    // Fix 8: Scaffold so we can add an "Add Songs" FAB
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick        = { showAddSongsSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                shape          = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add songs to playlist")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { scaffoldPadding ->
        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(scaffoldPadding),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 0.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                DetailHeader(
                    art = {
                        val artUris = songs.map { it.albumArtUri() }.distinct().take(4)
                        if (artUris.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.radialGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primaryContainer,
                                                MaterialTheme.colorScheme.background
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.QueueMusic,
                                    contentDescription = null,
                                    tint     = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(110.dp)
                                )
                            }
                        } else if (artUris.size < 4) {
                            AlbumArtImage(albumArtUri = artUris.first(), modifier = Modifier.fillMaxSize())
                        } else {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(modifier = Modifier.weight(1f)) {
                                    AlbumArtImage(albumArtUri = artUris[0], modifier = Modifier.weight(1f).fillMaxHeight())
                                    AlbumArtImage(albumArtUri = artUris[1], modifier = Modifier.weight(1f).fillMaxHeight())
                                }
                                Row(modifier = Modifier.weight(1f)) {
                                    AlbumArtImage(albumArtUri = artUris[2], modifier = Modifier.weight(1f).fillMaxHeight())
                                    AlbumArtImage(albumArtUri = artUris[3], modifier = Modifier.weight(1f).fillMaxHeight())
                                }
                            }
                        }
                    },
                    title     = playlist.name,
                    subtitle  = if (songs.isEmpty()) "Empty playlist" else
                        songs.map { it.artist }.distinct().take(3).joinToString(", "),
                    songCount = songs.size,
                    onBack    = onBack,
                    onPlayAll = { if (songs.isNotEmpty()) vm.playQueue(songs, 0) }
                )
            }

            if (songs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.AutoMirrored.Rounded.QueueMusic,
                                contentDescription = null,
                                tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "This playlist is empty",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Tap + to add songs",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(4.dp)) }

            itemsIndexed(songs, key = { _, s -> s.id }) { idx, song ->
                SwipeToRemoveRow(
                    onRemove = { deleteTarget = song }
                ) {
                    SongCard(
                        song        = song,
                        isPlaying   = currentSong?.id == song.id,
                        onClick     = { vm.playQueue(songs, idx) },
                        onLongClick = { onSongLongPress(song) }
                    )
                }
            }
        }
    }

    // Swipe-to-remove confirmation
    deleteTarget?.let { song ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Remove from Playlist") },
            text  = { Text("Remove \"${song.title}\" from ${playlist.name}?") },
            confirmButton = {
                TextButton(onClick = {
                    vm.removeSongFromPlaylist(playlist.id, song.id)
                    deleteTarget = null
                }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }

    // Fix 8: Add Songs bottom sheet — shows all songs with checkboxes
    if (showAddSongsSheet) {
        val alreadyInPlaylist = remember(songs) { songs.map { it.id }.toSet() }
        var searchQuery by remember { mutableStateOf("") }
        val filtered = remember(allSongs, searchQuery) {
            if (searchQuery.isBlank()) allSongs
            else allSongs.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.artist.contains(searchQuery, ignoreCase = true)
            }
        }

        ModalBottomSheet(
            onDismissRequest = { showAddSongsSheet = false; searchQuery = "" },
            containerColor   = MaterialTheme.colorScheme.surface,
            shape            = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Add Songs",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { showAddSongsSheet = false; searchQuery = "" }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                }

                // Search bar
                OutlinedTextField(
                    value         = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder   = { Text("Search songs…") },
                    leadingIcon   = { Icon(Icons.Rounded.Search, null) },
                    singleLine    = true,
                    shape         = RoundedCornerShape(14.dp),
                    modifier      = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )

                Spacer(Modifier.height(8.dp))

                // Song list
                LazyColumn(
                    contentPadding      = PaddingValues(start = 12.dp, end = 12.dp, top = 0.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filtered, key = { it.id }) { song ->
                        val alreadyAdded = song.id in alreadyInPlaylist
                        AddSongRow(
                            song         = song,
                            alreadyAdded = alreadyAdded,
                            onAdd        = {
                                if (!alreadyAdded) {
                                    vm.addSongToPlaylist(playlist.id, song)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddSongRow(
    song: Song,
    alreadyAdded: Boolean,
    onAdd: () -> Unit
) {
    Card(
        shape  = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (alreadyAdded)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
            ) {
                AlbumArtImage(albumArtUri = song.albumArtUri(), modifier = Modifier.fillMaxSize())
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text     = song.title,
                    style    = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color    = if (alreadyAdded)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text  = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            if (alreadyAdded) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = "Already in playlist",
                    tint     = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                FilledTonalIconButton(
                    onClick = onAdd,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add to playlist",
                        modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// ── Swipe-to-remove wrapper ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToRemoveRow(
    onRemove: () -> Unit,
    content:  @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { it * 0.4f }
    )

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart ||
            dismissState.currentValue == SwipeToDismissBoxValue.StartToEnd) {
            onRemove()
            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }

    SwipeToDismissBox(
        state            = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.Settled     -> MaterialTheme.colorScheme.surfaceVariant
                    else -> MaterialTheme.colorScheme.errorContainer
                },
                tween(200), label = "swipeColor"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd)
                    Alignment.CenterStart else Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Rounded.DeleteOutline,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    ) {
        content()
    }
}
