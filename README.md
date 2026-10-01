# Motion Photo Studio

![Logo Motion Photo Studio](assets/motion-mark-small.png)

## Bahasa Indonesia
Aplikasi Android native (Java) yang mengambil frame tengah dari MP4 dan menyimpan JPEG + MP4 dalam satu berkas Motion Photo. Semua diproses offline. Pilih sampul pada 25%, 50%, atau 75% durasi; koreksi warna/kontras dan pengurangan noise ringan bekerja pada foto sampul saja, bukan videonya. Stabilisasi video dan perbaikan blur berat belum tersedia. Android 10+; batas ukuran 300 MB. Google Photos dan Samsung Gallery perlu diverifikasi pada perangkat nyata; deteksi Motion Photo bukan jaminan lintas galeri. Tidak membuat Apple Live Photo atau live wallpaper.

Buka proyek di Android Studio (JDK 17), sync Gradle, lalu jalankan modul `app`. Proyek belum menyertakan Gradle wrapper; buat wrapper melalui instalasi Gradle tepercaya sebelum CI diaktifkan. Jangan masukkan file dari `uploads` atau kunci signing ke repo.

Rilis signed membutuhkan GitHub Secrets `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`. Simpan cadangan keystore secara offline: kehilangan kunci berarti APK versi berikutnya tidak dapat di-update dengan identitas sama. Draft release hanya dibuat setelah build signed berhasil. Menghapus artifacts/caches tidak menghapus riwayat workflow GitHub.

## English
Native Android app that extracts a middle frame from an MP4 and packages the JPEG and original MP4 as a single Motion Photo file. Offline processing, Android 10+, 300 MB cap. Gallery recognition must be tested on real Google Photos and Samsung Gallery devices. This does not create an Apple Live Photo or live wallpaper.

Open in Android Studio with JDK 17. Signing secrets must be stored in GitHub Secrets, never in the repository. The v0.1.0 signed draft exists. New v0.2.0 changes need a fresh CI run and signed draft. Cover adjustments do not modify the video; video stabilization is not implemented.

Built by xykal — XyVerse Technology Global
