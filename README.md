# Colour-os-settings

Stage 1: Galaxy Battery integration test for Samsung Device Care
(`com.samsung.android.lool`, `com.samsung.android.sm.battery.ui.BatteryActivity`).
This is not the final redesigned Battery UI.

1. Download the APK from Releases (or the Actions build artifact).
2. Install it and enable Galaxy Battery Hook Test in LSPosed.
3. Scope only Samsung Device Care (com.samsung.android.lool).
4. Force stop Device Care and reopen Settings > Battery.
5. Look for "Custom battery hook active" and optionally the "Custom battery test" overflow entry. If the menu is unavailable, the toast and LSPosed logs are the primary test.
6. Disable the module and restart Device Care to reverse all changes.

No system APK replacement, battery setting changes, or persistent background services. Xposed API stubs are compile-only. Device compatibility is untested. Debug APK signing keys may change between CI builds, so later test APKs may require uninstalling the previous version.
