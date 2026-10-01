package id.xyverse.motionphoto;

/** Coarse global translation estimator for handheld shake (not rotation or rolling shutter). */
public final class MotionEstimator {
    private MotionEstimator() {}

    public static int[] luma(int[] argb) {
        int[] values = new int[argb.length];
        for (int i = 0; i < argb.length; i++) {
            int c = argb[i];
            values[i] = (((c >> 16) & 255) * 54 + ((c >> 8) & 255) * 183 + (c & 255) * 19) >> 8;
        }
        return values;
    }

    public static int[] shift(int[] previous, int[] current, int width, int height) {
        int best = Integer.MAX_VALUE, bestX = 0, bestY = 0;
        int border = 9;
        for (int dy = -5; dy <= 5; dy++) {
            for (int dx = -5; dx <= 5; dx++) {
                int error = 0;
                for (int y = border; y < height - border; y += 3) {
                    for (int x = border; x < width - border; x += 3) {
                        error += Math.abs(previous[y * width + x] - current[(y + dy) * width + x + dx]);
                    }
                }
                if (error < best) { best = error; bestX = dx; bestY = dy; }
            }
        }
        return new int[]{bestX, bestY};
    }

    public static float[][] corrections(float[] x, float[] y, int radius) {
        float[][] corrections = new float[2][x.length];
        for (int i = 0; i < x.length; i++) {
            float sx = 0, sy = 0;
            int count = 0;
            for (int j = Math.max(0, i - radius); j <= Math.min(x.length - 1, i + radius); j++) {
                sx += x[j]; sy += y[j]; count++;
            }
            corrections[0][i] = sx / count - x[i];
            corrections[1][i] = sy / count - y[i];
        }
        return corrections;
    }
}
