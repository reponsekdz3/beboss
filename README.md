# BeBoss — Offline-First Multi-Branch POS, Inventory & Licensing System

**BeBoss** (`com.aistudio.beboss.shopmgmt`) is an offline-first Android Point of Sale (POS), inventory ledger, multi-branch operations, customer credit ledger, and monthly licensing application built with Kotlin, Jetpack Compose (Material 3), and Room SQLite (v9 WAL).

---

## 1. Architecture Overview

- **Offline-First Core Store Operations:** All POS checkouts, inventory edits, supplier purchases, stock transfers, customer profiles, credit debt repayments, branches, and staff accounts are stored locally in **Room SQLite (`beboss_database.db`, schema v9)** with Write-Ahead Logging (`journal_mode=WAL`) enabled so transactions commit without waiting for network connectivity.
- **Automatic Synchronous Internet Branch DataShare:** Whenever internet connectivity is detected (`NetworkConnectivityObserver`) or branch mutations occur while online, `BeBossViewModel` schedules a debounced two-way sync via `CloudSyncManager` and `SupabaseAdminClient`:
  - Pushes GZIP-compressed, HMAC-SHA256-signed `BBSHARE1:` branch snapshots (`ShopDataTransferManager`) to the Supabase `branch_sync_snapshots` table keyed by a deterministic `shop_sync_key`.
  - Pulls remote snapshots from sibling branch devices (`device_id != localDeviceId`) and merges records using **Last-Write-Wins (LWW)** conflict resolution (`BeBossRepository.mergeShopDataPackageLww`) based on `updatedAt` timestamps while deduplicating immutable sale items, repayments, purchases, and stock transfers by ID and preserving the longest active `subscriptionExpiresAt`.
- **Zero-Data Local WiFi/Hotspot Mesh & Token Transfer:** Devices on the same local WiFi or mobile hotspot can discover nearby BeBoss hubs via UDP/mDNS (`LocalNetworkSyncServer`) and perform bidirectional HTTP `BBSHARE1:` merges on port `8989`, or export/import `.beboss` files and signed `BBSHARE1:` tokens directly.
- **Supabase Usage Scope (Admin Licensing & Sync Relay Only):** Supabase holds `admin_profiles`, `shop_accounts`, `payment_requests`, `unlock_codes`, `admin_audit_logs`, and `branch_sync_snapshots`. Primary business workflows remain functional offline in Room SQLite.

---

## 2. Monthly USSD Payment, WhatsApp Verification & Unlock Code Flow

Subscription pricing scales dynamically with active branches, staff accounts, and billing duration (`OfflineSubscriptionManager`):

| Plan Tier | Active Branches | Included Staff | Base Monthly Rate |
| :--- | :--- | :--- | :--- |
| **Solo Store** | 1 Branch | 1 User (Owner) | **5,000 FRw / month** |
| **Dual Store** | 2 Branches | Up to 3 Staff | **10,000 FRw / month** |
| **Multi-Branch Pro** | 3+ Branches | Up to 5 Staff | **20,000 FRw / month** |

Staff add-ons scale beyond tier limits (`+2,000 FRw` for 2–3 workers, `+4,000 FRw` for 4–5 workers, `+1,000 FRw` per additional worker beyond 5), with multi-month discounts (`10%` on 3 months, `20%` on 12 months).

### End-to-End Renewal Lifecycle
1. **Dial USSD:** From **Settings** or **Subscription**, the shop owner taps MTN MoMo (`*182*8*1*...#`) or Airtel Money (`*500*...#`) to complete payment on their phone.
2. **Submit Claim + Account Details:** The user enters their payer phone and optional Mobile Money transaction reference, alongside their created **BeBoss `@username`** and **registered phone number**, then sends the WhatsApp verification message (`wa.me`) to the Admin Support number (`BuildConfig.BEBOSS_ADMIN_WHATSAPP`) and queues the claim into Supabase `payment_requests`.
3. **Admin Real Payment Audit & Code Generation:** The administrator signs into the **BeBoss Web Admin Platform** (`/admin-web/index.html`) or the **In-App Admin Console** (`Settings -> Admin Console`) using real **Supabase GoTrue Email/Password Authentication** (`admin_profiles` RLS). After checking their phone's Mobile Money receipt against the expected tier fee badge, the admin clicks **Verify & Issue `BB2` Code** (or **Reject** with a reason) and sends the generated `BB2-...` code back to the shop owner on WhatsApp.
4. **Account-Bound Activation in Settings:** The shop owner pastes the `BB2-...` code in **Settings -> Insert Admin Unlock Code**. `SubscriptionSecurityManager` validates the HMAC-SHA256 signature (`BuildConfig.BEBOSS_UNLOCK_SIGNING_SECRET`), checks account binding (`@username` + normalized 9-digit phone), checks local anti-replay vouchers (`redeemed_vouchers`) and cloud revocation status (`REVOKED` check on `unlock_codes`), extends `subscriptionExpiresAt` for the month, and marks the code `REDEEMED`.

---

## 3. Web Admin Platform & In-App Admin Console

- **Standalone Web Dashboard:** `/admin-web/index.html` (and bundled Android asset `/app/src/main/assets/admin_portal.html` for 1-tap export from Settings).
- **Supabase Auth + RLS Enforcement:** Protected by Supabase GoTrue Email/Password login and `public.is_beboss_admin()` PostgreSQL security-definer policies (`/admin-web/supabase_schema.sql`).
- **Capabilities:**
  - Live auto-polling KPI overview (pending queue, monthly revenue volume, verified shops, online branch snapshots, tier breakdown SVG chart).
  - Fee-audited verification queue (`Verify & Issue BB2`, `Reject` with reason, 1-click WhatsApp `wa.me` reply).
  - WebCrypto HMAC-SHA256 `BB2` unlock code generator, codes ledger with **Revoke** control, live `branch_sync_snapshots` monitor, and `admin_audit_logs` trail.

