# Product brief

## Ringkasan
- Aplikasi Android native, pemrosesan offline.
- Input MP4 melalui pemilih dokumen sistem.
- Ambil satu frame di tengah sebagai cover JPEG.
- Sisipkan metadata XMP dan MP4 utuh di belakang JPEG.
- Simpan melalui MediaStore ke Pictures/MotionPhoto.
- Target Google Photos dan Samsung Gallery; kompatibilitas perlu diuji.
- Bukan Apple Live Photo maupun wallpaper bergerak.
- Batas 300 MB untuk mencegah penggunaan ruang tanpa batas.
- Tidak meminta izin akses seluruh penyimpanan.
- Rilis signed memerlukan keystore yang disimpan permanen oleh pemilik.

## Scope and constraints
Single local conversion, no cloud, no analytics, no watermark. Target Android API 29+. Uses a JPEG XMP APP1 packet with Camera MotionPhoto and legacy MicroVideo metadata, Container Directory entries and an MP4 tail. The motion duration is the original MP4 duration; file format alone cannot guarantee playback in a specific gallery. The midpoint frame is a sync frame; the visual cover may differ slightly from the exact midpoint. Large decoded frames may trigger memory pressure: improve with a scaled-frame API before raising the input cap.

## Security and release
SAF supplies input access; output uses MediaStore pending state and cleans up failures. No credentials shipped to the client. A signed release must use persistent owner-held signing credentials; CI may receive only GitHub Secrets and should remove transient artifacts. CI run history is retained by GitHub and cannot honestly be described as erased.

## Milestones
1. Source and writer (2026-10-01): local implementation; build unverified.
2. CI and signed draft release: blocked until persistent keystore and repository automation available.
3. Device validation: real MP4s on Google Photos and Samsung Gallery, including audio, portrait rotation, and large files.

## Offline stabilization v0.3 (experimental)
For videos up to 30 seconds, sample 5 frames/s at low resolution, estimate global translations with luma block matching, smooth the motion path, and apply interpolated per-frame matrix corrections in Media3 Transformer. Re-encode H.264 with requested 8–25 Mbps depending on source resolution and retain the audio track. The Motion Photo embeds the processed video; no separate MP4 export. This does not correct rotation, rolling shutter, strong parallax, motion blur, or compression already in the source. Cropping is required; the output is not guaranteed to survive TikTok recompression unchanged. Test on physical low/mid/high-tier devices and real moving-subject footage before publishing.

## Trim before edit (v0.4)
After file selection, display a dedicated range editor with two handles and a native video preview without stock controls. Permit a 1–30-second interval anywhere in the source. Preserve the precise interval in Media3 clipping before optional stabilization; choose the still cover from the selected interval. Do not claim frame-perfect preview seeking (device decoders may snap to a keyframe).
