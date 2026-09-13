package com.autosms.bharatpe.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.autosms.bharatpe.data.preferences.AppPreferences

/**
 * Detects installed BharatPe application variants on the device.
 */
object PackageDetector {

    data class DetectedApp(
        val packageName: String,
        val appLabel: String
    )

    /**
     * Scan for installed apps whose package name starts with "com.bharatpe".
     * Returns a list of detected apps with their labels.
     */
    fun detectBharatPeApps(context: Context): List<DetectedApp> {
        val pm = context.packageManager
        val detectedApps = mutableListOf<DetectedApp>()

        // Check known package names first
        for (pkg in Constants.KNOWN_BHARATPE_PACKAGES) {
            try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                val label = pm.getApplicationLabel(appInfo).toString()
                detectedApps.add(DetectedApp(pkg, label))
            } catch (_: PackageManager.NameNotFoundException) {
                // Not installed, skip
            }
        }

        // Also scan all installed packages for any com.bharatpe.* variants we might not know about
        try {
            val installedPackages = pm.getInstalledApplications(0)
            for (appInfo in installedPackages) {
                if (appInfo.packageName.startsWith(Constants.BHARATPE_PACKAGE_PREFIX)
                    && detectedApps.none { it.packageName == appInfo.packageName }
                ) {
                    val label = pm.getApplicationLabel(appInfo).toString()
                    detectedApps.add(DetectedApp(appInfo.packageName, label))
                }
            }
        } catch (_: Exception) {
            // Some devices may restrict package listing
        }

        return detectedApps
    }

    /**
     * Auto-detect and save the BharatPe package name.
     * Prefers "business" or "merchant" variants over the consumer app.
     * Returns the detected package name, or empty string if none found.
     */
    fun autoDetectAndSave(context: Context, preferences: AppPreferences): String {
        val apps = detectBharatPeApps(context)
        if (apps.isEmpty()) return ""

        // Prefer business/merchant variants
        val preferred = apps.firstOrNull {
            it.packageName.contains("business", ignoreCase = true) ||
            it.packageName.contains("merchant", ignoreCase = true)
        } ?: apps.first()

        preferences.bharatPePackage = preferred.packageName
        return preferred.packageName
    }

    /**
     * Check if the configured BharatPe package is actually installed.
     */
    fun isConfiguredPackageInstalled(context: Context, packageName: String): Boolean {
        if (packageName.isBlank()) return false
        return try {
            context.packageManager.getApplicationInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Open the BharatPe app (for testing).
     */
    fun openBharatPeApp(context: Context, packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        return if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } else {
            false
        }
    }
}
