package cn.edu.ctgu.labms.service;

import cn.edu.ctgu.labms.dao.AuditLogDao;
import cn.edu.ctgu.labms.dao.PurchaseOrderDao;
import cn.edu.ctgu.labms.domain.Equipment;
import cn.edu.ctgu.labms.domain.OrderStatus;
import cn.edu.ctgu.labms.domain.PurchaseOrder;
import cn.edu.ctgu.labms.domain.Role;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 设备申购、审批与验收入库联动服务
 */
public class PurchaseService {
    private final PurchaseOrderDao purchaseOrderDao;
    private final EquipmentService equipmentService;
    private final AuthService authService;
    private final AuditLogDao auditLogDao;

    public PurchaseService(PurchaseOrderDao purchaseOrderDao,
                           EquipmentService equipmentService,
                           AuthService authService,
                           AuditLogDao auditLogDao) {
        this.purchaseOrderDao = Objects.requireNonNull(purchaseOrderDao);
        this.equipmentService = Objects.requireNonNull(equipmentService);
        this.authService = Objects.requireNonNull(authService);
        this.auditLogDao = Objects.requireNonNull(auditLogDao);
    }

    /**
     * 提交设备申购单（指导教师、实验室管理员、系统管理员可发起）
     */
    public PurchaseOrder submitOrder(String applicantId, String equipmentName,
                                     String modelSpec, String category, String manufacturer,
                                     int quantity, double unitPrice, String reason) {
        authService.requireRole(applicantId, Role.TEACHER, Role.LAB_MANAGER, Role.ADMIN);
        if (equipmentName == null || equipmentName.trim().isEmpty()) {
            throw new IllegalArgumentException("申购设备名称不能为空");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("申购数量必须大于0");
        }
        if (unitPrice < 0) {
            throw new IllegalArgumentException("申购单价不能为负数");
        }
        String orderId = purchaseOrderDao.nextOrderId();
        PurchaseOrder order = new PurchaseOrder(
                orderId,
                applicantId,
                equipmentName.trim(),
                modelSpec == null ? "标准型" : modelSpec.trim(),
                category == null ? "通用设备" : category.trim(),
                manufacturer == null ? "未知厂商" : manufacturer.trim(),
                quantity,
                unitPrice,
                reason == null ? "教学科研需要" : reason.trim(),
                OrderStatus.PENDING,
                null,
                null,
                LocalDateTime.now()
        );
        purchaseOrderDao.save(order);
        auditLogDao.append(applicantId, "PURCHASE_SUBMIT", orderId,
                String.format("提交设备申购单: %s x%d, 预算总价=¥%.2f",
                        order.getEquipmentName(), quantity, order.getTotalAmount()));
        return order;
    }

    /**
     * 审批申购单（批准或驳回，仅实验室管理员或系统管理员可审批）
     */
    public PurchaseOrder reviewOrder(String orderId, String reviewerId,
                                     boolean approved, String reviewComment) {
        authService.requireRole(reviewerId, Role.LAB_MANAGER, Role.ADMIN);
        PurchaseOrder order = getOrderOrThrow(orderId);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException(String.format(
                    "申购单[%s]当前状态为【%s】，不可重复审批",
                    orderId, order.getStatus().getDisplayName()));
        }
        OrderStatus newStatus = approved ? OrderStatus.APPROVED : OrderStatus.REJECTED;
        order.setStatus(newStatus);
        order.setReviewerId(reviewerId);
        order.setReviewComment(reviewComment == null ? (approved ? "同意采购" : "预算不符予以驳回") : reviewComment);
        purchaseOrderDao.save(order);
        auditLogDao.append(reviewerId, approved ? "PURCHASE_APPROVE" : "PURCHASE_REJECT", orderId,
                String.format("审批申购单[%s]: 结果=%s, 意见=%s",
                        orderId, newStatus.getDisplayName(), order.getReviewComment()));
        return order;
    }

    /**
     * 对已批准的申购单执行到货验收并自动批量生成在库(IN_STOCK)设备台账
     */
    public List<Equipment> receiveOrderAndInbound(String orderId, String operatorId,
                                                  String location, LocalDate purchaseDate,
                                                  int warrantyYears) {
        authService.requireRole(operatorId, Role.LAB_MANAGER, Role.ADMIN);
        PurchaseOrder order = getOrderOrThrow(orderId);
        if (order.getStatus() != OrderStatus.APPROVED) {
            throw new IllegalStateException(String.format(
                    "申购单[%s]状态为【%s】，仅【已批准】状态的申购单可执行验收入库",
                    orderId, order.getStatus().getDisplayName()));
        }
        if (warrantyYears <= 0) {
            throw new IllegalArgumentException("质保年限必须大于0");
        }
        LocalDate inboundDate = (purchaseDate == null) ? LocalDate.now() : purchaseDate;
        LocalDate warrantyUntil = inboundDate.plusYears(warrantyYears);

        List<Equipment> createdEquipments = new ArrayList<>();
        for (int i = 0; i < order.getQuantity(); i++) {
            Equipment eq = equipmentService.registerEquipment(
                    operatorId,
                    null,
                    order.getEquipmentName(),
                    order.getModelSpec(),
                    order.getCategory(),
                    order.getManufacturer(),
                    order.getUnitPrice(),
                    inboundDate,
                    warrantyUntil,
                    location
            );
            createdEquipments.add(eq);
        }

        order.setStatus(OrderStatus.COMPLETED);
        purchaseOrderDao.save(order);
        auditLogDao.append(operatorId, "PURCHASE_INBOUND_COMPLETE", orderId,
                String.format("申购单[%s]验收入库完成，自动批量建账 %d 台设备(%s)",
                        orderId, createdEquipments.size(), order.getEquipmentName()));
        return createdEquipments;
    }

    /**
     * 一站式审批通过并完成到货批量入库
     */
    public List<Equipment> approveAndInboundOrder(String orderId, String reviewerId,
                                                  String reviewComment, String location,
                                                  LocalDate purchaseDate, int warrantyYears) {
        reviewOrder(orderId, reviewerId, true, reviewComment);
        return receiveOrderAndInbound(orderId, reviewerId, location, purchaseDate, warrantyYears);
    }

    public PurchaseOrder getOrderOrThrow(String orderId) {
        if (orderId == null || orderId.trim().isEmpty()) {
            throw new IllegalArgumentException("申购单号不能为空");
        }
        return purchaseOrderDao.findById(orderId.trim())
                .orElseThrow(() -> new IllegalArgumentException("未找到申购单: " + orderId));
    }

    public List<PurchaseOrder> findAllOrders() {
        return purchaseOrderDao.findAll();
    }
}
