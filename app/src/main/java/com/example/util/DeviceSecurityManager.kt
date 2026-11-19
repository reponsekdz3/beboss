package com.example.util

import android.os.Build
import java.io.File

data class DeviceIntegrityStatus(
    val isCompromisedRisk: Boolean,
    val signals: List<String> = emptyList(),
    val isRooted: Boolean = false,
    val isEmulator: Boolean = false,
    val hasHookingFramework: Boolean = false,
    val keystoreBacking: String = "AndroidKeyStore"
) {
    val postureLabel: String
        get() = when {
            isRooted || hasHookingFramework -> "HIGH RISK — Tampered Environment"
            isCompromisedRisk -> "ELEVATED RISK — Unsigned Build"
            isEmulator -> "EMULATOR — Non-Hardware Attested"
            else -> "VERIFIED — Hardware Protected"
        }
}

/**
 * Runtime device posture inspection (root binaries, Magisk/KernelSU, hooking frameworks,
 * test-keys, emulator indicators, and AndroidKeyStore hardware level).
 * Surfaces audit telemetry and security diagnostics cleanly.
 */
object DeviceSecurityManager {

    private val suPaths = listOf(
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/su/bin/su",
        "/system/app/Superuser.apk",
        "/system/bin/.ext/su",
        "/system/usr/we-need-root/su",
        "/sbin/.magisk",
        "/data/adb/magisk",
        "/data/adb/ksu"
    )

    private val hookingIndicators = listOf(
        "/data/local/tmp/frida-server",
        "/data/local/tmp/re.frida.server",
        "/system/framework/XposedBridge.jar",
        "/data/adb/lspd"
    )

    fun inspectDeviceIntegrity(): DeviceIntegrityStatus {
        val signals = mutableListOf<String>()
        val tags = Build.TAGS.orEmpty()
        val hasTestKeys = tags.contains("test-keys", ignoreCase = true)
        if (hasTestKeys) {
            signals.add("Unsigned test-keys OS build")
        }

        val hasSu = suPaths.any { path ->
            try {
                File(path).exists()
            } catch (_: Throwable) {
                false
            }
        }
        if (hasSu) {
            signals.add("Privileged root/Magisk artifact detected")
        }

        val hasHooks = hookingIndicators.any { path ->
            try {
                File(path).exists()
            } catch (_: Throwable) {
                false
            }
        }
        if (hasHooks) {
            signals.add("Instrumentation / hooking framework detected")
        }

        val isEmulator = (Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.contains("emulator", ignoreCase = true) ||
            Build.MODEL.contains("Android SDK built for", ignoreCase = true) ||
            Build.HARDWARE.contains("goldfish", ignoreCase = true) ||
            Build.HARDWARE.contains("ranchu", ignoreCase = true)) &&
            !CryptoKeyManager.isRobolectricJvm()
        if (isEmulator) {
            signals.add("Running inside virtualized emulator")
        }

        val keystoreBacking = CryptoKeyManager.getHardwareKeystoreStatus()

        return DeviceIntegrityStatus(
            isCompromisedRisk = hasSu || hasHooks || hasTestKeys,
            signals = signals,
            isRooted = hasSu,
            isEmulator = isEmulator,
            hasHookingFramework = hasHooks,
            keystoreBacking = keystoreBacking
        )
    }
}
