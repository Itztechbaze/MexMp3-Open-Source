package com.mexmp3.mextv.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mexmp3.mextv.R
import com.mexmp3.mextv.data.db.PlaylistEntity
import com.mexmp3.mextv.ui.components.EmptyState
import com.mexmp3.mextv.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(vm: MainViewModel, onPlaylistClick: (PlaylistEntity) -> Unit) {
    val playlists         by vm.playlists.collectAsState()
    var showCreateDialog  by remember { mutableStateOf(false) }
    var newPlaylistName   by remember { mutableStateOf("") }
    var deleteTarget      by remember { mutableStateOf<PlaylistEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick         = { showCreateDialog = true },
                containerColor  = MaterialTheme.colorScheme.primary,
                shape           = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.playlist_new))
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (playlists.isEmpty()) {
            EmptyState(
                title   = "No Playlists",
                message = "Tap + to create your first playlist",
                icon    = Icons.AutoMirrored.Rounded.QueueMusic,
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                contentPadding      = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier            = Modifier.padding(padding)
            ) {
                items(playlists, key = { it.id }) { playlist ->
                    PlaylistRow(
                        playlist  = playlist,
                        onClick   = { onPlaylistClick(playlist) },
                        onDelete  = { deleteTarget = playlist }
                    )
                }
                item { Spacer(Modifier.height(100.dp)) }
            }
        }
    }

    // ── Create dialog ─────────────────────────────────────────────────────────
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false; newPlaylistName = "" },
            title            = { Text(stringResource(R.string.playlist_new)) },
            text             = {
                OutlinedTextField(
                    value          = newPlaylistName,
                    onValueChange  = { newPlaylistName = it },
                    label          = { Text(stringResource(R.string.playlist_name_hint)) },
                    singleLine     = true,
                    shape          = RoundedCornerShape(12.dp)
                )
            },
            confirmButton    = {
                TextButton(
                    enabled = newPlaylistName.isNotBlank(),
                    onClick = {
                        vm.createPlaylist(newPlaylistName.trim())
                        newPlaylistName = ""
                        showCreateDialog = false
                    }
                ) { Text(stringResource(R.string.playlist_create)) }
            },
            dismissButton    = {
                TextButton(onClick = { showCreateDialog = false; newPlaylistName = "" }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // ── Delete confirmation ───────────────────────────────────────────────────
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title            = { Text(stringResource(R.string.playlist_delete)) },
            text             = { Text(stringResource(R.string.playlist_delete_confirm, target.name)) },
            confirmButton    = {
                TextButton(onClick = { vm.deletePlaylist(target); deleteTarget = null }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton    = {
                TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun PlaylistRow(
    playlist: PlaylistEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
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
                imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text     = playlist.name,
                style    = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Rounded.DeleteOutline,
                    contentDescription = stringResource(R.string.playlist_delete),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
