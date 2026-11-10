package cn.edu.ctgu.labms.domain;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 设备申购单实体
 */
public class PurchaseOrder {
    private final String orderId;
    private final String applicantId;
    private final String equipmentName;
    private final String modelSpec;
    private final String category;
    private final String manufacturer;
    private final int quantity;
    private final double unitPrice;
    private final String reason;
    private OrderStatus status;
    private String reviewerId;
    private String reviewComment;
    private final LocalDateTime createdAt;

    public PurchaseOrder(String orderId, String applicantId, String equipmentName,
                         String modelSpec, String category, String manufacturer,
                         int quantity, double unitPrice, String reason,
                         OrderStatus status, String reviewerId, String reviewComment,
                         LocalDateTime createdAt) {
        this.orderId = Objects.requireNonNull(orderId, "orderId cannot be null");
        this.applicantId = Objects.requireNonNull(applicantId, "applicantId cannot be null");
        this.equipmentName = Objects.requireNonNull(equipmentName, "equipmentName cannot be null");
        this.modelSpec = Objects.requireNonNull(modelSpec, "modelSpec cannot be null");
        this.category = Objects.requireNonNull(category, "category cannot be null");
        this.manufacturer = Objects.requireNonNull(manufacturer, "manufacturer cannot be null");
        if (quantity <= 0) {
            throw new IllegalArgumentException("申购数量必须大于0");
        }
        if (unitPrice < 0) {
            throw new IllegalArgumentException("预计单价不能为负数");
        }
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.reason = Objects.requireNonNull(reason, "reason cannot be null");
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.reviewerId = reviewerId;
        this.reviewComment = reviewComment;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
    }

    public String getOrderId() {
        return orderId;
    }

    public String getApplicantId() {
        return applicantId;
    }

    public String getEquipmentName() {
        return equipmentName;
    }

    public String getModelSpec() {
        return modelSpec;
    }

    public String getCategory() {
        return category;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public double getTotalAmount() {
        return quantity * unitPrice;
    }

    public String getReason() {
        return reason;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = Objects.requireNonNull(status);
    }

    public String getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(String reviewerId) {
        this.reviewerId = reviewerId;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return String.format("PurchaseOrder{id='%s', name='%s', spec='%s', qty=%d, unitPrice=%.2f, total=%.2f, status=%s}",
                orderId, equipmentName, modelSpec, quantity, unitPrice, getTotalAmount(), status.getDisplayName());
    }
}
