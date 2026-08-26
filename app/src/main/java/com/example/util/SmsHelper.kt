package com.example.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import android.widget.Toast
import androidx.core.content.ContextCompat

object SmsHelper {

    fun hasSmsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Sends an SMS directly if permission is granted, otherwise opens the default SMS App.
     */
    fun sendSmsOrOpenIntent(context: Context, phoneNumber: String, message: String): Boolean {
        val cleanNumber = phoneNumber.trim().replace(" ", "").replace("-", "")
        if (cleanNumber.isBlank()) {
            Toast.makeText(context, "Phone number is empty", Toast.LENGTH_SHORT).show()
            return false
        }

        if (hasSmsPermission(context)) {
            return try {
                val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }

                val parts = smsManager.divideMessage(message)
                if (parts.size > 1) {
                    smsManager.sendMultipartTextMessage(cleanNumber, null, parts, null, null)
                } else {
                    smsManager.sendTextMessage(cleanNumber, null, message, null, null)
                }
                Toast.makeText(context, "SMS sent successfully to $cleanNumber", Toast.LENGTH_SHORT).show()
                true
            } catch (e: Exception) {
                // Fallback to intent if direct send fails
                openSmsApp(context, cleanNumber, message)
                true
            }
        } else {
            // Open standard SMS composer
            openSmsApp(context, cleanNumber, message)
            return true
        }
    }

    fun openSmsApp(context: Context, phoneNumber: String, message: String) {
        try {
            val cleanNumber = phoneNumber.trim().replace(" ", "").replace("-", "")
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$cleanNumber")
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not launch SMS app: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun openWhatsApp(context: Context, phoneNumber: String, message: String) {
        try {
            val cleanNumber = phoneNumber.trim()
                .replace(" ", "")
                .replace("-", "")
                .replace("+", "")

            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanNumber&text=${Uri.encode(message)}")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "WhatsApp not installed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