---

## 4. Database Schema (Room SQLite v9)

Defined in `AppDatabase.kt` (`version = 9`) and `DatabaseMigrations.kt` (`MIGRATION_1_2` through `MIGRATION_8_9`):
- `shop_profile` — Shop identity, currency, targets, thermal printer width, subscription expiry/status, optional custom backup server URL.
- `branches` — Multi-branch locations with HQ flag and `updatedAt` LWW timestamp.
- `users` — Role-based staff accounts (`OWNER`, `MANAGER`, `CASHIER`), salted PIN/password hashes (`SecurityUtils`), branch assignment, and granular permission flags.
- `products` — Branch-scoped SKUs, barcode, wholesale cost price, retail price, stock quantity, minimum threshold, `updatedAt`.
- `customers` & `customer_payments` — Customer directory, credit limit, debt balance (`updatedAt`), and repayment history.
- `sales` & `sale_items` — Checkout headers, payment channels (`CASH`, `MOMO`, `CARD`, `CREDIT_DEBT`), branch/cashier attribution, and itemized line items.
- `purchase_records` — Supplier restock inflows and payment status.
- `stock_transfers` — Inter-branch stock transfer audit records.
- `sync_queue` — Pending local sync tracking.
- `redeemed_vouchers` — Anti-replay ledger storing SHA-256 fingerprints of redeemed unlock codes.
- `payment_verification_requests` — Offline-first queue of USSD payment proof claims synced to Supabase.

---

## 5. Configuration & Environment Variables

Sensitive keys and backend configuration live in `.env` (via **Secrets panel in Google AI Studio**), documented in `.env.example`, and are injected via the Secrets Gradle Plugin into `BuildConfig`:

```properties
SUPABASE_URL=https://your-project-ref.supabase.co
SUPABASE_ANON_KEY=your-supabase-anon-key
SUPABASE_ADMIN_EMAIL=admin@beboss.app
BEBOSS_UNLOCK_SIGNING_SECRET=your-hmac-signing-secret
BEBOSS_ADMIN_WHATSAPP=+250788765432
GEMINI_API_KEY=your-optional-key
```

### Setting Up Supabase
1. Run `/admin-web/supabase_schema.sql` inside your Supabase SQL Editor.
2. In Supabase **Authentication -> Providers**, ensure **Email** provider is enabled.
3. Configure `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `SUPABASE_ADMIN_EMAIL`, `BEBOSS_UNLOCK_SIGNING_SECRET`, and `BEBOSS_ADMIN_WHATSAPP` in the **Secrets panel**.
4. Ensure `BEBOSS_UNLOCK_SIGNING_SECRET` matches the signing secret configured in the Admin Web Portal settings if changed from the default.

---

## 6. Key Implemented Modules & Screens

- **Authentication & Onboarding (`AuthScreen.kt`):** Edge-to-edge (`WindowInsets.safeDrawing`) 4-digit PIN pad + `BiometricPrompt` fingerprint unlock, staff username/password sign-in, branch-aware operator carousel, and a 3-step Shop Registration Wizard with live PIN/password strength feedback.
- **Dashboard (`DashboardScreen.kt`):** Active branch switcher pill, live Auto-Sync status indicator, monthly license countdown pill, daily revenue/profit summary, low-stock quick restock list, and recent transactions.
- **POS & Sales History (`SalesPosScreen.kt`, `SalesHistoryScreen.kt`):** CameraX barcode scanner (`CameraBarcodeScanner.kt`), multi-payment checkout, 58mm/80mm Bluetooth/USB ESC/POS receipt formatting, and WhatsApp receipt sharing.
- **Inventory & Purchases (`InventoryScreen.kt`, `PurchasesLedgerView.kt`):** Product catalog CRUD, stock adjustments, margin calculation, supplier purchase inflows, and Excel CSV export (`ExcelCsvExporter.kt`).
- **Customers & Analytics (`CustomersScreen.kt`, `AnalyticsScreen.kt`):** Debt ledger, repayment tracking, WhatsApp/SMS reminders, P&L period reports, charts (`AnalyticsChart.kt`), and Android PDF generation (`PdfReportGenerator.kt`).
- **Settings, Branches & Admin (`SettingsScreen.kt`, `AdminVerificationConsoleDialog.kt`, `BranchManagementDialog.kt`, `StaffManagementDialog.kt`, `ShopOwnerSyncHubDialog.kt`):** Branch setup & transfers, role permissions & WhatsApp/PDF staff passes, Room database health (`PRAGMA integrity_check`, WAL checkpoint, `VACUUM`), USSD/WhatsApp renewal, Settings unlock code activation, and Supabase-authenticated Admin Console.

---

## 7. Verification & Tests

Local JVM and Robolectric unit tests reside in `/app/src/test/java/com/example/`:
- `BranchDataShareTest.kt` — `BBSHARE1:` compressed token round-trip, HMAC tampering rejection, and LWW conflict resolution.
- `SubscriptionSecurityTest.kt` — `BB` / `BB2` unlock code HMAC verification, anti-replay blocking, username + phone normalization binding.
- `PricingCalculatorTest.kt` — Branch & worker subscription pricing tier and multi-month discount math.
- `SecurityUtilsTest.kt` — Salted PIN/password hashing and verification.
- `ExampleRobolectricTest.kt` & `ExampleUnitTest.kt` — Application & JVM sanity checks.

Run tests with:
```bash
gradle :app:testDebugUnitTest
```
