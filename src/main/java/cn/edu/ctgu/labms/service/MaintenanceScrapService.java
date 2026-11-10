package cn.edu.ctgu.labms.service;

import cn.edu.ctgu.labms.dao.AuditLogDao;
import cn.edu.ctgu.labms.dao.MaintenanceScrapDao;
import cn.edu.ctgu.labms.domain.Equipment;
import cn.edu.ctgu.labms.domain.EquipmentStatus;
import cn.edu.ctgu.labms.domain.MaintenanceScrapRecord;
import cn.edu.ctgu.labms.domain.OrderStatus;
import cn.edu.ctgu.labms.domain.Role;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 设备故障报修、维修完工与报废审批服务
 */
public class MaintenanceScrapService {
    private final MaintenanceScrapDao maintenanceScrapDao;
    private final EquipmentService equipmentService;
    private final AuthService authService;
    private final AuditLogDao auditLogDao;

    public MaintenanceScrapService(MaintenanceScrapDao maintenanceScrapDao,
                                   EquipmentService equipmentService,
                                   AuthService authService,
                                   AuditLogDao auditLogDao) {
        this.maintenanceScrapDao = Objects.requireNonNull(maintenanceScrapDao);
        this.equipmentService = Objects.requireNonNull(equipmentService);
        this.authService = Objects.requireNonNull(authService);
        this.auditLogDao = Objects.requireNonNull(auditLogDao);
    }

    /**
     * 设备故障报修（状态由 IN_STOCK 或 BORROWED -> MAINTENANCE）
     */
    public MaintenanceScrapRecord reportFault(String equipmentCode, String reporterId, String reason) {
        authService.getActiveUserOrThrow(reporterId);
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("报修故障原因不能为空");
        }
        Equipment equipment = equipmentService.getByCodeOrThrow(equipmentCode);
        if (equipment.getStatus() != EquipmentStatus.IN_STOCK
                && equipment.getStatus() != EquipmentStatus.BORROWED) {
            throw new IllegalStateException(String.format(
                    "报修失败：设备[%s-%s]当前状态为【%s】，无法发起报修",
                    equipment.getCode(), equipment.getName(), equipment.getStatus().getDisplayName()));
        }

        equipmentService.transitionStatus(
                equipmentCode,
                EquipmentStatus.MAINTENANCE,
                reporterId,
                "故障报修：" + reason.trim()
        );

        String recordId = maintenanceScrapDao.nextRecordId(MaintenanceScrapRecord.RecordType.MAINTENANCE);
        MaintenanceScrapRecord record = new MaintenanceScrapRecord(
                recordId,
                equipmentCode,
                reporterId,
                MaintenanceScrapRecord.RecordType.MAINTENANCE,
                reason.trim(),
                OrderStatus.PENDING,
                null,
                null,
                LocalDateTime.now()
        );
        maintenanceScrapDao.save(record);
        auditLogDao.append(reporterId, "MAINTENANCE_REPORT", recordId,
                String.format("发起设备[%s]维修工单，原因：%s", equipmentCode, reason.trim()));
        return record;
    }

    /**
     * 完成设备维修（状态由 MAINTENANCE -> IN_STOCK）
     */
    public MaintenanceScrapRecord completeRepair(String recordId, String handlerId, String resolution) {
        authService.requireRole(handlerId, Role.LAB_MANAGER, Role.ADMIN);
        MaintenanceScrapRecord record = getRecordOrThrow(recordId);
        if (record.getType() != MaintenanceScrapRecord.RecordType.MAINTENANCE) {
            throw new IllegalStateException("该记录不是维修工单: " + recordId);
        }
        if (record.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("维修工单[" + recordId + "]已处理完毕，不可重复完工");
        }

        equipmentService.transitionStatus(
                record.getEquipmentCode(),
                EquipmentStatus.IN_STOCK,
                handlerId,
                "维修完成恢复在库：" + (resolution == null ? "修复正常" : resolution)
        );

        record.setStatus(OrderStatus.COMPLETED);
        record.setHandlerId(handlerId);
        record.setResolution(resolution == null ? "已修复并通过检测" : resolution.trim());
        maintenanceScrapDao.save(record);

        auditLogDao.append(handlerId, "MAINTENANCE_COMPLETE", recordId,
                String.format("完成设备[%s]维修工单，处理结果：%s",
                        record.getEquipmentCode(), record.getResolution()));
        return record;
    }

    /**
     * 提交并审批设备报废（状态由 IN_STOCK 或 MAINTENANCE -> SCRAPPED 永久终态）
     * 仅实验室管理员或系统管理员有权批准报废
     */
    public MaintenanceScrapRecord approveScrap(String equipmentCode, String applicantId,
                                               String approverId, String reason, String resolution) {
        authService.getActiveUserOrThrow(applicantId);
        authService.requireRole(approverId, Role.LAB_MANAGER, Role.ADMIN);
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("报废原因不能为空");
        }
        Equipment equipment = equipmentService.getByCodeOrThrow(equipmentCode);
        if (equipment.getStatus() != EquipmentStatus.IN_STOCK
                && equipment.getStatus() != EquipmentStatus.MAINTENANCE) {
            throw new IllegalStateException(String.format(
                    "报废失败：设备[%s-%s]当前状态为【%s】，仅【在库可用】或【维修中】的设备可执行报废",
                    equipment.getCode(), equipment.getName(), equipment.getStatus().getDisplayName()));
        }

        // 若该设备存在未结案的维修工单，同步将其结案注明转报废
        List<MaintenanceScrapRecord> relatedRecords = maintenanceScrapDao.findByEquipmentCode(equipmentCode);
        for (MaintenanceScrapRecord r : relatedRecords) {
            if (r.getType() == MaintenanceScrapRecord.RecordType.MAINTENANCE
                    && r.getStatus() == OrderStatus.PENDING) {
                r.setStatus(OrderStatus.COMPLETED);
                r.setHandlerId(approverId);
                r.setResolution("无法修复，已转报废流程处理");
                maintenanceScrapDao.save(r);
            }
        }

        equipmentService.transitionStatus(
                equipmentCode,
                EquipmentStatus.SCRAPPED,
                approverId,
                "报废获批：" + reason.trim()
        );

        String recordId = maintenanceScrapDao.nextRecordId(MaintenanceScrapRecord.RecordType.SCRAP);
        MaintenanceScrapRecord scrapRecord = new MaintenanceScrapRecord(
                recordId,
                equipmentCode,
                applicantId,
                MaintenanceScrapRecord.RecordType.SCRAP,
                reason.trim(),
                OrderStatus.COMPLETED,
                approverId,
                resolution == null ? "批准报废并核销资产台账" : resolution.trim(),
                LocalDateTime.now()
        );
        maintenanceScrapDao.save(scrapRecord);
        auditLogDao.append(approverId, "EQUIPMENT_SCRAP", recordId,
                String.format("批准设备[%s]永久报废，原因：%s", equipmentCode, reason.trim()));
        return scrapRecord;
    }

    public MaintenanceScrapRecord getRecordOrThrow(String recordId) {
        if (recordId == null || recordId.trim().isEmpty()) {
            throw new IllegalArgumentException("工单ID不能为空");
        }
        return maintenanceScrapDao.findById(recordId.trim())
                .orElseThrow(() -> new IllegalArgumentException("未找到维修/报废记录: " + recordId));
    }

    public List<MaintenanceScrapRecord> findAllRecords() {
        return maintenanceScrapDao.findAll();
    }
}
