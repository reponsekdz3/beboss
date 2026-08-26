package com.example.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

data class PickedContactInfo(
    val name: String,
    val phone: String,
    val email: String = "",
    val address: String = ""
)

object ContactsHelper {

    fun hasContactsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Reads all contacts stored in the phone address book to allow bulk customer import.
     */
    fun fetchAllPhoneContacts(context: Context): List<PickedContactInfo> {
        val contactsList = mutableListOf<PickedContactInfo>()
        if (!hasContactsPermission(context)) return contactsList

        val contentResolver = context.contentResolver
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        try {
            contentResolver.query(uri, projection, null, null, "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC")?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                val seenPhones = mutableSetOf<String>()

                while (cursor.moveToNext()) {
                    val name = if (nameIndex != -1) cursor.getString(nameIndex) ?: "" else ""
                    val number = if (numberIndex != -1) cursor.getString(numberIndex) ?: "" else ""
                    val cleanPhone = number.replace(" ", "").replace("-", "")

                    if (name.isNotBlank() && cleanPhone.isNotBlank() && !seenPhones.contains(cleanPhone)) {
                        seenPhones.add(cleanPhone)
                        contactsList.add(PickedContactInfo(name = name.trim(), phone = number.trim()))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return contactsList
    }

    /**
     * Extracts details from single picked contact URI.
     */
    fun extractContactDetails(context: Context, contactUri: Uri): PickedContactInfo? {
        var name = ""
        var phone = ""
        var email = ""

        val contentResolver = context.contentResolver

        try {
            contentResolver.query(contactUri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        name = cursor.getString(nameIndex) ?: ""
                    }
                    val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                    val contactId = if (idIndex != -1) cursor.getString(idIndex) else null

                    val hasPhoneIndex = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
                    val hasPhone = if (hasPhoneIndex != -1) cursor.getInt(hasPhoneIndex) else 0

                    if (contactId != null && hasPhone > 0) {
                        contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                            arrayOf(contactId),
                            null
                        )?.use { phoneCursor ->
                            if (phoneCursor.moveToFirst()) {
                                val numIdx = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                if (numIdx != -1) {
                                    phone = phoneCursor.getString(numIdx) ?: ""
                                }
                            }
                        }
                    }

                    if (contactId != null) {
                        contentResolver.query(
                            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                            null,
                            "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
                            arrayOf(contactId),
                            null
                        )?.use { emailCursor ->
                            if (emailCursor.moveToFirst()) {
                                val emailIdx = emailCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
                                if (emailIdx != -1) {
                                    email = emailCursor.getString(emailIdx) ?: ""
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            name = contactUri.lastPathSegment ?: ""
        }

        return if (name.isNotBlank() || phone.isNotBlank()) {
            PickedContactInfo(name = name.ifBlank { "Contact" }, phone = phone, email = email)
        } else {
            null
        }
    }
}
