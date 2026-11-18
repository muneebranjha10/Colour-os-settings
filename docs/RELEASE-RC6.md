# ColourOS Battery v1.1 RC6 — Rounded Touch Fix

Made by Muneeb.

- Removed the fixed rectangular pressed foreground from all module glass selectors, which appeared on top of the rounded moving selection in the user recording.
- The power, charging, battery protection and usage controls now rely on the existing glass renderer for touch selection motion, with a rounded keyboard-focus outline.
- Other module rows use a softer rounded ripple mask.
- No changes to protection limits, native performance mode behavior, sensor reading or charging logic.
- Twenty existing regression tests plus a source check for glass touch wiring. Device validation still required. Previous power-mode switch concern not investigated in this visual fix.

All Samsung Galaxy devices targeted for support — compatibility still in testing. Requires LSPosed and Samsung Device Care scope only.