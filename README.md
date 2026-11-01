# Recent-Recall

A floating button that opens your recent apps with one tap, so you can skip the gesture fuss. Built for one phone (Samsung Galaxy A17 5G) and shared as a debug build.

**This is a debug build, not a production release.** It is unsigned for release, not on Google Play, and has only had source review, unit tests and CI. It has not been tested on a physical device yet.

## Install on a Samsung phone
1. Download the `recent-recall-debug-apk` artifact from the latest GitHub Actions run, unzip it and install `app-debug.apk` (allow installs from your browser or Files app when asked).
2. Sideloaded apps are blocked from accessibility by default. Open Settings > Apps > Recent-Recall > three dots (top right) > Allow restricted settings.
3. Open Settings > Accessibility > Installed apps > Recent-Recall and turn it on. Confirm the prompt.
4. Open Recent-Recall to adjust the button. Android does not let an app turn this on for you, and it may need turning on again after a restart on some devices.

## What it does
- Floating button drawn as an accessibility overlay, so "Display over other apps" is not needed.
- Tap and long press can open Recents, Recent-Recall settings, Quick Settings (Android 12+) or the notification shade.
- Icon (two styles) or plain dot, adjustable opacity and size, drag to move (only after a deliberate movement), lock position, position saved.
- Per-app rules. Precedence: a blocked app never shows the button (block wins over allow). If the allow list is empty the button shows over every app that is not blocked. If it has entries, only those apps show it.
- Settings "Show system apps" and "Show hidden apps" (both off by default, saved). They only change what the picker lists. Saved rules for system or hidden apps stay active when they are off. The picker says its list may be incomplete.
- A second launcher icon, "Recents", and a long-press shortcut open Recents directly. You can put the icon on your dock yourself. It does not replace your launcher's app drawer button.
- Hides on the lock screen, when the screen is off, and over permission, install and biometric prompts.
- No network permission, no analytics, no ads. The service reads only the package name of the window in front.

## Build
CI runs unit tests, lint and `assembleDebug` with Gradle 8.7, JDK 17, AGP 8.5.2, Kotlin 1.9.24. The Gradle wrapper jar is not committed yet (see docs/STATUS.md), so build locally with Gradle 8.7 installed or open the project in Android Studio.

See `docs/ANDROID_REQUIREMENTS.md` for the researched Android rules and `docs/STATUS.md` for what is verified and what is not.
