package com.mexmp3.mextv.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mexmp3.mextv.R
import com.mexmp3.mextv.data.Song
import com.mexmp3.mextv.ui.components.EmptyState
import com.mexmp3.mextv.ui.components.LoadingState
import com.mexmp3.mextv.ui.components.SongCard
import com.mexmp3.mextv.ui.viewmodel.LibraryState
import com.mexmp3.mextv.ui.viewmodel.MainViewModel

private enum class SongSortOrder { TITLE, RECENTLY_ADDED, MOST_PLAYED }

@Composable
fun SongsScreen(
    vm: MainViewModel,
    onSongLongPress: (Song) -> Unit
) {
    val libraryState by vm.libraryState.collectAsState()
    val currentSong  by vm.currentSong.collectAsState()
    val recentIds    by vm.recentSongIds.collectAsState()

    var sortOrder    by remember { mutableStateOf(SongSortOrder.TITLE) }
    var showSortMenu by remember { mutableStateOf(false) }

    when (val state = libraryState) {
        is LibraryState.Loading -> LoadingState()

        is LibraryState.Empty -> EmptyState(
            title       = stringResource(R.string.library_empty_title),
            message     = stringResource(R.string.library_empty_msg),
            icon        = Icons.Rounded.MusicOff,
            actionLabel = stringResource(R.string.library_scan),
            onAction    = { vm.scanLibrary() }
        )

        is LibraryState.Error -> EmptyState(
            title       = stringResource(R.string.error_generic),
            message     = state.message,
            icon        = Icons.Rounded.ErrorOutline,
            actionLabel = stringResource(R.string.error_retry),
            onAction    = { vm.scanLibrary() }
        )

        is LibraryState.Ready -> {
            val songs = remember(state.songs, sortOrder, recentIds) {
                when (sortOrder) {
                    SongSortOrder.TITLE -> state.songs
                    SongSortOrder.RECENTLY_ADDED -> state.songs.sortedByDescending { it.dateAdded }
                    SongSortOrder.MOST_PLAYED -> {
                        // Rank by how many times the song appears in recent history
                        val playCount = recentIds.groupingBy { it }.eachCount()
                        state.songs.sortedByDescending { playCount[it.id] ?: 0 }
                    }
                }
            }
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text     = stringResource(R.string.library_songs_count, songs.size),
                        style    = MaterialTheme.typography.labelMedium,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    // Sort button
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Sort,
                                contentDescription = "Sort",
                                tint = if (sortOrder != SongSortOrder.TITLE)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded         = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Title (A–Z)") },
                                onClick = { sortOrder = SongSortOrder.TITLE; showSortMenu = false },
                                trailingIcon = {
                                    if (sortOrder == SongSortOrder.TITLE)
                                        Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Recently Added") },
                                onClick = { sortOrder = SongSortOrder.RECENTLY_ADDED; showSortMenu = false },
                                trailingIcon = {
                                    if (sortOrder == SongSortOrder.RECENTLY_ADDED)
                                        Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Most Played") },
                                onClick = { sortOrder = SongSortOrder.MOST_PLAYED; showSortMenu = false },
                                trailingIcon = {
                                    if (sortOrder == SongSortOrder.MOST_PLAYED)
                                        Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary)
                                }
                            )
                        }
                    }
                }
                LazyColumn(
                    contentPadding      = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(songs, key = { _, s -> s.id }) { index, song ->
                        SongCard(
                            song        = song,
                            isPlaying   = currentSong?.id == song.id,
                            onClick     = { vm.playQueue(songs, index) },
                            onLongClick = { onSongLongPress(song) }
                        )
                    }
                    item { Spacer(Modifier.height(120.dp)) }
                }
            }
        }
    }
}
