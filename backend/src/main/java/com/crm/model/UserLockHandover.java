package com.crm.model;

import java.time.LocalDateTime;

public class UserLockHandover {
    private long id;
    private long targetUserId;
    private long recipientUserId;
    private long lockedByUserId;
    private String lockReason;
    private LocalDateTime createdAt;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getTargetUserId() { return targetUserId; }
    public void setTargetUserId(long targetUserId) { this.targetUserId = targetUserId; }
    public long getRecipientUserId() { return recipientUserId; }
    public void setRecipientUserId(long recipientUserId) { this.recipientUserId = recipientUserId; }
    public long getLockedByUserId() { return lockedByUserId; }
    public void setLockedByUserId(long lockedByUserId) { this.lockedByUserId = lockedByUserId; }
    public String getLockReason() { return lockReason; }
    public void setLockReason(String lockReason) { this.lockReason = lockReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
