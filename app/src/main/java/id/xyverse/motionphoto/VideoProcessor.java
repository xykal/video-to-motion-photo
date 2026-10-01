package id.xyverse.motionphoto;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.MediaMetadataRetriever;
import android.os.Handler;
import android.os.Looper;
import android.net.Uri;
import androidx.media3.common.Effect;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.effect.MatrixTransformation;
import androidx.media3.transformer.Composition;
import androidx.media3.transformer.DefaultEncoderFactory;
import androidx.media3.transformer.EditedMediaItem;
import androidx.media3.transformer.Effects;
import androidx.media3.transformer.ExportException;
import androidx.media3.transformer.ExportResult;
import androidx.media3.transformer.TransformationRequest;
import androidx.media3.transformer.Transformer;
import androidx.media3.transformer.VideoEncoderSettings;
import java.io.File;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@UnstableApi
public final class VideoProcessor {
    private static final long MAX_DURATION_MS = 30000;
    private static final int SAMPLE_MS = 200;
    private final Context context;

    public VideoProcessor(Context context) { this.context = context.getApplicationContext(); }

    public void stabilize(File source, File output) throws Exception {
        final float[][] offsets;
        final int bitrate;
        try (MediaMetadataRetriever retriever = new MediaMetadataRetriever()) {
            retriever.setDataSource(source.getAbsolutePath());
            String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (duration == null) throw new IllegalArgumentException("Durasi video tidak diketahui");
            long durationMs = Long.parseLong(duration);
            if (durationMs <= 0 || durationMs > MAX_DURATION_MS) {
                throw new IllegalArgumentException("Stabilisasi saat ini mendukung video 1–30 detik");
            }
            String w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            String h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            if (w == null || h == null) throw new IllegalArgumentException("Dimensi video tidak tersedia");
            long pixels = (long) Integer.parseInt(w) * Integer.parseInt(h);
            bitrate = pixels > 1920L * 1080 ? 25000000 : pixels > 1280L * 720 ? 12000000 : 8000000;
            int samples = (int) (durationMs / SAMPLE_MS) + 1;
            float[] x = new float[samples], y = new float[samples];
            int[] previous = null;
            for (int i = 0; i < samples; i++) {
                long timeUs = Math.min(durationMs - 1, (long) i * SAMPLE_MS) * 1000;
                Bitmap bitmap = retriever.getScaledFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST, 128, 128);
                if (bitmap == null) throw new IllegalArgumentException("Frame video tidak bisa dianalisis");
                Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 96, 96, true);
                if (scaled != bitmap) bitmap.recycle();
                int[] pixelsData = new int[96 * 96];
                scaled.getPixels(pixelsData, 0, 96, 0, 0, 96, 96);
                scaled.recycle();
                int[] current = MotionEstimator.luma(pixelsData);
                if (previous != null) {
                    int[] shift = MotionEstimator.shift(previous, current, 96, 96);
                    x[i] = x[i - 1] + shift[0];
                    y[i] = y[i - 1] + shift[1];
                }
                previous = current;
            }
            offsets = MotionEstimator.corrections(x, y, 5);
        }
        MatrixTransformation motion = presentationTimeUs -> {
            float position = Math.max(0, Math.min(offsets[0].length - 1, presentationTimeUs / (SAMPLE_MS * 1000f)));
            int first = (int) position, second = Math.min(first + 1, offsets[0].length - 1);
            float blend = position - first;
            float dx = offsets[0][first] * (1 - blend) + offsets[0][second] * blend;
            float dy = offsets[1][first] * (1 - blend) + offsets[1][second] * blend;
            Matrix matrix = new Matrix();
            matrix.setScale(1.16f, 1.16f);
            matrix.postTranslate(Math.max(-.06f, Math.min(.06f, dx / 48f)),
                    Math.max(-.06f, Math.min(.06f, -dy / 48f)));
            return matrix;
        };
        CountDownLatch completed = new CountDownLatch(1);
        AtomicReference<Exception> failure = new AtomicReference<>();
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                Effect effect = motion;
                EditedMediaItem item = new EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(source)))
                        .setEffects(new Effects(Collections.emptyList(), Collections.singletonList(effect))).build();
                Transformer transformer = new Transformer.Builder(context)
                        .setTransformationRequest(new TransformationRequest.Builder().setVideoMimeType(MimeTypes.VIDEO_H264).build())
                        .setEncoderFactory(new DefaultEncoderFactory.Builder(context)
                                .setRequestedVideoEncoderSettings(new VideoEncoderSettings.Builder().setBitrate(bitrate).build()).build())
                        .addListener(new Transformer.Listener() {
                            @Override public void onCompleted(Composition composition, ExportResult result) { completed.countDown(); }
                            @Override public void onError(Composition composition, ExportResult result, ExportException error) {
                                failure.set(error); completed.countDown();
                            }
                        }).build();
                transformer.start(item, output.getAbsolutePath());
            } catch (Exception error) { failure.set(error); completed.countDown(); }
        });
        if (!completed.await(8, TimeUnit.MINUTES)) throw new IllegalStateException("Pemrosesan terlalu lama. Coba video lebih pendek");
        if (failure.get() != null) throw failure.get();
        if (!output.isFile() || output.length() < 12) throw new IllegalStateException("Video hasil kosong");
    }
}
