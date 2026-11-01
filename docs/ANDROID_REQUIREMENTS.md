# Android requirements for Recent-Recall

Checked 2026-11-02. Status per row: VERIFIED means read on developer.android.com in this run. UNVERIFIED means stated from platform knowledge and not re-read in this run; check before relying on it.

| Topic | Finding | Status | Source |
|---|---|---|---|
| Accessibility service declaration | Service must declare `android.permission.BIND_ACCESSIBILITY_SERVICE`, `exported=true`, the `android.accessibilityservice.AccessibilityService` intent filter and an `android.accessibilityservice` meta-data XML. | VERIFIED | https://developer.android.com/guide/topics/ui/accessibility/service |
| Global actions | `performGlobalAction()` is the call for system-wide actions such as Back and notifications. | VERIFIED | same guide |
| GLOBAL_ACTION_RECENTS | Opens Recents. Available since API 21. | UNVERIFIED | https://developer.android.com/reference/android/accessibilityservice/AccessibilityService |
| GLOBAL_ACTION_QUICK_SETTINGS | Opens Quick Settings. API 17 constant is old but treated here as Android 12+ for safety, so the app guards it with SDK_INT >= 31. | UNVERIFIED | same reference |
| Window content | `android:canRetrieveWindowContent` is only needed to inspect the UI tree. Set false here; events then carry no source node. Package name is still available on window state events. | VERIFIED (config attr), event behaviour from AccessibilityEvent reference | accessibility guide; https://developer.android.com/reference/android/view/accessibility/AccessibilityEvent |
| Event cost | `typeAllMask` is resource heavy. This app asks only for `typeWindowStateChanged`. | VERIFIED | accessibility guide |
| Overlay type | TYPE_ACCESSIBILITY_OVERLAY is available to an enabled accessibility service and does not need SYSTEM_ALERT_WINDOW. TYPE_APPLICATION_OVERLAY needs the "Display over other apps" special access. Decision: use TYPE_ACCESSIBILITY_OVERLAY. | UNVERIFIED | https://developer.android.com/reference/android/view/WindowManager.LayoutParams ; https://developer.android.com/reference/android/Manifest.permission#SYSTEM_ALERT_WINDOW |
| Foreground service | Apps targeting Android 12+ cannot start a foreground service from the background except in listed cases. This app uses no foreground service: the enabled accessibility service is bound by the system. | VERIFIED | https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start |
| Usage access | PACKAGE_USAGE_STATS needs the user to enable it in Settings. Not used: window state events give the package name already. | UNVERIFIED | https://developer.android.com/reference/android/app/usage/UsageStatsManager |
| Package visibility | Targeting API 30+ filters other apps. `<queries>` declares what you need. QUERY_ALL_PACKAGES is for rare cases (accessibility apps are listed as an example) and Google Play has a policy for it. | VERIFIED | https://developer.android.com/training/package-visibility/declaring |
| Decision on QUERY_ALL_PACKAGES | Declared, because the rules picker must list apps without a launcher icon and system apps. Only used in the picker. Sideloaded debug build, not on Play. Justification must be restated if ever published. | decision | this document |
| Reboot behaviour | An enabled accessibility service is normally rebound by the system after restart. Some devices turn services off or kill them. The app never promises persistence and shows live status. | UNVERIFIED | https://developer.android.com/guide/topics/ui/accessibility/service |
| Restricted settings (sideloaded) | Android 13+ blocks accessibility for sideloaded apps until Allow restricted settings is used in App info. | UNVERIFIED (seen only in third party posts) | needs an official source |

## Checklist and result
- Overlay without extra permission: implemented (TYPE_ACCESSIBILITY_OVERLAY). Device check pending.
- No window content read: implemented in config. Device check pending.
- No foreground service, no network permission: implemented.
- Deep links to Accessibility settings and App info: implemented.
- Rows marked UNVERIFIED need a second official source before this document can be called complete.
