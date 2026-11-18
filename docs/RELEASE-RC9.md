# ColourOS Battery v1.1 RC9 — Motion Sync

Made by Muneeb.

- Glass and active icon/text colors follow a shared animation position rather than delayed native confirmation. Actual mode remains confirmed by Samsung.
- Cached fixed gradients, removed changing shader matrices per frame and duplicate moving highlight pass.
- One native dialog window transition in both directions, instead of a stacked content animation; reduced motion retained.
- Usage selection waits for its transition before native navigation; matching green protection switch tint.
- First launch migrates saved Fast preference to Smooth; Fast remains available.
- Existing health refresh and colors retained. 32 automated checks, compatibility and visual FPS still need physical-device testing. Root and LSPosed Device Care scope only.
