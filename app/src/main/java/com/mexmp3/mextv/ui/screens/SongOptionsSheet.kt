package com.mexmp3.mextv.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mexmp3.mextv.R
import com.mexmp3.mextv.data.Song
import com.mexmp3.mextv.data.albumArtUri
import com.mexmp3.mextv.data.db.PlaylistEntity
import com.mexmp3.mextv.ui.components.AlbumArtImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongOptionsSheet(
    song: Song,
    playlists: List<PlaylistEntity>,
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: (PlaylistEntity) -> Unit,
    onEditInfo: () -> Unit,
    onDelete: () -> Unit = {}
) {
    var showPlaylistPicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest  = onDismiss,
        containerColor    = MaterialTheme.colorScheme.surface,
        shape             = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            // ── Song header ────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(10.dp))
                ) {
                    AlbumArtImage(
                        albumArtUri = song.albumArtUri(),
                        modifier    = Modifier.fillMaxSize()
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(song.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1)
                    Text(song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1)
                }
            }

            HorizontalDivider(
                modifier  = Modifier.padding(vertical = 16.dp),
                color     = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            )

            // ── Actions ────────────────────────────────────────────────────
            if (!showPlaylistPicker) {
                OptionRow(Icons.Rounded.QueuePlayNext, stringResource(R.string.song_options_play_next)) {
                    onPlayNext(); onDismiss()
                }
                OptionRow(Icons.Rounded.AddToQueue, stringResource(R.string.song_options_add_queue)) {
                    onAddToQueue(); onDismiss()
                }
                OptionRow(Icons.AutoMirrored.Rounded.PlaylistAdd, stringResource(R.string.song_options_add_playlist)) {
                    showPlaylistPicker = true
                }
                OptionRow(Icons.Rounded.Edit, stringResource(R.string.song_options_edit)) {
                    onEditInfo(); onDismiss()
                }
                OptionRow(Icons.Rounded.DeleteOutline, "Delete from device",
                    tint = MaterialTheme.colorScheme.error) {
                    onDelete(); onDismiss()
                }
            } else {
                // ── Playlist picker ────────────────────────────────────────
                TextButton(onClick = { showPlaylistPicker = false }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Back")
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Add to playlist",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                if (playlists.isEmpty()) {
                    Text(
                        "No playlists yet. Create one in the Playlists tab.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    playlists.forEach { playlist ->
                        OptionRow(Icons.AutoMirrored.Rounded.QueueMusic, playlist.name) {
                            onAddToPlaylist(playlist)
                            onDismiss()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionRow(
    icon: ImageVector,
    label: String,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    TextButton(
        onClick  = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = tint)
            Spacer(Modifier.width(16.dp))
            Text(
                text  = label,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
