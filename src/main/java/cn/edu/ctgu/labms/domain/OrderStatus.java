package cn.edu.ctgu.labms.domain;

/**
 * 申购单及工单审批流转状态枚举
 */
public enum OrderStatus {
    PENDING("待审批/待处理"),
    APPROVED("已批准"),
    REJECTED("已驳回"),
    COMPLETED("已完成/已入库");

    private final String displayName;

    OrderStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
