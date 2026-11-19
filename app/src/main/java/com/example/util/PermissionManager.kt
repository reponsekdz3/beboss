package com.example.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

data class AppPermissionItem(
    val permission: String,
    val title: String,
    val titleRw: String,
    val description: String,
    val descriptionRw: String,
    val isGranted: Boolean,
    val isCrucial: Boolean = true,
    val iconName: String = "shield"
)

object PermissionManager {

    fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun hasContactsPermission(context: Context): Boolean {
        return hasPermission(context, Manifest.permission.READ_CONTACTS)
    }

    fun hasSmsPermission(context: Context): Boolean {
        return hasPermission(context, Manifest.permission.SEND_SMS)
    }

    fun hasCameraPermission(context: Context): Boolean {
        return hasPermission(context, Manifest.permission.CAMERA)
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true
        }
    }

    fun hasNearbyWifiPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasPermission(context, Manifest.permission.NEARBY_WIFI_DEVICES)
        } else {
            true
        }
    }

    fun getAllPermissionsStatus(context: Context): List<AppPermissionItem> {
        val list = mutableListOf<AppPermissionItem>()

        // Contacts
        list.add(
            AppPermissionItem(
                permission = Manifest.permission.READ_CONTACTS,
                title = "Phone Contacts Access",
                titleRw = "Uburenganzira bwa Contacts",
                description = "Import customer names, phones and addresses directly from your device phonebook with 1 tap.",
                descriptionRw = "Kwinjiza abakiriya, amazina na telefone zabo ako kanya uvanye muri telefoni yawe.",
                isGranted = hasContactsPermission(context),
                isCrucial = true,
                iconName = "contacts"
            )
        )

        // SMS / Messages
        list.add(
            AppPermissionItem(
                permission = Manifest.permission.SEND_SMS,
                title = "Direct SMS Messaging",
                titleRw = "Kohereza Ubutumwa bwa SMS",
                description = "Send instant official SMS payment reminders, debt alerts, and sale receipts directly to customers.",
                descriptionRw = "Kohereza ubutumwa bwa SMS bw'amadeni, inyemezabuguzi na raporo z'ubucuruzi ako kanya.",
                isGranted = hasSmsPermission(context),
                isCrucial = true,
                iconName = "sms"
            )
        )

        // Camera
        list.add(
            AppPermissionItem(
                permission = Manifest.permission.CAMERA,
                title = "Camera Barcode Scanner",
                titleRw = "Kamera yo Gusoma Barcode",
                description = "Scan product barcodes and collaborator QR passes directly using your device camera.",
                descriptionRw = "Gusoma barcode z'ibicuruzwa na QR pass z'abakozi ukoresheje kamera ya telefoni.",
                isGranted = hasCameraPermission(context),
                isCrucial = false,
                iconName = "camera"
            )
        )

        // Notifications
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(
                AppPermissionItem(
                    permission = Manifest.permission.POST_NOTIFICATIONS,
                    title = "Business Alerts & Notifications",
                    titleRw = "Imenyesha ry'Ubucuruzi",
                    description = "Receive instant alerts when stock runs out, debt payment is due, or branch shift finishes.",
                    descriptionRw = "Kumenyeshwa ibicuruzwa bishize mu bubiko, amadeni arangiye n'amasaha y'akazi.",
                    isGranted = hasNotificationPermission(context),
                    isCrucial = false,
                    iconName = "notifications"
                )
            )
            list.add(
                AppPermissionItem(
                    permission = Manifest.permission.NEARBY_WIFI_DEVICES,
                    title = "Nearby Branch LAN Sync",
                    titleRw = "Guhuza Amashami kuri Wi-Fi",
                    description = "Discover local master POS hubs over Wi-Fi without requiring device location access.",
                    descriptionRw = "Kubona amashami ari kuri Wi-Fi imwe mudakenera GPS.",
                    isGranted = hasNearbyWifiPermission(context),
                    isCrucial = false,
                    iconName = "wifi"
                )
            )
        }

        return list
    }

    fun getRequiredPermissionsList(): List<String> {
        val list = mutableListOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.CAMERA
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return list
    }

    fun hasAllCrucialPermissions(context: Context): Boolean {
        val required = getRequiredPermissionsList()
        return required.all { hasPermission(context, it) }
    }

    fun getUngrantedPermissions(context: Context): List<String> {
        return getRequiredPermissionsList().filter { !hasPermission(context, it) }
    }
}
