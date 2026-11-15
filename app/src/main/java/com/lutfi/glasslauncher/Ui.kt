package com.lutfi.glasslauncher

import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PhoneIphone
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material.icons.outlined.VerticalSplit
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ---------- Tokens ----------
// Graphite cabin at dusk, one warm horizon glow, champagne trim as the only accent.

val Night = Color(0xFF121316)
val Ink = Color(0xFFF4F2EE)
val InkSoft = Ink.copy(alpha = 0.66f)
val Champagne = Color(0xFFE3CFA8)
val OnChampagne = Color(0xFF1A1712)
val Horizon = Color(0xFFC9894A)

// Barlow: a grotesk drawn from highway signage, made to be read at a glance.
val Barlow = FontFamily(
    Font(R.font.barlow_regular, FontWeight.Normal),
    Font(R.font.barlow_medium, FontWeight.Medium),
    Font(R.font.barlow_semibold, FontWeight.SemiBold)
)
val BarlowCondensed = FontFamily(
    Font(R.font.barlow_semicondensed_medium, FontWeight.Medium),
    Font(R.font.barlow_semicondensed_semibold, FontWeight.SemiBold)
)

@Composable
fun GlassTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalTextStyle provides TextStyle(fontFamily = Barlow, color = Ink, fontSize = 18.sp),
        content = content
    )
}

/** Graphite background with a single warm glow along the bottom edge. Drawn once, no live blur. */
fun Modifier.horizonBackground(): Modifier = this.drawBehind {
    drawRect(Night)
    val warmCenter = Offset(size.width * 0.5f, size.height * 1.12f)
    val warmRadius = size.width * 0.62f
    drawCircle(
        brush = Brush.radialGradient(listOf(Horizon.copy(alpha = 0.5f), Color.Transparent), warmCenter, warmRadius),
        radius = warmRadius,
        center = warmCenter
    )
    val coolCenter = Offset(size.width * 0.94f, 0f)
    val coolRadius = size.width * 0.32f
    drawCircle(
        brush = Brush.radialGradient(listOf(Color(0x38788CAA), Color.Transparent), coolCenter, coolRadius),
        radius = coolRadius,
        center = coolCenter
    )
}

/** Glass panel: faint white fill, hairline edge that is brighter on top (light from above). */
fun Modifier.glass(radius: Dp = 24.dp, fill: Float = 0.07f): Modifier {
    val shape = RoundedCornerShape(radius)
    return this
        .clip(shape)
        .background(Color.White.copy(alpha = fill))
        .border(
            1.dp,
            Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.26f), Color.White.copy(alpha = 0.10f))),
            shape
        )
}

// ---------- Home ----------

@Composable
fun HomeScreen(onOpenDrawer: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                ClockHeader()
                NavigationPanel(Modifier.fillMaxWidth().weight(1f))
            }
            NowPlayingPanel(Modifier.width(420.dp).fillMaxHeight())
        }
        Dock(onOpenDrawer)
    }
}

