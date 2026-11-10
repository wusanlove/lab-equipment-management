package cn.edu.ctgu.labms.service;

import cn.edu.ctgu.labms.dao.AuditLogDao;
import cn.edu.ctgu.labms.dao.UserDao;
import cn.edu.ctgu.labms.domain.AuditLog;
import cn.edu.ctgu.labms.domain.Role;
import cn.edu.ctgu.labms.domain.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 用户认证、RBAC权限校验与安全审计服务
 */
public class AuthService {
    private final UserDao userDao;
    private final AuditLogDao auditLogDao;

    public AuthService(UserDao userDao, AuditLogDao auditLogDao) {
        this.userDao = Objects.requireNonNull(userDao);
        this.auditLogDao = Objects.requireNonNull(auditLogDao);
    }

    /**
     * 使用SHA-256对明文密码进行摘要计算
     */
    public static String hashPassword(String rawPassword) {
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(encoded.length * 2);
            for (byte b : encoded) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) {
                    hex.append('0');
                }
                hex.append(h);
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * 注册新用户
     */
    public User registerUser(String username, String rawPassword, String realName,
                             Role role, String department) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (realName == null || realName.trim().isEmpty()) {
            throw new IllegalArgumentException("真实姓名不能为空");
        }
        if (role == null) {
            throw new IllegalArgumentException("角色不能为空");
        }
        String normalizedUsername = username.trim();
        if (userDao.findByUsername(normalizedUsername).isPresent()) {
            throw new IllegalArgumentException("用户名已存在: " + normalizedUsername);
        }
        String userId = userDao.nextId();
        User user = new User(
                userId,
                normalizedUsername,
                hashPassword(rawPassword),
                realName.trim(),
                role,
                department == null ? "未分配" : department.trim(),
                true
        );
        userDao.save(user);
        auditLogDao.append(userId, "USER_REGISTER", userId,
                String.format("注册用户 %s(%s), 角色=%s", user.getRealName(), user.getUsername(), role.getDisplayName()));
        return user;
    }

    /**
     * 用户登录认证
     */
    public User login(String username, String rawPassword) {
        if (username == null || rawPassword == null) {
            throw new IllegalArgumentException("用户名或密码不能为空");
        }
        User user = userDao.findByUsername(username.trim())
                .orElseThrow(() -> new SecurityException("登录失败：用户名不存在 [" + username + "]"));
        if (!user.isActive()) {
            auditLogDao.append(user.getId(), "USER_LOGIN_DENIED", user.getId(), "尝试登录已禁用账号");
            throw new SecurityException("登录失败：账号已被禁用 [" + username + "]");
        }
        String expectedHash = hashPassword(rawPassword);
        if (!user.getPasswordHash().equals(expectedHash)) {
            auditLogDao.append(user.getId(), "USER_LOGIN_FAILED", user.getId(), "登录密码错误");
            throw new SecurityException("登录失败：密码错误");
        }
        auditLogDao.append(user.getId(), "USER_LOGIN_SUCCESS", user.getId(),
                String.format("用户 %s(%s) 登录成功", user.getRealName(), user.getRole().getDisplayName()));
        return user;
    }

    /**
     * 获取有效（已启用）用户，若不存在或被禁用则抛出异常
     */
    public User getActiveUserOrThrow(String userId) {
        if (userId == null) {
            throw new IllegalArgumentException("操作人ID不能为空");
        }
        User user = userDao.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + userId));
        if (!user.isActive()) {
            throw new SecurityException("操作被拒绝：用户账号已禁用 [" + user.getUsername() + "]");
        }
        return user;
    }

    /**
     * 校验用户是否具备指定角色集合中的任意一种角色
     */
    public boolean hasRole(String userId, Role... allowedRoles) {
        User user = getActiveUserOrThrow(userId);
        for (Role r : allowedRoles) {
            if (user.getRole() == r) {
                return true;
            }
        }
        return false;
    }

    /**
     * 强制要求用户具备指定角色权限，否则记录审计并抛出 SecurityException
     */
    public User requireRole(String userId, Role... allowedRoles) {
        User user = getActiveUserOrThrow(userId);
        for (Role r : allowedRoles) {
            if (user.getRole() == r) {
                return user;
            }
        }
        String requiredDesc = Arrays.toString(allowedRoles);
        auditLogDao.append(userId, "PERMISSION_DENIED", userId,
                String.format("越权访问拦截：当前角色=%s，所需角色=%s", user.getRole(), requiredDesc));
        throw new SecurityException(String.format("权限不足：用户 %s(%s) 无权执行此操作，需要角色 %s",
                user.getRealName(), user.getRole().getDisplayName(), requiredDesc));
    }

    /**
     * 启用或禁用用户账号（仅系统管理员可操作，且不可禁用自身）
     */
    public User setUserActiveStatus(String adminId, String targetUserId, boolean active) {
        requireRole(adminId, Role.ADMIN);
        if (adminId.equals(targetUserId) && !active) {
            throw new IllegalArgumentException("系统管理员不能禁用自己的账号");
        }
        User target = userDao.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("目标用户不存在: " + targetUserId));
        target.setActive(active);
        userDao.save(target);
        auditLogDao.append(adminId, active ? "USER_ENABLE" : "USER_DISABLE", targetUserId,
                String.format("管理员将用户 %s(%s) 状态变更为 %s",
                        target.getRealName(), target.getUsername(), active ? "启用" : "禁用"));
        return target;
    }

    /**
     * 变更用户角色（仅系统管理员可操作）
     */
    public User changeUserRole(String adminId, String targetUserId, Role newRole) {
        requireRole(adminId, Role.ADMIN);
        Objects.requireNonNull(newRole, "新角色不能为空");
        User target = userDao.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("目标用户不存在: " + targetUserId));
        Role oldRole = target.getRole();
        target.setRole(newRole);
        userDao.save(target);
        auditLogDao.append(adminId, "USER_ROLE_CHANGE", targetUserId,
                String.format("用户 %s 角色由 %s 变更为 %s",
                        target.getRealName(), oldRole.getDisplayName(), newRole.getDisplayName()));
        return target;
    }

    public List<User> findAllUsers() {
        return userDao.findAll();
    }

    public List<AuditLog> getAuditLogs() {
        return auditLogDao.findAll();
    }
}
