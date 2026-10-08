package com.crm.service.profile;

import com.crm.dao.profile.ProfileDAO;
import com.crm.service.audit.AuditLogService;
import com.crm.util.JsonUtil;
import jakarta.servlet.http.Part;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class AvatarService {

    private static final long MAX_SIZE =
            2L * 1024 * 1024;

    // Reject highly compressed, oversized images before allocating decoded pixels.
    private static final int MAX_IMAGE_DIMENSION = 8192;
    private static final long MAX_IMAGE_PIXELS = 16_000_000L;

    private final ProfileDAO dao =
            new ProfileDAO();

    private final AuditLogService audit =
            new AuditLogService();

    public Map<String, Object> upload(
            long userId,
            Part file
    ) throws Exception {

        if (
                file == null ||
                file.getSize() <= 0
        ) {
            throw new IllegalArgumentException(
                    "Chưa chọn ảnh"
            );
        }

        if (file.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException(
                    "Ảnh không được vượt quá 2MB"
            );
        }

        String contentType =
                file.getContentType();

        if (
                !"image/jpeg".equalsIgnoreCase(
                        contentType
                ) &&
                !"image/png".equalsIgnoreCase(
                        contentType
                )
        ) {
            throw new IllegalArgumentException(
                    "Chỉ chấp nhận JPG hoặc PNG"
            );
        }

        String expectedFormat =
                "image/png".equalsIgnoreCase(contentType)
                        ? "png"
                        : "jpeg";

        BufferedImage original =
                readImage(file, expectedFormat);

        int side =
                Math.min(
                        original.getWidth(),
                        original.getHeight()
                );

        int x =
                (original.getWidth() - side) / 2;

        int y =
                (original.getHeight() - side) / 2;

        BufferedImage square =
                original.getSubimage(
                        x,
                        y,
                        side,
                        side
                );

        BufferedImage avatar =
                resize(
                        square,
                        512,
                        512
                );

        BufferedImage thumbnail =
                resize(
                        square,
                        128,
                        128
                );

        String extension =
                "image/png".equalsIgnoreCase(
                        contentType
                )
                        ? "png"
                        : "jpg";

        String id =
                UUID.randomUUID()
                        .toString();

        Path directory =
                Paths.get(
                        System.getProperty(
                                "user.home"
                        ),
                        "crm_uploads",
                        "avatars"
                );

        Files.createDirectories(
                directory
        );

        String avatarName =
                id + "." + extension;

        String thumbName =
                id + "_thumb." + extension;

        Path avatarPath =
                directory.resolve(
                        avatarName
                );

        Path thumbPath =
                directory.resolve(
                        thumbName
                );

        boolean avatarWritten = ImageIO.write(
                avatar,
                extension.equals("jpg")
                        ? "jpg"
                        : "png",
                avatarPath.toFile()
        );

        boolean thumbnailWritten = ImageIO.write(
                thumbnail,
                extension.equals("jpg")
                        ? "jpg"
                        : "png",
                thumbPath.toFile()
        );

        if (!avatarWritten || !thumbnailWritten) {
            throw new IOException("Avatar image writer unavailable");
        }

        String avatarUrl =
                "/crm/uploads/avatars/" +
                avatarName;

        String thumbnailUrl =
                "/crm/uploads/avatars/" +
                thumbName;

        Map<String, Object> before =
                dao.findByUserId(
                        userId
                );

        dao.updateAvatar(
                userId,
                avatarUrl,
                thumbnailUrl
        );

        Map<String, Object> after =
                dao.findByUserId(
                        userId
                );

        audit.log(
                userId,
                "USER",
                String.valueOf(
                        userId
                ),
                "UPDATE_AVATAR",
                "Cập nhật ảnh đại diện",
                JsonUtil.getGson()
                        .toJson(before),
                JsonUtil.getGson()
                        .toJson(after)
        );

        Map<String, Object> data =
                new LinkedHashMap<>();

        data.put(
                "avatarUrl",
                avatarUrl
        );

        data.put(
                "thumbnailUrl",
                thumbnailUrl
        );

        return data;
    }

    private BufferedImage readImage(
            Part file,
            String expectedFormat
    ) {
        try (
                InputStream input = file.getInputStream();
                ImageInputStream imageInput = new MemoryCacheImageInputStream(input)
        ) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("File ảnh không hợp lệ");
            }

            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!"jpeg".equals(format) && !"png".equals(format)) {
                    throw new IllegalArgumentException("Chỉ chấp nhận ảnh JPG hoặc PNG thực tế");
                }
                if (!format.equals(expectedFormat)) {
                    throw new IllegalArgumentException("Định dạng ảnh không khớp JPG/PNG đã chọn");
                }

                reader.setInput(imageInput, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0) {
                    throw new IllegalArgumentException("File ảnh không hợp lệ");
                }
                if (width > MAX_IMAGE_DIMENSION || height > MAX_IMAGE_DIMENSION
                        || (long) width * height > MAX_IMAGE_PIXELS) {
                    throw new IllegalArgumentException(
                            "Ảnh tối đa 8192px mỗi chiều và 16 triệu điểm ảnh"
                    );
                }

                BufferedImage original = reader.read(0);
                if (original == null) {
                    throw new IllegalArgumentException("File ảnh không hợp lệ");
                }
                return original;
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("File ảnh không hợp lệ");
        }
    }

    private BufferedImage resize(
            BufferedImage source,
            int width,
            int height
    ) {

        BufferedImage result =
                new BufferedImage(
                        width,
                        height,
                        source.getColorModel().hasAlpha()
                                ? BufferedImage.TYPE_INT_ARGB
                                : BufferedImage.TYPE_INT_RGB
                );

        Graphics2D graphics =
                result.createGraphics();

        graphics.setComposite(AlphaComposite.Src);

        graphics.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR
        );

        graphics.drawImage(
                source,
                0,
                0,
                width,
                height,
                null
        );

        graphics.dispose();

        return result;
    }
}
