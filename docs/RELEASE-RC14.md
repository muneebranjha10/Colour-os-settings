# ColourOS Battery v1.1 RC14 — Better Balanced & Cleaner Mode UI

**Made by Muneeb · Samsung Device Care LSPosed testing release**

- Removed the long technical "Balanced: Samsung Light · Performance: Samsung Standard" sentence under the power-mode cards.
- Fixed the profile labels and selection logic: **Balanced restores native Samsung Standard** if the verified helper is present. Power saving continues to control Samsung's native saver.
- **Light** gets its own optional control in **Power profiles**, including a verified readback; Samsung Standard must not be confused with an enhanced performance boost.
- The third card is now **Performance settings**, opening Samsung Enhanced processing if the native activity is exposed or Samsung Performance profile as fallback. It no longer pretends to apply a high-performance mode.
- Added **Power profiles**, **Charging controls** and a structured **Battery tools** submenu. Standard / Fast & Super fast wired to the previously verified native charging policy; unplug before switching when instructed.
- Refined spring sliding and synchronized icon/label scale, with reduced motion safeguards. No new system-wide hooks, root grants, or unsupported kernel tuning.
- Preserved battery health, locally imported OPPO Sans, charging meter, yellow/blue/green battery bar, original Samsung settings and inline battery protection.

**Limitations:** ColorOS visual/motion fidelity is approximate, not a byte-perfect reproduction. The specific Samsung profile helper is firmware-dependent. CI checks packaging and logic, not thermal or benchmark performance on real devices.

**Install:** Install APK, enable and scope to Samsung Device Care in LSPosed, reboot, then open Settings → Battery. If app signing differs, uninstall the prior APK first, then re-enable and rescope. Confirm Balanced through the original Samsung Performance profile screen, and test charging controls unplugged.
