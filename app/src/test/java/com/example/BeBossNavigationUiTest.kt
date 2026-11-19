package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.example.data.model.Sale
import com.example.data.model.SaleItem
import com.example.data.model.SaleWithItems
import com.example.data.model.ShopProfile
import com.example.ui.components.BeBossBottomNav
import com.example.ui.components.ReceiptDialog
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppScreen
import com.example.util.AppLanguage
import com.example.util.ReceiptGenerator
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BeBossNavigationUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `bottom navigation tabs meet touch target size and switch active screen on tap`() {
        var activeScreen by mutableStateOf(AppScreen.DASHBOARD)

        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                BeBossBottomNav(
                    currentScreen = activeScreen,
                    cartItemCount = 2,
                    lowStockCount = 3,
                    debtorCount = 1,
                    language = AppLanguage.ENGLISH,
                    onNavigate = { target -> activeScreen = target }
                )
            }
        }

        val tabs = listOf(
            "bottom_nav_dashboard",
            "bottom_nav_sales_pos",
            "bottom_nav_inventory",
            "bottom_nav_analytics",
            "bottom_nav_customers",
            "bottom_nav_settings"
        )
        tabs.forEach { tag ->
            composeTestRule.onNodeWithTag(tag)
                .assertIsDisplayed()
                .assertHeightIsAtLeast(48.dp)
        }

        composeTestRule.onNodeWithTag("bottom_nav_sales_pos").performClick()
        composeTestRule.waitForIdle()
        assertEquals(AppScreen.SALES_POS, activeScreen)

        composeTestRule.onNodeWithTag("bottom_nav_settings").performClick()
        composeTestRule.waitForIdle()
        assertEquals(AppScreen.SETTINGS, activeScreen)
    }

    @Test
    fun `receipt dialog displays tax breakdown and offline verification seal`() {
        val profile = ShopProfile(
            id = 1L,
            shopName = "BeBoss Flagship",
            taxRate = 18.0,
            showTaxOnReceipt = true,
            currencySymbol = "FRw"
        )
        val sale = Sale(
            id = "s_ui_1",
            receiptNumber = "RC-UI-101",
            customerName = "Claire",
            totalAmount = 11_800.0,
            amountPaid = 11_800.0
        )
        val expectedSeal = ReceiptGenerator.computeOfflineReceiptSeal(sale, profile)

        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                ReceiptDialog(
                    saleWithItems = SaleWithItems(
                        sale = sale,
                        items = listOf(
                            SaleItem(
                                id = "si_ui_1",
                                saleId = sale.id,
                                productId = "p_ui_1",
                                productName = "Arabica Coffee",
                                quantitySold = 1.0,
                                unitPriceAtSale = 11_800.0,
                                subtotal = 11_800.0
                            )
                        )
                    ),
                    shopProfile = profile,
                    onDismiss = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Incl. Tax (18.0%):", substring = false).assertIsDisplayed()
        composeTestRule.onNodeWithText("Seal: $expectedSeal", substring = false).assertIsDisplayed()
    }
}
