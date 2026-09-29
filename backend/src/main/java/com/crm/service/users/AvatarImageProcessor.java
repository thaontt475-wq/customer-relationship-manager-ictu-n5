package com.crm.service.users;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;

/** Bounded decoding, content validation and center crop; no client MIME trust. */
public class AvatarImageProcessor {
    public static final int MAX_BYTES = 2 * 1024 * 1024;
    public static final long MAX_PIXELS = 16_000_000L;
    public record Images(BufferedImage image, BufferedImage thumbnail) { }

    public Images process(InputStream input, String filename, long declaredSize)
            throws IOException, AvatarException {
        if (declaredSize > MAX_BYTES) throw new AvatarException(413, "Ảnh không được vượt quá 2 MiB (2.097.152 byte).");
        if (input == null || filename == null || filename.isBlank())
            throw new AvatarException(400, "Vui lòng chọn ảnh JPG/JPEG hoặc PNG.");
        String lower = filename.toLowerCase(Locale.ROOT);
        boolean jpeg = lower.endsWith(".jpg") || lower.endsWith(".jpeg");
        boolean png = lower.endsWith(".png");
        if (!jpeg && !png) throw invalid();
        byte[] bytes = input.readNBytes(MAX_BYTES + 1);
        if (bytes.length > MAX_BYTES) throw new AvatarException(413, "Ảnh không được vượt quá 2 MiB (2.097.152 byte).");
        if (bytes.length == 0) throw new AvatarException(400, "File ảnh trống.");
        try (var stream = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw invalid();
            var reader = readers.next();
            try {
                String format = reader.getFormatName();
                if (!(jpeg && "JPEG".equalsIgnoreCase(format)) && !(png && "PNG".equalsIgnoreCase(format))) throw invalid();
                reader.setInput(stream, true, true);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > MAX_PIXELS)
                    throw new AvatarException(400, "Ảnh vượt quá giới hạn 16 triệu điểm ảnh.");
                boolean[] warning = {false};
                reader.addIIOReadWarningListener((source, message) -> warning[0] = true);
                BufferedImage decoded = reader.read(0);
                if (decoded == null || warning[0]) throw invalid();
                BufferedImage square = resizeSquare(decoded, 512);
                return new Images(square, resizeSquare(square, 128));
            } finally { reader.dispose(); }
        } catch (IOException | IllegalArgumentException e) {
            throw invalid();
        }
    }

    private static AvatarException invalid() {
        return new AvatarException(400, "File phải là ảnh JPG/JPEG hoặc PNG hợp lệ, có phần mở rộng đúng với nội dung.");
    }

    private static BufferedImage resizeSquare(BufferedImage source, int size) {
        int side = Math.min(source.getWidth(), source.getHeight());
        int x = (source.getWidth() - side) / 2, y = (source.getHeight() - side) / 2;
        BufferedImage result = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = result.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.drawImage(source, 0, 0, size, size, x, y, x + side, y + side, null);
        } finally { graphics.dispose(); }
        return result;
    }
}
