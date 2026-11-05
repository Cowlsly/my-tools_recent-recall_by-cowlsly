# Sensitive screens

The button hides when the foreground package matches the lists in `app/src/main/java/com/cowslylabs/recentrecall/logic/PackageRules.kt`. Edit the constants `SENSITIVE_EXACT` and `SENSITIVE_CONTAINS` and rebuild to change them. The Privacy checkbox "Hide over sensitive screens" in the app turns the whole list on or off (default on).

Also hidden, independent of the list and the toggle: lock screen (KeyguardManager), screen off, and apps on the user's blocklist.

Covered by the list: permission controller, package installers, Samsung biometric and auth prompts, credential manager, Samsung Pass and Pay, Google Wallet, common password managers and authenticators, and package names containing bank, paypal, wallet, password and similar words.

Limits, stated plainly:
- The service reads package names only (canRetrieveWindowContent=false). It cannot tell a password page inside Settings from another Settings page, and cannot see FLAG_SECURE windows from an unlisted app.
- A banking app whose package name has none of the listed words is not hidden until you block it.
- Not yet tried on a phone.
