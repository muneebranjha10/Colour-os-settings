# ColourOS Battery v1.1 RC16 — Responsive ColorOS-Style Motion

**Made by Muneeb · Samsung Device Care LSPosed test APK**

## What changed
- Reviewed the supplied six-second RC15 performance-mode demonstration. The selected green glass was slow and stretched across neighboring tiles during changes.
- Replaced the old oversized head/tail union with a **single, rapidly moving filled glass capsule**, leaving a restrained liquid trailing/leading edge (up to ~8 dp) and a subtle short-lived stretch.
- Shortened adjacent and multi-tile transitions to nominal **195/245 ms**, adjusted by the user motion preset and capped at 350 ms. Faster initial interpolation and tighter drag following help the selector respond to each gesture.
- **Default Smooth/ColorOS Flow animation speed is now 1×**, down from 1.22×. Fast and Relaxed remain optional.
- The refraction shader is no longer evaluated on every moving frame; the animation uses cached gradients, returning to refractive rendering only at rest. This reduces per-frame work.
- Removed an additional 350 ms delay from the callback after confirmed Samsung power/profile switches. UI selection still only remains active when firmware readback supports the requested state.
- Softer icon/label scaling with existing reduced-motion and low-power UI handling.
- All previous direct Standard/Light and Power Saving controls, charging options, health/watts, fonts and battery protection retained.

## Install and check
Install the APK, enable in LSPosed with Samsung Device Care scoped, then reboot/restart Device Care. Open Battery and tap through Balanced → Power Saving → Performance, and swipe between Light/Standard. Select **Customize appearance → Animation speed → ColorOS flow** for the tuned default.

## Limitations
RC16 is **ColorOS-inspired**, not a binary-identical implementation of OPPO's native ColorOS 17 animation engine. 120 Hz and flawless motion cannot be guaranteed without on-device frame tracing. Automated CI validates the APK and code paths, not real-world frame pacing or profile compatibility.
