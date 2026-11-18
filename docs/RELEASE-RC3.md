# ColourOS Battery v1.1 RC3 — Liquid Flow

Made by Muneeb.

- Replaced moving outlines with a filled liquid shape, stretching from the current card to the next. Both selectors share one frame-driven morph renderer.
- Cached gradients and reused path/paint. Removed per-transition bitmap capture and shader initialization; labels stay above the glass. Prevent redundant restart when the requested mode is confirmed.
- Four additional geometry tests (14 total). No phone FPS benchmark performed.
- All Samsung Galaxy devices targeted for support — compatibility in testing. Requires root and LSPosed scoped to Device Care. Unsupported controls use stock settings; some models may not display the dashboard.
- Previous Balanced-mode report remains uninvestigated; mode switching semantics unchanged. Public beta.
