package com.example.util

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract

data class PickedContactInfo(
    val name: String,
    val phone: String,
    val email: String = ""
)

object ContactsHelper {

    fun extractContactDetails(context: Context, contactUri: Uri): PickedContactInfo? {
        var name = ""
        var phone = ""
        var email = ""

        val contentResolver = context.contentResolver

        try {
            // Query for Contact ID and Name
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
            // If direct permission lookup fails, extract from standard lookup URI
            name = contactUri.lastPathSegment ?: ""
        }

        return if (name.isNotBlank() || phone.isNotBlank()) {
            PickedContactInfo(name = name.ifBlank { "Contact" }, phone = phone, email = email)
        } else {
            null
        }
    }
}
