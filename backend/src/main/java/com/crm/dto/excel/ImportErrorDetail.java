package com.crm.dto.excel;

/**
 * Detailed error record for a row during Excel import (CRM-32).
 */
public class ImportErrorDetail {
    private int rowNumber;
    private String field;
    private String message;
    private String rawData;

    public ImportErrorDetail() {
    }

    public ImportErrorDetail(int rowNumber, String field, String message, String rawData) {
        this.rowNumber = rowNumber;
        this.field = field;
        this.message = message;
        this.rawData = rawData;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(int rowNumber) {
        this.rowNumber = rowNumber;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRawData() {
        return rawData;
    }

    public void setRawData(String rawData) {
        this.rawData = rawData;
    }
}
