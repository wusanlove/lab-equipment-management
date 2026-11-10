package cn.edu.ctgu.labms.dao;

import cn.edu.ctgu.labms.domain.MaintenanceScrapRecord;
import cn.edu.ctgu.labms.domain.OrderStatus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 设备维修与报废记录数据访问对象（内存仓储实现）
 */
public class MaintenanceScrapDao {
    private final Map<String, MaintenanceScrapRecord> storage = new LinkedHashMap<>();
    private final AtomicInteger seq = new AtomicInteger(300);

    public String nextRecordId(MaintenanceScrapRecord.RecordType type) {
        String prefix = (type == MaintenanceScrapRecord.RecordType.SCRAP) ? "SC-2026-" : "MT-2026-";
        return prefix + seq.incrementAndGet();
    }

    public MaintenanceScrapRecord save(MaintenanceScrapRecord record) {
        storage.put(record.getRecordId(), record);
        return record;
    }

    public Optional<MaintenanceScrapRecord> findById(String recordId) {
        return Optional.ofNullable(storage.get(recordId));
    }

    public List<MaintenanceScrapRecord> findByEquipmentCode(String equipmentCode) {
        return storage.values().stream()
                .filter(r -> r.getEquipmentCode().equals(equipmentCode))
                .collect(Collectors.toList());
    }

    public List<MaintenanceScrapRecord> findByTypeAndStatus(MaintenanceScrapRecord.RecordType type, OrderStatus status) {
        return storage.values().stream()
                .filter(r -> (type == null || r.getType() == type) && (status == null || r.getStatus() == status))
                .collect(Collectors.toList());
    }

    public List<MaintenanceScrapRecord> findAll() {
        return new ArrayList<>(storage.values());
    }

    public void clear() {
        storage.clear();
        seq.set(300);
    }
}
