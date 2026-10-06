import com.crm.service.profile.AvatarService;
import com.google.gson.Gson;
import jakarta.servlet.http.Part;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.zip.CRC32;

/** DEV-only synthetic image fixtures and pure image checks; never reads or writes a user account. */
public final class AvatarImageFixture {
    private static int checks;

    public static void main(String[] args) {
        try {
            if (args.length == 2 && "generate".equals(args[0])) {
                // Pipe synthetic image bytes into the test process; do not print this result to logs.
                System.out.print(Base64.getEncoder().encodeToString(generate(args[1])));
            } else if (args.length == 1 && "inspect".equals(args[0])) {
                byte[] bytes = System.in.readNBytes(2 * 1024 * 1024 + 1);
                if (bytes.length > 2 * 1024 * 1024) throw new IllegalArgumentException();
                System.out.print(new Gson().toJson(inspect(bytes)));
            } else if (args.length == 1 && "check".equals(args[0])) {
                check();
                System.out.println("PASS " + checks + " DEV avatar image checks; no DAO access or account mutation.");
            } else {
                throw new IllegalArgumentException();
            }
        } catch (Exception | AssertionError e) {
            System.err.println("DEV avatar fixture or validation check failed; no image or account data logged.");
            System.exit(1);
        }
    }

    private static byte[] generate(String mode) throws IOException {
        if ("corrupt-png".equals(mode)) {
            return Arrays.copyOf(generate("png-alpha"), 24);
        }
        if ("oversized-pixels".equals(mode)) {
            byte[] bytes = generate("png-alpha");
            ByteBuffer.wrap(bytes).putInt(16, 4097).putInt(20, 4097);
            CRC32 crc = new CRC32();
            crc.update(bytes, 12, 17);
            ByteBuffer.wrap(bytes).putInt(29, (int) crc.getValue());
            return bytes;
        }

        boolean alpha = "png-alpha".equals(mode);
        boolean tooWide = "oversized-dimensions".equals(mode);
        String format = switch (mode) {
            case "png-alpha", "oversized-dimensions" -> "png";
            case "jpg" -> "jpg";
            case "gif" -> "gif";
            default -> throw new IllegalArgumentException();
        };
        BufferedImage image = new BufferedImage(tooWide ? 8193 : 96, tooWide ? 1 : 64,
                alpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            if (!alpha) {
                graphics.setColor(new Color(238, 242, 247));
                graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            }
            graphics.setColor(new Color(82, 119, 180, alpha ? 128 : 255));
            graphics.fillRect(24, 16, 48, 32);
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (!ImageIO.write(image, format, output)) throw new IOException();
        return output.toByteArray();
    }

    private static Map<String, Object> inspect(byte[] bytes) throws IOException {
        try (var imageInput = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) throw new IllegalArgumentException();
            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInput, true, true);
                BufferedImage image = reader.read(0);
                Map<String, Object> info = new LinkedHashMap<>();
                info.put("format", reader.getFormatName().toLowerCase(Locale.ROOT));
                info.put("width", image.getWidth());
                info.put("height", image.getHeight());
                info.put("hasAlpha", image.getColorModel().hasAlpha());
                info.put("alphaTopLeft", image.getRGB(0, 0) >>> 24);
                info.put("alphaCenter", image.getRGB(image.getWidth() / 2, image.getHeight() / 2) >>> 24);
                return info;
            } finally {
                reader.dispose();
            }
        }
    }

    private static Part part(byte[] bytes, String mime, long declaredSize, boolean brokenStream) {
        return (Part) Proxy.newProxyInstance(Part.class.getClassLoader(), new Class<?>[]{Part.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getSize" -> declaredSize;
                    case "getContentType" -> mime;
                    case "getInputStream" -> {
                        if (brokenStream) throw new IOException();
                        yield new ByteArrayInputStream(bytes);
                    }
                    default -> null;
                });
    }

    private static void check() throws Exception {
        AvatarService service = new AvatarService();
        byte[] png = generate("png-alpha"), jpg = generate("jpg");
        reject(() -> service.upload(0, null));
        reject(() -> service.upload(0, part(new byte[0], "image/png", 0, false)));
        reject(() -> service.upload(0, part(png, "application/pdf", png.length, false)));
        reject(() -> service.upload(0, part(png, "image/png", 2 * 1024 * 1024 + 1, false)));

        Method decode = AvatarService.class.getDeclaredMethod("readImage", Part.class, String.class);
        Method resize = AvatarService.class.getDeclaredMethod("resize", BufferedImage.class, int.class, int.class);
        decode.setAccessible(true);
        resize.setAccessible(true);
        reject(() -> invoke(decode, service, part(generate("gif"), "image/png", 1024, false), "png"));
        reject(() -> invoke(decode, service, part(generate("corrupt-png"), "image/png", 24, false), "png"));
        reject(() -> invoke(decode, service, part(generate("oversized-dimensions"), "image/png", 1024, false), "png"));
        reject(() -> invoke(decode, service, part(generate("oversized-pixels"), "image/png", 1024, false), "png"));
        reject(() -> invoke(decode, service, part(png, "image/jpeg", png.length, false), "jpeg"));
        reject(() -> invoke(decode, service, part(png, "image/png", png.length, true), "png"));

        BufferedImage decodedPng = (BufferedImage) invoke(decode, service, part(png, "image/png", png.length, false), "png");
        verify(decodedPng.getWidth() == 96 && decodedPng.getHeight() == 64);
        BufferedImage decodedJpg = (BufferedImage) invoke(decode, service, part(jpg, "image/jpeg", jpg.length, false), "jpeg");
        verify(decodedJpg.getWidth() == 96 && decodedJpg.getHeight() == 64 && !decodedJpg.getColorModel().hasAlpha());
        BufferedImage square = decodedPng.getSubimage(16, 0, 64, 64);
        for (int side : new int[]{512, 128}) {
            BufferedImage image = (BufferedImage) invoke(resize, service, square, side, side);
            verify(image.getWidth() == side && image.getHeight() == side && image.getColorModel().hasAlpha());
            verify((image.getRGB(0, 0) >>> 24) == 0 && (image.getRGB(side / 2, side / 2) >>> 24) == 128);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            verify(ImageIO.write(image, "png", output));
            Map<String, Object> persisted = inspect(output.toByteArray());
            verify(Boolean.TRUE.equals(persisted.get("hasAlpha"))
                    && Integer.valueOf(0).equals(persisted.get("alphaTopLeft"))
                    && Integer.valueOf(128).equals(persisted.get("alphaCenter")));
        }
    }

    private static Object invoke(Method method, Object target, Object... args) throws Exception {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Exception cause) throw cause;
            throw new AssertionError();
        }
    }

    private static void reject(Check check) throws Exception {
        try {
            check.run();
        } catch (IllegalArgumentException e) {
            checks++;
            return;
        }
        throw new AssertionError("Invalid image accepted");
    }

    private static void verify(boolean success) {
        if (!success) throw new AssertionError("Image validation mismatch");
        checks++;
    }

    private interface Check { void run() throws Exception; }
}
