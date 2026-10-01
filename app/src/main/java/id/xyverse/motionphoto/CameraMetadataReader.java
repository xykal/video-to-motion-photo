package id.xyverse.motionphoto;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Strict, bounded ISO-BMFF/QuickTime metadata reader. Never infers camera model. */
public final class CameraMetadataReader {
    private static final long MAX_METADATA_BYTES = 2L * 1024 * 1024;
    private CameraMetadataReader() {}

    public static String readCamera(RandomAccessFile file) throws IOException {
        Reader reader = new Reader(file);
        reader.walk(0, file.length(), 0);
        return reader.camera();
    }

    private static final class Reader {
        private final RandomAccessFile file;
        private final Map<Integer, String> keys = new HashMap<>();
        private final Map<Integer, String> values = new HashMap<>();
        private String make, model;
        private long scanned;

        Reader(RandomAccessFile file) { this.file = file; }

        void walk(long start, long end, int depth) throws IOException {
            if (depth > 5 || start < 0 || end > file.length()) return;
            long pos = start;
            while (pos <= end - 8) {
                file.seek(pos);
                long size = Integer.toUnsignedLong(file.readInt());
                int tag = file.readInt();
                long header = 8;
                if (size == 1) { if (pos > end - 16) return; size = file.readLong(); header = 16; }
                if (size == 0) size = end - pos;
                if (size < header || size > end - pos) return;
                long content = pos + header, boxEnd = pos + size;
                if (tag == fourcc("moov") || tag == fourcc("udta") || tag == fourcc("trak")) {
                    walk(content, boxEnd, depth + 1);
                } else if (tag == fourcc("meta") && boxEnd - content >= 4) {
                    walk(content + 4, boxEnd, depth + 1);
                } else if (tag == fourcc("keys") && boxEnd - content <= MAX_METADATA_BYTES) {
                    parseKeys(content, boxEnd);
                } else if (tag == fourcc("ilst") && boxEnd - content <= MAX_METADATA_BYTES) {
                    parseItems(content, boxEnd);
                }
                pos = boxEnd;
            }
        }

        private void parseKeys(long pos, long end) throws IOException {
            if (end - pos < 8) return;
            scanned += end - pos;
            if (scanned > MAX_METADATA_BYTES) return;
            file.seek(pos + 4);
            long count = Integer.toUnsignedLong(file.readInt());
            for (int i = 1; i <= count && i <= 256 && file.getFilePointer() <= end - 8; i++) {
                long entry = file.getFilePointer();
                long size = Integer.toUnsignedLong(file.readInt());
                int namespace = file.readInt();
                if (size < 8 || size > 256 || size > end - entry) return;
                byte[] bytes = new byte[(int) size - 8];
                file.readFully(bytes);
                if (namespace == fourcc("mdta")) keys.put(i, new String(bytes, StandardCharsets.UTF_8));
            }
        }

        private void parseItems(long pos, long end) throws IOException {
            scanned += end - pos;
            if (scanned > MAX_METADATA_BYTES) return;
            while (pos <= end - 16) {
                file.seek(pos);
                long size = Integer.toUnsignedLong(file.readInt());
                int tag = file.readInt();
                if (size < 16 || size > end - pos) return;
                long inner = pos + 8, itemEnd = pos + size;
                while (inner <= itemEnd - 16) {
                    file.seek(inner);
                    long dataSize = Integer.toUnsignedLong(file.readInt());
                    int type = file.readInt();
                    if (dataSize < 16 || dataSize > itemEnd - inner) break;
                    if (type == fourcc("data") && dataSize <= 256) {
                        file.skipBytes(8); // data type and locale
                        byte[] bytes = new byte[(int) dataSize - 16];
                        file.readFully(bytes);
                        String value = safeText(bytes);
                        if (value != null) {
                            if (tag == 0xa96d616b) make = value; // QuickTime make tag
                            else if (tag == 0xa96d6f64) model = value; // QuickTime model tag
                            else values.put(tag, value); // mdta index, resolved after keys
                        }
                    }
                    inner += dataSize;
                }
                pos = itemEnd;
            }
        }

        String camera() {
            for (Map.Entry<Integer, String> item : values.entrySet()) {
                String key = keys.get(item.getKey());
                if (key == null) continue;
                if (key.equals("com.apple.quicktime.make")) make = item.getValue();
                else if (key.equals("com.apple.quicktime.model")) model = item.getValue();
            }
            if (make == null && model == null) return null;
            if (make == null) return model;
            if (model == null || model.toLowerCase(java.util.Locale.ROOT).startsWith(make.toLowerCase(java.util.Locale.ROOT))) return model == null ? make : model;
            return make + " " + model;
        }
    }

    private static String safeText(byte[] bytes) {
        String value = new String(bytes, StandardCharsets.UTF_8).trim();
        if (value.length() < 2 || value.length() > 64 || !value.matches("[\\p{L}\\p{N} ._+\\-/]+")) return null;
        return value;
    }
    private static int fourcc(String text) {
        byte[] bytes = text.getBytes(StandardCharsets.ISO_8859_1);
        return ((bytes[0] & 255) << 24) | ((bytes[1] & 255) << 16) | ((bytes[2] & 255) << 8) | (bytes[3] & 255);
    }
}
