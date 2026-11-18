# ColourOS Battery 1.1 RC1

Public test candidate for rooted Samsung Galaxy devices using LSPosed, scope only Samsung Device Care (com.samsung.android.lool). Samsung One UI 8 and 9 are the target; One UI 8 functionality has been reported by the project owner's friends, model unspecified. No compatibility certification for every Galaxy, One UI version, ROM, or non-Samsung device. The hook requires the recognized BatteryActivity in the main Device Care process; unknown targets retain stock UI.

## Features
- Real ASOC/BSOH/SOH, Samsung battery cycle and usage fallbacks, source labels.
- Charging power estimates, thermal alert, target reminder (only while Battery page is open), charge graph.
- Inline native charge protection only for recognized Android 16/17 values; unsupported configuration opens stock settings.
- Performance and charging helpers restricted to the original test model SM-S911B on API 36/37 with signature and state checks; other models use native settings. Exact compatibility with firmware revisions is not guaranteed.
- Draggable selectors, glass strength, speed, haptics, show/hide watts, low-power UI and monitor visibility.
- Window-local status bars, no system-wide hooks, no charging firmware modifications.
- Compatibility report from Custom features.

## Install / recovery
Install signed test APK, enable module and scope Device Care only, reboot, open Settings > Battery. A CI debug key may differ from earlier builds; uninstall the old module if required. To recover, disable module and reboot, or use the original Samsung Battery page in the overflow. Settings changed by the user remain Samsung settings and are not reset by uninstalling. No automatic root request. No network permissions or persistent background service.

## Test status
Ten automated JVM regression checks cover parsing, numeric safeguards, compatibility gating and preference bounds. CI also verifies APK signing and the Xposed entry. These are not device tests, heat benchmarks, or optical-renderer tests. Report exact model, One UI version, and LSPosed logs with private data removed. Keep older release available for rollback.
