package com.crm.service.users;

import org.junit.jupiter.api.Test;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Arrays;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

class AvatarImageProcessorTest {
    private final AvatarImageProcessor processor = new AvatarImageProcessor();
    static byte[] image(String format, int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var g = image.createGraphics();
        g.setColor(Color.RED); g.fillRect(0, 0, width, height);
        g.dispose();
        var out = new ByteArrayOutputStream();
        ImageIO.write(image, format, out);
        return out.toByteArray();
    }
    private AvatarImageProcessor.Images process(byte[] bytes, String name) throws Exception {
        return processor.process(new ByteArrayInputStream(bytes), name, bytes.length);
    }
    @Test void acceptsJpgJpegPngAndAllOrientations() throws Exception {
        for (String name : new String[]{"photo.jpg", "PHOTO.JPEG", "photo.png"}) {
            for (int[] size : new int[][]{{600,300}, {300,600}, {300,300}}) {
                var result = process(image(name.endsWith("png") ? "png" : "jpg", size[0], size[1]), name);
                assertEquals(512, result.image().getWidth());
                assertEquals(512, result.image().getHeight());
                assertEquals(128, result.thumbnail().getWidth());
                assertEquals(128, result.thumbnail().getHeight());
            }
        }
    }
    @Test void acceptsExactlyTwoMiBRejectsOneByteMoreEvenWithFalseDeclaredSize() throws Exception {
        byte[] exact = Arrays.copyOf(image("png", 10, 10), AvatarImageProcessor.MAX_BYTES);
        assertNotNull(process(exact, "test.png"));
        byte[] large = Arrays.copyOf(exact, exact.length + 1);
        assertEquals(413, assertThrows(AvatarException.class, () -> process(large, "test.png")).getStatus());
        assertEquals(413, assertThrows(AvatarException.class, () -> processor.process(new ByteArrayInputStream(large), "test.png", 1)).getStatus());
    }
    @Test void rejectsOtherFormatsSpoofedNamesMismatchEmptyAndCorruptImages() throws Exception {
        for (String name : new String[]{"x.pdf", "x.gif", "x.exe", "x.svg", "x"}) {
            assertThrows(AvatarException.class, () -> process(image("png", 10, 10), name));
        }
        for (byte[] bytes : new byte[][]{"%PDF-1.0".getBytes(), "MZfake".getBytes(), image("gif",10,10), new byte[0]}) {
            assertThrows(AvatarException.class, () -> process(bytes, "fake.jpg"));
        }
        assertThrows(AvatarException.class, () -> process(image("png",10,10), "fake.jpg"));
        assertThrows(AvatarException.class, () -> process(Arrays.copyOf(image("png",10,10), 40), "broken.png"));
    }
    @Test void cropsCenterRatherThanStretching() throws Exception {
        for (boolean landscape : new boolean[]{true, false}) {
            BufferedImage source = new BufferedImage(landscape ? 300 : 100, landscape ? 100 : 300, BufferedImage.TYPE_INT_RGB);
            var g = source.createGraphics();
            g.setColor(Color.RED); g.fillRect(0,0,source.getWidth(),source.getHeight());
            g.setColor(Color.BLUE); g.fillRect(landscape ? 100 : 0, landscape ? 0 : 100,100,100); g.dispose();
            var out = new ByteArrayOutputStream(); ImageIO.write(source,"png",out);
            var result = process(out.toByteArray(), "center.png");
            assertEquals(Color.BLUE.getRGB(), result.image().getRGB(0,0));
            assertEquals(Color.BLUE.getRGB(), result.image().getRGB(511,511));
        }
    }
    @Test void rejectsExcessiveDecodedPixels() throws Exception {
        byte[] bytes = image("png", 4001, 4000);
        assertTrue(bytes.length < AvatarImageProcessor.MAX_BYTES);
        assertThrows(AvatarException.class, () -> process(bytes, "large.png"));
    }
}
