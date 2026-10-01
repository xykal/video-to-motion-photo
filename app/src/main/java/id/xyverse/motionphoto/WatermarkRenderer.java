package id.xyverse.motionphoto;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Path;
import android.media.MediaMetadataRetriever;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Draws a translucent editorial badge; not a realtime background blur/refraction. */
public final class WatermarkRenderer {
    public static final class Info {
        public final String camera;
        public final String resolution;
        public final String frameMegapixels;
        public final String recordedAt;
        public Info(String camera, String resolution, String frameMegapixels, String recordedAt) {
            this.camera = camera == null ? "tidak terdata" : camera;
            this.resolution = resolution;
            this.frameMegapixels = frameMegapixels;
            this.recordedAt = recordedAt;
        }
    }
    private WatermarkRenderer() {}

    public static Info inspect(MediaMetadataRetriever retriever, String camera) {
        int width = parse(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
        int height = parse(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
        int rotation = parse(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION));
        if (rotation == 90 || rotation == 270) { int swap = width; width = height; height = swap; }
        String dimensions = width > 0 && height > 0 ? width + "×" + height : "Resolusi tak tersedia";
        String megapixels = width > 0 && height > 0
                ? String.format(Locale.US, "%.1f MP frame", (double) width * height / 1000000) : "MP tak tersedia";
        String date = "Waktu rekam tak tersedia";
        String raw = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE);
        if (raw != null && raw.matches("[0-9]{8}T[0-9]{6}(\\.[0-9]+)?Z")) {
            try {
                String iso = raw.substring(0, 4) + "-" + raw.substring(4, 6) + "-" + raw.substring(6, 8)
                        + "T" + raw.substring(9, 11) + ":" + raw.substring(11, 13) + ":"
                        + raw.substring(13, 15) + "Z";
                date = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm 'UTC'", Locale.forLanguageTag("id-ID"))
                        .withZone(ZoneOffset.UTC).format(Instant.parse(iso));
            } catch (Exception ignored) { /* No verifiable original recording time. */ }
        }
        return new Info(camera, dimensions, megapixels, date);
    }

    private static int parse(String input) {
        try { return input == null ? 0 : Integer.parseInt(input); }
        catch (NumberFormatException ignored) { return 0; }
    }

    public static Bitmap makeBadge(int width, Info info) {
        width = Math.max(290, Math.min(800, width));
        int height = Math.round(width * .22f);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        RectF frame = new RectF(4, 4, width - 4, height - 4);
        float radius = height * .25f;
        paint.setShadowLayer(8, 0, 5, 0x66000000);
        paint.setColor(0x86192024);
        canvas.drawRoundRect(frame, radius, radius, paint);
        paint.clearShadowLayer();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        paint.setColor(0x99ffffff);
        canvas.drawRoundRect(frame, radius, radius, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x66ffffff);
        canvas.drawRoundRect(new RectF(18, 12, width - 18, 17), 3, 3, paint);
        float scale = width / 800f;
        paint.setColor(0xffd45c4f);
        canvas.drawCircle(42 * scale, 47 * scale, 16 * scale, paint);
        paint.setColor(0xffffffff);
        paint.setTextSize(27 * scale);
        paint.setFakeBoldText(true);
        canvas.drawText("MOTION  /  ORIGINAL", 72 * scale, 54 * scale, paint);
        paint.setFakeBoldText(false);
        paint.setTextSize(24 * scale);
        String camera = "Kamera asal: " + info.camera;
        while (paint.measureText(camera) > width - 64 * scale && camera.length() > 18) {
            camera = camera.substring(0, camera.length() - 2) + "…";
        }
        canvas.drawText(camera, 32 * scale, 100 * scale, paint);
        paint.setColor(0xffdae2e1);
        paint.setTextSize(21 * scale);
        String details = info.resolution + "  ·  " + info.frameMegapixels;
        canvas.drawText(details, 32 * scale, 137 * scale, paint);
        paint.setTextSize(18 * scale);
        canvas.drawText(info.recordedAt, 32 * scale, 162 * scale, paint);
        return bitmap;
    }

    public static Bitmap stampCover(Bitmap cover, Info info) {
        Bitmap output = cover.copy(Bitmap.Config.ARGB_8888, true);
        Bitmap badge = makeBadge(Math.min(800, Math.max(290, output.getWidth() - 32)), info);
        Canvas canvas = new Canvas(output);
        float factor = Math.min(1f, (output.getWidth() - 24f) / badge.getWidth());
        int x = 12, y = Math.max(0, Math.round(output.getHeight() - 12 - badge.getHeight() * factor));
        int blurWidth = Math.min(output.getWidth() - x, Math.round(badge.getWidth() * factor));
        int blurHeight = Math.min(output.getHeight() - y, Math.round(badge.getHeight() * factor));
        if (blurWidth > 16 && blurHeight > 16) {
            Bitmap region = Bitmap.createBitmap(output, x, y, blurWidth, blurHeight);
            Bitmap small = Bitmap.createScaledBitmap(region, Math.max(4, blurWidth / 12), Math.max(4, blurHeight / 12), true);
            Bitmap blurred = Bitmap.createScaledBitmap(small, blurWidth, blurHeight, true);
            canvas.save();
            Path rounded = new Path();
            rounded.addRoundRect(new RectF(x, y, x + blurWidth, y + blurHeight),
                    blurHeight / 4f, blurHeight / 4f, Path.Direction.CW);
            canvas.clipPath(rounded);
            canvas.drawBitmap(blurred, x, y, null);
            canvas.restore();
            region.recycle(); small.recycle(); blurred.recycle();
        }
        canvas.save();
        canvas.translate(x, y);
        canvas.scale(factor, factor);
        canvas.drawBitmap(badge, 0, 0, null);
        canvas.restore();
        badge.recycle();
        return output;
    }
}
