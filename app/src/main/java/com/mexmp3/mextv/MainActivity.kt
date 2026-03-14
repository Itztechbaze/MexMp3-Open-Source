package com.mexmp3.mextv

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import kotlinx.coroutines.launch
import com.mexmp3.mextv.data.Album
import com.mexmp3.mextv.data.Artist
import com.mexmp3.mextv.data.Folder
import com.mexmp3.mextv.data.Song
import com.mexmp3.mextv.data.db.PlaylistEntity
import com.mexmp3.mextv.ui.components.MiniPlayer
import com.mexmp3.mextv.ui.screens.*
import com.mexmp3.mextv.ui.theme.MexMp3Theme
import com.mexmp3.mextv.ui.viewmodel.MainViewModel
import com.mexmp3.mextv.util.PermissionRationaleDialog
import com.mexmp3.mextv.util.rememberAudioPermissionState
import com.mexmp3.mextv.util.rememberNotificationPermissionState
import com.mexmp3.mextv.util.requestBatteryOptimizationExemption

// ── Navigation destinations ───────────────────────────────────────────────────
private object Route {
    const val ONBOARDING    = "onboarding"
    const val SONGS         = "songs"
    const val ALBUMS        = "albums"
    const val ARTISTS       = "artists"
    const val PLAYLISTS     = "playlists"
    const val FOLDERS       = "folders"
    const val RECENT        = "recent"
    const val SEARCH        = "search"
    const val NOW_PLAYING   = "now_playing"
    const val SETTINGS      = "settings"
    // Detail screens
    const val ALBUM_DETAIL    = "album_detail"
    const val ARTIST_DETAIL   = "artist_detail"
    const val FOLDER_DETAIL   = "folder_detail"
    const val PLAYLIST_DETAIL = "playlist_detail"
}

// Intent extra used by notification tap to go straight to Now Playing
const val EXTRA_OPEN_NOW_PLAYING = "open_now_playing"

private data class BottomNavItem(val route: String, val labelRes: Int, val icon: ImageVector)

private val bottomNavItems = listOf(
    BottomNavItem(Route.SONGS,     R.string.nav_songs,     Icons.Rounded.MusicNote),
    BottomNavItem(Route.ALBUMS,    R.string.nav_albums,    Icons.Rounded.Album),
    BottomNavItem(Route.ARTISTS,   R.string.nav_artists,   Icons.Rounded.Person),
    BottomNavItem(Route.PLAYLISTS, R.string.nav_playlists, Icons.AutoMirrored.Rounded.QueueMusic),
    BottomNavItem(Route.FOLDERS,   R.string.nav_folders,   Icons.Rounded.Folder),
    BottomNavItem(Route.RECENT,    R.string.nav_recent,    Icons.Rounded.History),
)

private val bottomNavRoutes = bottomNavItems.map { it.route }.toSet()

// ── Activity ──────────────────────────────────────────────────────────────────
class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    // mutableStateOf so Compose reacts when onNewIntent fires with a new value
    private var openNowPlaying by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        openNowPlaying = intent?.getBooleanExtra(EXTRA_OPEN_NOW_PLAYING, false) == true
        setContent {
            val themeName by vm.themeName.collectAsState()
            MexMp3Theme(themeName = themeName) {
                AppRoot(
                    vm             = vm,
                    openNowPlaying = openNowPlaying,
                    onNowPlayingConsumed = { openNowPlaying = false }
                )
            }
        }
    }

    // Called when notification taps re-deliver the intent to an already-running activity
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openNowPlaying = intent.getBooleanExtra(EXTRA_OPEN_NOW_PLAYING, false)
    }
}

