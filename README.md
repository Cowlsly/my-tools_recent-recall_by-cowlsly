# Recent-Recall

A personal Android floating button for opening recent apps without swiping.

## Build or download on your phone

Open [Actions > Android debug build](https://github.com/Cowlsly/my-tools_recent-recall_by-cowlsly/actions/workflows/android.yml), then choose **Run workflow** on `root`. Editing and committing any document on `root` also starts a build automatically.

When the run is green, download **recent-recall-debug-apk** under Artifacts, unzip it and install `app-debug.apk`. The ZIP also records the exact app source commit and APK checksum. GitHub sign-in is required for artifact downloads.

## Current status

The Android source is on [draft PR #1](https://github.com/Cowlsly/my-tools_recent-recall_by-cowlsly/pull/1), branch `feature/recent-recall-app`. This default branch has artwork and build instructions. Until the app project is merged here, the workflow explicitly builds that candidate branch and records its commit; it does not merge it. After the project is merged, the same workflow builds root itself automatically.

App commit `e5cd71a3c5d2476e066e7931ec90e8cb23c206e9` passed unit tests, Android lint and APK packaging in [run 41367238460](https://github.com/Cowlsly/my-tools_recent-recall_by-cowlsly/actions/runs/41367238460). The updated workflow requires its own successful run. This is ready for a personal debug-install trial, not a claim that physical-device testing is complete.

See [build instructions and limitations](docs/AUTO_BUILD.md) and [app setup instructions](https://github.com/Cowlsly/my-tools_recent-recall_by-cowlsly/blob/feature/recent-recall-app/README.md). Enable Recent-Recall in Android Accessibility after installation; root and Shizuku are not required.

The app's name is “Recent-Recall”, as if recalling recent spells.
