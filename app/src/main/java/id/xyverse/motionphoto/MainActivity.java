package id.xyverse.motionphoto;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private static final int PICK_VIDEO = 10, TRIM_VIDEO = 11;
    private static final long MAX_BYTES = 300L * 1024 * 1024;
    private static final int INK = 0xff202521, MUTED = 0xff656d67, CREAM = 0xfff7f4ed;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private TextView status, export, enhanceButton, denoiseButton, stabilizeButton;
    private ProgressMeter progressMeter;
    private long startedAtMs;
    private volatile int lastProgressValue = -1;
    private volatile String lastProgressStage = "";
    private final TextView[] coverButtons = new TextView[3];
    private ImageView preview;
    private Uri selected, pending;
    private long clipStartMs, clipEndMs;
    private boolean enhance, denoise, busy, stabilize = true;
    private int coverIndex = 1, previewGeneration;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(CREAM);
        getWindow().setNavigationBarColor(CREAM);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(CREAM);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(24), dp(38), dp(24), dp(40));
        scroll.addView(page);
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.mipmap.ic_launcher);
        page.addView(logo, new LinearLayout.LayoutParams(dp(58), dp(58)));
        TextView label = text("MOTION PHOTO STUDIO", 12, 0xffab534b, true);
        page.addView(label, margin(0, 20));
        TextView title = text("A moment,\nmade to move.", 34, INK, true);
        page.addView(title, margin(0, 6));
        page.addView(text("Ubah klip favorit jadi foto bergerak. Diproses di HP, tanpa unggah video.", 15, MUTED, false), margin(0, 12));

        preview = new ImageView(this);
        preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        preview.setBackground(shape(0xffe8e4da, 22, 0));
        preview.setContentDescription("Pratinjau frame sampul video");
        page.addView(preview, new LinearLayout.LayoutParams(-1, dp(206)));
        TextView pick = action("Pilih video MP4  ↗", false);
        pick.setOnClickListener(v -> {
            if (busy) return;
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("video/mp4");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, PICK_VIDEO);
        });
        page.addView(pick, margin(14, 16));
        page.addView(text("01  PILIH FOTO SAMPUL", 12, MUTED, true), margin(22, 12));
        LinearLayout choices = new LinearLayout(this);
        for (int i = 0; i < 3; i++) {
            final int position = i;
            TextView button = action(new String[]{"Awal · 25%", "Tengah · 50%", "Akhir · 75%"}[i], false);
            button.setTextSize(12);
            button.setOnClickListener(v -> { coverIndex = position; refreshStyles(); showPreview(); });
            LinearLayout.LayoutParams cell = new LinearLayout.LayoutParams(0, dp(46), 1);
            if (i > 0) cell.leftMargin = dp(6);
            choices.addView(button, cell);
            coverButtons[i] = button;
        }
        page.addView(choices, margin(10, 0));
        page.addView(text("02  TAMPILAN SAMPUL", 12, MUTED, true), margin(24, 12));
        enhanceButton = action("Cerah + warna", false);
        enhanceButton.setOnClickListener(v -> { enhance = !enhance; refreshStyles(); showPreview(); });
        page.addView(enhanceButton, margin(0, 8));
        denoiseButton = action("Kurangi noise ringan", false);
        denoiseButton.setOnClickListener(v -> { denoise = !denoise; refreshStyles(); showPreview(); });
        page.addView(denoiseButton, margin(8, 8));
        page.addView(text("03  VIDEO LEBIH TENANG", 12, MUTED, true), margin(24, 12));
        stabilizeButton = action("Stabilkan video · aktif", false);
        stabilizeButton.setOnClickListener(v -> { stabilize = !stabilize; refreshStyles(); });
        page.addView(stabilizeButton, margin(0, 8));
        page.addView(text("Stabilisasi translasi offline, durasi maks. 30 detik. Crop tepi 8% dan encode H.264 bitrate tinggi. Koreksi warna/noise hanya pada sampul; sumber buram tidak bisa dipulihkan.", 12, MUTED, false), margin(10, 0));
        export = action("Buat Motion Photo  →", true);
        export.setOnClickListener(v -> {
            if (selected == null || busy) return;
            busy = true;
            startedAtMs = SystemClock.elapsedRealtime();
            lastProgressValue = -1;
            lastProgressStage = "";
            progressMeter.setVisibility(View.VISIBLE);
            progressMeter.setPercent(0);
            export.setAlpha(.55f);
            status.setText("Menyiapkan video…");
            Uri input = selected;
            int index = coverIndex;
            boolean correction = enhance, smoothing = denoise, stabilizeVideo = stabilize;
            long startMs = clipStartMs, endMs = clipEndMs;
            worker.execute(() -> {
                try {
                    convert(input, index, correction, smoothing, stabilizeVideo, startMs, endMs);
                    postProgress("Selesai. Cek Pictures/MotionPhoto di galeri", 100);
                } catch (Exception error) {
                    runOnUiThread(() -> { progressMeter.setVisibility(View.GONE); status.setText("Gagal: " + error.getMessage()); });
                } finally {
                    runOnUiThread(() -> { busy = false; export.setAlpha(1f); });
                }
            });
        });
        page.addView(export, margin(24, 0));
        progressMeter = new ProgressMeter(this);
        progressMeter.setVisibility(View.GONE);
        page.addView(progressMeter, new LinearLayout.LayoutParams(-1, dp(8)));
        status = text("Pilih MP4 maksimal 300 MB untuk mulai.", 13, MUTED, false);
        page.addView(status, margin(12, 0));
        page.addView(text("Offline • Android 10+ • Powered by " + Brand.NAME, 11, MUTED, false), margin(34, 0));
        setContentView(scroll);
        refreshStyles();
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == PICK_VIDEO && result == RESULT_OK && data != null && data.getData() != null) {
            pending = data.getData();
            Intent trim = new Intent(this, TrimActivity.class);
            trim.setData(pending);
            trim.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(trim, TRIM_VIDEO);
        } else if (request == TRIM_VIDEO && result == RESULT_OK && data != null && pending != null) {
            long start = data.getLongExtra("startMs", -1);
            long end = data.getLongExtra("endMs", -1);
            if (start < 0 || end - start < 1000 || end - start > 30000) {
                status.setText("Durasi potongan tidak valid. Pilih ulang video.");
                return;
            }
            selected = pending;
            clipStartMs = start;
            clipEndMs = end;
            status.setText("Potongan " + TrimRangeView.format(end - start) + " siap. Atur sampul lalu ekspor.");
            showPreview();
        }
    }

    private void showPreview() {
        Uri input = selected;
        if (input == null) return;
        int generation = ++previewGeneration;
        int index = coverIndex;
        boolean correction = enhance, smoothing = denoise;
        long startMs = clipStartMs, endMs = clipEndMs;
        worker.execute(() -> {
            try (MediaMetadataRetriever retriever = new MediaMetadataRetriever()) {
                retriever.setDataSource(this, input);
                Bitmap frame = getCover(retriever, index, startMs, endMs);
                Bitmap adjusted = CoverProcessor.process(frame, correction, smoothing);
                if (adjusted != frame) frame.recycle();
                Bitmap result = adjusted;
                runOnUiThread(() -> {
                    if (generation == previewGeneration && !isDestroyed()) preview.setImageBitmap(result);
                    else result.recycle();
                });
            } catch (Exception ignored) {
                runOnUiThread(() -> { if (generation == previewGeneration) status.setText("Pratinjau tidak tersedia. Coba video lain atau ekspor langsung."); });
            }
        });
    }

    private Bitmap getCover(MediaMetadataRetriever retriever, int index, long startMs, long endMs) {
        String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        if (duration == null) throw new IllegalArgumentException("Durasi video tidak tersedia");
        long millis = Long.parseLong(duration);
        if (millis <= 0) throw new IllegalArgumentException("Durasi video tidak valid");
        long clipEnd = endMs > 0 ? Math.min(endMs, millis) : millis;
        if (clipEnd <= startMs) throw new IllegalArgumentException("Durasi potongan tidak valid");
        long microseconds = (startMs + (clipEnd - startMs) * (index + 1) / 4) * 1000;
        Bitmap frame = retriever.getScaledFrameAtTime(microseconds, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 1600, 1600);
        if (frame == null) throw new IllegalArgumentException("Frame tidak bisa dibaca");
        return frame;
    }

    private void convert(Uri source, int index, boolean correction, boolean smoothing,
                         boolean stabilizeVideo, long startMs, long endMs) throws Exception {
        File temp = File.createTempFile("motion_", ".mp4", getCacheDir());
        long declaredSize = -1;
        try (Cursor cursor = getContentResolver().query(source, new String[]{OpenableColumns.SIZE}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst() && !cursor.isNull(0)) declaredSize = cursor.getLong(0);
        } catch (RuntimeException ignored) { /* Some document providers omit size; stream limit still applies. */ }
        final long expectedSize = declaredSize;
        Uri destination = null;
        File processed = null;
        try {
            long length = 0;
            try (InputStream in = getContentResolver().openInputStream(source);
                 OutputStream out = new FileOutputStream(temp)) {
                if (in == null) throw new IllegalStateException("Video tidak bisa dibuka");
                byte[] buffer = new byte[65536];
                int count;
                while ((count = in.read(buffer)) != -1) {
                    length += count;
                    if (length > MAX_BYTES) throw new IllegalArgumentException("Video melebihi 300 MB");
                    out.write(buffer, 0, count);
                    if (expectedSize > 0) postProgress("Menyalin video", (int) Math.min(10, length * 10 / expectedSize));
                }
            }
            postProgress("Video siap", 10);
            if (endMs <= startMs) throw new IllegalArgumentException("Pilih bagian video untuk dipangkas");
            {
                processed = new File(getCacheDir(), "processed_" + java.util.UUID.randomUUID() + ".mp4");
                new VideoProcessor(this).process(temp, processed, startMs, endMs, stabilizeVideo,
                        (stage, percent) -> postProgress(stage, 10 + percent * 80 / 100));
                temp.delete();
                temp = processed;
                length = processed.length();
                processed = null;
            }
            postProgress("Menyiapkan foto sampul", 90);
            byte[] jpeg;
            try (MediaMetadataRetriever retriever = new MediaMetadataRetriever()) {
                retriever.setDataSource(temp.getAbsolutePath());
                String mime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE);
                if (!"video/mp4".equalsIgnoreCase(mime)) throw new IllegalArgumentException("Hanya MP4 yang didukung");
                Bitmap frame = getCover(retriever, index, 0, 0);
                Bitmap adjusted = CoverProcessor.process(frame, correction, smoothing);
                if (adjusted != frame) frame.recycle();
                try (ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
                    try {
                        if (!adjusted.compress(Bitmap.CompressFormat.JPEG, 90, bytes)) throw new IllegalStateException("Gagal mengode JPEG");
                        jpeg = bytes.toByteArray();
                    } finally { adjusted.recycle(); }
                }
            }
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "MP_" + System.currentTimeMillis() + ".jpg");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MotionPhoto");
            values.put(MediaStore.Images.Media.IS_PENDING, 1);
            destination = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (destination == null) throw new IllegalStateException("Tidak bisa membuat foto di galeri");
            postProgress("Menyimpan Motion Photo", 92);
            final long outputLength = length;
            try (InputStream video = new FileInputStream(temp);
                 ParcelFileDescriptor fd = getContentResolver().openFileDescriptor(destination, "w")) {
                if (fd == null) throw new IllegalStateException("Tidak bisa menulis foto");
                try (OutputStream output = new FileOutputStream(fd.getFileDescriptor())) {
                    MotionPhotoWriter.write(jpeg, video, outputLength, output,
                            copied -> postProgress("Menyimpan Motion Photo", 92 + (int) (copied * 7 / outputLength)));
                    output.flush();
                }
            }
            ContentValues ready = new ContentValues();
            ready.put(MediaStore.Images.Media.IS_PENDING, 0);
            if (getContentResolver().update(destination, ready, null, null) != 1) throw new IllegalStateException("Gagal memublikasikan foto");
            destination = null;
        } finally {
            if (destination != null) getContentResolver().delete(destination, null, null);
            if (!temp.delete() && temp.exists()) temp.deleteOnExit();
            if (processed != null && processed.exists()) processed.delete();
        }
    }

    private void postProgress(String stage, int value) {
        int bounded = Math.max(0, Math.min(100, value));
        if (bounded == lastProgressValue && stage.equals(lastProgressStage)) return;
        lastProgressValue = bounded;
        lastProgressStage = stage;
        runOnUiThread(() -> {
            if (isDestroyed()) return;
            int percent = bounded;
            progressMeter.setPercent(percent);
            String eta = "perkiraan belum tersedia";
            if (percent >= 5 && percent < 100) {
                long elapsed = Math.max(1, SystemClock.elapsedRealtime() - startedAtMs);
                long remainingSeconds = Math.min(3600, elapsed * (100 - percent) / percent / 1000);
                eta = "perkiraan ±" + (remainingSeconds < 60 ? remainingSeconds + " detik" : (remainingSeconds / 60 + 1) + " menit");
            }
            status.setText(stage + " · " + percent + "%" + (percent < 100 ? " · " + eta : ""));
        });
    }

    private void refreshStyles() {
        for (int i = 0; i < coverButtons.length; i++) coverButtons[i].setBackground(shape(i == coverIndex ? INK : 0xffffffff, 14, 0xffe4e4dd));
        for (int i = 0; i < coverButtons.length; i++) coverButtons[i].setTextColor(i == coverIndex ? Color.WHITE : INK);
        enhanceButton.setBackground(shape(enhance ? INK : Color.WHITE, 14, 0xffe4e4dd));
        enhanceButton.setTextColor(enhance ? Color.WHITE : INK);
        denoiseButton.setBackground(shape(denoise ? INK : Color.WHITE, 14, 0xffe4e4dd));
        denoiseButton.setTextColor(denoise ? Color.WHITE : INK);
        stabilizeButton.setText(stabilize ? "Stabilkan video · aktif" : "Stabilkan video · nonaktif");
        stabilizeButton.setBackground(shape(stabilize ? INK : Color.WHITE, 14, 0xffe4e4dd));
        stabilizeButton.setTextColor(stabilize ? Color.WHITE : INK);
    }

    private TextView action(String value, boolean primary) {
        TextView view = text(value, 15, primary ? Color.WHITE : INK, true);
        view.setGravity(Gravity.CENTER);
        view.setMinHeight(dp(50));
        view.setBackground(shape(primary ? 0xffd45c4f : Color.WHITE, 14, 0xffe4e4dd));
        view.setClickable(true);
        view.setFocusable(true);
        return view;
    }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value); view.setTextSize(size); view.setTextColor(color);
        if (bold) view.setTypeface(null, 1);
        return view;
    }
    private GradientDrawable shape(int color, int radius, int stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color); drawable.setCornerRadius(dp(radius));
        if (stroke != 0) drawable.setStroke(dp(1), stroke);
        return drawable;
    }
    private LinearLayout.LayoutParams margin(int top, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(top); params.bottomMargin = dp(bottom);
        return params;
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    @Override protected void onDestroy() { worker.shutdown(); super.onDestroy(); }
}