@Composable
fun ClockHeader() {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val date = Date(now)
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Text(
            SimpleDateFormat("HH:mm", Locale.UK).format(date),
            fontFamily = BarlowCondensed,
            fontWeight = FontWeight.SemiBold,
            fontSize = 128.sp,
            lineHeight = 116.sp,
            letterSpacing = (-2).sp,
            style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
        )
        Text(
            SimpleDateFormat("EEEE, d MMMM", Locale.UK).format(date),
            color = InkSoft,
            fontSize = 22.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}

@Composable
fun NavigationPanel(modifier: Modifier) {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    var home by remember { mutableStateOf(prefs.get("home")) }
    var office by remember { mutableStateOf(prefs.get("office")) }
    var editing by remember { mutableStateOf<String?>(null) }

    Column(modifier.glass().padding(32.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Navigate", fontFamily = BarlowCondensed, fontWeight = FontWeight.SemiBold, fontSize = 36.sp)
            Text("Opens Google Maps with directions", color = InkSoft, fontSize = 18.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Destination(
                Modifier.weight(1f), Icons.Outlined.Home, "Home",
                if (home.isBlank()) "Tap to set address" else home,
                primary = false,
                onClick = { if (home.isBlank()) editing = "home" else Actions.navigate(context, home) },
                onLongClick = { editing = "home" }
            )
            Destination(
                Modifier.weight(1f), Icons.Outlined.Work, "Office",
                if (office.isBlank()) "Tap to set address" else office,
                primary = false,
                onClick = { if (office.isBlank()) editing = "office" else Actions.navigate(context, office) },
                onLongClick = { editing = "office" }
            )
            Destination(
                Modifier.weight(1f), Icons.Outlined.Search, "Search Maps", "Any destination",
                primary = true,
                onClick = { Actions.openMaps(context) },
                onLongClick = { Actions.openMaps(context) }
            )
        }
    }

    editing?.let { key ->
        AddressDialog(
            title = if (key == "home") "Home address" else "Office address",
            initial = prefs.get(key),
            onSave = { value ->
                val v = value.trim()
                prefs.set(key, v)
                if (key == "home") home = v else office = v
                editing = null
            },
            onDismiss = { editing = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Destination(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    primary: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val fg = if (primary) OnChampagne else Ink
    val base = modifier.clip(shape)
    val styled = if (primary) {
        base.background(Champagne)
    } else {
        base.background(Color.White.copy(alpha = 0.08f)).border(1.dp, Color.White.copy(alpha = 0.16f), shape)
    }
    Column(
        styled
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(28.dp))
        Text(title, color = fg, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Text(
            subtitle,
            color = fg.copy(alpha = 0.72f),
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun AddressDialog(title: String, initial: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontFamily = Barlow) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Type an address or place name, the same way you'd search in Google Maps.", fontFamily = Barlow)
                OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true)
                Text("Press and hold the button later to change it.", fontFamily = Barlow, fontSize = 14.sp)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(text) }) { Text("Save address", fontFamily = Barlow) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", fontFamily = Barlow) } }
    )
}

// ---------- Now playing ----------

private data class NowPlaying(
    val controller: MediaController?,
    val title: String,
    val artist: String,
    val playing: Boolean,
    val art: Bitmap?,
    val app: String
)

@Composable
fun NowPlayingPanel(modifier: Modifier) {
    val context = LocalContext.current
    var access by remember { mutableStateOf(Media.hasAccess(context)) }
    var np by remember { mutableStateOf<NowPlaying?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            access = Media.hasAccess(context)
            val c = Media.current(context)
            np = if (c == null) null else {
                val md = c.metadata
                val appLabel = try {
                    val pm = context.packageManager
                    pm.getApplicationLabel(pm.getApplicationInfo(c.packageName, 0)).toString()
                } catch (e: Exception) {
                    ""
                }
                NowPlaying(
                    controller = c,
                    title = md?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "",
                    artist = md?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                        ?: md?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE) ?: "",
                    playing = c.playbackState?.state == PlaybackState.STATE_PLAYING,
                    art = md?.getBitmap(MediaMetadata.METADATA_KEY_ART)
                        ?: md?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART),
                    app = appLabel
                )
            }
            delay(1000)
        }
    }

    val current = np
    Column(modifier.glass().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.SmartDisplay, contentDescription = null, tint = InkSoft, modifier = Modifier.size(20.dp))
            Text(current?.app?.ifBlank { null } ?: "YouTube", color = InkSoft, fontSize = 16.sp)
        }

        val artShape = RoundedCornerShape(16.dp)
        val artModifier = Modifier.fillMaxWidth().weight(1f).clip(artShape)
        val art: ImageBitmap? = remember(current?.art) { current?.art?.asImageBitmap() }
        if (art != null) {
            Image(art, contentDescription = null, contentScale = ContentScale.Crop, modifier = artModifier)
        } else {
            Box(artModifier.background(Brush.linearGradient(listOf(Color(0xFF3A3330), Color(0xFF1C1D21)))))
        }

        when {
            !access -> {
                Text("Show what's playing", fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                Text("Allow notification access once so this panel can show and control YouTube.", color = InkSoft, fontSize = 16.sp)
                PanelButton("Allow access") { Actions.openMediaAccessSettings(context) }
            }
            current == null -> {
                Text("Nothing playing", fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                Text("Start a podcast or playlist and it shows up here.", color = InkSoft, fontSize = 16.sp)
                PanelButton("Open YouTube") { Actions.openYouTube(context) }
            }
            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        current.title.ifBlank { "Untitled" },
                        fontSize = 24.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp,
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                    Text(current.artist, color = InkSoft, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                val tc = current.controller?.transportControls
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RoundButton(Icons.Filled.SkipPrevious, "Previous", big = false) { tc?.skipToPrevious() }
                    RoundButton(
                        if (current.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        if (current.playing) "Pause" else "Play",
                        big = true
                    ) { if (current.playing) tc?.pause() else tc?.play() }
                    RoundButton(Icons.Filled.SkipNext, "Next", big = false) { tc?.skipToNext() }
                }
            }
        }
    }
}

@Composable
fun RoundButton(icon: ImageVector, label: String, big: Boolean, onClick: () -> Unit) {
    val s = if (big) 88.dp else 64.dp
    val m = Modifier.size(s).clip(CircleShape)
    val styled = if (big) m.background(Ink) else m.border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
    Box(styled.clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = label, tint = if (big) Night else Ink, modifier = Modifier.size(if (big) 36.dp else 28.dp))
    }
}

@Composable
fun PanelButton(label: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .border(1.dp, Color.White.copy(alpha = 0.24f), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ---------- Dock ----------

@Composable
fun Dock(onOpenDrawer: () -> Unit) {
    val context = LocalContext.current
    Row(
        Modifier.fillMaxWidth().glass(radius = 20.dp, fill = 0.06f).padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DockItem(Icons.Outlined.Place, "Maps") { Actions.openMaps(context) }
        DockItem(Icons.Outlined.SmartDisplay, "YouTube") { Actions.openYouTube(context) }
        DockItem(Icons.Outlined.VerticalSplit, "Split screen") { Actions.driveMode(context) }
        DockItem(Icons.Outlined.PhoneIphone, "CarPlay") { Actions.openCarPlay(context, onOpenDrawer) }
        DockItem(Icons.Outlined.CameraAlt, "Cameras") { Actions.openCamera(context, onOpenDrawer) }
        DockItem(Icons.Outlined.AcUnit, "Climate") { Actions.openClimate(context, onOpenDrawer) }
        DockItem(Icons.Outlined.Apps, "All apps", onClick = onOpenDrawer)
    }
}

@Composable
fun DockItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .width(128.dp)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Ink, modifier = Modifier.size(28.dp))
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

// ---------- All apps ----------

private class AppWithIcon(val app: AppEntry, val icon: ImageBitmap?)

@Composable
fun AppDrawer(onClose: () -> Unit) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<AppWithIcon>?>(null) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) {
            Apps.launchables(context).map { AppWithIcon(it, Apps.icon(context, it)) }
        }
    }
    val all = apps
    val shown = when {
        all == null -> emptyList()
        query.isBlank() -> all
        else -> all.filter { it.app.label.contains(query.trim(), ignoreCase = true) }
    }

    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                Modifier.height(56.dp).glass(radius = 16.dp).clickable(onClick = onClose).padding(start = 14.dp, end = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = Ink)
                Text("Home", fontSize = 18.sp, fontWeight = FontWeight.Medium)
            }
            Text("All apps", fontFamily = BarlowCondensed, fontWeight = FontWeight.SemiBold, fontSize = 36.sp)
            Spacer(Modifier.weight(1f))
            Row(
                Modifier.width(360.dp).height(56.dp).glass(radius = 16.dp).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = Ink)
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text("Search apps", color = InkSoft, fontSize = 18.sp)
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(fontFamily = Barlow, color = Ink, fontSize = 18.sp),
                        cursorBrush = SolidColor(Champagne),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Box(
                Modifier.height(56.dp).clip(RoundedCornerShape(16.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    .clickable { Actions.openHomeSettings(context) }
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Switch launcher", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth().glass(fill = 0.05f).padding(24.dp)) {
            when {
                all == null -> Text("Loading apps…", color = InkSoft, modifier = Modifier.align(Alignment.Center))
                shown.isEmpty() -> Text("No apps match “$query”", color = InkSoft, modifier = Modifier.align(Alignment.Center))
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(128.dp),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(shown, key = { it.app.pkg + "/" + it.app.cls }) { item ->
                        Column(
                            Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { Apps.launch(context, item.app); onClose() }
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val icon = item.icon
                            if (icon != null) {
                                Image(icon, contentDescription = null, modifier = Modifier.size(72.dp))
                            } else {
                                Box(Modifier.size(72.dp).glass(radius = 18.dp, fill = 0.12f), contentAlignment = Alignment.Center) {
                                    Text(item.app.label.take(1), fontFamily = BarlowCondensed, fontSize = 28.sp)
                                }
                            }
                            Text(item.app.label, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}
