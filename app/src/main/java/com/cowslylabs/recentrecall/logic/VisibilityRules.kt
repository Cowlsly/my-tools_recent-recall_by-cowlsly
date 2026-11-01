package com.cowslylabs.recentrecall.logic

enum class RuleState { NONE, ALLOW, BLOCK }

/**
 * Precedence (documented in README):
 * 1. A blocked app never shows the button. Blocklist wins over allowlist.
 * 2. If the allowlist is empty, the button shows over every app that is not blocked.
 * 3. If the allowlist has entries, the button shows only over allowed apps.
 * When the foreground app is not known yet (null), only rule 2 can show the button.
 */
object VisibilityRules {
    fun shouldShow(pkg: String?, allow: Set<String>, block: Set<String>): Boolean {
        if (pkg != null && pkg in block) return false
        if (allow.isEmpty()) return true
        return pkg != null && pkg in allow
    }

    fun ruleFor(pkg: String, allow: Set<String>, block: Set<String>): RuleState = when {
        pkg in block -> RuleState.BLOCK
        pkg in allow -> RuleState.ALLOW
        else -> RuleState.NONE
    }

    fun nextState(s: RuleState): RuleState = when (s) {
        RuleState.NONE -> RuleState.ALLOW
        RuleState.ALLOW -> RuleState.BLOCK
        RuleState.BLOCK -> RuleState.NONE
    }

    /** Puts [pkg] in exactly one list (or none) and returns the new (allow, block). */
    fun applyRule(
        pkg: String,
        state: RuleState,
        allow: Set<String>,
        block: Set<String>,
    ): Pair<Set<String>, Set<String>> {
        val a = allow - pkg
        val b = block - pkg
        return when (state) {
            RuleState.NONE -> a to b
            RuleState.ALLOW -> (a + pkg) to b
            RuleState.BLOCK -> a to (b + pkg)
        }
    }
}

/** Decides which apps the rules picker lists. Saved rules are never touched by this. */
object PickerFilter {
    fun include(isSystem: Boolean, isHidden: Boolean, showSystem: Boolean, showHidden: Boolean): Boolean =
        (!isSystem || showSystem) && (!isHidden || showHidden)
}

/** Heuristic package lists. Not exhaustive; users can also use the blocklist. */
object PackageRules {
    private val ignored = setOf(
        "android",
        "com.android.systemui",
        "com.samsung.android.honeyboard",
        "com.sec.android.inputmethod",
        "com.google.android.inputmethod.latin",
    )
    private val sensitive = setOf(
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.samsung.android.biometrics.app.setting",
    )

    /** Windows from these packages do not change which app is considered in front. */
    fun isIgnored(pkg: String): Boolean = pkg in ignored

    /** The button hides over these (permission, install and biometric prompts). */
    fun isSensitive(pkg: String?): Boolean = pkg != null && pkg in sensitive
}
