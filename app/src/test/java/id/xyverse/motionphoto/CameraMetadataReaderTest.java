package id.xyverse.motionphoto;

import org.junit.Test;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

public final class CameraMetadataReaderTest {
    @Test public void readsQuickTimeModelWhenPresent() throws Exception {
        byte[] model = box("\u00a9mod", box("data", concat(new byte[8], "Pixel 9".getBytes(StandardCharsets.UTF_8))));
        byte[] movie = box("moov", box("udta", box("meta", concat(new byte[4], box("ilst", model)))));
        File temp = File.createTempFile("camera_", ".mp4");
        try {
            try (FileOutputStream out = new FileOutputStream(temp)) { out.write(movie); }
            try (RandomAccessFile file = new RandomAccessFile(temp, "r")) {
                assertEquals("Pixel 9", CameraMetadataReader.readCamera(file));
            }
        } finally { assertTrue(temp.delete()); }
    }
    @Test public void missingMetadataIsNotInvented() throws Exception {
        File temp = File.createTempFile("camera_", ".mp4");
        try {
            try (FileOutputStream out = new FileOutputStream(temp)) { out.write(box("mdat", new byte[12])); }
            try (RandomAccessFile file = new RandomAccessFile(temp, "r")) {
                assertNull(CameraMetadataReader.readCamera(file));
            }
        } finally { assertTrue(temp.delete()); }
    }
    private static byte[] box(String tag, byte[] value) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        DataOutputStream writer = new DataOutputStream(output);
        writer.writeInt(8 + value.length);
        if (tag.charAt(0) == '\u00a9') writer.writeByte(0xa9);
        else writer.writeByte((byte) tag.charAt(0));
        for (int i = 1; i < 4; i++) writer.writeByte((byte) tag.charAt(i));
        writer.write(value);
        return output.toByteArray();
    }
    private static byte[] concat(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }
}
