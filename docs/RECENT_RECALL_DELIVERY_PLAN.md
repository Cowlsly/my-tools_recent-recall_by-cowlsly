# Recent-Recall: execution plan (2026-11-02)

## Verified baseline
Default branch root contains a README describing a floating Recents shortcut. Android Gradle files and CI were not found at conventional paths; inspect full tree and other branches before deciding to scaffold. Branch marla/recent-recall-delivery created for isolated work.

## Research
- Android AccessibilityService.performGlobalAction(GLOBAL_ACTION_RECENTS): https://developer.android.com/reference/android/accessibilityservice/AccessibilityService
- MIT licensed Assistive Menu Tool with floating overlay, Recents, opacity and size: https://f-droid.org/packages/com.joaohouto.assistivemenutool/
- Apache-2.0 AssistiveDotApp with Samsung restricted-settings caveat: https://github.com/abdallahnatsheh/AssistiveDotApp
- MIT TouchAssist floating overlay and drag gestures: https://github.com/RithikAR25/TouchAssist
Review license notices and dependency safety before reusing code; prefer small original implementation over wholesale copying.

## Delivery milestones
P0 inventory: list all files/branches/PRs and existing artifacts; record actual code state.
P1 native Kotlin Android app: MainActivity onboarding, AccessibilityService, WindowManager overlay, draggable dot; tap calls RECENTS; clear service disabled state; no INTERNET permission.
P2 user settings: opacity, size, edge snap, position lock, tap/long-press actions (Recents, Settings, Quick Settings), local persistence, start/stop control.
P3 optional features: allow/block lists where reliable and privacy-preserving, icon customization, launcher dock integration only if supported. Never block P1 release on these.
P4 CI: Gradle wrapper, unit tests, lint, assembleDebug; upload installable APK and SHA256 as workflow artifacts. Provide separate signed release when owner-controlled signing configured; never commit signing keys.
P5 test on Samsung Galaxy A17 Android 16: overlay permission, accessibility permission including restricted settings if necessary, recents toggle, dragging vs tap, rotate, restart, battery use, revoke permission, uninstall.

## Continuous execution loop for Symphony and Marla
1. Fetch latest repository state and prior checkpoint.
2. Select highest priority unmet acceptance criterion.
3. Make smallest working code change on a branch.
4. Run build/tests; inspect actual logs, fix and retry.
5. Commit and push; preserve evidence and artifacts.
6. Update checklist with commit, test outcomes, next task and blockers.
7. Repeat until a downloadable APK is ready. If blocked by physical-device verification, deliver the APK with precise owner test steps rather than declaring full verification.

## Exit criteria
A verified CI-built APK link; checksum; installation instructions; passing build; device-only tests clearly labelled pending until observed. No claims of completion without artifact and evidence.
