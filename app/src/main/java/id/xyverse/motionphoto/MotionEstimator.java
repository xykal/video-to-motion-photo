package id.xyverse.motionphoto;

import java.util.Arrays;

/** Coarse, multi-region global translation estimator; ignores isolated moving subjects. */
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
        if (width < 64 || height < 64 || previous.length != width * height || current.length != previous.length) {
            throw new IllegalArgumentException("Frame analisis tidak valid");
        }
        int[] shiftsX = new int[6], shiftsY = new int[6];
        int reliable = 0;
        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 3; column++) {
                int originX = 14 + column * (width - 44) / 2;
                int originY = 16 + row * (height - 48);
                int best = Integer.MAX_VALUE, bestX = 0, bestY = 0;
                for (int dy = -8; dy <= 8; dy++) {
                    for (int dx = -8; dx <= 8; dx++) {
                        int error = 0;
                        for (int y = 0; y < 18; y += 2) {
                            for (int x = 0; x < 18; x += 2) {
                                int position = (originY + y) * width + originX + x;
                                error += Math.abs(previous[position] - current[position + dy * width + dx]);
                            }
                        }
                        if (error < best) { best = error; bestX = dx; bestY = dy; }
                    }
                }
                if (best < 81 * 45) {
                    shiftsX[reliable] = bestX;
                    shiftsY[reliable] = bestY;
                    reliable++;
                }
            }
        }
        if (reliable < 4) return new int[]{0, 0};
        Arrays.sort(shiftsX, 0, reliable);
        Arrays.sort(shiftsY, 0, reliable);
        return new int[]{shiftsX[reliable / 2], shiftsY[reliable / 2]};
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
