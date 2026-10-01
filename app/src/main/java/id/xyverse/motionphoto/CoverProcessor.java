package id.xyverse.motionphoto;

import android.graphics.Bitmap;

/** CPU-only image adjustment for the still cover; never modifies the MP4. */
public final class CoverProcessor {
    private CoverProcessor() {}

    public static Bitmap process(Bitmap source, boolean enhance, boolean denoise) {
        if (!enhance && !denoise) return source;
        int width = source.getWidth(), height = source.getHeight();
        int[] pixels = new int[width * height];
        source.getPixels(pixels, 0, width, 0, 0, width, height);
        if (denoise) {
            int[] filtered = pixels.clone();
            for (int y = 1; y < height - 1; y++) {
                for (int x = 1; x < width - 1; x++) {
                    int index = y * width + x;
                    int center = pixels[index];
                    int red = channel(center, 16) * 4, green = channel(center, 8) * 4;
                    int blue = channel(center, 0) * 4, total = 4;
                    int[] offsets = {-width, width, -1, 1};
                    for (int offset : offsets) {
                        int neighbor = pixels[index + offset];
                        int difference = Math.abs(channel(center, 16) - channel(neighbor, 16))
                                + Math.abs(channel(center, 8) - channel(neighbor, 8))
                                + Math.abs(channel(center, 0) - channel(neighbor, 0));
                        if (difference < 72) {
                            red += channel(neighbor, 16);
                            green += channel(neighbor, 8);
                            blue += channel(neighbor, 0);
                            total++;
                        }
                    }
                    filtered[index] = 0xff000000 | ((red / total) << 16) | ((green / total) << 8) | (blue / total);
                }
            }
            pixels = filtered;
        }
        if (enhance) {
            for (int i = 0; i < pixels.length; i++) {
                int color = pixels[i];
                double r = channel(color, 16), g = channel(color, 8), b = channel(color, 0);
                double luminance = (r * 0.2126 + g * 0.7152 + b * 0.0722);
                r = clamp((r - 127.5) * 1.08 + 133 + (r - luminance) * .12);
                g = clamp((g - 127.5) * 1.08 + 133 + (g - luminance) * .12);
                b = clamp((b - 127.5) * 1.08 + 133 + (b - luminance) * .12);
                pixels[i] = 0xff000000 | (((int) r) << 16) | (((int) g) << 8) | (int) b;
            }
        }
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        result.setPixels(pixels, 0, width, 0, 0, width, height);
        return result;
    }

    private static int channel(int pixel, int shift) { return (pixel >> shift) & 255; }
    private static double clamp(double value) { return Math.max(0, Math.min(255, value)); }
}
