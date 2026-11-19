package com.example.util

import android.os.Build
import java.io.File

data class DeviceIntegrityStatus(
    val isCompromisedRisk: Boolean,
    val signals: List<String> = emptyList()
)

/**
 * Runtime device posture inspection (root binaries, test-keys, debuggable/emulator indicators).
 * Never locks out legitimate offline merchants silently; surfaces audit & posture telemetry cleanly.
 */
object DeviceSecurityManager {

    private val suPaths = listOf(
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/su/bin/su",
        "/system/app/Superuser.apk",
        "/system/bin/.ext/su",
        "/system/usr/we-need-root/su"
    )

    fun inspectDeviceIntegrity(): DeviceIntegrityStatus {
        val signals = mutableListOf<String>()
        val tags = Build.TAGS.orEmpty()
        if (tags.contains("test-keys", ignoreCase = true)) {
            signals.add("Unsigned test-keys build")
        }
        val hasSu = suPaths.any { path ->
            try {
                File(path).exists()
            } catch (_: Throwable) {
                false
            }
        }
        if (hasSu) {
            signals.add("Privileged su binary detected")
        }
        val isEmulator = (Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.contains("emulator", ignoreCase = true) ||
            Build.MODEL.contains("Android SDK built for", ignoreCase = true)) &&
            !CryptoKeyManager.isRobolectricJvm()
        if (isEmulator) {
            signals.add("Running inside emulator environment")
        }
        return DeviceIntegrityStatus(
            isCompromisedRisk = hasSu || tags.contains("test-keys", ignoreCase = true),
            signals = signals
        )
    }
}
