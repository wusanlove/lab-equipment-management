package cn.edu.ctgu.labms.domain;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 设备维修与报废记录实体
 */
public class MaintenanceScrapRecord {

    public enum RecordType {
        MAINTENANCE("维修工单"),
        SCRAP("报废申请");

        private final String displayName;

        RecordType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private final String recordId;
    private final String equipmentCode;
    private final String reporterId;
    private final RecordType type;
    private final String reason;
    private OrderStatus status;
    private String handlerId;
    private String resolution;
    private final LocalDateTime createdAt;

    public MaintenanceScrapRecord(String recordId, String equipmentCode, String reporterId,
                                  RecordType type, String reason, OrderStatus status,
                                  String handlerId, String resolution, LocalDateTime createdAt) {
        this.recordId = Objects.requireNonNull(recordId, "recordId cannot be null");
        this.equipmentCode = Objects.requireNonNull(equipmentCode, "equipmentCode cannot be null");
        this.reporterId = Objects.requireNonNull(reporterId, "reporterId cannot be null");
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.reason = Objects.requireNonNull(reason, "reason cannot be null");
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.handlerId = handlerId;
        this.resolution = resolution;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
    }

    public String getRecordId() {
        return recordId;
    }

    public String getEquipmentCode() {
        return equipmentCode;
    }

    public String getReporterId() {
        return reporterId;
    }

    public RecordType getType() {
        return type;
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

    public String getHandlerId() {
        return handlerId;
    }

    public void setHandlerId(String handlerId) {
        this.handlerId = handlerId;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return String.format("MaintenanceScrapRecord{id='%s', eq='%s', type=%s, reason='%s', status=%s, resolution='%s'}",
                recordId, equipmentCode, type.getDisplayName(), reason, status.getDisplayName(),
                resolution == null ? "-" : resolution);
    }
}
