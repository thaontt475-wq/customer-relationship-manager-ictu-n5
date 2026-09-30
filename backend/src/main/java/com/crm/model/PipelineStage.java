package com.crm.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Entity representing a Pipeline Stage in CRM-47 (Pipeline bán hàng).
 *
 * Managed fields:
 * - name: Tên stage
 * - code: Mã stage
 * - stageOrder: Thứ tự hiển thị trong quy trình (Order, >= 1)
 * - winProbability: Tỷ lệ thắng (Win probability %, 0 - 100%)
 * - requirements: Điều kiện rời stage (Requirements/Exit criteria)
 * - isWon: Giai đoạn thành công (100%)
 * - isLost: Giai đoạn thất bại (0%)
 * - active: Trạng thái kích hoạt
 */
public class PipelineStage implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long pipelineId = 1L;
    private String code;
    private String name;
    private int stageOrder = 1;
    private int winProbability = 0;
    private String requirements;
    private boolean won = false;
    private boolean lost = false;
    private boolean active = true;
    private long opportunityCount = 0;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public PipelineStage() {
    }

    public PipelineStage(Long id, Long pipelineId, String code, String name, int stageOrder,
                         int winProbability, String requirements, boolean won, boolean lost, boolean active) {
        this.id = id;
        this.pipelineId = pipelineId != null ? pipelineId : 1L;
        this.code = code;
        this.name = name;
        this.stageOrder = stageOrder;
        this.winProbability = winProbability;
        this.requirements = requirements;
        this.won = won;
        this.lost = lost;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPipelineId() {
        return pipelineId;
    }

    public void setPipelineId(Long pipelineId) {
        this.pipelineId = pipelineId != null ? pipelineId : 1L;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getStageOrder() {
        return stageOrder;
    }

    public void setStageOrder(int stageOrder) {
        this.stageOrder = stageOrder;
    }

    public int getWinProbability() {
        return winProbability;
    }

    public void setWinProbability(int winProbability) {
        this.winProbability = winProbability;
    }

    public String getRequirements() {
        return requirements;
    }

    public void setRequirements(String requirements) {
        this.requirements = requirements;
    }

    public boolean isWon() {
        return won;
    }

    public void setWon(boolean won) {
        this.won = won;
    }

    public boolean isLost() {
        return lost;
    }

    public void setLost(boolean lost) {
        this.lost = lost;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public long getOpportunityCount() {
        return opportunityCount;
    }

    public void setOpportunityCount(long opportunityCount) {
        this.opportunityCount = opportunityCount;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "PipelineStage{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", stageOrder=" + stageOrder +
                ", winProbability=" + winProbability +
                ", won=" + won +
                ", lost=" + lost +
                ", active=" + active +
                ", opportunityCount=" + opportunityCount +
                '}';
    }
}