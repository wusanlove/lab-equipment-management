package cn.edu.ctgu.labms.dao;

import cn.edu.ctgu.labms.domain.Consumable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 实验耗材数据访问对象（内存仓储实现）
 */
public class ConsumableDao {
    private final Map<String, Consumable> storage = new LinkedHashMap<>();
    private final AtomicInteger seq = new AtomicInteger(400);

    public String nextCode() {
        return "CS-2026-" + seq.incrementAndGet();
    }

    public Consumable save(Consumable consumable) {
        storage.put(consumable.getCode(), consumable);
        return consumable;
    }

    public Optional<Consumable> findByCode(String code) {
        return Optional.ofNullable(storage.get(code));
    }

    public List<Consumable> findLowStock() {
        return storage.values().stream()
                .filter(Consumable::isLowStock)
                .collect(Collectors.toList());
    }

    public List<Consumable> findAll() {
        return new ArrayList<>(storage.values());
    }

    public void clear() {
        storage.clear();
        seq.set(400);
    }
}
