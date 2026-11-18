package com.example

import android.Manifest
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.PermissionManager
import com.example.util.SupabaseAdminClient
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AdminAndPermissionsTest {

    @Test
    fun `first-launch login permissions omit broad storage or file access`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val required = PermissionManager.getRequiredPermissionsList()
        val statuses = PermissionManager.getAllPermissionsStatus(context)

        assertFalse(required.contains(Manifest.permission.READ_EXTERNAL_STORAGE))
        assertFalse(required.contains(Manifest.permission.READ_MEDIA_IMAGES))
        assertFalse(statuses.any { it.iconName == "storage" })
        assertFalse(
            statuses.any {
                it.permission == Manifest.permission.READ_EXTERNAL_STORAGE ||
                    it.permission == Manifest.permission.READ_MEDIA_IMAGES
            }
        )
        assertTrue(required.contains(Manifest.permission.READ_CONTACTS))
        assertTrue(required.contains(Manifest.permission.CAMERA))
    }

    @Test
    fun `master admin email is locked strictly to reponsekdz at gmail dot com`() = runBlocking {
        assertEquals("reponsekdz@gmail.com", SupabaseAdminClient.MASTER_ADMIN_EMAIL)
        assertEquals("reponsekdz@gmail.com", SupabaseAdminClient.getDefaultAdminEmail())
        assertTrue(SupabaseAdminClient.isAllowedMasterAdminEmail("reponsekdz@gmail.com"))
        assertTrue(SupabaseAdminClient.isAllowedMasterAdminEmail("  REPONSEKDZ@GMAIL.COM "))
        assertFalse(SupabaseAdminClient.isAllowedMasterAdminEmail("other@beboss.app"))
        assertFalse(SupabaseAdminClient.isAllowedMasterAdminEmail("admin@beboss.app"))

        val signInRes = SupabaseAdminClient.signInAdmin("unauthorized@example.com", "secret123")
        assertTrue(signInRes.isFailure)
        assertTrue(signInRes.exceptionOrNull()?.message?.contains("reponsekdz@gmail.com") == true)

        val signUpRes = SupabaseAdminClient.signUpAdmin("unauthorized@example.com", "secret123", "Fake Admin")
        assertTrue(signUpRes.isFailure)
        assertTrue(signUpRes.exceptionOrNull()?.message?.contains("reponsekdz@gmail.com") == true)
    }
}
