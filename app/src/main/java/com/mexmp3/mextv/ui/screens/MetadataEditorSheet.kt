package com.mexmp3.mextv.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mexmp3.mextv.R
import com.mexmp3.mextv.data.Song

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetadataEditorSheet(
    song: Song,
    onDismiss: () -> Unit,
    onSave: (title: String, artist: String, album: String) -> Unit
) {
    var title by remember { mutableStateOf(song.title) }
    var artist by remember { mutableStateOf(song.artist) }
    var album by remember { mutableStateOf(song.album) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.meta_editor_title),
                style = MaterialTheme.typography.titleLarge
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.meta_title_hint)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Rounded.MusicNote, null) }
            )

            OutlinedTextField(
                value = artist,
                onValueChange = { artist = it },
                label = { Text(stringResource(R.string.meta_artist_hint)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Rounded.Person, null) }
            )

            OutlinedTextField(
                value = album,
                onValueChange = { album = it },
                label = { Text(stringResource(R.string.meta_album_hint)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Rounded.Album, null) }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.meta_cancel))
                }
                Button(
                    onClick = {
                        onSave(title.trim(), artist.trim(), album.trim())
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    enabled = title.isNotBlank()
                ) {
                    Text(stringResource(R.string.meta_save))
                }
            }
        }
    }
}
