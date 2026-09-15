# BharatPe Auto SMS 📱➡️📲

An automated, background-first native Android utility that detects incoming **BharatPe for Business** payment notifications and immediately forwards a customizable SMS receipt (e.g., `"20₹ Received From RINKAL RAVINDR CHAURPAGAR."`) to a secondary phone (such as a JioBharat keypad phone) with **zero manual intervention**.

---

## 🎯 The Problem Solved

Small merchants and shopkeepers frequently face this workflow challenge:
1. Customers scan the store QR code and pay via UPI through BharatPe.
2. The BharatPe merchant app delivers a payment notification to the store owner's primary smartphone (e.g., Redmi Note 10S).
3. The store counter or cashier uses a basic keypad phone (e.g., JioBharat) that cannot run Android or UPI apps.
4. Previously, the merchant had to manually unlock their phone, read the amount, type an SMS, and send it to the counter phone for each payment.

**BharatPe Auto SMS** automates this entire pipeline completely in the background. The moment a customer pays, an SMS confirmation lands on the keypad phone in seconds.

---

## 🚀 Key Features

* **⚡ Zero Manual Confirmation**: Fully automated background operation. Once enabled, incoming genuine payments automatically trigger the SMS without requiring app opens, taps, dialogs, or confirmation clicks.
* **🌐 Marathi Name Transliteration (मराठी नाव रूपांतरण)**: Built-in 100% offline hybrid transliteration engine (150+ curated Marathi dictionary + rule-based phonetic engine) that converts English customer names into Marathi Devanagari script (e.g., `RINKAL RAVINDR CHAURPAGAR` → `रिंकल रवींद्र चौरपगार`, `Miss DISHA SURESH RANDIVE` → `दिशा सुरेश रणदिवे`). Supports UCS-2 Unicode multi-part SMS for keypad phones (such as JioBharat).
* **🎯 Strict Regex Payment Parser**: Accurately extracts payment amounts and sender names (e.g., `Received 20.00 Rupees From RINKAL RAVINDR CHAURPAGAR.`). Employs negative filtering to safely reject promotional notices, cashback alerts, daily greetings, and KYC reminders.
* **🛡️ Triple-Layer Duplicate Protection**:
  * *Layer 1*: In-memory LRU cache for rapid successive system notification deliveries.
  * *Layer 2*: SHA-256 time-bucketed hashing combining package name, amount, sender name, and 5-minute time windows.
  * *Layer 3*: Persistent Room database query ensuring only un-sent transactions can trigger dispatch.
* **📶 Dual SIM Management**: Automatically discovers active SIM cards via Android's `SubscriptionManager`, allowing users to select the exact SIM used to send SMS messages or use the system default.
* **🎨 Modern Jetpack Compose UI**: Built with Material Design 3, dynamic theme colors, real-time reactive transaction feeds, live permission monitoring, and in-app parser test benches.
* **🔒 100% Private & Offline**: Requires **NO INTERNET PERMISSION** (`android.permission.INTERNET` is not even declared in `AndroidManifest.xml`). All parsing, duplicate checking, and database storage occur entirely on the device.

---

## 🏗️ Architecture & Technology Stack

```
com.autosms.bharatpe
├── data/
│   ├── db/                 Room Database, TransactionEntity, TransactionDao
│   ├── preferences/        SharedPreferences wrapper for settings
│   └── repository/         Repository layer with reactive Kotlin Flows
├── parser/
│   ├── NotificationParser      Strict payment regex matching & negative pattern filters
│   ├── AmountFormatter         Currency formatting & SMS template processor
│   └── MarathiTransliterator   100% offline Latin to Marathi Devanagari transliterator
├── duplicate/
│   └── DuplicateDetector   SHA-256 time-bucket hash generation & LRU caching
├── service/
│   ├── PaymentNotificationListener  NotificationListenerService background interceptor
│   ├── SmsSender           SmsManager wrapper with dual-SIM & delivery callbacks
│   └── BootReceiver        Xiaomi/Android boot completion handler
├── ui/
│   ├── screens/            MainScreen, HistoryScreen, SettingsScreen
│   ├── theme/              Material 3 Color, Type, Theme tokens
│   └── viewmodel/          MainViewModel, HistoryViewModel (StateFlows)
└── util/
    ├── Constants           Action strings, channel IDs, thresholds
    ├── PackageDetector     Android 11+ package visibility detection
    └── PermissionHelper    System intents for settings & permission prompts
```

* **Language**: Kotlin 1.9.24
* **UI Toolkit**: Jetpack Compose (BOM 2024.06.00) + Material 3
* **Local Database**: Room 2.6.1 + KSP
* **Asynchronous**: Coroutines & StateFlow / SharedFlow
* **Target SDK**: Android 14 (API 34) | **Min SDK**: Android 8.0 (API 26)

---

## ⚙️ Xiaomi / Redmi (MIUI & HyperOS) Setup Guide

Because Xiaomi devices aggressively terminate background services, follow these settings on devices like the **Redmi Note 10S**:

1. **Autostart**:
   * Open **Settings > Apps > Manage Apps > BharatPe Auto SMS**.
   * Turn **Autostart** to **ON**.
2. **Battery Saver**:
   * In *Manage Apps > BharatPe Auto SMS > Battery saver*, select **No restrictions**.
3. **Lock in Recent Apps**:
   * Open the Recent Apps tray, press and hold on the **BharatPe Auto SMS** card, and tap the **Padlock icon**.
4. **Permissions**:
   * Grant **SMS** and **Phone** permissions.
   * Tap **Notification Access** and enable the toggle for **BharatPe Auto SMS**.

---

## 🧪 Automated Testing

The project includes unit tests covering regex extraction, promotional notification rejection, and duplicate hashing logic:

```bash
# Run unit tests
./gradlew testDebugUnitTest
```

---

## 📦 Building the APK

```bash
# Assemble Debug APK
./gradlew assembleDebug

# Output APK path:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).
