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
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material.icons.filled.Navigation
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
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.VerticalSplit
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ---------- Warna & gaya kaca ----------

val Accent = Color(0xFFFF8A3D)
val OnAccent = Color(0xFF1A0D00)
val Bg = Color(0xFF0B1024)
val TextSoft = Color.White.copy(alpha = 0.82f)

/** Latar: biru gelap + cahaya oren, biru dan ungu. Dilukis sekali, tiada blur live (laju). */
fun Modifier.glowBackground(): Modifier = this.drawBehind {
    drawRect(Bg)
    fun glow(color: Color, cx: Float, cy: Float, r: Float) {
        val center = Offset(size.width * cx, size.height * cy)
        val radius = size.width * r
        drawCircle(
            brush = Brush.radialGradient(listOf(color, Color.Transparent), center = center, radius = radius),
            radius = radius,
            center = center
        )
    }
    glow(Color(0x8CFF8030), 0.18f, 0.22f, 0.42f)
    glow(Color(0x99286EFF), 0.82f, 0.78f, 0.45f)
    glow(Color(0x59963CFF), 0.70f, 0.10f, 0.40f)
}

/** Panel kaca: putih separa lutsinar + garis tepi halus. */
fun Modifier.glass(radius: Dp = 28.dp, alpha: Float = 0.12f): Modifier {
    val shape = RoundedCornerShape(radius)
    return this
        .clip(shape)
        .background(Color.White.copy(alpha = alpha))
        .border(1.dp, Color.White.copy(alpha = 0.28f), shape)
}

// ---------- Skrin utama ----------

@Composable
fun HomeScreen(onOpenDrawer: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            MapsCard(Modifier.weight(1.55f).fillMaxHeight())
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                ClockCard(Modifier.fillMaxWidth())
                MediaCard(Modifier.fillMaxWidth().weight(1f))
            }
        }
        Dock(onOpenDrawer)
    }
}

