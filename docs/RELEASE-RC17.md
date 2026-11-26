# ColourOS Battery v1.1 RC17 — Synchronized Liquid Flow

**Made by Muneeb · Experimental Samsung Device Care LSPosed module**

RC17 responds to the actual 136999.mp4 S23 screen recording: the secondary Light/Standard selector looked delayed, and the highlight overlapped adjacent buttons for too long.

**Changes**
- Both the primary three-mode selector and the inline Light/Standard profile selector animate immediately on tap, instead of waiting for Samsung profile verification for the second selector.
- The selected value always reconciles to the real Samsung readback after applying the profile; no fake successful mode changes.
- Shorter, sharper motion (nominal ~170 ms adjacent / ~210 ms larger move with Smooth settings).
- Small trailing liquid lip and a restrained one-shot specular edge during movement, without heavyweight blur or a second animation loop.
- Text follows the glass animation with a small, synchronized emphasis.
- RC16 charging, protection, battery health, ColorOS reference bar and font-import functionality retained.

**Limitations:** ColorOS 17 uses OPPO's proprietary Living Flow effects; this is a visual re-creation based on recordings and published design descriptions, not a binary-exact port. Automated build tests do not establish real-world frame rate or universal profile-switching compatibility. No additional root hooks or kernel modifications.

**Install:** Install RC17 APK, enable the module in LSPosed, scope Samsung Device Care only, and restart Device Care or reboot. If debug signing differs, uninstall the previous module, then re-enable and scope the new APK. Test Light ↔ Standard in both selectors; share a short recording if the transition still looks wrong.
