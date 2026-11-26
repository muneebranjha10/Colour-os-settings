# ColourOS Battery v1.1 RC11 — Power Saving Color and Performance Compatibility

**Made by Muneeb · Experimental LSPosed APK for Samsung Device Care**

- Battery percentage bar turns ColorOS-style yellow when the platform reports Power Saving active, with a smooth animated transition. Otherwise blue while charging and green while discharging.
- Power-mode liquid-glass selection warms to amber in Power Saving and returns to green in Standard/Light modes.
- Removed the S23-only gate for a **read-verified** Samsung Light/Standard profile helper. Other Samsung models are supported only when Device Care advertises the helper, exposes a valid firmware profile and confirms the change; otherwise the tile opens native Performance settings.
- Normal "Performance" is Samsung **Standard** profile, **not an overclock**. Super saving still uses Samsung's saver; no fabricated performance mode.
- Previous health, charge-cycle, protection, wattage, motion and accessibility features retained.

## Install
Install the APK, enable and scope it to **Samsung Device Care** in LSPosed, then restart Device Care or reboot. Keep the original page available from the overflow menu. Installation over a differently signed debug APK may require uninstalling the old module first.

## Limitations
Automated tests and CI verify build, packaging and control wiring, not runtime compatibility across phones. Samsung profile helper names vary across firmware; unsupported devices use native Samsung settings. Battery color reflects the platform's confirmed Power Saving state, not a requested-but-failed transition.
