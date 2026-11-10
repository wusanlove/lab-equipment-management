package cn.edu.ctgu.labms.service;

import cn.edu.ctgu.labms.dao.AuditLogDao;
import cn.edu.ctgu.labms.dao.EquipmentDao;
import cn.edu.ctgu.labms.domain.Equipment;
import cn.edu.ctgu.labms.domain.EquipmentStatus;
import cn.edu.ctgu.labms.domain.Role;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * 实验设备台账管理、状态机流转校验与仓库统计服务
 */
public class EquipmentService {

    /**
     * 仓库设备盘点汇总指标值对象
     */
    public static class InventoryStatistics {
        private final int totalCount;
        private final int inStockCount;
        private final int borrowedCount;
        private final int maintenanceCount;
        private final int scrappedCount;
        private final double totalAssetValue;
        private final double intactRate;

        public InventoryStatistics(int totalCount, int inStockCount, int borrowedCount,
                                   int maintenanceCount, int scrappedCount,
                                   double totalAssetValue, double intactRate) {
            this.totalCount = totalCount;
            this.inStockCount = inStockCount;
            this.borrowedCount = borrowedCount;
            this.maintenanceCount = maintenanceCount;
            this.scrappedCount = scrappedCount;
            this.totalAssetValue = totalAssetValue;
            this.intactRate = intactRate;
        }

        public int getTotalCount() {
            return totalCount;
        }

        public int getInStockCount() {
            return inStockCount;
        }

        public int getBorrowedCount() {
            return borrowedCount;
        }

        public int getMaintenanceCount() {
            return maintenanceCount;
        }

        public int getScrappedCount() {
            return scrappedCount;
        }

        public double getTotalAssetValue() {
            return totalAssetValue;
        }

        public double getIntactRate() {
            return intactRate;
        }

        @Override
        public String toString() {
            return String.format(
                    "库存总台数: %d | 在库可用: %d | 已借出: %d | 维修中: %d | 已报废: %d | 资产总值: ¥%.2f | 设备完好率: %.2f%%",
                    totalCount, inStockCount, borrowedCount, maintenanceCount,
                    scrappedCount, totalAssetValue, intactRate);
        }
    }

    private final EquipmentDao equipmentDao;
    private final AuthService authService;
    private final AuditLogDao auditLogDao;

    public EquipmentService(EquipmentDao equipmentDao, AuthService authService, AuditLogDao auditLogDao) {
        this.equipmentDao = Objects.requireNonNull(equipmentDao);
        this.authService = Objects.requireNonNull(authService);
        this.auditLogDao = Objects.requireNonNull(auditLogDao);
    }

    /**
     * 登记新设备入账（自动生成或指定设备编码）
     */
    public Equipment registerEquipment(String operatorId, String customCode, String name,
                                       String modelSpec, String category, String manufacturer,
                                       double unitPrice, LocalDate purchaseDate,
                                       LocalDate warrantyUntil, String location) {
        authService.requireRole(operatorId, Role.ADMIN, Role.LAB_MANAGER);
        String code = (customCode == null || customCode.trim().isEmpty())
                ? equipmentDao.nextCode()
                : customCode.trim();
        if (equipmentDao.findByCode(code).isPresent()) {
            throw new IllegalArgumentException("设备编码已存在: " + code);
        }
        if (warrantyUntil.isBefore(purchaseDate)) {
            throw new IllegalArgumentException("质保到期日不能早于购置日期");
        }
        Equipment equipment = new Equipment(
                code, name, modelSpec, category, manufacturer,
                unitPrice, purchaseDate, warrantyUntil, location, EquipmentStatus.IN_STOCK
        );
        equipmentDao.save(equipment);
        auditLogDao.append(operatorId, "EQUIPMENT_REGISTER", code,
                String.format("登记设备入库: %s (%s), 位置=%s, 单价=%.2f", name, modelSpec, location, unitPrice));
        return equipment;
    }

