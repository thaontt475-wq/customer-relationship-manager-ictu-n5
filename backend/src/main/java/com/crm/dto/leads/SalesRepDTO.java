package com.crm.dto.leads;

public class SalesRepDTO {
    private String id;
    private String name;
    private String initials;
    private String title;
    private String dept;
    private String region;
    private int currentLeads;
    private int maxCapacity = 25;
    private boolean isOnline = true;

    public SalesRepDTO() {
    }

    public SalesRepDTO(String id, String name, String initials, String title, String dept, String region, int currentLeads, int maxCapacity, boolean isOnline) {
        this.id = id;
        this.name = name;
        this.initials = initials;
        this.title = title;
        this.dept = dept;
        this.region = region;
        this.currentLeads = currentLeads;
        this.maxCapacity = maxCapacity;
        this.isOnline = isOnline;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInitials() {
        return initials;
    }

    public void setInitials(String initials) {
        this.initials = initials;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDept() {
        return dept;
    }

    public void setDept(String dept) {
        this.dept = dept;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public int getCurrentLeads() {
        return currentLeads;
    }

    public void setCurrentLeads(int currentLeads) {
        this.currentLeads = currentLeads;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(int maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }
}
