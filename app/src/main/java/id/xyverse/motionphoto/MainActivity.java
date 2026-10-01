package id.xyverse.motionphoto;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
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
    private static final int PICK_VIDEO = 10;
    private static final long MAX_BYTES = 300L * 1024 * 1024;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private TextView status;
    private Button pick;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 80, 48, 48);
        root.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(this);
        title.setText("Video ke Motion Photo");
        title.setTextSize(26);
        root.addView(title);
        status = new TextView(this);
        status.setText("Pilih MP4 (maks. 300 MB). Foto diambil dari tengah video. Hasil disimpan ke Pictures/MotionPhoto.");
        status.setTextSize(16);
        LinearLayout.LayoutParams spacing = new LinearLayout.LayoutParams(-1, -2);
        spacing.topMargin = 32;
        root.addView(status, spacing);
        pick = new Button(this);
        pick.setText("Pilih video");
        pick.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("video/mp4");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, PICK_VIDEO);
        });
        root.addView(pick, spacing);
        TextView about = new TextView(this);
        about.setText("Tentang · Powered by " + Brand.NAME + "\nOffline; video tidak diunggah. Dukungan galeri berbeda menurut perangkat.");
        root.addView(about, spacing);
        setContentView(root);
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != PICK_VIDEO || result != RESULT_OK || data == null || data.getData() == null) return;
        Uri source = data.getData();
        pick.setEnabled(false);
        status.setText("Memproses video…");
        worker.execute(() -> {
            try {
                convert(source);
                runOnUiThread(() -> status.setText("Selesai. Periksa Pictures/MotionPhoto di galeri."));
            } catch (Exception e) {
                runOnUiThread(() -> status.setText("Gagal: " + e.getMessage()));
            } finally {
                runOnUiThread(() -> pick.setEnabled(true));
            }
        });
    }

    private void convert(Uri source) throws Exception {
        File temp = File.createTempFile("motion_", ".mp4", getCacheDir());
        Uri destination = null;
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
                }
            }
            if (length < 12) throw new IllegalArgumentException("Video kosong");
            byte[] jpeg;
            try (MediaMetadataRetriever retriever = new MediaMetadataRetriever()) {
                retriever.setDataSource(temp.getAbsolutePath());
                String mime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE);
                if (!"video/mp4".equalsIgnoreCase(mime)) throw new IllegalArgumentException("Hanya MP4 yang didukung");
                String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                if (duration == null) throw new IllegalArgumentException("Durasi video tidak tersedia");
                long midpointUs = Math.multiplyExact(Long.parseLong(duration), 500L);
                Bitmap frame = retriever.getFrameAtTime(midpointUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                if (frame == null) throw new IllegalArgumentException("Frame video tidak bisa dibaca");
                try (ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
                    if (!frame.compress(Bitmap.CompressFormat.JPEG, 90, bytes)) throw new IllegalStateException("Gagal mengode JPEG");
                    jpeg = bytes.toByteArray();
                } finally {
                    frame.recycle();
                }
            }
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "MP_" + System.currentTimeMillis() + ".jpg");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MotionPhoto");
            values.put(MediaStore.Images.Media.IS_PENDING, 1);
            destination = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (destination == null) throw new IllegalStateException("Tidak bisa membuat foto di galeri");
            try (InputStream video = new FileInputStream(temp);
                 ParcelFileDescriptor fd = getContentResolver().openFileDescriptor(destination, "w")) {
                if (fd == null) throw new IllegalStateException("Tidak bisa menulis foto");
                try (OutputStream output = new FileOutputStream(fd.getFileDescriptor())) {
                    MotionPhotoWriter.write(jpeg, video, length, output);
                    output.flush();
                }
            }
            ContentValues ready = new ContentValues();
            ready.put(MediaStore.Images.Media.IS_PENDING, 0);
            if (getContentResolver().update(destination, ready, null, null) != 1) {
                throw new IllegalStateException("Gagal memublikasikan foto");
            }
            destination = null;
        } finally {
            if (destination != null) getContentResolver().delete(destination, null, null);
            if (!temp.delete() && temp.exists()) temp.deleteOnExit();
        }
    }

    @Override protected void onDestroy() {
        worker.shutdown();
        super.onDestroy();
    }
}
