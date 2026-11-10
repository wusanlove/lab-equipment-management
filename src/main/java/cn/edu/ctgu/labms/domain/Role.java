package cn.edu.ctgu.labms.domain;

/**
 * 系统用户角色枚举（RBAC权限控制）
 */
public enum Role {
    ADMIN("系统管理员"),
    LAB_MANAGER("实验室管理员"),
    TEACHER("指导教师"),
    STUDENT("学生");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
