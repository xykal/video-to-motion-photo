package id.xyverse.motionphoto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/** Writes a JPEG APP1 XMP packet and appends an unmodified MP4 at EOF. */
public final class MotionPhotoWriter {
    private static final byte[] XMP_ID = "http://ns.adobe.com/xap/1.0/\0".getBytes(StandardCharsets.US_ASCII);

    private MotionPhotoWriter() {}

    public static void write(byte[] jpeg, InputStream mp4, long mp4Length, OutputStream out) throws IOException {
        if (jpeg.length < 4 || (jpeg[0] & 255) != 255 || (jpeg[1] & 255) != 216 ||
                (jpeg[jpeg.length - 2] & 255) != 255 || (jpeg[jpeg.length - 1] & 255) != 217) {
            throw new IOException("JPEG tidak valid");
        }
        if (mp4Length < 12) throw new IOException("MP4 kosong atau terlalu kecil");
        byte[] head = new byte[12];
        readFully(mp4, head);
        if (head[4] != 'f' || head[5] != 't' || head[6] != 'y' || head[7] != 'p') {
            throw new IOException("Video bukan MP4 (ftyp tidak ditemukan)");
        }
        String xml = "<x:xmpmeta xmlns:x=\"adobe:ns:meta/\"><rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">"
                + "<rdf:Description xmlns:Camera=\"http://ns.google.com/photos/1.0/camera/\" "
                + "Camera:MotionPhoto=\"1\" Camera:MotionPhotoVersion=\"1\" "
                + "Camera:MotionPhotoPresentationTimestampUs=\"0\" "
                + "Camera:MicroVideo=\"1\" Camera:MicroVideoVersion=\"1\" "
                + "Camera:MicroVideoOffset=\"" + mp4Length + "\" "
                + "xmlns:Container=\"http://ns.google.com/photos/1.0/container/\" "
                + "xmlns:Item=\"http://ns.google.com/photos/1.0/container/item/\">"
                + "<Container:Directory><rdf:Seq>"
                + "<rdf:li rdf:parseType=\"Resource\"><Container:Item Item:Mime=\"image/jpeg\" Item:Semantic=\"Primary\" Item:Padding=\"0\"/></rdf:li>"
                + "<rdf:li rdf:parseType=\"Resource\"><Container:Item Item:Mime=\"video/mp4\" Item:Semantic=\"MotionPhoto\" Item:Length=\"" + mp4Length + "\"/></rdf:li>"
                + "</rdf:Seq></Container:Directory></rdf:Description></rdf:RDF></x:xmpmeta>";
        ByteArrayOutputStream packet = new ByteArrayOutputStream();
        packet.write(XMP_ID);
        packet.write(xml.getBytes(StandardCharsets.UTF_8));
        int size = packet.size() + 2;
        if (size > 65535) throw new IOException("Metadata terlalu besar");
        out.write(jpeg, 0, 2);
        out.write(255); out.write(225);
        out.write(size >> 8); out.write(size & 255);
        packet.writeTo(out);
        out.write(jpeg, 2, jpeg.length - 2);
        out.write(head);
        long remaining = mp4Length - head.length;
        byte[] buffer = new byte[65536];
        while (remaining > 0) {
            int count = mp4.read(buffer, 0, (int) Math.min(buffer.length, remaining));
            if (count < 0) throw new IOException("MP4 terpotong saat disalin");
            out.write(buffer, 0, count);
            remaining -= count;
        }
    }

    private static void readFully(InputStream in, byte[] bytes) throws IOException {
        int offset = 0;
        while (offset < bytes.length) {
            int count = in.read(bytes, offset, bytes.length - offset);
            if (count < 0) throw new IOException("Video terpotong");
            offset += count;
        }
    }
}
