package com.example.util

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat

/**
 * Hardware Strong Biometric authentication manager backed by an AndroidKeyStore
 * CryptoObject AES-256-GCM cipher. Strictly disallows weak biometrics, device
 * credential fallbacks, or non-cryptographic unlock shortcuts.
 */
object BiometricAuthManager {

    fun isBiometricAvailable(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    fun promptBiometric(
        activity: Activity,
        title: String = "Biometric Verification",
        subtitle: String = "Verify strong biometric sensor to unlock BeBoss",
        negativeButtonText: String = "Use PIN Instead",
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onCancelToPin: () -> Unit = {},
        onFailed: () -> Unit
    ) {
        if (!isBiometricAvailable(activity)) {
            onError("Strong biometric authentication is not enrolled or available on this device.")
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val cancellationSignal = CancellationSignal()
            try {
                val cipher = CryptoKeyManager.createBiometricStrongCipher()
                val cryptoObject = BiometricPrompt.CryptoObject(cipher)

                val prompt = BiometricPrompt.Builder(activity)
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setNegativeButton(negativeButtonText, executor) { _: DialogInterface?, _: Int ->
                        onCancelToPin()
                    }
                    .build()

                prompt.authenticate(
                    cryptoObject,
                    cancellationSignal,
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                            super.onAuthenticationSucceeded(result)
                            if (result?.cryptoObject?.cipher != null) {
                                onSuccess()
                            } else {
                                onError("Hardware cryptographic verification did not return an unlocked cipher.")
                            }
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                            super.onAuthenticationError(errorCode, errString)
                            if (errorCode == BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED ||
                                errorCode == BiometricPrompt.BIOMETRIC_ERROR_CANCELED
                            ) {
                                onCancelToPin()
                            } else {
                                onError(errString?.toString() ?: "Biometric authentication error")
                            }
                        }

                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                            onFailed()
                        }
                    }
                )
            } catch (e: Exception) {
                onError("Biometric hardware key initialization failed: ${e.localizedMessage ?: "Unavailable"}")
            }
        } else {
            onError("Strong biometric authentication requires Android 9+.")
        }
    }
}
