package cn.edu.ctgu.labms.controller;

import cn.edu.ctgu.labms.dao.InMemoryDatabase;
import cn.edu.ctgu.labms.domain.AuditLog;
import cn.edu.ctgu.labms.domain.BorrowRecord;
import cn.edu.ctgu.labms.domain.Consumable;
import cn.edu.ctgu.labms.domain.Equipment;
import cn.edu.ctgu.labms.domain.EquipmentStatus;
import cn.edu.ctgu.labms.domain.MaintenanceScrapRecord;
import cn.edu.ctgu.labms.domain.PurchaseOrder;
import cn.edu.ctgu.labms.domain.Role;
import cn.edu.ctgu.labms.domain.User;
import cn.edu.ctgu.labms.service.AuthService;
import cn.edu.ctgu.labms.service.BorrowService;
import cn.edu.ctgu.labms.service.ConsumableService;
import cn.edu.ctgu.labms.service.EquipmentService;
import cn.edu.ctgu.labms.service.MaintenanceScrapService;
import cn.edu.ctgu.labms.service.PurchaseService;

import java.time.LocalDate;
import java.util.List;

/**
 * 实验室设备与耗材管理系统统一门面控制器（Facade模式）
 * 聚合协调七大核心业务模块：
 * 1. 用户与RBAC权限管理模块
 * 2. 设备全生命周期台账管理模块
 * 3. 设备申购审批与自动入库模块
 * 4. 设备借用与归还验收联动模块
 * 5. 设备故障报修与报废核销模块
 * 6. 仓库库存盘点与完好率统计模块
 * 7. 实验耗材出入库与低库存预警模块
 */
public class LabSystemFacade {
    private final InMemoryDatabase database;
    private final AuthService authService;
    private final EquipmentService equipmentService;
    private final PurchaseService purchaseService;
    private final MaintenanceScrapService maintenanceScrapService;
    private final BorrowService borrowService;
    private final ConsumableService consumableService;

    public LabSystemFacade() {
        this(new InMemoryDatabase());
    }

    public LabSystemFacade(InMemoryDatabase database) {
        this.database = database;
        this.authService = new AuthService(database.getUserDao(), database.getAuditLogDao());
        this.equipmentService = new EquipmentService(database.getEquipmentDao(), authService, database.getAuditLogDao());
        this.purchaseService = new PurchaseService(database.getPurchaseOrderDao(), equipmentService, authService, database.getAuditLogDao());
        this.maintenanceScrapService = new MaintenanceScrapService(database.getMaintenanceScrapDao(), equipmentService, authService, database.getAuditLogDao());
        this.borrowService = new BorrowService(database.getBorrowRecordDao(), equipmentService, maintenanceScrapService, authService, database.getAuditLogDao());
        this.consumableService = new ConsumableService(database.getConsumableDao(), authService, database.getAuditLogDao());
    }

    // ==================== 模块1：用户与RBAC权限管理 ====================

    public User registerUser(String username, String password, String realName, Role role, String department) {
        return authService.registerUser(username, password, realName, role, department);
    }

    public User login(String username, String password) {
        return authService.login(username, password);
    }

    public User setUserActive(String adminId, String targetUserId, boolean active) {
        return authService.setUserActiveStatus(adminId, targetUserId, active);
    }

    public User changeUserRole(String adminId, String targetUserId, Role newRole) {
        return authService.changeUserRole(adminId, targetUserId, newRole);
    }

    public List<User> listAllUsers() {
        return authService.findAllUsers();
    }

    // ==================== 模块2：实验设备台账管理 ====================

    public Equipment registerEquipment(String operatorId, String customCode, String name,
                                       String modelSpec, String category, String manufacturer,
                                       double unitPrice, LocalDate purchaseDate,
                                       LocalDate warrantyUntil, String location) {
        return equipmentService.registerEquipment(
                operatorId, customCode, name, modelSpec, category,
                manufacturer, unitPrice, purchaseDate, warrantyUntil, location
        );
    }

    public List<Equipment> queryEquipment(String keyword, String category,
                                          EquipmentStatus status, String location) {
        return equipmentService.queryEquipment(keyword, category, status, location);
    }

    public Equipment relocateEquipment(String operatorId, String code, String newLocation) {
        return equipmentService.updateLocation(operatorId, code, newLocation);
    }

    // ==================== 模块3：设备申购与自动入库 ====================

