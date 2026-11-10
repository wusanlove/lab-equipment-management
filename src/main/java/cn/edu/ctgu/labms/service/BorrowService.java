package cn.edu.ctgu.labms.service;

import cn.edu.ctgu.labms.dao.AuditLogDao;
import cn.edu.ctgu.labms.dao.BorrowRecordDao;
import cn.edu.ctgu.labms.domain.BorrowRecord;
import cn.edu.ctgu.labms.domain.Equipment;
import cn.edu.ctgu.labms.domain.EquipmentStatus;
import cn.edu.ctgu.labms.domain.MaintenanceScrapRecord;
import cn.edu.ctgu.labms.domain.Role;
import cn.edu.ctgu.labms.domain.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 设备借用、归还（含受损自动转修联动）与超期检查服务
 */
public class BorrowService {
    private final BorrowRecordDao borrowRecordDao;
    private final EquipmentService equipmentService;
    private final MaintenanceScrapService maintenanceScrapService;
    private final AuthService authService;
    private final AuditLogDao auditLogDao;

    public BorrowService(BorrowRecordDao borrowRecordDao,
                         EquipmentService equipmentService,
                         MaintenanceScrapService maintenanceScrapService,
                         AuthService authService,
                         AuditLogDao auditLogDao) {
        this.borrowRecordDao = Objects.requireNonNull(borrowRecordDao);
        this.equipmentService = Objects.requireNonNull(equipmentService);
        this.maintenanceScrapService = Objects.requireNonNull(maintenanceScrapService);
        this.authService = Objects.requireNonNull(authService);
        this.auditLogDao = Objects.requireNonNull(auditLogDao);
    }

    /**
     * 申请并审批借出设备（设备状态由 IN_STOCK -> BORROWED）
     */
    public BorrowRecord borrowEquipment(String equipmentCode, String borrowerId,
                                        String approverId, String purpose,
                                        LocalDate borrowDate, LocalDate dueDate) {
        User borrower = authService.getActiveUserOrThrow(borrowerId);
        authService.requireRole(approverId, Role.LAB_MANAGER, Role.ADMIN, Role.TEACHER);
        if (dueDate.isBefore(borrowDate)) {
            throw new IllegalArgumentException("应还日期不能早于借用日期");
        }
        Equipment equipment = equipmentService.getByCodeOrThrow(equipmentCode);
        if (equipment.getStatus() != EquipmentStatus.IN_STOCK) {
            throw new IllegalStateException(String.format(
                    "借用失败：设备[%s-%s]当前状态为【%s】，仅【在库可用】设备可借出",
                    equipment.getCode(), equipment.getName(), equipment.getStatus().getDisplayName()));
        }

        equipmentService.transitionStatus(
                equipmentCode,
                EquipmentStatus.BORROWED,
                approverId,
                String.format("借予用户 %s(%s)，用途：%s", borrower.getRealName(), borrower.getId(), purpose)
        );

        String recordId = borrowRecordDao.nextRecordId();
        BorrowRecord record = new BorrowRecord(
                recordId,
                equipmentCode,
                borrowerId,
                purpose,
                borrowDate,
                dueDate,
                null,
                null,
                approverId
        );
        borrowRecordDao.save(record);
        auditLogDao.append(approverId, "EQUIPMENT_BORROW", recordId,
                String.format("批准借出设备[%s]给 %s，应还日期=%s",
                        equipmentCode, borrower.getRealName(), dueDate));
        return record;
    }

    /**
     * 归还设备验收：
     * - 若 conditionOk == true，则设备恢复为 IN_STOCK（在库可用）
     * - 若 conditionOk == false，则自动将设备流转至 MAINTENANCE（维修中）并自动创建维修工单
     */
    public BorrowRecord returnEquipment(String recordId, String operatorId,
                                        LocalDate returnDate, boolean conditionOk,
                                        String damageReason) {
        authService.requireRole(operatorId, Role.LAB_MANAGER, Role.ADMIN, Role.TEACHER);
        BorrowRecord record = getRecordOrThrow(recordId);
        if (record.isReturned()) {
            throw new IllegalStateException("归还失败：借用记录[" + recordId + "]已归还，不可重复操作");
        }
        if (returnDate.isBefore(record.getBorrowDate())) {
            throw new IllegalArgumentException("归还日期不能早于借出日期");
        }

        record.setReturnDate(returnDate);
        record.setReturnConditionOk(conditionOk);
        borrowRecordDao.save(record);

        String equipmentCode = record.getEquipmentCode();
        if (conditionOk) {
            equipmentService.transitionStatus(
                    equipmentCode,
                    EquipmentStatus.IN_STOCK,
                    operatorId,
                    "借用归还验收完好，恢复在库可用"
            );
            auditLogDao.append(operatorId, "EQUIPMENT_RETURN_OK", recordId,
                    String.format("设备[%s]归还验收完好，已恢复在库", equipmentCode));
        } else {
            String faultDesc = (damageReason == null || damageReason.trim().isEmpty())
                    ? "借用归还验收发现设备受损，自动转入维修"
                    : "归还验收受损：" + damageReason.trim();
            MaintenanceScrapRecord ticket = maintenanceScrapService.reportFault(
                    equipmentCode,
                    operatorId,
                    faultDesc
            );
            auditLogDao.append(operatorId, "EQUIPMENT_RETURN_DAMAGED", recordId,
                    String.format("设备[%s]归还受损，已自动转入维修状态并生成工单[%s]",
                            equipmentCode, ticket.getRecordId()));
        }
        return record;
    }

    /**
     * 查询截至指定日期仍未归还且已超期的借用记录
     */
    public List<BorrowRecord> findOverdueRecords(LocalDate currentDate) {
        LocalDate checkDate = (currentDate == null) ? LocalDate.now() : currentDate;
        return borrowRecordDao.findAll().stream()
                .filter(r -> !r.isReturned() && checkDate.isAfter(r.getDueDate()))
                .collect(Collectors.toList());
    }

    public BorrowRecord getRecordOrThrow(String recordId) {
        if (recordId == null || recordId.trim().isEmpty()) {
            throw new IllegalArgumentException("借用记录ID不能为空");
        }
        return borrowRecordDao.findById(recordId.trim())
                .orElseThrow(() -> new IllegalArgumentException("未找到借用记录: " + recordId));
    }

    public List<BorrowRecord> findAllRecords() {
        return borrowRecordDao.findAll();
    }
}
