package cn.edu.ctgu.labms.domain;

import java.util.Objects;

/**
 * 实验室低值易耗品与危化耗材实体
 */
public class Consumable {
    private final String code;
    private String name;
    private String spec;
    private String unit;
    private int currentStock;
    private int safetyThreshold;
    private boolean hazardous;
    private String location;

    public Consumable(String code, String name, String spec, String unit,
                      int currentStock, int safetyThreshold, boolean hazardous, String location) {
        this.code = Objects.requireNonNull(code, "code cannot be null");
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.spec = Objects.requireNonNull(spec, "spec cannot be null");
        this.unit = Objects.requireNonNull(unit, "unit cannot be null");
        if (currentStock < 0) {
            throw new IllegalArgumentException("当前库存不能为负数");
        }
        if (safetyThreshold < 0) {
            throw new IllegalArgumentException("安全库存阈值不能为负数");
        }
        this.currentStock = currentStock;
        this.safetyThreshold = safetyThreshold;
        this.hazardous = hazardous;
        this.location = Objects.requireNonNull(location, "location cannot be null");
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = Objects.requireNonNull(name);
    }

    public String getSpec() {
        return spec;
    }

    public void setSpec(String spec) {
        this.spec = Objects.requireNonNull(spec);
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = Objects.requireNonNull(unit);
    }

    public int getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(int currentStock) {
        if (currentStock < 0) {
            throw new IllegalArgumentException("当前库存不能为负数");
        }
        this.currentStock = currentStock;
    }

    public int getSafetyThreshold() {
        return safetyThreshold;
    }

    public void setSafetyThreshold(int safetyThreshold) {
        if (safetyThreshold < 0) {
            throw new IllegalArgumentException("安全库存阈值不能为负数");
        }
        this.safetyThreshold = safetyThreshold;
    }

    public boolean isHazardous() {
        return hazardous;
    }

    public void setHazardous(boolean hazardous) {
        this.hazardous = hazardous;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = Objects.requireNonNull(location);
    }

    public boolean isLowStock() {
        return currentStock < safetyThreshold;
    }

    @Override
    public String toString() {
        return String.format("Consumable{code='%s', name='%s', spec='%s', stock=%d%s, threshold=%d%s, hazardous=%s, lowStock=%s}",
                code, name, spec, currentStock, unit, safetyThreshold, unit,
                hazardous ? "危化品" : "普通", isLowStock() ? "【预警】" : "正常");
    }
}
