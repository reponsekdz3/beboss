# BeBoss Business POS & ERP — Enterprise Edition 🚀

**BeBoss** is a full-featured, offline-first Android Point of Sale (POS), Inventory, Multi-Branch, and Financial Management application built specifically for retail shops, wholesalers, supermarkets, and multi-location businesses.

---

## 🌟 Core Pillars & Key Problems Solved

### 1. 🛡️ Real-Time Stock Exhaustion & Overselling Prevention
- **The Problem:** Previously, items with 0 or low stock could still be added and checked out, resulting in negative stock, inaccurate physical inventory counts, and loss of cash control.
- **The Solution:** 
  - Real-time stock validation during checkout. If any item is out of stock or requested in excess of `quantityInStock`, the checkout is blocked with an exact itemized warning.
  - POS items with 0 stock display an "Out of Stock" badge and open an immediate **Quick Restock Modal** directly from POS, Search, or Inventory.
  - Zero and low-stock alerts are highlighted dynamically in red with direct restock shortcuts.

### 2. 📊 Excel-Style Stock & Inflow Purchases Spreadsheets
- **The Problem:** Shop managers need structured, tabular data arranged like Excel spreadsheets for auditing buying prices, selling prices, unit margins, stock valuations, and supplier purchase orders.
- **The Solution:**
  - **Stock Spreadsheet:** Structured table displaying Index, Name, Category, Stock Level, Cost Price, Selling Price, Margin %, and Total Stock Value.
  - **Purchases & Inflow Ledger:** Complete historical tracking of all stock purchases from suppliers with purchase date, invoice number, supplier details, payment status (Paid, Credit, Partial), and auto-calculated total investments.
  - **Excel / CSV Export & Import:** One-tap export and import of all inventory and sales data.

### 3. 🔄 Multi-Branch & Worker Offline Synchronization Hub
- **The Problem:** Multi-branch shops frequently operate with spotty or zero internet connectivity. Shop owners need to aggregate data across remote branches without losing offline speed.
- **The Solution:**
  - **Offline JSON Exchange Packages:** Export full encrypted data packets from remote branch devices (via WhatsApp, Bluetooth, or SD Card) and import directly into the Main Shop Owner device.
  - **Automatic Conflict-Free Room Merge:** Aggregates products, sales, users, and customer credit balances seamlessly.
  - **Cloud Sync Push:** Instant one-tap synchronization with Firebase Firestore when internet is available.

### 4. 📇 Modern Universal Search & Quick Action Speed Dial
- **The Problem:** Finding products, customers, transactions, or purchase orders during busy retail hours was slow.
- **The Solution:**
  - Global Search across 4 domains simultaneously: **Products**, **Customers**, **Sales History**, and **Purchases**.
  - One-tap actions directly from search results: Add to Cart, Quick Restock, View Receipt, Call Customer, or View Debt Balance.

### 5. 🔐 Device Permissions & Advanced Hardware Integration
- **The Problem:** Advanced capabilities (customer contact importing, SMS debt reminder sending, local backup storage, barcode scanning) require runtime permissions with graceful degradation.
- **The Solution:**
  - **Runtime Permission Hub:** Interactive permission manager dialog for `READ_CONTACTS`, `SEND_SMS`, `CAMERA`, and `STORAGE`.
  - Live permission health badges in **Settings** showing real-time access status.

### 6. 💰 Real-Time Financial Calculations & Profit Margins
- Accurate automated calculation of:
  - **Gross Revenue**, **Cost of Goods Sold (COGS)**, and **Net Profit**.
  - **Customer Debt Balances** & partial payment tracking.
  - **Total Inventory Valuation** at cost price and expected sales value.
  - **Dynamic Multi-Branch & Staff Subscription Pricing Engine**.

---

## 📱 App Modules & Screen Guide

### 1. Dashboard (`DashboardScreen.kt`)
- Daily KPIs: Today's Revenue, Net Profit, Sales Count, Low Stock Warnings.
- Quick shortcut buttons to open POS, Excel Inventory, Debtors, and Reports.
- Real-time recent transaction feed with interactive receipt viewing and WhatsApp receipt sharing.

### 2. POS & Cashier Desk (`SalesPosScreen.kt`)
- Fast category filtering and real-time search.
- Interactive cart with quantity multipliers, custom item discounts, and multi-currency formatting.
- Payment method selector: **Cash**, **Mobile Money (MoMo/M-Pesa/Airtel)**, **Bank Card / POS**, or **Credit / Unpaid Debt**.
- Customer selector with direct customer balance lookup.

### 3. Inventory & Inflow Ledger (`InventoryScreen.kt` & `PurchasesLedgerView.kt`)
- **Stock Spreadsheet Tab:** Tabular view of all products with inline + / - stock adjustment, sorting by name, stock, cost, sell price, or profit margin.
- **Purchases & Inflow Tab:** Comprehensive supplier order log with invoice numbers, supplier phone, unit cost, new selling price updates, and total expense metrics.

### 4. Customers & Debt Management (`CustomersScreen.kt`)
- Customer directory with live debt balances and contact details.
- Record partial or full debt payments with automatic balance updates.
- One-tap SMS & WhatsApp debt payment reminder generation.

### 5. Analytics & Visual Reports (`AnalyticsScreen.kt`)
- Interactive period selector: **Today**, **This Week**, **This Month**, **This Year**, or **All Time**.
- Revenue and profit trend charts.
- Top 5 best-selling products by quantity and revenue.
- Category revenue distribution breakdown.

### 6. Settings & Security Hub (`SettingsScreen.kt`)
- Shop profile configuration (Shop Name, Owner, Phone, Currency Code, Symbol, Receipt Footer).
- Staff & Cashier access management with PIN and password protection.
- Multi-Branch setup and assignment.
- Branch & Worker Data Synchronization Hub.
- Device Permissions & Access Management.
- Biometric Fingerprint lock/unlock toggle.
- Dual-Language toggle: **English** and **Ikinyarwanda (Kinyarwanda)**.

---

## 🛠️ Architecture & Technical Stack

- **UI Framework:** 100% Jetpack Compose with Material 3 design system.
- **Architecture Pattern:** MVVM (Model-View-ViewModel) with Kotlin Coroutines and StateFlow.
- **Local Persistence:** Room Database with TypeConverters for instant offline query performance.
- **Biometric Security:** AndroidX Biometric Prompt API.
- **Printing & Reports:** Native Android PDF Document Canvas & Canvas Thermal Receipt printing engines.
- **Export & Import:** RFC 4180-compliant CSV and JSON backup parsers.

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Iguana / Ladybug or later
- JDK 17+
- Android SDK 34 (compileSdk 34, minSdk 26)

### Build & Run
```bash
# Verify compilation
gradle :app:assembleDebug

# Run unit tests
gradle :app:testDebugUnitTest
```

---

## 📄 License & Security
Encrypted with local on-device hardware encryption. Designed for reliable, high-speed retail operations.
