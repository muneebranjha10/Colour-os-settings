# ColourOS Battery v1.1 RC15 — Direct Performance Profiles

**Made by Muneeb · Galaxy Battery customization for LSPosed**

## Changes
- Tapping **Performance mode** now switches directly to the phone's Samsung **Standard** performance profile when supported. It is Samsung's normal full-processing setting, **not** an undocumented overclock.
- Tapping **Balanced mode** switches to Samsung **Light**, which prioritizes battery life and cooling. Power saving still controls Samsung's real saver.
- Added an inline **Performance profile** card with direct **Light / Standard** buttons. Uses the same sliding liquid-glass selector style as the existing Standard / Fast charging controls; supports tapping and dragging.
- Power Profiles submenu also switches those modes directly. Enhanced processing, if present, remains available as a separate Samsung setting, not a fake high-performance slider.
- The selected tile follows **real readback** of `sem_low_heat_mode`; if Samsung doesn't confirm a change the UI returns to the actual state.
- The rest of the RC14 visual, font, battery health, charging speed, and protection features are retained.

## Compatibility and safety
The screenshot supplied in development confirms that the Galaxy S23 native Performance profile only offers **Standard** and **Light**. There is no stock third overclocking profile, and the performance-profile setting does not affect games. No kernel frequency changes, new privileged scope, or unsafe background tasks are introduced. Where the Samsung helper isn't available, the module does not pretend to change performance.

**Installation:** Install APK, enable in LSPosed with **Samsung Device Care** as the only scope, then restart Device Care or reboot. If a debug signature mismatch occurs, uninstall the old module first, then enable/scope the new one.

**Validation:** CI tests build/package and check wiring; direct profile switching and animation smoothness still need hardware verification.
