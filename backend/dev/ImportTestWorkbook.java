import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.UUID;

/** DEV fixture generator; caller captures stdout into memory, never logs or persists the workbook. */
public final class ImportTestWorkbook {
    public static void main(String[] args) {
        try {
            if (args.length != 2) throw new IllegalArgumentException();
            String mode = args[0], email = args[1];
            String password = "Import-" + UUID.randomUUID() + "A9";
            try (Workbook book = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                Sheet sheet = book.createSheet("Users");
                String[] headers = {"fullName", "email", "password", "status"};
                Row header = sheet.createRow(0);
                for (int i = 0; i < headers.length; i++) header.createCell(i).setCellValue(mode.equals("missing-header") && i == 1 ? "wrongColumn" : headers[i]);
                row(sheet, 1, "Sprint 2 import disposable", email, password, "ACTIVE");
                if (mode.equals("mixed")) {
                    row(sheet, 2, "Duplicate in file", email, password, "ACTIVE");
                    row(sheet, 3, "Malformed email", "invalid-email", password, "ACTIVE");
                } else if (!mode.equals("valid") && !mode.equals("missing-header")) throw new IllegalArgumentException();
                book.write(output);
                // The orchestrator pipes this binary fixture; no main account credentials are included.
                System.out.print(Base64.getEncoder().encodeToString(output.toByteArray()));
            }
        } catch (Exception e) { System.err.println("DEV fixture generation failed; no secret details logged."); System.exit(1); }
    }
    private static void row(Sheet sheet, int index, String name, String email, String password, String status) {
        Row row = sheet.createRow(index);
        String[] values = {name, email, password, status};
        for (int i = 0; i < values.length; i++) row.createCell(i).setCellValue(values[i]);
    }
}
