package com.mexmp3.mextv.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mexmp3.mextv.R
import com.mexmp3.mextv.ui.components.SongCard
import com.mexmp3.mextv.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(vm: MainViewModel) {
    val query by vm.searchQuery.collectAsState()
    val results by vm.searchResults.collectAsState()
    val currentSong by vm.currentSong.collectAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        try { focusRequester.requestFocus() } catch (_: Exception) {}
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { vm.setSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .focusRequester(focusRequester),
            placeholder = { Text(stringResource(R.string.search_hint)) },
            leadingIcon = {
                Icon(Icons.Rounded.Search, contentDescription = null)
            },
            trailingIcon = {
                AnimatedVisibility(query.isNotEmpty()) {
                    IconButton(onClick = { vm.setSearchQuery("") }) {
                        Icon(Icons.Rounded.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        AnimatedContent(
            targetState = query.isEmpty() to results.isEmpty(),
            label = "search_content"
        ) { (emptyQuery, emptyResults) ->
            when {
                emptyQuery -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Rounded.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Search your music",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                emptyResults -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Rounded.SearchOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No results for \"$query\"",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text(
                                "${results.size} results",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                            )
                        }
                        itemsIndexed(results, key = { _, s -> s.id }) { index, song ->
                            SongCard(
                                song = song,
                                isPlaying = currentSong?.id == song.id,
                                onClick = { vm.playQueue(results, index) }
                            )
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}
