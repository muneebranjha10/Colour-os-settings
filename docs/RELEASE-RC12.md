# ColourOS Battery v1.1 RC12 — Liquid Battery Bar Studio

**Made by Muneeb · Samsung Device Care LSPosed testing build**

### Battery Bar Studio
- Three visual modes: Crystal glow, Liquid wave, Clean minimal.
- Animated percentage fill and smooth Green / Yellow (Power Saving) / Blue (charging) transitions.
- Flowing surface highlight and a traveling light reflection inside the bar; a brief flash after color changes.
- Independent toggles for traveling reflection and continuous charging animation.
- The bar's animation loop pauses when the screen loses focus, the module is detached, or Low Power UI is active.

### New submenus
- **Battery tools**: health/cycles, live charging diagnostics, Samsung battery protection, power saving/performance and original Battery.
- **Wattage accuracy**: explains battery-side power, not wall charger output, with a shortcut to sensor diagnostics.

### Sensor reliability
- A direction-unverified charging sensor does not produce an authoritative W number.
- Suppress near-zero charging watts that would look precise despite unreliable readings; preserve a real zero at full charge.
- Credible battery-side watts display at one decimal and estimated sources use the approximation mark.
- No root grants, kernel tuning, permanent service, or charger-controller modifications.

### Test status
GitHub Actions builds the APK and runs automated checks. Glass fluidity, exact wattage and compatibility across Samsung models **must be checked on real devices**. Install and scope only to Samsung Device Care in LSPosed, then restart Device Care or reboot. Debug signature may differ across builds.
