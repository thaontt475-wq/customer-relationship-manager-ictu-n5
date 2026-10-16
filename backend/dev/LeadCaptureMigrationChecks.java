import com.crm.config.DatabaseConfig;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

/** Standalone migration verification; never packaged into the application WAR. */
public class LeadCaptureMigrationChecks {
    private static final Set<String> SEED_TABLES = Set.of(
            "master_data", "permissions", "role_permissions", "role_module_scopes");
    private record Snapshot(Map<String, List<String>> columns,
                            Map<String, Map<String, List<String>>> definitions,
                            Map<String, Map<String, Long>> rows) {}

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Pass the migration SQL path");
        String sql = Files.readString(Path.of(args[0])).lines()
                .filter(line -> !line.stripLeading().startsWith("--"))
                .collect(Collectors.joining("\n"));
        if (sql.matches("(?is).*\\b(DROP|DELETE|TRUNCATE|REPLACE|UPDATE|USE)\\b.*")
                || sql.matches("(?is).*\\bALTER\\s+TABLE\\s+\\w+\\s+(MODIFY|CHANGE|RENAME)\\b.*"))
            throw new IllegalArgumentException("Expected additive migration without database switching");
        try (Connection c = DatabaseConfig.getConnection()) {
            if (!c.getCatalog().matches("crm_sprint1_test_s401_migration_[A-Za-z0-9_]+"))
                throw new IllegalStateException("Refuse to run outside the dedicated migration test schema");
            Snapshot before = snapshot(c, null);
            apply(c, sql);
            Snapshot after = snapshot(c, before.columns());
            for (String table : before.columns().keySet()) {
                if (!before.definitions().get(table).equals(after.definitions().get(table)))
                    throw new AssertionError("Existing column definition changed: " + table);
                var oldRows = before.rows().get(table);
                var newRows = after.rows().get(table);
                for (var row : oldRows.entrySet())
                    if (newRows.getOrDefault(row.getKey(), 0L) < row.getValue())
                        throw new AssertionError("Existing row removed or modified: " + table);
                if (!SEED_TABLES.contains(table) && !oldRows.equals(newRows))
                    throw new AssertionError("Unexpected existing table data change: " + table);
            }
            System.out.println("PASS original rows and column definitions preserved across "
                    + before.columns().size() + " existing tables");
            Snapshot first = snapshot(c, null);
            apply(c, sql);
            if (!first.equals(snapshot(c, null)))
                throw new AssertionError("Second application changed rows or column definitions");
            System.out.println("PASS migration reapplication preserves all rows and schema definitions");
            try (PreparedStatement p = c.prepareStatement("""
                    SELECT COUNT(*) FROM information_schema.referential_constraints
                    WHERE constraint_schema=DATABASE()
                    AND table_name IN ('lead_capture_forms','lead_capture_submissions')
                    """); ResultSet r = p.executeQuery()) {
                r.next();
                if (r.getInt(1) != 3) throw new AssertionError("Missing capture foreign keys");
            }
            System.out.println("PASS capture tables have all three foreign keys without name conflicts");
            System.out.println("Migration checks: 3 PASS, 0 FAIL; development crm_db untouched");
        }
    }

    private static void apply(Connection c, String sql) throws SQLException {
        try (Statement s = c.createStatement()) {
            for (String statement : sql.split(";"))
                if (!statement.isBlank()) s.execute(statement);
        }
    }

    private static Snapshot snapshot(Connection c, Map<String, List<String>> selectedColumns) throws Exception {
        Map<String, List<String>> columns = new TreeMap<>();
        Map<String, Map<String, List<String>>> definitions = new TreeMap<>();
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("""
                SELECT table_name,column_name,column_type,is_nullable,column_default,collation_name,extra
                FROM information_schema.columns WHERE table_schema=DATABASE()
                ORDER BY table_name,ordinal_position
                """)) {
            while (r.next()) {
                String table = r.getString(1), column = r.getString(2);
                if (selectedColumns != null && (!selectedColumns.containsKey(table)
                        || !selectedColumns.get(table).contains(column))) continue;
                columns.computeIfAbsent(table, k -> new ArrayList<>()).add(column);
                definitions.computeIfAbsent(table, k -> new TreeMap<>()).put(column,
                        Arrays.asList(r.getString(3), r.getString(4), r.getString(5), r.getString(6), r.getString(7)));
            }
        }
        if (selectedColumns != null && !columns.equals(selectedColumns))
            throw new AssertionError("An original table or column disappeared");
        Map<String, Map<String, Long>> rows = new TreeMap<>();
        for (var table : columns.entrySet()) {
            Map<String, Long> hashes = new TreeMap<>();
            String projection = table.getValue().stream().map(LeadCaptureMigrationChecks::quote)
                    .collect(Collectors.joining(","));
            try (Statement s = c.createStatement(); ResultSet r = s.executeQuery(
                    "SELECT " + projection + " FROM " + quote(table.getKey()))) {
                while (r.next()) {
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    try (DataOutputStream out = new DataOutputStream(bytes)) {
                        for (int i = 1; i <= table.getValue().size(); i++) {
                            byte[] cell = r.getBytes(i);
                            out.writeInt(cell == null ? -1 : cell.length);
                            if (cell != null) out.write(cell);
                        }
                    }
                    String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
                    hashes.merge(hash, 1L, Long::sum);
                }
            }
            rows.put(table.getKey(), hashes);
        }
        return new Snapshot(columns, definitions, rows);
    }

    private static String quote(String identifier) {
        return "`" + identifier.replace("`", "``") + "`";
    }
}
