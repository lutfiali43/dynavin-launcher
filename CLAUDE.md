# Projek: Glass Launcher untuk Dynavin (Toyota Alphard AGH30)

Pemilik: Lutfi. Bukan developer — terang dalam Bahasa Melayu yang mudah, elak istilah teknikal.

## Unit
- Dynavin V40 Series, Android 14, Snapdragon (Kryo 260, Adreno 610), RAM 8GB.
- Developer options dikunci Dynavin (belum ada ADB). Install melalui APK: browser head unit → File Manager.
- Kamera 360 guna kotak berasingan. Ada app CarPlay Dynavin (wireless).

## Peraturan keselamatan
- Jangan root, flash, atau ubah tetapan kereta / Factory settings / kamera.
- Launcher asal Dynavin mesti kekal; sentiasa ada jalan patah balik (butang "Tukar launcher").
- Jangan sorok bar sistem (bar A/C Dynavin mungkin di situ).

## Binaan
- Kotlin + Jetpack Compose. Bina hanya di GitHub Actions (.github/workflows/build.yml).
- Setiap push ke main → release baru `launcher.apk` di /releases/latest.
- Kunci tandatangan tetap: app/glass.keystore (supaya update boleh install atas versi lama).
