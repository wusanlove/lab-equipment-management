package cn.edu.ctgu.labms.domain;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * 系统安全审计日志实体
 */
public class AuditLog {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String logId;
    private final String operatorId;
    private final String action;
    private final String targetId;
    private final String detail;
    private final LocalDateTime timestamp;

    public AuditLog(String logId, String operatorId, String action,
                    String targetId, String detail, LocalDateTime timestamp) {
        this.logId = Objects.requireNonNull(logId, "logId cannot be null");
        this.operatorId = Objects.requireNonNull(operatorId, "operatorId cannot be null");
        this.action = Objects.requireNonNull(action, "action cannot be null");
        this.targetId = Objects.requireNonNull(targetId, "targetId cannot be null");
        this.detail = Objects.requireNonNull(detail, "detail cannot be null");
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp cannot be null");
    }

    public String getLogId() {
        return logId;
    }

    public String getOperatorId() {
        return operatorId;
    }

    public String getAction() {
        return action;
    }

    public String getTargetId() {
        return targetId;
    }

    public String getDetail() {
        return detail;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return String.format("[%s] Log#%s | 操作人:%s | 动作:%s | 目标:%s | 详情:%s",
                timestamp.format(FORMATTER), logId, operatorId, action, targetId, detail);
    }
}
