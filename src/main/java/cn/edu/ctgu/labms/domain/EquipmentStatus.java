package cn.edu.ctgu.labms.domain;

/**
 * 实验设备全生命周期状态枚举
 */
public enum EquipmentStatus {
    IN_STOCK("在库可用"),
    BORROWED("已借出"),
    MAINTENANCE("维修中"),
    SCRAPPED("已报废");

    private final String displayName;

    EquipmentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * 校验从当前状态转移到目标状态是否合法
     */
    public boolean canTransitionTo(EquipmentStatus target) {
        if (target == null || this == target) {
            return false;
        }
        switch (this) {
            case IN_STOCK:
                return target == BORROWED || target == MAINTENANCE || target == SCRAPPED;
            case BORROWED:
                return target == IN_STOCK || target == MAINTENANCE;
            case MAINTENANCE:
                return target == IN_STOCK || target == SCRAPPED;
            case SCRAPPED:
                // 已报废为终态，不可再流转到任何其他状态
                return false;
            default:
                return false;
        }
    }
}
