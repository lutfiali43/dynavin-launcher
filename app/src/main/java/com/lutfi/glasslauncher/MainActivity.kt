package com.lutfi.glasslauncher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
    private val showDrawer = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LauncherRoot(showDrawer) }
    }

    // Pressing Home while on the launcher closes All apps and returns to the home screen.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        showDrawer.value = false
    }
}

@Composable
fun LauncherRoot(showDrawer: MutableState<Boolean>) {
    GlassTheme {
        Box(Modifier.fillMaxSize().horizonBackground()) {
            if (showDrawer.value) {
                BackHandler { showDrawer.value = false }
                AppDrawer(onClose = { showDrawer.value = false })
            } else {
                // Back does nothing on the home screen.
                BackHandler { }
                HomeScreen(onOpenDrawer = { showDrawer.value = true })
            }
        }
    }
}