// ── Root composable ───────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppRoot(
    vm: MainViewModel,
    openNowPlaying: Boolean = false,
    onNowPlayingConsumed: () -> Unit = {}
) {
    val navController  = rememberNavController()
    val onboardingDone by vm.isOnboardingDone.collectAsState()
    val currentSong    by vm.currentSong.collectAsState()
    val isPlaying      by vm.isPlaying.collectAsState()
    val playlists      by vm.playlists.collectAsState()

    var selectedSong      by remember { mutableStateOf<Song?>(null) }
    var showSongOptions   by remember { mutableStateOf(false) }
    var showMetaEditor    by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Android 10+ system delete permission launcher
    val context = androidx.compose.ui.platform.LocalContext.current
    val deleteRequestLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            // User approved system delete — rescan library and show success
            vm.scanLibrary()
            scope.launch { snackbarHostState.showSnackbar("Song deleted successfully") }
        }
    }

    // Detail navigation targets (held in AppRoot so they survive recomposition)
    var selectedAlbum    by remember { mutableStateOf<Album?>(null) }
    var selectedArtist   by remember { mutableStateOf<Artist?>(null) }
    var selectedFolder   by remember { mutableStateOf<Folder?>(null) }
    var selectedPlaylist by remember { mutableStateOf<PlaylistEntity?>(null) }

    // ── Permissions ───────────────────────────────────────────────────────────
    val audioPerm = rememberAudioPermissionState {
        vm.scanLibrary()
    }
    val notifPerm = rememberNotificationPermissionState {}

    // Auto-request once onboarding completes; also scan if already granted
    LaunchedEffect(onboardingDone) {
        if (onboardingDone) {
            if (audioPerm.granted) {
                vm.scanLibrary()   // scan immediately if permission already held
            } else {
                audioPerm.request()
            }
            if (!notifPerm.granted) notifPerm.request()
            // Request battery optimisation exemption so the OS doesn't kill
            // the music service when the screen turns off (critical for Vivo,
            // Xiaomi, Oppo and other aggressive OEM battery management ROMs)
            requestBatteryOptimizationExemption(context)
        }
    }

    // Navigate to Now Playing whenever notification is tapped.
    // openNowPlaying is a Compose State on the Activity so this recomposes
    // every time the user taps the notification even when app is already open.
    LaunchedEffect(openNowPlaying) {
        if (openNowPlaying) {
            onNowPlayingConsumed()          // reset the flag immediately
            navController.navigate(Route.NOW_PLAYING) {
                launchSingleTop = true      // don't stack duplicates
            }
        }
    }

    val startDest = if (onboardingDone) Route.SONGS else Route.ONBOARDING

    val navBackEntry by navController.currentBackStackEntryAsState()
    val currentRoute  = navBackEntry?.destination?.route

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            // FIX #3: navigationBarsPadding ensures system nav doesn't cover mini-player
            Column(modifier = Modifier.navigationBarsPadding()) {
                AnimatedVisibility(
                    visible = currentSong != null && currentRoute != Route.NOW_PLAYING,
                    enter   = slideInVertically { it } + fadeIn(tween(200)),
                    exit    = slideOutVertically { it } + fadeOut(tween(200))
                ) {
                    currentSong?.let { song ->
                        MiniPlayer(
                            song        = song,
                            isPlaying   = isPlaying,
                            onPlayPause = { vm.togglePlayPause() },
                            onNext      = { vm.skipNext() },
                            onExpand    = { navController.navigate(Route.NOW_PLAYING) }
                        )
                    }
                }
                AnimatedVisibility(visible = currentRoute in bottomNavRoutes) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp
                    ) {
                        bottomNavItems.forEach { item ->
                            NavigationBarItem(
                                selected = currentRoute == item.route,
                                onClick  = {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState    = true
                                    }
                                },
                                icon  = { Icon(item.icon, stringResource(item.labelRes)) },
                                label = { Text(stringResource(item.labelRes)) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor    = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            AppNavHost(
                vm            = vm,
                navController = navController,
                startDest     = startDest,
                onSongLongPress = { song ->
                    selectedSong    = song
                    showSongOptions = true
                },
                onAlbumClick = { album ->
                    selectedAlbum = album
                    navController.navigate(Route.ALBUM_DETAIL)
                },
                onArtistClick = { artist ->
                    selectedArtist = artist
                    navController.navigate(Route.ARTIST_DETAIL)
                },
                onFolderClick = { folder ->
                    selectedFolder = folder
                    navController.navigate(Route.FOLDER_DETAIL)
                },
                onPlaylistClick = { playlist ->
                    selectedPlaylist = playlist
                    navController.navigate(Route.PLAYLIST_DETAIL)
                },
                selectedAlbum    = selectedAlbum,
                selectedArtist   = selectedArtist,
                selectedFolder   = selectedFolder,
                selectedPlaylist = selectedPlaylist
            )

            if (audioPerm.showRationale) {
                PermissionRationaleDialog(
                    title     = stringResource(R.string.perm_audio_title),
                    message   = stringResource(R.string.perm_audio_message),
                    onGrant   = audioPerm.request,
                    onDismiss = audioPerm.dismissRationale
                )
            }
            if (notifPerm.showRationale) {
                PermissionRationaleDialog(
                    title     = stringResource(R.string.perm_notification_title),
                    message   = stringResource(R.string.perm_notification_message),
                    onGrant   = notifPerm.request,
                    onDismiss = notifPerm.dismissRationale
                )
            }
        }
    }

    if (showSongOptions && selectedSong != null) {
        SongOptionsSheet(
            song            = selectedSong!!,
            playlists       = playlists,
            onDismiss       = { showSongOptions = false },
            onPlayNext      = {
                vm.playNext(selectedSong!!)
                showSongOptions = false
            },
            onAddToQueue    = {
                vm.addToQueue(selectedSong!!)
                showSongOptions = false
            },
            onAddToPlaylist = { playlist ->
                vm.addSongToPlaylist(playlist.id, selectedSong!!)
                showSongOptions = false
            },
            onEditInfo = {
                showSongOptions = false
                showMetaEditor  = true
            },
            onDelete = {
                showSongOptions  = false
                showDeleteConfirm = true
            }
        )
    }

    if (showDeleteConfirm && selectedSong != null) {
        val songToDelete = selectedSong!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Song") },
            text  = { Text("Delete \"${songToDelete.title}\" from your device? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        // Android 11+ — use system delete request dialog
                        val uri = android.content.ContentUris.withAppendedId(
                            android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            songToDelete.id
                        )
                        val pendingIntent = android.provider.MediaStore.createDeleteRequest(
                            context.contentResolver,
                            listOf(uri)
                        )
                        deleteRequestLauncher.launch(
                            androidx.activity.result.IntentSenderRequest.Builder(
                                pendingIntent.intentSender
                            ).build()
                        )
                    } else {
                        // Android 10 and below — direct delete
                        vm.deleteSong(songToDelete) { success ->
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    if (success) "Song deleted successfully"
                                    else "Could not delete song"
                                )
                            }
                        }
                    }
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showMetaEditor && selectedSong != null) {
        MetadataEditorSheet(
            song      = selectedSong!!,
            onDismiss = { showMetaEditor = false },
            onSave    = { title, artist, album ->
                vm.updateSongMetadata(selectedSong!!, title, artist, album)
                showMetaEditor = false
            }
        )
    }
}