    public PurchaseOrder submitPurchaseOrder(String applicantId, String equipmentName,
                                             String modelSpec, String category,
                                             String manufacturer, int quantity,
                                             double unitPrice, String reason) {
        return purchaseService.submitOrder(
                applicantId, equipmentName, modelSpec, category,
                manufacturer, quantity, unitPrice, reason
        );
    }

    public PurchaseOrder reviewPurchaseOrder(String orderId, String reviewerId,
                                             boolean approved, String comment) {
        return purchaseService.reviewOrder(orderId, reviewerId, approved, comment);
    }

    public List<Equipment> receivePurchaseOrder(String orderId, String operatorId,
                                                String location, LocalDate purchaseDate,
                                                int warrantyYears) {
        return purchaseService.receiveOrderAndInbound(orderId, operatorId, location, purchaseDate, warrantyYears);
    }

    public List<Equipment> approveAndInboundPurchaseOrder(String orderId, String reviewerId,
                                                          String comment, String location,
                                                          LocalDate purchaseDate, int warrantyYears) {
        return purchaseService.approveAndInboundOrder(orderId, reviewerId, comment, location, purchaseDate, warrantyYears);
    }

    // ==================== 模块4：设备借用与归还联动 ====================

    public BorrowRecord borrowEquipment(String equipmentCode, String borrowerId,
                                        String approverId, String purpose,
                                        LocalDate borrowDate, LocalDate dueDate) {
        return borrowService.borrowEquipment(equipmentCode, borrowerId, approverId, purpose, borrowDate, dueDate);
    }

    public BorrowRecord returnEquipment(String recordId, String operatorId,
                                        LocalDate returnDate, boolean conditionOk,
                                        String damageReason) {
        return borrowService.returnEquipment(recordId, operatorId, returnDate, conditionOk, damageReason);
    }

    public List<BorrowRecord> listOverdueBorrows(LocalDate asOfDate) {
        return borrowService.findOverdueRecords(asOfDate);
    }

    // ==================== 模块5：设备维修与报废管理 ====================

    public MaintenanceScrapRecord reportFault(String equipmentCode, String reporterId, String reason) {
        return maintenanceScrapService.reportFault(equipmentCode, reporterId, reason);
    }

    public MaintenanceScrapRecord completeRepair(String recordId, String handlerId, String resolution) {
        return maintenanceScrapService.completeRepair(recordId, handlerId, resolution);
    }

    public MaintenanceScrapRecord approveScrap(String equipmentCode, String applicantId,
                                               String approverId, String reason, String resolution) {
        return maintenanceScrapService.approveScrap(equipmentCode, applicantId, approverId, reason, resolution);
    }

    // ==================== 模块6：仓库库存统计与盘点 ====================

    public EquipmentService.InventoryStatistics getInventoryStatistics() {
        return equipmentService.getInventoryStatistics();
    }

    public List<Equipment> listAllEquipment() {
        return equipmentService.findAll();
    }

    // ==================== 模块7：实验耗材管理与预警 ====================

    public Consumable registerConsumable(String operatorId, String customCode, String name,
                                         String spec, String unit, int initialStock,
                                         int safetyThreshold, boolean hazardous, String location) {
        return consumableService.registerConsumable(
                operatorId, customCode, name, spec, unit,
                initialStock, safetyThreshold, hazardous, location
        );
    }

    public Consumable inboundConsumable(String operatorId, String code, int quantity, String remark) {
        return consumableService.inboundStock(operatorId, code, quantity, remark);
    }

    public Consumable outboundConsumable(String operatorId, String code, int quantity, String purpose) {
        return consumableService.outboundStock(operatorId, code, quantity, purpose);
    }

    public List<Consumable> listLowStockConsumables() {
        return consumableService.getLowStockWarnings();
    }

    public List<Consumable> listAllConsumables() {
        return consumableService.findAll();
    }

    // ==================== 审计日志与底层服务访问器 ====================

    public List<AuditLog> listAuditLogs() {
        return authService.getAuditLogs();
    }

    public InMemoryDatabase getDatabase() {
        return database;
    }

    public AuthService getAuthService() {
        return authService;
    }

    public EquipmentService getEquipmentService() {
        return equipmentService;
    }

    public PurchaseService getPurchaseService() {
        return purchaseService;
    }

    public BorrowService getBorrowService() {
        return borrowService;
    }

    public MaintenanceScrapService getMaintenanceScrapService() {
        return maintenanceScrapService;
    }

    public ConsumableService getConsumableService() {
        return consumableService;
    }
}
