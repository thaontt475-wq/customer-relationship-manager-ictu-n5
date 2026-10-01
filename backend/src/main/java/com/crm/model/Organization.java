package com.crm.model;

import java.util.ArrayList;
import java.util.List;

public class Organization {

    private long id;
    private String name;
    private Long parentId;
    private Long managerId;
    private String managerName;
    private String managerRole;
    private String region;
    private boolean active;
    private int memberCount;
    private List<Member> members = new ArrayList<>();

    public Organization() {
    }

    public Organization(long id, String name, Long parentId, Long managerId,
                        String managerName, String managerRole, String region,
                        boolean active, int memberCount, List<Member> members) {
        this.id = id;
        this.name = name;
        this.parentId = parentId;
        this.managerId = managerId;
        this.managerName = managerName;
        this.managerRole = managerRole;
        this.region = region;
        this.active = active;
        this.memberCount = memberCount;
        setMembers(members);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Long getManagerId() {
        return managerId;
    }

    public void setManagerId(Long managerId) {
        this.managerId = managerId;
    }

    public String getManagerName() {
        return managerName;
    }

    public void setManagerName(String managerName) {
        this.managerName = managerName;
    }

    public String getManagerRole() {
        return managerRole;
    }

    public void setManagerRole(String managerRole) {
        this.managerRole = managerRole;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }

    public List<Member> getMembers() {
        if (members == null) {
            members = new ArrayList<>();
        }
        return members;
    }

    public void setMembers(List<Member> members) {
        this.members = members == null ? new ArrayList<>() : new ArrayList<>(members);
    }

    public static class Member {

        private long id;
        private String name;
        private String email;
        private String role;
        private String joinedDate;

        public Member() {
        }

        public Member(long id, String name, String email, String role, String joinedDate) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.role = role;
            this.joinedDate = joinedDate;
        }

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getJoinedDate() {
            return joinedDate;
        }

        public void setJoinedDate(String joinedDate) {
            this.joinedDate = joinedDate;
        }
    }
}
