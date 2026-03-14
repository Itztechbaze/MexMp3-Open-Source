package com.mexmp3.mextv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.mexmp3.mextv.data.Artist
import com.mexmp3.mextv.ui.components.EmptyState
import com.mexmp3.mextv.ui.components.LoadingState
import com.mexmp3.mextv.ui.viewmodel.LibraryState
import com.mexmp3.mextv.ui.viewmodel.MainViewModel

@Composable
fun ArtistsScreen(vm: MainViewModel, onArtistClick: (Artist) -> Unit) {
    val libraryState by vm.libraryState.collectAsState()
    val artists      by vm.artists.collectAsState()

    when (val state = libraryState) {
        is LibraryState.Loading -> LoadingState()

        is LibraryState.Empty -> EmptyState(
            icon    = Icons.Rounded.Person,
            title   = "No Artists",
            message = "Scan your library to find artists"
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
                items(artists, key = { it.id }) { artist ->
                    ArtistRow(artist = artist, onClick = { onArtistClick(artist) })
                }
                item { Spacer(Modifier.height(100.dp)) }
            }
        }
    }
}

@Composable
private fun ArtistRow(artist: Artist, onClick: () -> Unit) {
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
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = artist.name,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text  = "${artist.albumCount} albums \u2022 ${artist.songCount} songs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
