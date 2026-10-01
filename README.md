# Motion Photo Studio

![Logo Motion Photo Studio](assets/motion-mark-small.png)

## Bahasa Indonesia
Aplikasi Android native (Java) yang mengambil frame tengah dari MP4 dan menyimpan JPEG + MP4 dalam satu berkas Motion Photo. Semua diproses offline. Pilih sampul pada 25%, 50%, atau 75% durasi; koreksi warna/kontras dan pengurangan noise ringan bekerja pada foto sampul saja, bukan videonya. Stabilisasi translasi video offline (eksperimental) tersedia untuk durasi hingga 30 detik, dengan crop tepi dan encode ulang H.264 bitrate tinggi. Tidak memperbaiki blur berat, sumber resolusi rendah, atau kompresi ulang TikTok. Android 10+; batas ukuran 300 MB. Google Photos dan Samsung Gallery perlu diverifikasi pada perangkat nyata; deteksi Motion Photo bukan jaminan lintas galeri. Tidak membuat Apple Live Photo atau live wallpaper.

Buka proyek di Android Studio (JDK 17), sync Gradle, lalu jalankan modul `app`. Proyek belum menyertakan Gradle wrapper; buat wrapper melalui instalasi Gradle tepercaya sebelum CI diaktifkan. Jangan masukkan file dari `uploads` atau kunci signing ke repo.

Rilis signed membutuhkan GitHub Secrets `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`. Simpan cadangan keystore secara offline: kehilangan kunci berarti APK versi berikutnya tidak dapat di-update dengan identitas sama. Draft release hanya dibuat setelah build signed berhasil. Menghapus artifacts/caches tidak menghapus riwayat workflow GitHub.

## English
Native Android app that extracts a middle frame from an MP4 and packages the JPEG and original MP4 as a single Motion Photo file. Offline processing, Android 10+, 300 MB cap. Gallery recognition must be tested on real Google Photos and Samsung Gallery devices. This does not create an Apple Live Photo or live wallpaper.

Open in Android Studio with JDK 17. Signing secrets must be stored in GitHub Secrets, never in the repository. Experimental offline translation-only video stabilization is enabled by default for videos up to 30 seconds, with H.264 re-encoding at a higher requested bitrate. Color/noise adjustments affect the cover only. This does not restore detail missing from low-quality sources or prevent TikTok recompression. Device testing is required.

Built by xykal — XyVerse Technology Global

### Build notes / Catatan build
Release v0.3.1 enables R8 code shrinking, optimization and resource shrinking. Export progress shows the current phase and percentage; remaining time is an estimate, not a deadline. Installing an older draft over a newer one may be blocked by Android versionCode rules.
