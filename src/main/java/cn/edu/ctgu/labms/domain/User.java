package cn.edu.ctgu.labms.domain;

import java.util.Objects;

/**
 * 系统用户实体
 */
public class User {
    private final String id;
    private final String username;
    private String passwordHash;
    private String realName;
    private Role role;
    private String department;
    private boolean active;

    public User(String id, String username, String passwordHash, String realName,
                Role role, String department, boolean active) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.username = Objects.requireNonNull(username, "username cannot be null");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash cannot be null");
        this.realName = Objects.requireNonNull(realName, "realName cannot be null");
        this.role = Objects.requireNonNull(role, "role cannot be null");
        this.department = Objects.requireNonNull(department, "department cannot be null");
        this.active = active;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = Objects.requireNonNull(passwordHash);
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = Objects.requireNonNull(realName);
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = Objects.requireNonNull(role);
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = Objects.requireNonNull(department);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return String.format("User{id='%s', username='%s', realName='%s', role=%s, dept='%s', active=%s}",
                id, username, realName, role.getDisplayName(), department, active ? "启用" : "禁用");
    }
}
