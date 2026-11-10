package cn.edu.ctgu.labms.dao;

import cn.edu.ctgu.labms.domain.BorrowRecord;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 设备借还记录数据访问对象（内存仓储实现）
 */
public class BorrowRecordDao {
    private final Map<String, BorrowRecord> storage = new LinkedHashMap<>();
    private final AtomicInteger seq = new AtomicInteger(200);

    public String nextRecordId() {
        return "BR-2026-" + seq.incrementAndGet();
    }

    public BorrowRecord save(BorrowRecord record) {
        storage.put(record.getRecordId(), record);
        return record;
    }

    public Optional<BorrowRecord> findById(String recordId) {
        return Optional.ofNullable(storage.get(recordId));
    }

    public List<BorrowRecord> findByEquipmentCode(String equipmentCode) {
        return storage.values().stream()
                .filter(r -> r.getEquipmentCode().equals(equipmentCode))
                .collect(Collectors.toList());
    }

    public Optional<BorrowRecord> findActiveByEquipmentCode(String equipmentCode) {
        return storage.values().stream()
                .filter(r -> r.getEquipmentCode().equals(equipmentCode) && !r.isReturned())
                .findFirst();
    }

    public List<BorrowRecord> findByBorrowerId(String borrowerId) {
        return storage.values().stream()
                .filter(r -> r.getBorrowerId().equals(borrowerId))
                .collect(Collectors.toList());
    }

    public List<BorrowRecord> findAll() {
        return new ArrayList<>(storage.values());
    }

    public void clear() {
        storage.clear();
        seq.set(200);
    }
}
