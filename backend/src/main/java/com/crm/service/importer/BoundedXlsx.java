package com.crm.service.importer;

import java.io.*;
import java.util.Locale;
import java.util.zip.*;

/** Shared OOXML upload guard; never evaluates formulas or executes workbook content. */
public final class BoundedXlsx {
    public static final int MAX_BYTES = 5 * 1024 * 1024;
    private BoundedXlsx() {}

    public static byte[] read(InputStream input) throws IOException {
        byte[] bytes = input.readNBytes(MAX_BYTES + 1);
        if (bytes.length == 0) throw new IllegalArgumentException("File Excel rỗng");
        if (bytes.length > MAX_BYTES) throw new IllegalArgumentException("File Excel tối đa 5MB");
        long total = 0;
        int entries = 0;
        boolean contentTypes = false;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            byte[] buffer = new byte[8192];
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (++entries > 1000) throw new IllegalArgumentException("File Excel có quá nhiều thành phần");
                String name = entry.getName().toLowerCase(Locale.ROOT);
                if (entry.getName().equals("[Content_Types].xml")) contentTypes = true;
                if (name.contains("vbaproject") || name.contains("externallinks/"))
                    throw new IllegalArgumentException("Không chấp nhận macro hoặc liên kết ngoài trong Excel");
                long size = 0;
                int count;
                while ((count = zip.read(buffer)) != -1) {
                    total += count;
                    size += count;
                    if (total > 64L * 1024 * 1024 || size > 20L * 1024 * 1024)
                        throw new IllegalArgumentException("Nội dung giải nén Excel vượt giới hạn");
                }
            }
        } catch (ZipException error) {
            throw new IllegalArgumentException("File .xlsx không hợp lệ", error);
        }
        if (!contentTypes) throw new IllegalArgumentException("File phải có định dạng Excel .xlsx");
        return bytes;
    }
}
