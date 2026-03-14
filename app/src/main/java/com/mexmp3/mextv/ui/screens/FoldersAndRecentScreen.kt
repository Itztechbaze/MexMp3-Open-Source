package com.mexmp3.mextv.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mexmp3.mextv.data.Folder
import com.mexmp3.mextv.ui.components.EmptyState
import com.mexmp3.mextv.ui.components.LoadingState
import com.mexmp3.mextv.ui.components.SongCard
import com.mexmp3.mextv.ui.viewmodel.LibraryState
import com.mexmp3.mextv.ui.viewmodel.MainViewModel

// ── Folders Screen ────────────────────────────────────────────────────────────

@Composable
fun FoldersScreen(vm: MainViewModel, onFolderClick: (Folder) -> Unit) {
    val libraryState by vm.libraryState.collectAsState()
    val folders      by vm.folders.collectAsState()

    when (val state = libraryState) {
        is LibraryState.Loading -> LoadingState()

        is LibraryState.Empty -> EmptyState(
            icon    = Icons.Rounded.FolderOpen,
            title   = "No Folders",
            message = "Scan your library to browse by folder"
        )

        is LibraryState.Error -> EmptyState(
            icon        = Icons.Rounded.ErrorOutline,
            title       = "Something went wrong",
            message     = state.message,
            actionLabel = "Retry",
            onAction    = { vm.scanLibrary() }
        )

        is LibraryState.Ready -> {
            LazyColumn(
                contentPadding      = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(folders, key = { it.path }) { folder ->
                    FolderRow(folder = folder, onClick = { onFolderClick(folder) })
                }
                item { Spacer(Modifier.height(100.dp)) }
            }
        }
    }
}

@Composable
private fun FolderRow(folder: Folder, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape   = RoundedCornerShape(16.dp),
        colors  = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(folder.name,  style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${folder.songCount} songs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Recent Screen — shows recently played songs ───────────────────────────────

@Composable
fun RecentScreen(vm: MainViewModel) {
    val allSongs    by vm.allSongs.collectAsState()
    val recentIds   by vm.recentSongIds.collectAsState()
    val currentSong by vm.currentSong.collectAsState()

    // Build ordered list of recently played songs (most-recent first),
    // resolved from the DB play-history IDs against the current library.
    // Falls back to recently-added sort if play history is empty.
    val recentlyPlayed = remember(recentIds, allSongs) {
        if (recentIds.isNotEmpty()) {
            val songById = allSongs.associateBy { it.id }
            recentIds.mapNotNull { songById[it] }.distinctBy { it.id }.take(50)
        } else {
            allSongs.sortedByDescending { it.dateAdded }.take(50)
        }
    }

    if (recentlyPlayed.isEmpty()) {
        EmptyState(
            icon    = Icons.Rounded.History,
            title   = "Nothing played yet",
            message = "Songs you play will appear here"
        )
    } else {
        LazyColumn(
            contentPadding      = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(recentlyPlayed, key = { _, s -> s.id }) { index, song ->
                SongCard(
                    song      = song,
                    isPlaying = currentSong?.id == song.id,
                    onClick   = { vm.playQueue(recentlyPlayed, index) }
                )
            }
            item { Spacer(Modifier.height(100.dp)) }
        }
    }
}
