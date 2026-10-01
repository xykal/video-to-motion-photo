package id.xyverse.motionphoto;

import org.junit.Test;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public final class MotionPhotoProgressTest {
    @Test public void reportsBytesCopiedFromVideo() throws Exception {
        byte[] jpeg = {(byte) 255, (byte) 216, (byte) 255, (byte) 217};
        byte[] mp4 = {0, 0, 0, 12, 'f', 't', 'y', 'p', 'i', 's', 'o', 'm'};
        List<Long> counts = new ArrayList<>();
        MotionPhotoWriter.write(jpeg, new ByteArrayInputStream(mp4), mp4.length,
                new ByteArrayOutputStream(), counts::add);
        assertEquals(Long.valueOf(mp4.length), counts.get(counts.size() - 1));
    }
}
