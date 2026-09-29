package com.crm.dto.users;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class UserImportResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private int totalRows = 0;
    private int validRows = 0;
    private int errorRows = 0;
    private int importedCount = 0;
    private int skippedCount = 0;

    private boolean executed = false;
    private List<UserImportRow> rows = new ArrayList<>();

    public UserImportResult() {
    }

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getValidRows() {
        return validRows;
    }

    public void setValidRows(int validRows) {
        this.validRows = validRows;
    }

    public int getErrorRows() {
        return errorRows;
    }

    public void setErrorRows(int errorRows) {
        this.errorRows = errorRows;
    }

    public int getImportedCount() {
        return importedCount;
    }

    public void setImportedCount(int importedCount) {
        this.importedCount = importedCount;
    }

    public int getSkippedCount() {
        return skippedCount;
    }

    public void setSkippedCount(int skippedCount) {
        this.skippedCount = skippedCount;
    }

    public boolean isExecuted() {
        return executed;
    }

    public void setExecuted(boolean executed) {
        this.executed = executed;
    }

    public List<UserImportRow> getRows() {
        return rows;
    }

    public void setRows(List<UserImportRow> rows) {
        this.rows = rows;
    }

    public List<UserImportRow> getFailedRows() {
        List<UserImportRow> failed = new ArrayList<>();
        if (rows != null) {
            for (UserImportRow r : rows) {
                if (!r.isValid()) {
                    failed.add(r);
                }
            }
        }
        return failed;
    }

    public List<UserImportRow> getSuccessRows() {
        List<UserImportRow> success = new ArrayList<>();
        if (rows != null) {
            for (UserImportRow r : rows) {
                if (r.isValid()) {
                    success.add(r);
                }
            }
        }
        return success;
    }
}
