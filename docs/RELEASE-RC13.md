# ColourOS Battery RC13 — ColorOS Reference Fidelity

**Made by Muneeb · Experimental Samsung Device Care LSPosed APK**

## Reference-focused changes
- Inspected an original ColorOS battery-screen recording supplied during development, including its 99% to 100% charging state, text hierarchy and battery-bar shape.
- Switched the default battery bar to a **flat, clean ColorOS-style fill** with calibrated bright blue, green and amber yellow.
- Removed artificial continuously moving waves. A smooth percent fill and color-state crossfade remain; optional polished style has a **single** subtle reflection.
- Slightly refined bar height and rounded corners for the original proportions, with no added blur. Transition animators stop when the page loses focus.

## OPPO Sans typography
OPPO Sans 4.0 is the font target. Its official download and usage terms are at:
https://www.coloros.com/article/A00000074/

Per the official restrictions against other distribution channels, **the APK does not contain or redistribute OPPO's font**. To use it, download your own copy from OPPO's official page and choose **Custom features → OPPO Sans 4.0 font → Import official font file**. The module accepts a TTF/OTF via Android's file picker and stores it locally in the Samsung Device Care app data; reinstalling or clearing Device Care data may clear it. Fallback is the normal Samsung system font. The importer does not grant root or access unrelated device files.

## Testing and limitations
CI validates source wiring, Java compilation, automated regression tests, APK signing, Xposed entry and public release. Fonts, precise motion timing and firmware compatibility should still be checked on actual devices. This is a visual recreation, not a binary-identical or official ColorOS component. Other features from RC12 retained.

## Installation
Install the APK, enable **ColourOS Battery** in LSPosed, scope **Samsung Device Care** only, and reboot/restart Device Care. A signature mismatch from a different build may require removing the old APK and re-enabling LSPosed scope. The original Samsung Battery view remains available.
