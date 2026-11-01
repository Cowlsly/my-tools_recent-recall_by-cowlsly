# Status (2026-11-02)

## Done in source (reviewed by reading, not run on a device)
Settings storage, visibility rules, gesture interpreter, position clamping, overlay controller, accessibility service, main settings screen with permission status, app rules picker, launcher alias and shortcuts, CI workflow, README.

## Not done or not verified
- Gradle wrapper jar and gradlew script are not committed (binary file could not be written with the tools available). CI uses setup-gradle with Gradle 8.7 instead. Smallest fix: run `gradle wrapper --gradle-version 8.7` locally and commit the result.
- Runtime copies of the PNGs: they are copied from /res/drawable into the build by a Gradle task, not committed under app/src/main/res/drawable. The originals are untouched.
- Adaptive launcher icon: the launcher PNG is used as is.
- The button is not hidden on every possible sensitive screen. Only a short package list (permission, installer, biometric prompts) plus the lock screen and screen off.
- Foreground app detection uses window state events and can lag or miss some system windows.
- Rotation and display-size handling is written but untested.
- ANDROID_REQUIREMENTS.md has rows marked UNVERIFIED.
- No emulator or physical device testing has been done.
