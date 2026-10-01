package id.xyverse.motionphoto;

import org.junit.Test;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

public final class MotionPhotoWriterTest {
    private static final byte[] JPEG = {(byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xd9};
    private static final byte[] VIDEO = {0, 0, 0, 12, 'f', 't', 'y', 'p', 'i', 's', 'o', 'm'};

    @Test public void createsXmpAndKeepsVideoAtEnd() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        MotionPhotoWriter.write(JPEG, new ByteArrayInputStream(VIDEO), VIDEO.length, output);
        byte[] result = output.toByteArray();
        String content = new String(result, StandardCharsets.ISO_8859_1);
        assertTrue(content.contains("Camera:MotionPhoto=\"1\""));
        assertTrue(content.contains("Item:Length=\"12\""));
        assertEquals(0xff, result[0] & 255);
        assertEquals(0xd8, result[1] & 255);
        assertArrayEquals(VIDEO, java.util.Arrays.copyOfRange(result, result.length - VIDEO.length, result.length));
    }

    @Test(expected = IOException.class) public void rejectsShortVideo() throws Exception {
        MotionPhotoWriter.write(JPEG, new ByteArrayInputStream(VIDEO), 11, new ByteArrayOutputStream());
    }

    @Test(expected = IOException.class) public void rejectsIncorrectVideoHeader() throws Exception {
        MotionPhotoWriter.write(JPEG, new ByteArrayInputStream(new byte[12]), 12, new ByteArrayOutputStream());
    }
}
