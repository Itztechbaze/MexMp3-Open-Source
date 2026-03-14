package com.mexmp3.mextv.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mexmp3.mextv.data.Album
import com.mexmp3.mextv.ui.components.AlbumArtImage
import com.mexmp3.mextv.ui.components.EmptyState
import com.mexmp3.mextv.ui.components.LoadingState
import com.mexmp3.mextv.ui.viewmodel.LibraryState
import com.mexmp3.mextv.ui.viewmodel.MainViewModel

@Composable
fun AlbumsScreen(vm: MainViewModel, onAlbumClick: (Album) -> Unit) {
    val libraryState by vm.libraryState.collectAsState()
    val albums       by vm.albums.collectAsState()

    when (val state = libraryState) {
        is LibraryState.Loading -> LoadingState()

        is LibraryState.Empty -> EmptyState(
            icon         = Icons.Rounded.Album,
            title        = "No Albums",
            message      = "Scan your library to browse albums",
            actionLabel  = "Scan",
            onAction     = { vm.scanLibrary() }
        )

        is LibraryState.Error -> EmptyState(
            icon        = Icons.Rounded.ErrorOutline,
            title       = "Something went wrong",
            message     = state.message,
            actionLabel = "Retry",
            onAction    = { vm.scanLibrary() }
        )

        is LibraryState.Ready -> {
            LazyVerticalGrid(
                columns             = GridCells.Fixed(2),
                contentPadding      = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(albums, key = { it.id }) { album ->
                    AlbumCard(album = album, onClick = { onAlbumClick(album) })
                }
                item(span = { GridItemSpan(2) }) { Spacer(Modifier.height(100.dp)) }
            }
        }
    }
}

@Composable
private fun AlbumCard(album: Album, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape   = RoundedCornerShape(20.dp),
        colors  = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Album art — taller ratio so it has room to breathe
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            ) {
                AlbumArtImage(albumArtUri = album.albumArtUri, modifier = Modifier.fillMaxSize())

                // Song count pill overlay on the art
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.75f),
                    tonalElevation = 0.dp
                ) {
                    Text(
                        text = "${album.songCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Text block — enough padding so nothing feels jammed
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text     = album.name,
                    style    = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color    = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text     = album.artist,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
