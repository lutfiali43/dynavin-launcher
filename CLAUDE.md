# Project: Glass Launcher for Dynavin (Toyota Alphard AGH30)

Owner: Lutfi. Not a developer: explain in plain English, no jargon. App text is in English.

## Unit
- Dynavin V40 Series, Android 14, Snapdragon (Kryo 260, Adreno 610), 8 GB RAM.
- Developer options are locked by Dynavin (no ADB yet). Installs go through the APK: head unit browser → File Manager.
- The 360 camera uses a separate box. There's a Dynavin CarPlay app (wireless).

## Safety rules
- Never root, flash, or change car settings, factory settings or camera settings.
- The original Dynavin launcher stays installed; always keep a way back (the "Switch launcher" button).
- Don't hide the system bars (the Dynavin A/C bar may live there).

## Design
- Graphite #121316, one warm horizon glow, champagne #E3CFA8 as the only accent, ink #F4F2EE.
- Font: Barlow (body) and Barlow Semi Condensed (clock, headings), bundled in res/font.
- One filled button per screen. Spacing scale 4/8/16/24/32. Touch targets 56dp+.
- Mockup canvas: https://claude.ai/artifact/ECPFzmn34ZwuoVsjbSaLL4

## Build
- Kotlin + Jetpack Compose. Builds only on GitHub Actions (.github/workflows/build.yml).
- Every push to main publishes a new `launcher.apk` at /releases/latest.
- Fixed signing key: app/glass.keystore (so updates install over older versions).
