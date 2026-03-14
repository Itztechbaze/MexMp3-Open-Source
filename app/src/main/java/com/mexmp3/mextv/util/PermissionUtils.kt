package com.mexmp3.mextv.util

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
fun rememberAudioPermissionState(onGranted: () -> Unit): PermissionState {
    val context = LocalContext.current
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        Manifest.permission.READ_MEDIA_AUDIO
    else
        Manifest.permission.READ_EXTERNAL_STORAGE

    var showRationale by remember { mutableStateOf(false) }
    var permGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permGranted = granted
        if (granted) onGranted()
        else showRationale = true
    }

    return PermissionState(
        granted = permGranted,
        showRationale = showRationale,
        request = { launcher.launch(permission) },
        dismissRationale = { showRationale = false }
    )
}

@Composable
fun rememberNotificationPermissionState(onGranted: () -> Unit): PermissionState {
    val context = LocalContext.current
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return PermissionState(granted = true, showRationale = false, request = {}, dismissRationale = {})
    }
    val permission = Manifest.permission.POST_NOTIFICATIONS
    var showRationale by remember { mutableStateOf(false) }
    var permGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permGranted = granted
        if (granted) onGranted()
        else showRationale = true
    }
    return PermissionState(
        granted = permGranted,
        showRationale = showRationale,
        request = { launcher.launch(permission) },
        dismissRationale = { showRationale = false }
    )
}

data class PermissionState(
    val granted: Boolean,
    val showRationale: Boolean,
    val request: () -> Unit,
    val dismissRationale: () -> Unit
)

/**
 * Requests that the OS exclude this app from battery optimisation (Doze mode).
 * Without this, Android can kill the music service after a few minutes of
 * screen-off time, especially on aggressive OEM ROMs (Vivo, Xiaomi, Oppo, etc.).
 * This is the #1 battery-kill prevention measure for background music apps.
 */
fun requestBatteryOptimizationExemption(context: android.content.Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val pm = context.getSystemService(android.content.Context.POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
            runCatching {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            }
        }
    }
}

@Composable
fun PermissionRationaleDialog(
    title: String,
    message: String,
    onGrant: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = { onGrant(); onDismiss() }) {
                Text("Grant Permission")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not Now")
            }
        }
    )
}
