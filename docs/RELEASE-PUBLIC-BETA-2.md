# ColourOS Battery v1.1 — Public Beta 2: Liquid Battery

Made by Muneeb.

- Tap the existing battery bar to expand/collapse a compact glass-styled card with measured level, temperature, voltage, current, stored charge and battery-side power. Uses existing telemetry, unavailable reads show a dash.
- Dynamic battery status accents: green normal, blue active charging, amber power saving, red low battery (15% or below). Charging takes precedence. Optional in Customize appearance.
- Flowing charging highlight clipped to actual battery fill. Animation only while visibly charging, not full, not paused, not on Health screen, respects system animation and Low Power UI settings.
- Beta first-launch warning remains one-time across upgrades. Existing ColorOS design, selector glass, health reader and native controls preserved. Main branch remains unchanged.
- 44 automated regression tests and source wiring checks, signature and LSPosed entry verification. Physical-device visuals, FPS, sensor permissions and Samsung compatibility still need testing. Install only one module version at a time. Scope Samsung Device Care only.
