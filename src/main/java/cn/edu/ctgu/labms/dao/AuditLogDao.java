package cn.edu.ctgu.labms.dao;

import cn.edu.ctgu.labms.domain.AuditLog;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 系统安全审计日志数据访问对象（内存仓储实现）
 */
public class AuditLogDao {
    private final List<AuditLog> logs = new ArrayList<>();
    private final AtomicInteger seq = new AtomicInteger(5000);

    public AuditLog append(String operatorId, String action, String targetId, String detail) {
        String id = "LOG-" + seq.incrementAndGet();
        AuditLog entry = new AuditLog(id, operatorId, action, targetId, detail, LocalDateTime.now());
        logs.add(entry);
        return entry;
    }

    public List<AuditLog> findByOperator(String operatorId) {
        return logs.stream()
                .filter(l -> l.getOperatorId().equals(operatorId))
                .collect(Collectors.toList());
    }

    public List<AuditLog> findByAction(String action) {
        return logs.stream()
                .filter(l -> l.getAction().equalsIgnoreCase(action))
                .collect(Collectors.toList());
    }

    public List<AuditLog> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(logs));
    }

    public void clear() {
        logs.clear();
        seq.set(5000);
    }
}
