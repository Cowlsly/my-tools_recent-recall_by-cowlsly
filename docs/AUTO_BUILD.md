# Build an APK from your phone

Every pushed commit on a branch containing `.github/workflows/android.yml` starts **Android debug build**, including edits to Markdown or text documents. PRs targeting `root` also run the checks. There are no path filters. Commit messages containing GitHub skip-CI directives can suppress a run.

## One-time branch situation

The Android app is currently in draft PR #1, `feature/recent-recall-app`. `root` has artwork and documentation, not the app. The workflow installed on `root` checks out the app candidate if the triggering root revision has no `app/build.gradle.kts`. It records both the trigger commit and the exact app source commit in `BUILD-INFO.txt` and the run summary. A root document edit rebuilds the candidate; it does not merge it or apply root-only document changes to the app source.

After the app is reviewed and merged into `root`, this fallback stops automatically: pushes build the triggering revision itself. Other branches build their own source and fail clearly if the Android project is missing. Nothing auto-merges or publishes a release.

## Get your APK

1. Edit a document on `root` or the app branch and commit it. Alternatively open **Actions > Android debug build > Run workflow**, select `root`, and run it without changing any files.
2. Open the resulting run and wait for a green result. Tests and lint must pass before the APK is uploaded.
3. Open **Artifacts > recent-recall-debug-apk** or the **Download APK ZIP** link in the run summary. Sign in to GitHub if prompted.
4. Unzip the download. It contains `app-debug.apk`, `BUILD-INFO.txt` and `SHA256SUMS.txt`. Install the APK and follow the accessibility setup in the app-branch README.

APK downloads are retained for 14 days; reports for 7 days. Rebuild to get a fresh download after expiry. A failed run includes whatever test/lint reports were produced, but no APK advertised as successful.

## What passing means

Tests, Android lint and APK packaging passed. It does not mean the floating overlay has been verified on your phone. First check: open Recents by tap, settings by long press, drag and position lock, opacity, allow/block rules, rotation, screen lock/unlock, service reconnect and reboot. Package-based sensitive-screen hiding is best effort and cannot identify every secure screen.

These are debug-signed test APKs, not release builds. Hosted runners may generate different debug signing keys across runs. An update can therefore fail with a signature conflict; uninstalling the old test app first deletes its saved settings. Stable private signing is a separate follow-up before depending on update continuity. No signing secrets are required by this workflow.
