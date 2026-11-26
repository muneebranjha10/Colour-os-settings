<div align="center">

<img src="docs/banner.svg" alt="ColourOS Battery — Made by Muneeb" width="100%">

[![Build](https://github.com/muneebranjha10/Colour-os-settings/actions/workflows/build.yml/badge.svg)](https://github.com/muneebranjha10/Colour-os-settings/actions/workflows/build.yml)
[![Releases](https://img.shields.io/github/v/release/muneebranjha10/Colour-os-settings?include_prereleases&style=flat-square&color=30bb69)](https://github.com/muneebranjha10/Colour-os-settings/releases)
![Platform](https://img.shields.io/badge/Platform-Samsung%20Galaxy-263c32?style=flat-square)
![Framework](https://img.shields.io/badge/Framework-LSPosed-263c32?style=flat-square)

**A ColorOS-inspired battery experience, integrated into Samsung Device Care.**

[Download APK](https://github.com/muneebranjha10/Colour-os-settings/releases) · [Report a bug](https://github.com/muneebranjha10/Colour-os-settings/issues/new)

</div>

## Overview

ColourOS Battery replaces the appearance of the supported native Battery activity with a custom dashboard while preserving access to Samsung's original controls. This is an independent community project, not an official Samsung or OPPO product.

## Features

| Dashboard | Custom studio |
| --- | --- |
| Live battery percentage and charging estimates | Three selectable glass palettes |
| Battery-side charging power and recent graph | Adjustable animation speed |
| ASOC, BSOH and cycle count with source labels | Draggable, filled morphing power and charging selectors |
| Inline battery protection on supported firmware | Optional haptics and wattage display |
| Native charging and power settings | Low Power UI and monitor visibility |
| Temperature and charge-target alerts (page only) | Compatibility report |

Battery-side wattage is not wall or charger output. Health values are firmware estimates; unavailable readings are not fabricated. Inline charging protection requires recognized settings. Some controls open Samsung's native settings rather than applying changes directly.

### Crystal glass and motion

RC5 introduced a continuously tracking charge limit thumb with press expansion, four-step snapping on release, animated Basic/Maximum and usage tabs, and a spring-based crystal glass renderer with moving specular highlights. These are iOS-inspired optical approximations, not Apple's compositor or a copy of its proprietary effects. Native battery limits remain 80/85/90/95%. Dashboard polling yields during drag.


Battery Health now opens and closes with a brief same-window crossfade, without dashboard scaling or parallax. The back button and system back trigger the animated return. Module dialogs have soft entrance motion, rows have touch ripples, and the battery dashboard has an animated entrance and toolbar-back exit. Low Power UI reduces transitions to a short fade; system-disabled animators skip these effects. Native Samsung pages retain their own transitions.

### RC6 touch rendering fix

Removed the stationary rectangular Android press ripple from the power, charging, protection and usage glass selectors. Their moving glass remains the only touch selection effect. Other custom rows use a rounded ripple mask. Keyboard focus remains visible. The fix does not change native power mode activation logic.

### RC7 ColorOS refinement

Neutral charcoal and silver glass, restrained green selection accents, a white/silver charge-limit thumb, and an in-window Battery Health crossfade. The dashboard stays stationary when returning; polling pauses while health is open. Toolbar/system back share the close path (legacy fallback is scoped to BatteryActivity). Native Samsung pages retain their own transitions.

### RC8 Classic Flow

Classic green glass, original emerald and neutral silver options; Smooth motion uses 22% longer base timing and Relaxed 60% longer. Lower settings share aligned adaptive rows. Health refreshes every 30 seconds (60 in Low Power UI) while visible and focused. Readings show source and check time; live kernel counters take precedence. Samsung saved values are distinguished from live ASOC. Firmware may update cycles infrequently; no synthetic increments or assumption of 100% health. Imported snapshots pause automatic refresh until manual refresh or reopening. No new root grants.

### RC9 Motion Sync

Glass selection and icon/label colors now follow the same visual clock; real settings confirmation still determines the logical selected mode. Cached gradients avoid per-frame shader matrix changes and the secondary dialog content animation was removed. The first RC9 launch migrates saved Fast to Smooth (Fast remains selectable). Native pages retain their OS transitions. All Samsung Galaxy models are targeted, compatibility remains in testing.

### RC15 Direct Samsung profile switching and inline performance card
The three top buttons are actionable again. On Galaxy firmware that exposes the verified Samsung Device Care profile helper, **Balanced** selects Samsung **Light** (lower processing power and reduced heat), **Power saving** enables the built-in saver, and **Performance** selects Samsung **Standard**, the normal unrestricted profile. The last mode is **not an extra overclock or gaming-performance boost**: Samsung's native Performance profile offers only Light and Standard on the original S23, and only their confirmed settings are used. All profile writes use the same read-verified Device Care helper and show actual firmware state, reverting the visual choice if confirmation fails; on unsupported phones the profile controls do not claim success.

An **inline Performance profile** card now shows a glass sliding selector with two buttons, Light and Standard, mirroring the existing Standard / Fast charging card. Both support taps and horizontal swipes. The updated **Power profiles** submenu also directly selects Light or Standard and continues to provide Samsung's advanced processing and power-saving settings. The original Samsung page is preserved.

### RC14 Mode accuracy and cleaner ColorOS-style motion
Removed the long technical sentence below the three mode tiles. The old **Balanced = Light** and **Performance = Standard** wording and behavior could misrepresent Samsung's operating modes: Samsung describes Standard as the everyday default, and Light as the lower-power profile. **Balanced now selects Light** where the native Samsung helper supports a verified profile change; on unsupported devices it switches off Power saving and directs existing Light users to native Samsung profile controls when needed. **Power saving** continues to use Samsung's verified saver. The third tile is **Performance settings**, a shortcut to Samsung Enhanced processing if exposed, otherwise the Samsung Performance profile page; it never claims to overclock or confirms a nonexistent boost. **Light** is selectable separately through Custom features → Power profiles, and Samsung readback confirms it. If Light is active, the three-mode selector does not falsely highlight Balanced.

The new **Power profiles**, **Charging controls**, and organized **Battery tools** submenus expose Standard/Light settings, enhanced processing, the native charging mode switch (Standard or Fast/Super fast together), fast wireless charging, battery protection, diagnostics, and the existing alerts. Charging speed remains charger- and firmware-dependent; features that are unsupported open native Samsung settings.

Glass selector movement has smoother damping and slight synchronized icon/label emphasis; reduced-motion and Low Power UI preserve simplified transitions. This is a ColorOS-inspired recreation, not an exact port of OPPO's proprietary animation implementation.

### RC13 Reference-inspired ColorOS fidelity
The charging battery bar is rebuilt against a user-provided ColorOS screen recording and public OPPO battery-page examples. At rest it is **solid and sharp**, with rounded ends and measured fill only. Its charging blue is calibrated from the original clip, with a similarly clean green and warm power-saving yellow. There is no fake constantly looping wave. The bar fills with a short ease and crossfades color on **verified** Android battery/state updates. An optional polished look offers a brief reflection after transitions; no animation loop is left active while idle. The bar's colors/shapes approximate the reference, but are not an authorized or binary-exact port of ColorOS's UI.

OPPO officially offers **OPPO Sans 4.0** under terms that allow free usage but prohibit alternative font-download channels. This module does not bundle or redistribute the font. Use **Custom features → OPPO Sans 4.0 font → Open OPPO's official font page**, download there, and **Import official font file** using Android's document picker. The selected TTF/OTF file is validated and stored locally in Samsung Device Care app data. It applies across our dashboard, health, and protection surfaces; native Samsung pages retain their own typography. Default fallback is the device system font.
Reference: https://www.coloros.com/article/A00000074/

### RC12 Liquid battery bar and tools
The battery bar is now a separate lightweight renderer with three selectable styles: Crystal glow, Liquid wave and Clean minimal. Its animated highlight, flowing crest and state-change flash stay clipped inside the real measured battery percentage. Animated fill and ColorOS green/yellow/blue transitions use short value animators; the charging loop runs only while the Battery page has focus, and stops in Low Power UI. The new **Battery bar studio**, **Battery tools** and **Wattage accuracy** submenus are in Custom features.

The charging W display is still **battery-side power**, not wall-charger wattage. This release displays at most one decimal for credible readings, hides direction-unverified values and suspicious tiny charging readings, and labels derived values as approximate. Firmware sensors and device-specific hardware need real-device testing.

### RC11 Power Saving color and adaptive performance
Power Saving now animates the battery level indicator to warm ColorOS-style yellow even when plugged in; the level indicator returns to blue for charging or green for regular discharging when Samsung confirms Power Saving is off. The selected glass mode gets a brief yellow-gold crossfade on confirmed Power Saving. Power mode controls no longer hardcode one Galaxy model: the module checks that it is running inside Samsung Device Care, the firmware profile is valid, the native Light/Standard helper matches the expected method contract, and the helper itself advertises support. Unsupported models continue to the original Samsung performance settings; no overclocking, direct kernel writes, or unverified mode claims. Real-device support still requires testing.

### RC10 Liquid Lens experiment

Only the selector renderer changes: time-based finger-following tail, rounded rest geometry, damped settling, and optional GPU optical bending of analytically reconstructed local tile edges (Android 13+ hardware Canvas). No screenshot or screen-wide refraction, no new dashboard features or palette changes. Cached Canvas gradient fallback is retained for low-power mode or shader failure. Actual GPU shader execution and frame rate need device testing.

## Compatibility

**All Samsung Galaxy devices are targeted for support — compatibility is still in testing.** Root and LSPosed are required. This is not a guarantee that every model or firmware works yet.

| Environment | Status |
| --- | --- |
| Galaxy S23 SM-S911B, One UI 9 | Original development target; earlier builds tested by the author |
| One UI 8 | Community-reported working on earlier builds; model coverage unknown |
| Other Samsung Galaxy devices | Experimental, dependent on Device Care activity and permissions |
| Non-Samsung devices / unrooted devices | Unsupported |

The v1.1 RC15 is a public testing release, not a universal compatibility certification. Firmware-specific helpers are restricted to the original test model and recognized API levels; other devices use native settings fallback. No system-wide icon hooks or charging firmware modifications.

## Installation

1. Download the latest APK from [Releases](https://github.com/muneebranjha10/Colour-os-settings/releases).
2. Install the APK and enable **ColourOS Battery** in LSPosed.
3. Select **Samsung Device Care** (`com.samsung.android.lool`) only.
4. Reboot and open **Settings → Battery**.

If the APK cannot update because of a signature mismatch, uninstall the previous module first. CI uses a debug signature that can differ between runs. Re-enable the module and scope after reinstalling.

## Troubleshooting & recovery

- If the UI does not appear, check the LSPosed scope and Device Care version.
- If a control is unavailable, use the original Samsung Battery settings.
- To return to the original interface, use the overflow menu or disable the module and reboot.
- When reporting a bug, include your Galaxy model, Android/One UI version, Device Care version, steps to reproduce, and relevant LSPosed logs with personal data removed.

Balanced behavior varies by firmware. RC14 uses Samsung Standard as its default Balanced profile and separates Light as a supported optional setting; Enhanced processing may be missing on some models. User-selected Samsung settings are not reset by uninstalling the module.

## Build & verification

```sh
gradle :app:testDebugUnitTest :app:assembleDebug --no-daemon
```

GitHub Actions runs at least forty-two automated regression checks, builds the APK and verifies its signature and LSPosed entry. These checks do not replace device testing or validate every glass effect. No network permission or permanent background service is required.

---

<div align="center">

<img src="docs/icon.svg" width="56" alt="ColourOS Battery icon">

**Made by Muneeb**

</div>
