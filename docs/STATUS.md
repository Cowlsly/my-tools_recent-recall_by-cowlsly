# Status (2026-11-02)

## Done in source (reviewed by reading; unit tests run in CI; not run on a device)
Settings storage, visibility rules, gesture interpreter, overlay controller, accessibility service, settings screen with permission status, app rules picker, launcher alias and shortcuts, CI workflow, README, Gradle wrapper, committed PNG copies.

Gap-closing commit:
- Rotation, display size and font size: the button is placed from a saved fraction inside the usable area (screen minus system bars and cutout) on every refresh. The service re-places it on configuration changes and on display changes, and a 3 second watchdog re-checks.
- Sensitive screens: wider documented list, user toggle, see docs/SENSITIVE_SCREENS.md.
- Foreground detection: ForegroundTracker ignores keyboards and system windows, forgets the app when the screen turns off, and the watchdog re-checks lock and screen state. Still package name only.
- Service lifecycle: onServiceConnected tears down before setting up, so reconnects and process restarts are clean; add-view failures retry up to 3 times.
- Permissions: the app and the checklist no longer ask for Display over other apps.

## Not done or not verified
- Adaptive launcher icon: the launcher PNG is used as is.
- Foreground detection still depends on window-state events; a missed event can leave a stale app until the next event or screen change.
- Sensitive hiding is by package name only.
- ANDROID_REQUIREMENTS.md still has rows marked UNVERIFIED.
- No emulator or physical device testing has been done, including rotation, service rebind and process death.

## Build-readiness review (2026-11-03 AWST)

GitHub Actions run 41367238460 for app commit e5cd71a3c5d2476e066e7931ec90e8cb23c206e9 completed successfully. Its job steps confirm unit tests, Android lint, assembleDebug and APK upload succeeded. Artifact 13107672707 is an unexpired APK ZIP at review time. This is server-side build evidence, not emulator or physical-device validation.

The Gradle wrapper JAR is tracked; the README statement saying it was absent was stale and has been corrected. Automatic builds now include root commits (using the explicitly documented app-candidate fallback while root lacks the project), any file changes including documents, and manual Run workflow. The new workflow needs its own successful run before being treated as verified.

Verdict: ready for a personal debug-install trial; not verified as finished. No app code was changed in this build-automation review. PR #1 stays draft and unmerged pending device checks. Remaining limitations above still apply.