    /**
     * 按设备编码获取设备，不存在则抛出异常
     */
    public Equipment getByCodeOrThrow(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("设备编码不能为空");
        }
        return equipmentDao.findByCode(code.trim())
                .orElseThrow(() -> new IllegalArgumentException("未找到设备: " + code));
    }

    /**
     * 设备状态机安全转移校验与更新
     * 规则：
     * - SCRAPPED 为终态，严禁向 IN_STOCK / BORROWED / MAINTENANCE 流转
     * - BORROWED 仅可流转至 IN_STOCK(正常归还) 或 MAINTENANCE(故障报修/受损归还)
     * - MAINTENANCE 仅可流转至 IN_STOCK(维修完成) 或 SCRAPPED(报废获批)
     * - IN_STOCK 可流转至 BORROWED / MAINTENANCE / SCRAPPED
     */
    public Equipment transitionStatus(String code, EquipmentStatus targetStatus,
                                      String operatorId, String reason) {
        authService.getActiveUserOrThrow(operatorId);
        Equipment equipment = getByCodeOrThrow(code);
        EquipmentStatus currentStatus = equipment.getStatus();
        if (!currentStatus.canTransitionTo(targetStatus)) {
            throw new IllegalStateException(String.format(
                    "非法设备状态流转：设备[%s-%s]当前状态为【%s】，不允许流转至【%s】",
                    equipment.getCode(), equipment.getName(),
                    currentStatus.getDisplayName(),
                    targetStatus == null ? "NULL" : targetStatus.getDisplayName()));
        }
        equipment.setStatus(targetStatus);
        equipmentDao.save(equipment);
        auditLogDao.append(operatorId, "EQUIPMENT_STATUS_CHANGE", code,
                String.format("设备状态由【%s】变更为【%s】，原因：%s",
                        currentStatus.getDisplayName(), targetStatus.getDisplayName(),
                        reason == null ? "-" : reason));
        return equipment;
    }

    /**
     * 变更设备存放实验室位置
     */
    public Equipment updateLocation(String operatorId, String code, String newLocation) {
        authService.requireRole(operatorId, Role.ADMIN, Role.LAB_MANAGER);
        if (newLocation == null || newLocation.trim().isEmpty()) {
            throw new IllegalArgumentException("新存放位置不能为空");
        }
        Equipment equipment = getByCodeOrThrow(code);
        String oldLocation = equipment.getLocation();
        equipment.setLocation(newLocation.trim());
        equipmentDao.save(equipment);
        auditLogDao.append(operatorId, "EQUIPMENT_RELOCATE", code,
                String.format("存放位置由 [%s] 变更为 [%s]", oldLocation, newLocation.trim()));
        return equipment;
    }

    /**
     * 多条件组合检索设备台账
     */
    public List<Equipment> queryEquipment(String keyword, String category,
                                          EquipmentStatus status, String location) {
        return equipmentDao.search(keyword, category, status, location);
    }

    public List<Equipment> findAll() {
        return equipmentDao.findAll();
    }

    /**
     * 统计仓库设备盘点指标（总数、各状态数量、资产总值、完好率）
     * 完好率 = (在库可用数 + 已借出正常使用数) / 总台数 * 100%
     */
    public InventoryStatistics getInventoryStatistics() {
        List<Equipment> all = equipmentDao.findAll();
        int total = all.size();
        int inStock = 0;
        int borrowed = 0;
        int maintenance = 0;
        int scrapped = 0;
        double totalValue = 0.0;

        for (Equipment eq : all) {
            totalValue += eq.getUnitPrice();
            switch (eq.getStatus()) {
                case IN_STOCK:
                    inStock++;
                    break;
                case BORROWED:
                    borrowed++;
                    break;
                case MAINTENANCE:
                    maintenance++;
                    break;
                case SCRAPPED:
                    scrapped++;
                    break;
                default:
                    break;
            }
        }
        double intactRate = (total == 0) ? 100.0 : ((inStock + borrowed) * 100.0 / total);
        return new InventoryStatistics(total, inStock, borrowed, maintenance, scrapped, totalValue, intactRate);
    }
}
