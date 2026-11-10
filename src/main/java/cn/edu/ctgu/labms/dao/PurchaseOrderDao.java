package cn.edu.ctgu.labms.dao;

import cn.edu.ctgu.labms.domain.OrderStatus;
import cn.edu.ctgu.labms.domain.PurchaseOrder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 设备申购单数据访问对象（内存仓储实现）
 */
public class PurchaseOrderDao {
    private final Map<String, PurchaseOrder> storage = new LinkedHashMap<>();
    private final AtomicInteger seq = new AtomicInteger(100);

    public String nextOrderId() {
        return "PO-2026-" + seq.incrementAndGet();
    }

    public PurchaseOrder save(PurchaseOrder order) {
        storage.put(order.getOrderId(), order);
        return order;
    }

    public Optional<PurchaseOrder> findById(String orderId) {
        return Optional.ofNullable(storage.get(orderId));
    }

    public List<PurchaseOrder> findByStatus(OrderStatus status) {
        return storage.values().stream()
                .filter(o -> o.getStatus() == status)
                .collect(Collectors.toList());
    }

    public List<PurchaseOrder> findByApplicant(String applicantId) {
        return storage.values().stream()
                .filter(o -> o.getApplicantId().equals(applicantId))
                .collect(Collectors.toList());
    }

    public List<PurchaseOrder> findAll() {
        return new ArrayList<>(storage.values());
    }

    public void clear() {
        storage.clear();
        seq.set(100);
    }
}
