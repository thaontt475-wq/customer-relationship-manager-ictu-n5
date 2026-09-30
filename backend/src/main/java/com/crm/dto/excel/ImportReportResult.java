package com.crm.dto.excel;

import java.util.ArrayList;
import java.util.List;

/**
 * Report summary for Excel import operations (CRM-32).
 * Contains total rows, valid rows, error rows, and detailed error messages.
 */
public class ImportReportResult {
    private int totalRows;
    private int successRows;
    private int failedRows;
    private String batchToken;
    private List<ImportRowData> validRows = new ArrayList<>();
    private List<ImportRowData> errorRows = new ArrayList<>();
    private List<ImportErrorDetail> errors = new ArrayList<>();
    private List<ImportRowData> items = new ArrayList<>();

    public ImportReportResult() {
    }

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getSuccessRows() {
        return successRows;
    }

    public void setSuccessRows(int successRows) {
        this.successRows = successRows;
    }

    public int getFailedRows() {
        return failedRows;
    }

    public void setFailedRows(int failedRows) {
        this.failedRows = failedRows;
    }

    public int getTotal() {
        return totalRows;
    }

    public int getCreated() {
        return successRows;
    }

    public int getSkipped() {
        return failedRows;
    }

    public int getValidCount() {
        return validRows != null ? validRows.size() : successRows;
    }

    public int getErrorCount() {
        return errorRows != null ? errorRows.size() : failedRows;
    }

    public String getBatchToken() {
        return batchToken;
    }

    public void setBatchToken(String batchToken) {
        this.batchToken = batchToken;
    }

    public List<ImportRowData> getValidRows() {
        return validRows;
    }

    public void setValidRows(List<ImportRowData> validRows) {
        this.validRows = validRows != null ? validRows : new ArrayList<>();
    }

    public List<ImportRowData> getErrorRows() {
        return errorRows;
    }

    public void setErrorRows(List<ImportRowData> errorRows) {
        this.errorRows = errorRows != null ? errorRows : new ArrayList<>();
    }

    public List<ImportErrorDetail> getErrors() {
        return errors;
    }

    public void setErrors(List<ImportErrorDetail> errors) {
        this.errors = errors != null ? errors : new ArrayList<>();
    }

    public void addError(ImportErrorDetail error) {
        if (this.errors == null) {
            this.errors = new ArrayList<>();
        }
        this.errors.add(error);
    }

    public List<ImportRowData> getItems() {
        return items;
    }

    public void setItems(List<ImportRowData> items) {
        this.items = items != null ? items : new ArrayList<>();
    }
}
