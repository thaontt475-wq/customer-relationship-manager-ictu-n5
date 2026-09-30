package com.crm.dto.excel;

import java.util.ArrayList;
import java.util.List;

/**
 * Data representing a single row parsed from an Excel file during import (CRM-32).
 */
public class ImportRowData {
    private int rowNum;
    private String fullName;
    private String email;
    private String username;
    private String phone;
    private String role;
    private String team;
    private boolean isValid = true;
    private List<String> errors = new ArrayList<>();
    private Long resolvedRoleId;
    private Long resolvedTeamId;
    private Long createdUserId;

    public ImportRowData() {
    }

    public ImportRowData(int rowNum, String fullName, String email, String username, String phone, String role, String team) {
        this.rowNum = rowNum;
        this.fullName = fullName;
        this.email = email;
        this.username = username;
        this.phone = phone;
        this.role = role;
        this.team = team;
    }

    public int getRowNum() {
        return rowNum;
    }

    public void setRowNum(int rowNum) {
        this.rowNum = rowNum;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        this.team = team;
    }

    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean valid) {
        isValid = valid;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
        this.isValid = (errors == null || errors.isEmpty());
    }

    public void addError(String error) {
        if (this.errors == null) {
            this.errors = new ArrayList<>();
        }
        this.errors.add(error);
        this.isValid = false;
    }

    public Long getResolvedRoleId() {
        return resolvedRoleId;
    }

    public void setResolvedRoleId(Long resolvedRoleId) {
        this.resolvedRoleId = resolvedRoleId;
    }

    public Long getResolvedTeamId() {
        return resolvedTeamId;
    }

    public void setResolvedTeamId(Long resolvedTeamId) {
        this.resolvedTeamId = resolvedTeamId;
    }

    public Long getCreatedUserId() {
        return createdUserId;
    }

    public void setCreatedUserId(Long createdUserId) {
        this.createdUserId = createdUserId;
    }
}
