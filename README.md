# Motion Photo Studio

![Logo Motion Photo Studio](assets/motion-mark-small.png)

Aplikasi Android native untuk mengubah potongan video MP4 menjadi **Motion Photo**: satu berkas JPEG dengan klip MP4 tertanam. Diproses di perangkat, tanpa unggah video ke server.

**[Unduh APK terbaru (draft release)](https://github.com/xykal/video-to-motion-photo/releases/tag/untagged-fadabafce0875d854d9e)** · [Lihat build CI](https://github.com/xykal/video-to-motion-photo/actions/workflows/android.yml)

> Draft release mungkin memerlukan login GitHub. Aplikasi belum dinyatakan lulus uji lintas perangkat atau filter TikTok.

## Fitur

1. Pilih MP4 melalui pemilih dokumen Android.
2. Pangkas bagian video sepanjang **1–30 detik**, dengan pratinjau dan tombol Batal/Selesai yang tetap terlihat.
3. Pilih frame sampul pada 25%, 50%, atau 75% bagian yang dipangkas.
4. Atur kecerahan/warna dan reduksi noise ringan **pada sampul saja**.
5. Aktifkan atau matikan stabilisasi geser video. Stabilisasi ini memotong sedikit bagian tepi; belum menangani rotasi, rolling shutter, atau blur gerak.
6. Aktifkan atau matikan watermark metadata. Saat aktif, area di balik label diberi efek blur GPU pada video; sampul menampilkan versi blur lokal. Jika efek GPU gagal, aplikasi mencoba ekspor **tanpa watermark** dan menampilkan pemberitahuan.
7. Simpan hasil ke `Pictures/MotionPhoto` melalui MediaStore.

Watermark membaca **make/model kamera hanya bila ditemukan pada tag QuickTime/MP4 yang didukung**. Bila tidak tersedia, label menyatakan tidak terdata. Angka MP adalah **megapiksel frame video, bukan sensor kamera**. Waktu rekam hanya ditulis bila metadata sumber berisi tanggal yang dapat dibaca. Lokasi tidak ditampilkan.

## Batasan penting

- Android 10+; berkas sumber maksimal **300 MB**. Pemrosesan video seluruhnya offline.
- Bitrate encode yang lebih tinggi mengurangi kehilangan kualitas tambahan, **bukan** mengembalikan detail pada video yang sudah pecah. Platform seperti TikTok dapat mengompres ulang video.
- Kompatibilitas Motion Photo berbeda antar galeri. Masalah warna setelah memakai filter TikTok **belum terverifikasi selesai** tanpa video contoh dan uji di perangkat.
- Watermark kaca adalah efek blur dan highlight eksperimental, **bukan simulasi optik/refraction fisik**.
- Proyek ini tidak membuat Apple Live Photo atau live wallpaper. Tidak ada modul `.so` buatan proyek; codec video mengandalkan Android/Media3.

## Menjalankan dari sumber

Buka proyek di Android Studio dengan JDK 17 dan Android SDK 35, sinkronkan Gradle, lalu jalankan modul `app`. Repo belum menyertakan Gradle wrapper; workflow CI menggunakan Gradle 8.9 dari `gradle/actions/setup-gradle`. Kode aplikasi Java berada di `app/src/main/java/id/xyverse/motionphoto/`.

Workflow push menjalankan unit test dan build debug. Draft signed release dibuat **hanya** ketika workflow `Android` dijalankan manual dengan opsi `release=true`. Signing menggunakan GitHub Secrets `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, dan `ANDROID_KEY_PASSWORD`. Jangan masukkan keystore atau kredensial ke repo. **Saat ini tidak ada cadangan keystore di luar GitHub**; kehilangan secrets berarti kunci untuk pembaruan APK tidak dapat dipulihkan.

Lihat [brief teknis](docs/PRD.md), [arah desain](docs/DESIGN.md), dan [kebijakan keamanan](SECURITY.md).

## English

Motion Photo Studio is an offline native Android app that trims an MP4 and saves a JPEG Motion Photo with an embedded clip. It supports cover selection, limited translation-only stabilization, and an optional experimental metadata watermark on both the still and video. Source camera details are shown only when supported video metadata actually contains them. It cannot restore lost detail, guarantee gallery compatibility, or guarantee color behavior after third-party filters. See the Indonesian sections above for limits, build instructions, and signing requirements.

Built by xykal — XyVerse Technology Global