// ── NavHost ───────────────────────────────────────────────────────────────────
@Composable
private fun AppNavHost(
    vm: MainViewModel,
    navController: NavHostController,
    startDest: String,
    onSongLongPress: (Song) -> Unit,
    onAlbumClick:    (Album) -> Unit,
    onArtistClick:   (Artist) -> Unit,
    onFolderClick:   (Folder) -> Unit,
    onPlaylistClick: (PlaylistEntity) -> Unit,
    selectedAlbum:    Album?,
    selectedArtist:   Artist?,
    selectedFolder:   Folder?,
    selectedPlaylist: PlaylistEntity?
) {
    NavHost(
        navController    = navController,
        startDestination = startDest,
        enterTransition  = { fadeIn(tween(220)) },
        exitTransition   = { fadeOut(tween(220)) },
        popEnterTransition  = { fadeIn(tween(220)) },
        popExitTransition   = { fadeOut(tween(220)) }
    ) {
        composable(Route.ONBOARDING) {
            OnboardingScreen(
                onScanClicked = {
                    vm.completeOnboarding()
                    navController.navigate(Route.SONGS) { popUpTo(Route.ONBOARDING) { inclusive = true } }
                },
                onSkip = {
                    vm.completeOnboarding()
                    navController.navigate(Route.SONGS) { popUpTo(Route.ONBOARDING) { inclusive = true } }
                }
            )
        }
        composable(Route.SONGS) {
            LibraryScaffold(vm = vm, navController = navController) {
                SongsScreen(vm = vm, onSongLongPress = onSongLongPress)
            }
        }
        composable(Route.ALBUMS) {
            LibraryScaffold(vm = vm, navController = navController) {
                AlbumsScreen(vm = vm, onAlbumClick = onAlbumClick)
            }
        }
        composable(Route.ARTISTS) {
            LibraryScaffold(vm = vm, navController = navController) {
                ArtistsScreen(vm = vm, onArtistClick = onArtistClick)
            }
        }
        composable(Route.PLAYLISTS) {
            LibraryScaffold(vm = vm, navController = navController) {
                PlaylistsScreen(vm = vm, onPlaylistClick = onPlaylistClick)
            }
        }
        composable(Route.FOLDERS) {
            LibraryScaffold(vm = vm, navController = navController) {
                FoldersScreen(vm = vm, onFolderClick = onFolderClick)
            }
        }
        composable(Route.RECENT) {
            LibraryScaffold(vm = vm, navController = navController) {
                RecentScreen(vm = vm)
            }
        }
        composable(Route.SEARCH)      { SearchScreen(vm = vm) }
        composable(Route.NOW_PLAYING) {
            NowPlayingScreen(vm = vm, onDismiss = { navController.popBackStack() })
        }
        composable(Route.SETTINGS)    { SettingsScreen(vm = vm) }

        // ── Detail screens ────────────────────────────────────────────────
        composable(Route.ALBUM_DETAIL) {
            selectedAlbum?.let { album ->
                AlbumDetailScreen(
                    album           = album,
                    vm              = vm,
                    onBack          = { navController.popBackStack() },
                    onSongLongPress = onSongLongPress
                )
            }
        }
        composable(Route.ARTIST_DETAIL) {
            selectedArtist?.let { artist ->
                ArtistDetailScreen(
                    artist          = artist,
                    vm              = vm,
                    onBack          = { navController.popBackStack() },
                    onSongLongPress = onSongLongPress
                )
            }
        }
        composable(Route.FOLDER_DETAIL) {
            selectedFolder?.let { folder ->
                FolderDetailScreen(
                    folder          = folder,
                    vm              = vm,
                    onBack          = { navController.popBackStack() },
                    onSongLongPress = onSongLongPress
                )
            }
        }
        composable(Route.PLAYLIST_DETAIL) {
            selectedPlaylist?.let { playlist ->
                PlaylistDetailScreen(
                    playlist        = playlist,
                    vm              = vm,
                    onBack          = { navController.popBackStack() },
                    onSongLongPress = onSongLongPress
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryScaffold(
    vm: MainViewModel,
    navController: NavHostController,
    content: @Composable () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MexMp3", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary) },
                actions = {
                    IconButton(onClick = { navController.navigate(Route.SEARCH) }) {
                        Icon(Icons.Rounded.Search, contentDescription = null)
                    }
                    IconButton(onClick = { navController.navigate(Route.SETTINGS) }) {
                        Icon(Icons.Rounded.Settings, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) { content() }
    }
}
