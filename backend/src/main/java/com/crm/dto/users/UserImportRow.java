package com.crm.dto.users;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class UserImportRow implements Serializable {
    private static final long serialVersionUID = 1L;

    private int rowNumber;
    private String username;
    private String fullName;
    private String email;
    private String phone;
    private String roleName;
    private String teamName;
    private String dataScope;
    private String password;

    private boolean valid = true;
    private List<String> errorMessages = new ArrayList<>();

    private Long resolvedTeamId;
    private Long resolvedRoleId;

    public UserImportRow() {
    }

    public UserImportRow(int rowNumber) {
        this.rowNumber = rowNumber;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(int rowNumber) {
        this.rowNumber = rowNumber;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getDataScope() {
        return dataScope;
    }

    public void setDataScope(String dataScope) {
        this.dataScope = dataScope;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public List<String> getErrorMessages() {
        return errorMessages;
    }

    public void setErrorMessages(List<String> errorMessages) {
        this.errorMessages = errorMessages;
    }

    public void addError(String error) {
        this.valid = false;
        if (this.errorMessages == null) {
            this.errorMessages = new ArrayList<>();
        }
        this.errorMessages.add(error);
    }

    public Long getResolvedTeamId() {
        return resolvedTeamId;
    }

    public void setResolvedTeamId(Long resolvedTeamId) {
        this.resolvedTeamId = resolvedTeamId;
    }

    public Long getResolvedRoleId() {
        return resolvedRoleId;
    }

    public void setResolvedRoleId(Long resolvedRoleId) {
        this.resolvedRoleId = resolvedRoleId;
    }
}
