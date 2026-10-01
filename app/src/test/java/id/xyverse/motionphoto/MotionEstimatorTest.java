package id.xyverse.motionphoto;

import org.junit.Test;
import static org.junit.Assert.*;

public final class MotionEstimatorTest {
    @Test public void detectsKnownTranslation() {
        int[] first = new int[96 * 96], second = new int[96 * 96];
        for (int y = 10; y < 86; y++) for (int x = 10; x < 86; x++) {
            int value = ((x * 131 + y * 367 + x * y * 7) ^ (x * y)) & 255;
            first[y * 96 + x] = value;
            second[(y + 3) * 96 + x - 2] = value;
        }
        assertArrayEquals(new int[]{-2, 3}, MotionEstimator.shift(first, second, 96, 96));
    }

    @Test public void foregroundMovementDoesNotDominateBackground() {
        int[] first = new int[96 * 96], second = new int[96 * 96];
        for (int y = 8; y < 88; y++) for (int x = 8; x < 88; x++) {
            int value = ((x * 131 + y * 367 + x * y * 7) ^ (x * y)) & 255;
            first[y * 96 + x] = value;
            second[(y + 2) * 96 + x + 1] = value;
        }
        for (int y = 16; y < 34; y++) for (int x = 14; x < 32; x++) second[y * 96 + x] = 255;
        assertArrayEquals(new int[]{1, 2}, MotionEstimator.shift(first, second, 96, 96));
    }

    @Test public void smoothingReturnsNonzeroCorrectionForSingleJolt() {
        float[][] result = MotionEstimator.corrections(new float[]{0, 0, 4, 0, 0}, new float[5], 2);
        assertTrue(result[0][2] < 0);
    }
}
