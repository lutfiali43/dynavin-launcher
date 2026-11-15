package com.lutfi.glasslauncher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.widget.Toast
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmap

/** Servis kosong: hanya perlu wujud supaya launcher boleh baca & kawal muzik yang sedang main. */
class MediaListener : NotificationListenerService()

data class AppEntry(val label: String, val pkg: String, val cls: String)

/** Simpanan kecil dalam unit (alamat Rumah / Pejabat). Tiada apa-apa dihantar keluar. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("glass", Context.MODE_PRIVATE)
    fun get(key: String): String = sp.getString(key, "") ?: ""
    fun set(key: String, value: String) = sp.edit().putString(key, value).apply()
}

object Apps {
    fun launchables(context: Context): List<AppEntry> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        val list = pm.queryIntentActivities(intent, 0)
        return list
            .filter { it.activityInfo.packageName != context.packageName }
            .map { AppEntry(it.loadLabel(pm).toString(), it.activityInfo.packageName, it.activityInfo.name) }
            .sortedBy { it.label.lowercase() }
    }

    fun icon(context: Context, app: AppEntry): ImageBitmap? = try {
        context.packageManager
            .getActivityIcon(ComponentName(app.pkg, app.cls))
            .toBitmap(128, 128)
            .asImageBitmap()
    } catch (e: Exception) {
        null
    }

    fun launch(context: Context, app: AppEntry) {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setClassName(app.pkg, app.cls)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        if (!Actions.start(context, intent)) Actions.toast(context, "Tak dapat buka ${app.label}")
    }
}

object Actions {
    const val MAPS = "com.google.android.apps.maps"
    const val YOUTUBE = "com.google.android.youtube"
    const val YT_MUSIC = "com.google.android.apps.youtube.music"

    fun toast(context: Context, msg: String) =
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()

    fun start(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (e: Exception) {
        false
    }

    fun launchPackage(context: Context, pkg: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(pkg) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return start(context, intent)
    }

    fun openMaps(context: Context) {
        if (!launchPackage(context, MAPS)) toast(context, "Google Maps tak dijumpai dalam unit")
    }

    fun openYouTube(context: Context) {
        if (!launchPackage(context, YOUTUBE) && !launchPackage(context, YT_MUSIC)) {
            toast(context, "YouTube tak dijumpai dalam unit")
        }
    }

    fun navigate(context: Context, address: String) {
        val nav = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" + Uri.encode(address)))
            .setPackage(MAPS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (start(context, nav)) return
        val geo = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(address)))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (!start(context, geo)) toast(context, "Tiada app peta untuk navigasi")
    }

    /** Mod Pandu: buka Maps, kemudian YouTube di sebelah (skrin terbelah, jika unit benarkan). */
    fun driveMode(context: Context) {
        val pm = context.packageManager
        val maps = pm.getLaunchIntentForPackage(MAPS)
        val media = pm.getLaunchIntentForPackage(YOUTUBE) ?: pm.getLaunchIntentForPackage(YT_MUSIC)
        if (maps == null || media == null) {
            toast(context, "Perlu Google Maps dan YouTube untuk Mod Pandu")
            return
        }
        maps.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (!start(context, maps)) return
        Handler(Looper.getMainLooper()).postDelayed({
            media.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK
            )
            start(context, media)
        }, 800)
    }

    private fun findApp(context: Context, pattern: String): String? {
        val regex = Regex(pattern, RegexOption.IGNORE_CASE)
        return Apps.launchables(context)
            .firstOrNull { regex.containsMatchIn(it.pkg + " " + it.label) }
            ?.pkg
    }

    /** Cari app berdasarkan nama; kalau tak jumpa, beritahu dan buka senarai semua app. */
    fun openByKeyword(context: Context, pattern: String, what: String, fallback: () -> Unit) {
        val pkg = findApp(context, pattern)
        if (pkg == null || !launchPackage(context, pkg)) {
            toast(context, "App $what belum dikenal pasti. Pilih dari Semua app.")
            fallback()
        }
    }

    fun openCarPlay(context: Context, fallback: () -> Unit) =
        openByKeyword(context, "carplay|zlink|carlink|autolink|easyconn|tlink", "CarPlay", fallback)

    fun openCamera(context: Context, fallback: () -> Unit) =
        openByKeyword(context, "avm|360|panoram", "Kamera 360", fallback)

    fun openClimate(context: Context, fallback: () -> Unit) =
        openByKeyword(context, "aircon|climate|canbus|carinfo|carsetting", "A/C", fallback)

    /** Pintu keluar: pilih semula launcher asal Dynavin. */
    fun openHomeSettings(context: Context) {
        val home = Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (!start(context, home)) {
            start(context, Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    fun openMediaAccessSettings(context: Context) {
        val i = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (!start(context, i)) toast(context, "Unit ini sorok tetapan akses notifikasi")
    }
}

object Media {
    fun hasAccess(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    fun current(context: Context): MediaController? {
        if (!hasAccess(context)) return null
        return try {
            val msm = context.getSystemService(MediaSessionManager::class.java) ?: return null
            val list = msm.getActiveSessions(ComponentName(context, MediaListener::class.java))
            list.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: list.firstOrNull()
        } catch (e: SecurityException) {
            null
        }
    }
}
