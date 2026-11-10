package cn.edu.ctgu.labms.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * 实验室设备台账实体
 */
public class Equipment {
    private final String code;
    private String name;
    private String modelSpec;
    private String category;
    private String manufacturer;
    private double unitPrice;
    private LocalDate purchaseDate;
    private LocalDate warrantyUntil;
    private String location;
    private EquipmentStatus status;

    public Equipment(String code, String name, String modelSpec, String category,
                     String manufacturer, double unitPrice, LocalDate purchaseDate,
                     LocalDate warrantyUntil, String location, EquipmentStatus status) {
        this.code = Objects.requireNonNull(code, "code cannot be null");
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.modelSpec = Objects.requireNonNull(modelSpec, "modelSpec cannot be null");
        this.category = Objects.requireNonNull(category, "category cannot be null");
        this.manufacturer = Objects.requireNonNull(manufacturer, "manufacturer cannot be null");
        if (unitPrice < 0) {
            throw new IllegalArgumentException("设备单价不能为负数");
        }
        this.unitPrice = unitPrice;
        this.purchaseDate = Objects.requireNonNull(purchaseDate, "purchaseDate cannot be null");
        this.warrantyUntil = Objects.requireNonNull(warrantyUntil, "warrantyUntil cannot be null");
        this.location = Objects.requireNonNull(location, "location cannot be null");
        this.status = Objects.requireNonNull(status, "status cannot be null");
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

    public String getModelSpec() {
        return modelSpec;
    }

    public void setModelSpec(String modelSpec) {
        this.modelSpec = Objects.requireNonNull(modelSpec);
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = Objects.requireNonNull(category);
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = Objects.requireNonNull(manufacturer);
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        if (unitPrice < 0) {
            throw new IllegalArgumentException("设备单价不能为负数");
        }
        this.unitPrice = unitPrice;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = Objects.requireNonNull(purchaseDate);
    }

    public LocalDate getWarrantyUntil() {
        return warrantyUntil;
    }

    public void setWarrantyUntil(LocalDate warrantyUntil) {
        this.warrantyUntil = Objects.requireNonNull(warrantyUntil);
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = Objects.requireNonNull(location);
    }

    public EquipmentStatus getStatus() {
        return status;
    }

    public void setStatus(EquipmentStatus status) {
        this.status = Objects.requireNonNull(status);
    }

    @Override
    public String toString() {
        return String.format("Equipment{code='%s', name='%s', spec='%s', category='%s', price=%.2f, location='%s', status=%s}",
                code, name, modelSpec, category, unitPrice, location, status.getDisplayName());
    }
}