@Composable
fun Pill(icon: ImageVector, label: String) {
    Row(
        Modifier.glass(radius = 999.dp, alpha = 0.16f).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        Text(label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun MapsCard(modifier: Modifier) {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    var home by remember { mutableStateOf(prefs.get("home")) }
    var office by remember { mutableStateOf(prefs.get("office")) }
    var editing by remember { mutableStateOf<String?>(null) }

    Column(modifier.glass().padding(28.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Pill(Icons.Outlined.Place, "Google Maps")
        Text(
            "Ke mana hari ni?",
            color = Color.White,
            fontSize = 52.sp,
            lineHeight = 56.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            DestinationButton(
                Modifier.weight(1f), Icons.Outlined.Home, "Rumah",
                if (home.isBlank()) "Tekan untuk set alamat" else home,
                accent = false,
                onClick = { if (home.isBlank()) editing = "home" else Actions.navigate(context, home) },
                onLongClick = { editing = "home" }
            )
            DestinationButton(
                Modifier.weight(1f), Icons.Outlined.Work, "Pejabat",
                if (office.isBlank()) "Tekan untuk set alamat" else office,
                accent = false,
                onClick = { if (office.isBlank()) editing = "office" else Actions.navigate(context, office) },
                onLongClick = { editing = "office" }
            )
            DestinationButton(
                Modifier.weight(1f), Icons.Filled.Navigation, "Navigasi", "Buka Google Maps",
                accent = true,
                onClick = { Actions.openMaps(context) },
                onLongClick = { Actions.openMaps(context) }
            )
        }
    }

    editing?.let { key ->
        AddressDialog(
            title = if (key == "home") "Alamat Rumah" else "Alamat Pejabat",
            initial = prefs.get(key),
            onSave = { value ->
                prefs.set(key, value.trim())
                if (key == "home") home = value.trim() else office = value.trim()
                editing = null
            },
            onDismiss = { editing = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DestinationButton(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val fg = if (accent) OnAccent else Color.White
    Column(
        modifier
            .clip(shape)
            .background(if (accent) Accent else Color.White.copy(alpha = 0.18f))
            .border(1.dp, Color.White.copy(alpha = 0.3f), shape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(28.dp))
        Text(title, color = fg, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Text(
            subtitle,
            color = fg.copy(alpha = 0.8f),
            fontSize = 13.sp,
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
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Taip alamat atau nama tempat, sama macam cari dalam Google Maps.")
                OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true)
                Text("Tip: tekan lama butang ni untuk tukar alamat nanti.", fontSize = 13.sp)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(text) }) { Text("Simpan") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
fun ClockCard(modifier: Modifier) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val ms = Locale("ms", "MY")
    val date = Date(now)
    val hour = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 5..11 -> "Selamat pagi,"
        in 12..13 -> "Selamat tengah hari,"
        in 14..18 -> "Selamat petang,"
        else -> "Selamat malam,"
    }

    Row(
        modifier.glass().padding(horizontal = 26.dp, vertical = 22.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                SimpleDateFormat("HH:mm", ms).format(date),
                color = Color.White,
                fontSize = 72.sp,
                lineHeight = 72.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(SimpleDateFormat("EEEE, d MMMM", ms).format(date), color = TextSoft, fontSize = 18.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(greeting, color = TextSoft, fontSize = 16.sp)
            Text("Lutfi", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ---------- Kad "Sedang main" ----------

private data class NowPlaying(
    val controller: MediaController?,
    val title: String,
    val artist: String,
    val playing: Boolean,
    val art: Bitmap?
)

@Composable
fun MediaCard(modifier: Modifier) {
    val context = LocalContext.current
    var access by remember { mutableStateOf(Media.hasAccess(context)) }
    var np by remember { mutableStateOf<NowPlaying?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            access = Media.hasAccess(context)
            val c = Media.current(context)
            np = if (c == null) null else {
                val md = c.metadata
                NowPlaying(
                    controller = c,
                    title = md?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "",
                    artist = md?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                        ?: md?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE) ?: "",
                    playing = c.playbackState?.state == PlaybackState.STATE_PLAYING,
                    art = md?.getBitmap(MediaMetadata.METADATA_KEY_ART)
                        ?: md?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                )
            }
            delay(1000)
        }
    }

    Column(modifier.glass().padding(horizontal = 26.dp, vertical = 22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Outlined.SmartDisplay, contentDescription = null, tint = Color(0xFFFF4D4D), modifier = Modifier.size(22.dp))
            Text("Sedang main", color = TextSoft, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }

        val current = np
        when {
            !access -> {
                Text(
                    "Benarkan akses supaya kad ni boleh tunjuk dan kawal YouTube.",
                    color = Color.White, fontSize = 17.sp
                )
                Spacer(Modifier.weight(1f))
                GlassButton("Benarkan akses", accent = true) { Actions.openMediaAccessSettings(context) }
            }
            current == null -> {
                Text("Tiada media sedang main", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                GlassButton("Buka YouTube", accent = true) { Actions.openYouTube(context) }
            }
            else -> {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    val art: ImageBitmap? = remember(current.art) { current.art?.asImageBitmap() }
                    val artMod = Modifier.size(84.dp).clip(RoundedCornerShape(18.dp))
                    if (art != null) {
                        Image(art, contentDescription = null, contentScale = ContentScale.Crop, modifier = artMod)
                    } else {
                        Box(artMod.background(Brush.linearGradient(listOf(Accent, Color(0xFF6A3DFF)))))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            current.title.ifBlank { "Tanpa tajuk" },
                            color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.SemiBold,
                            maxLines = 2, overflow = TextOverflow.Ellipsis
                        )
                        Text(current.artist, color = TextSoft, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.weight(1f))
                val tc = current.controller?.transportControls
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RoundButton(Icons.Filled.SkipPrevious, "Sebelum", big = false) { tc?.skipToPrevious() }
                    RoundButton(
                        if (current.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        if (current.playing) "Jeda" else "Main",
                        big = true
                    ) { if (current.playing) tc?.pause() else tc?.play() }
                    RoundButton(Icons.Filled.SkipNext, "Seterusnya", big = false) { tc?.skipToNext() }
                }
            }
        }
    }
}

@Composable
fun RoundButton(icon: ImageVector, label: String, big: Boolean, onClick: () -> Unit) {
    val s = if (big) 84.dp else 64.dp
    Box(
        Modifier
            .size(s)
            .clip(CircleShape)
            .background(if (big) Color.White else Color.White.copy(alpha = 0.14f))
            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = if (big) Bg else Color.White, modifier = Modifier.size(if (big) 36.dp else 28.dp))
    }
}

@Composable
fun GlassButton(label: String, accent: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .background(if (accent) Accent else Color.White.copy(alpha = 0.16f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (accent) OnAccent else Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ---------- Dok ----------

@Composable
fun Dock(onOpenDrawer: () -> Unit) {
    val context = LocalContext.current
    Row(
        Modifier.fillMaxWidth().glass(radius = 26.dp).padding(horizontal = 18.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DockItem(Icons.Outlined.Place, "Maps") { Actions.openMaps(context) }
        DockItem(Icons.Outlined.SmartDisplay, "YouTube") { Actions.openYouTube(context) }
        DockItem(Icons.Outlined.VerticalSplit, "Mod Pandu", accent = true) { Actions.driveMode(context) }
        DockItem(Icons.Outlined.PhoneIphone, "CarPlay") { Actions.openCarPlay(context, onOpenDrawer) }
        DockItem(Icons.Outlined.CameraAlt, "Kamera 360") { Actions.openCamera(context, onOpenDrawer) }
        DockItem(Icons.Outlined.AcUnit, "A/C") { Actions.openClimate(context, onOpenDrawer) }
        DockItem(Icons.Outlined.Apps, "Semua app", onClick = onOpenDrawer)
    }
}

@Composable
fun RowScope.DockItem(icon: ImageVector, label: String, accent: Boolean = false, onClick: () -> Unit) {
    val fg = if (accent) OnAccent else Color.White
    Column(
        Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (accent) Accent else Color.Transparent)
            .clickable(onClick = onClick)
            .width(if (accent) 124.dp else 100.dp)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(30.dp))
        Text(label, color = fg, fontSize = 14.sp, fontWeight = if (accent) FontWeight.SemiBold else FontWeight.Medium)
    }
}

// ---------- Semua app ----------

private class AppWithIcon(val app: AppEntry, val icon: ImageBitmap?)

@Composable
fun AppDrawer(onClose: () -> Unit) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<AppWithIcon>>(emptyList()) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) {
            Apps.launchables(context).map { AppWithIcon(it, Apps.icon(context, it)) }
        }
    }
    val shown = if (query.isBlank()) apps else apps.filter { it.app.label.contains(query.trim(), ignoreCase = true) }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                Modifier.size(56.dp).glass(radius = 16.dp, alpha = 0.14f).clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Kembali", tint = Color.White)
            }
            Text("Semua app", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Row(
                Modifier.width(360.dp).height(56.dp).glass(radius = 16.dp, alpha = 0.14f).padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = Color.White)
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text("Cari app", color = TextSoft, fontSize = 18.sp)
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
                        cursorBrush = SolidColor(Accent),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Row(
                Modifier.height(56.dp).glass(radius = 16.dp, alpha = 0.14f)
                    .clickable { Actions.openHomeSettings(context) }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.SwapHoriz, contentDescription = null, tint = Color.White)
                Text("Tukar launcher", color = Color.White, fontSize = 15.sp)
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(132.dp),
            modifier = Modifier.weight(1f).fillMaxWidth().glass().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(shown, key = { it.app.pkg + "/" + it.app.cls }) { item ->
                Column(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { Apps.launch(context, item.app); onClose() }
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val icon = item.icon
                    if (icon != null) {
                        Image(icon, contentDescription = null, modifier = Modifier.size(72.dp))
                    } else {
                        Box(
                            Modifier.size(72.dp).glass(radius = 20.dp, alpha = 0.2f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(item.app.label.take(1), color = Color.White, fontSize = 28.sp)
                        }
                    }
                    Text(
                        item.app.label,
                        color = Color.White,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
