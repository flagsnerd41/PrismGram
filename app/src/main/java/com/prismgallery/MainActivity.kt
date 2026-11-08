package com.prismgallery

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage

private val Ink = Color(0xFF17191D)
private val Muted = Color(0xFF858991)
private val Canvas = Color(0xFFF6F6F3)
private val Accent = Color(0xFF5969D9)

data class GalleryPhoto(val id: Long, val uri: android.net.Uri, val date: Long)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PrismGalleryApp() }
    }
}

@Composable
fun PrismGalleryApp() {
    val context = LocalContext.current
    var photos by remember { mutableStateOf(emptyList<GalleryPhoto>()) }
    var hasPermission by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf("Photos") }
    var selectedPhoto by remember { mutableStateOf<GalleryPhoto?>(null) }

    fun loadPhotos() {
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_ADDED
        )
        val result = mutableListOf<GalleryPhoto>()
        context.contentResolver.query(
            collection, projection, null, null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                result += GalleryPhoto(
                    id,
                    ContentUris.withAppendedId(collection, id),
                    cursor.getLong(dateColumn)
                )
            }
        }
        photos = result
    }

    val permission = if (Build.VERSION.SDK_INT >= 34) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) loadPhotos()
    }

    LaunchedEffect(Unit) {
        hasPermission = ContextCompat.checkSelfPermission(
            context, permission
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) loadPhotos()
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            background = Canvas,
            surface = Color.White,
            primary = Accent,
            onBackground = Ink,
            onSurface = Ink
        )
    ) {
        if (selectedPhoto != null) {
            PhotoViewer(photo = selectedPhoto!!, onBack = { selectedPhoto = null })
        } else {
            Box(Modifier.fillMaxSize().background(Canvas)) {
                Column(Modifier.fillMaxSize()) {
                    Spacer(Modifier.height(18.dp))
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 22.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("PrismGallery", style = MaterialTheme.typography.labelLarge, color = Muted)
                            Text(
                                if (selectedTab == "Photos") "Your moments" else selectedTab,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = { }) {
                            Icon(Icons.Rounded.Search, contentDescription = "Search", tint = Ink)
                        }
                        IconButton(onClick = { }) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = "More", tint = Ink)
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    if (!hasPermission) {
                        Column(
                            Modifier.fillMaxSize().padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Rounded.PhotoLibrary, null, tint = Accent, modifier = Modifier.size(54.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("Your photos, all in one place", style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(8.dp))
                            Text("PrismGallery reads photos stored on this device. Your library stays yours.", color = Muted)
                            Spacer(Modifier.height(20.dp))
                            Button(onClick = { permissionLauncher.launch(permission) }) {
                                Text("Allow photo access")
                            }
                        }
                    } else if (selectedTab == "Photos") {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 22.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${photos.size} items", color = Muted, style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.weight(1f))
                            Text("Newest first", color = Muted, style = MaterialTheme.typography.labelLarge)
                        }
                        Spacer(Modifier.height(12.dp))
                        if (photos.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No photos found yet", color = Muted)
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 112.dp),
                                contentPadding = PaddingValues(start = 3.dp, end = 3.dp, bottom = 112.dp),
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                items(photos, key = { it.id }) { photo ->
                                    AsyncImage(
                                        model = photo.uri,
                                        contentDescription = "Photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(0.88f)
                                            .clip(RoundedCornerShape(5.dp))
                                            .clickable { selectedPhoto = photo }
                                    )
                                }
                            }
                        }
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("$selectedTab is coming next", color = Muted)
                        }
                    }
                }

                NavigationPill(
                    selected = selectedTab,
                    onSelect = { selectedTab = it },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 22.dp)
                )
            }
        }
    }
}

@Composable
private fun NavigationPill(
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        "Photos" to Icons.Rounded.PhotoLibrary,
        "Albums" to Icons.Rounded.Folder,
        "Search" to Icons.Rounded.Search,
        "Settings" to Icons.Rounded.Settings
    )
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = Color(0xFFFDFDFC),
        shadowElevation = 16.dp,
        tonalElevation = 4.dp
    ) {
        Row(
            Modifier.padding(horizontal = 9.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { (label, icon) ->
                val active = selected == label
                val tint by animateColorAsState(if (active) Accent else Muted, label)
                Row(
                    Modifier
                        .clip(CircleShape)
                        .background(if (active) Color(0xFFE9EBFF) else Color.Transparent)
                        .clickable { onSelect(label) }
                        .padding(horizontal = if (active) 15.dp else 12.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
                    if (active) {
                        Text(label, color = Accent, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoViewer(photo: GalleryPhoto, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AsyncImage(
            model = photo.uri,
            contentDescription = "Selected photo",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
        Surface(
            modifier = Modifier.align(Alignment.TopStart).padding(18.dp),
            shape = CircleShape,
            color = Color(0xDD202124)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
        }
    }
}
