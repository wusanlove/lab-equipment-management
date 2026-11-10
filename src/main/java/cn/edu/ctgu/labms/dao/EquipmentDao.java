package cn.edu.ctgu.labms.dao;

import cn.edu.ctgu.labms.domain.Equipment;
import cn.edu.ctgu.labms.domain.EquipmentStatus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 实验设备数据访问对象（内存仓储实现）
 */
public class EquipmentDao {
    private final Map<String, Equipment> storage = new LinkedHashMap<>();
    private final AtomicInteger codeSeq = new AtomicInteger(1000);

    public String nextCode() {
        return "EQ-2026-" + codeSeq.incrementAndGet();
    }

    public Equipment save(Equipment equipment) {
        storage.put(equipment.getCode(), equipment);
        return equipment;
    }

    public Optional<Equipment> findByCode(String code) {
        return Optional.ofNullable(storage.get(code));
    }

    public List<Equipment> findAll() {
        return new ArrayList<>(storage.values());
    }

    public List<Equipment> findByStatus(EquipmentStatus status) {
        return storage.values().stream()
                .filter(e -> e.getStatus() == status)
                .collect(Collectors.toList());
    }

    /**
     * 多条件组合查询（关键字匹配编码/名称/型号、分类精确匹配、状态精确匹配、存放位置模糊匹配）
     */
    public List<Equipment> search(String keyword, String category, EquipmentStatus status, String location) {
        return storage.values().stream()
                .filter(e -> {
                    if (keyword != null && !keyword.trim().isEmpty()) {
                        String kw = keyword.trim().toLowerCase(Locale.ROOT);
                        boolean matched = e.getCode().toLowerCase(Locale.ROOT).contains(kw)
                                || e.getName().toLowerCase(Locale.ROOT).contains(kw)
                                || e.getModelSpec().toLowerCase(Locale.ROOT).contains(kw);
                        if (!matched) {
                            return false;
                        }
                    }
                    if (category != null && !category.trim().isEmpty()) {
                        if (!e.getCategory().equalsIgnoreCase(category.trim())) {
                            return false;
                        }
                    }
                    if (status != null && e.getStatus() != status) {
                        return false;
                    }
                    if (location != null && !location.trim().isEmpty()) {
                        if (!e.getLocation().toLowerCase(Locale.ROOT).contains(location.trim().toLowerCase(Locale.ROOT))) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    public boolean deleteByCode(String code) {
        return storage.remove(code) != null;
    }

    public void clear() {
        storage.clear();
        codeSeq.set(1000);
    }
}
