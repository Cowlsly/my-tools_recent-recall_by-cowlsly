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
