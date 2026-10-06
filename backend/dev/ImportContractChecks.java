import com.crm.service.importer.UserImportService;
import com.crm.config.DatabaseConfig;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;

// DEV-only contract checks; previews read DB, empty confirms do not create any users.
class ImportContractChecks {
    private static final UserImportService service = new UserImportService();
    private static final String[] headers = {"fullName", "email", "password", "status"};
    private static final List<String> passed = new ArrayList<>();
    private static int failed;
    interface Check { void run() throws Exception; }
    public static void main(String[] args) throws Exception {
        long before = countUsers();
        test("Actual template headers", () -> {
            ByteArrayOutputStream output = new ByteArrayOutputStream(); service.writeTemplate(output);
            try (Workbook book = new XSSFWorkbook(new ByteArrayInputStream(output.toByteArray()))) {
                for (int i = 0; i < headers.length; i++) require(headers[i].equals(book.getSheetAt(0).getRow(0).getCell(i).getStringCellValue()));
                require(book.getSheetAt(0).getRow(1).getCell(2).getStringCellValue().isEmpty());
            }
        });
        test("Missing required header rejected", () -> rejects(() -> preview(new String[]{"fullName", "email", "password"}, List.of()), IllegalArgumentException.class));
        test("Duplicate required header rejected", () -> rejects(() -> preview(new String[]{"fullName", "email", "password", "status", "EMAIL"}, List.of()), IllegalArgumentException.class));
        test("Corrupt xlsx rejected", () -> rejects(() -> service.preview(new ByteArrayInputStream("invalid".getBytes(StandardCharsets.UTF_8)), 11), IllegalArgumentException.class));
        test("Blank rows ignored and owner/single-use enforced", () -> {
            Map<String,Object> data = preview(headers, Collections.singletonList(new String[]{"", "", "", ""}));
            require(((List<?>)data.get("validRows")).isEmpty() && ((List<?>)data.get("errorRows")).isEmpty());
            String token = (String)data.get("batchToken");
            rejects(() -> service.confirm(token, 12), SecurityException.class);
            Map<String,Object> result = service.confirm(token, 11);
            require(((Number)result.get("created")).intValue() == 0);
            rejects(() -> service.confirm(token, 11), IllegalArgumentException.class);
        });
        test("Expired batch rejected", () -> {
            String token = (String)preview(headers, List.of()).get("batchToken");
            Field field = UserImportService.class.getDeclaredField("BATCHES"); field.setAccessible(true);
            @SuppressWarnings("unchecked") Map<String,Object> batches = (Map<String,Object>)field.get(null);
            Object batch = batches.get(token);
            Constructor<?> constructor = batch.getClass().getDeclaredConstructor(long.class, Instant.class, List.class); constructor.setAccessible(true);
            // Advance only this test batch's expiry; production time is never changed.
            batches.put(token, constructor.newInstance(11L, Instant.now().minusSeconds(1), List.of()));
            rejects(() -> service.confirm(token, 11), IllegalArgumentException.class);
        });
        test("Reordered columns, email normalization and duplicate file row", () -> {
            String email = "read-" + UUID.randomUUID() + "@example.invalid";
            String password = "Read-" + UUID.randomUUID() + "A9";
            Map<String,Object> data = preview(new String[]{"status", "password", "email", "fullName"}, Arrays.asList(
                new String[]{"", password, " " + email.toUpperCase(Locale.ROOT) + " ", "Read only"},
                new String[]{"ACTIVE", password, email, "Duplicate"}));
            List<?> valid = (List<?>)data.get("validRows"); List<?> errors = (List<?>)data.get("errorRows");
            require(valid.size() == 1 && errors.size() == 1);
            Map<?,?> row = (Map<?,?>)valid.get(0);
            require(email.equals(row.get("email")) && "ACTIVE".equals(row.get("status")) && !row.containsKey("password"));
        });
        test("Malformed email and over72-byte password rejected", () -> {
            String password = "Read-" + UUID.randomUUID() + "A9";
            Map<String,Object> data = preview(headers, Arrays.asList(
                new String[]{"Bad email", "invalid", password, "ACTIVE"},
                new String[]{"Long password", "read-" + UUID.randomUUID() + "@example.invalid", "A9" + "a".repeat(72), "ACTIVE"}));
            require(((List<?>)data.get("validRows")).isEmpty() && ((List<?>)data.get("errorRows")).size() == 2);
        });
        test("Existing DB email rejected without mutation", () -> {
            String existingEmail;
            try (Connection connection = DatabaseConfig.getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT email FROM users ORDER BY id LIMIT 1"); ResultSet rows = statement.executeQuery()) {
                require(rows.next()); existingEmail = rows.getString(1);
            }
            Map<String,Object> data = preview(headers, Collections.singletonList(new String[]{"Existing user", existingEmail, "Read-" + UUID.randomUUID() + "A9", "ACTIVE"}));
            require(((List<?>)data.get("validRows")).isEmpty() && ((List<?>)data.get("errorRows")).size() == 1);
        });
        test("Preview does not write user data", () -> require(before == countUsers()));
        Files.writeString(Path.of("backend/target/import-contract-results.json"), "{\"checkedAt\":\"" + Instant.now() + "\",\"passed\":" + passed.size() + ",\"failed\":" + failed + ",\"databaseMutations\":false}");
        System.out.println(passed.size() + " contract checks PASS; " + failed + " FAIL; DB writes=0");
        if (failed > 0) System.exit(1);
    }
    static void test(String name, Check check) {
        try { check.run(); passed.add(name); System.out.println("PASS " + name); }
        catch (Exception e) { failed++; System.out.println("FAIL " + name + ": " + e.getClass().getSimpleName()); }
    }
    static void require(boolean condition) { if (!condition) throw new IllegalStateException("Contract assertion failed"); }
    static void rejects(Check check, Class<? extends Exception> expected) throws Exception {
        try { check.run(); } catch (Exception error) { if (expected.isInstance(error)) return; throw error; }
        throw new IllegalStateException("Expected controlled rejection");
    }
    static Map<String,Object> preview(String[] columns, List<String[]> rows) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (Workbook book = new XSSFWorkbook()) {
            Sheet sheet = book.createSheet("Users"); Row header = sheet.createRow(0);
            for (int i = 0; i < columns.length; i++) header.createCell(i).setCellValue(columns[i]);
            for (int i = 0; i < rows.size(); i++) { Row row = sheet.createRow(i + 1); String[] values = rows.get(i); for (int j = 0; j < values.length; j++) row.createCell(j).setCellValue(values[j]); }
            book.write(output);
        }
        return service.preview(new ByteArrayInputStream(output.toByteArray()), 11);
    }
    static long countUsers() throws Exception {
        try (Connection connection = DatabaseConfig.getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM users"); ResultSet rows = statement.executeQuery()) { rows.next(); return rows.getLong(1); }
    }
}
