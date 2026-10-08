package com.crm.dto.customers;

/** Confirmation accepts only the server-side preview token, never client-supplied rows. */
public class CustomerImportConfirmRequest {
    public String batchToken;
    public String duplicateMode;
}
