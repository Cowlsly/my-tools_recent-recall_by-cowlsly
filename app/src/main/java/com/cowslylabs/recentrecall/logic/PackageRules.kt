package com.cowslylabs.recentrecall.logic

/**
 * Package lists used to hide the button on sensitive screens and to ignore helper windows.
 *
 * These are plain constants on purpose: edit them here and rebuild. They are documented in
 * docs/SENSITIVE_SCREENS.md. Matching works on the package name only, because the service never reads
 * window content (canRetrieveWindowContent is false). That means:
 * - it cannot tell a password page inside Settings from any other Settings page;
 * - it cannot see FLAG_SECURE, so a secure window from an unlisted app is not detected;
 * - lock screen and screen off are handled separately by the service (KeyguardManager and screen state).
 * Users can still add any app to the blocklist, and a toggle turns this whole list off.
 */
object PackageRules {
    /** Windows from these packages do not change which app is considered in front. */
    val IGNORED_EXACT: Set<String> = setOf(
        "android",
        "com.android.systemui",
        "com.samsung.android.honeyboard",
        "com.sec.android.inputmethod",
        "com.google.android.inputmethod.latin",
    )

    /** Any package containing one of these (lowercase) is a keyboard or similar helper window. */
    val IGNORED_CONTAINS: List<String> = listOf("inputmethod", "honeyboard")

    /** Exact package names where the button hides. */
    val SENSITIVE_EXACT: Set<String> = setOf(
        // Permission, install and biometric prompts
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.samsung.android.packageinstaller",
        "com.samsung.android.biometrics.app.setting",
        "com.samsung.android.authfw",
        "com.android.credentialmanager",
        // Samsung and Google payment and pass apps
        "com.samsung.android.samsungpass",
        "com.samsung.android.spay",
        "com.samsung.android.spayfw",
        "com.google.android.apps.walletnfc",
        // Common password managers and authenticators
        "com.google.android.apps.authenticator2",
        "com.azure.authenticator",
        "com.x8bit.bitwarden",
        "com.bitwarden.authenticator",
        "com.lastpass.lpandroid",
        "com.agilebits.onepassword",
        "com.onepassword.android",
        "com.dashlane",
        "proton.android.pass",
        "keepass2android.keepass2android",
        "org.keepassdroid",
    )

    /** Any package containing one of these (lowercase) is treated as sensitive. Broad on purpose. */
    val SENSITIVE_CONTAINS: List<String> = listOf(
        "permissioncontroller",
        "packageinstaller",
        "biometric",
        "credential",
        "bank",
        "paypal",
        "wallet",
        "password",
        "authenticator",
        "keepass",
        "bitwarden",
        "lastpass",
        "onepassword",
        "dashlane",
        "commbank",
        "westpac",
        "anz.",
        "au.com.nab",
    )

    fun isIgnored(pkg: String): Boolean {
        val p = pkg.lowercase()
        return p in IGNORED_EXACT || IGNORED_CONTAINS.any { p.contains(it) }
    }

    /** True when the button should hide over [pkg]. [enabled] is the user's toggle; false means never hide here. */
    fun isSensitive(pkg: String?, enabled: Boolean = true): Boolean {
        if (!enabled || pkg == null) return false
        val p = pkg.lowercase()
        return p in SENSITIVE_EXACT || SENSITIVE_CONTAINS.any { p.contains(it) }
    }
}

/** Remembers the last real app in front. Package names only. */
class ForegroundTracker {
    var current: String? = null
        private set

    /** Returns true when the app in front changed. Empty names and helper windows are ignored. */
    fun onWindowState(pkg: String?): Boolean {
        if (pkg.isNullOrEmpty() || PackageRules.isIgnored(pkg)) return false
        if (pkg == current) return false
        current = pkg
        return true
    }

    /** Forget the app in front, for example when the screen turns off and the next window is unknown. */
    fun reset() {
        current = null
    }
}
