package cn.edu.ctgu.labms.service;

import cn.edu.ctgu.labms.dao.AuditLogDao;
import cn.edu.ctgu.labms.dao.ConsumableDao;
import cn.edu.ctgu.labms.domain.Consumable;
import cn.edu.ctgu.labms.domain.Role;

import java.util.List;
import java.util.Objects;

/**
 * 实验室耗材出入库管理与低库存阈值预警服务
 */
public class ConsumableService {
    private final ConsumableDao consumableDao;
    private final AuthService authService;
    private final AuditLogDao auditLogDao;

    public ConsumableService(ConsumableDao consumableDao,
                             AuthService authService,
                             AuditLogDao auditLogDao) {
        this.consumableDao = Objects.requireNonNull(consumableDao);
        this.authService = Objects.requireNonNull(authService);
        this.auditLogDao = Objects.requireNonNull(auditLogDao);
    }

    /**
     * 登记新增实验耗材品类
     */
    public Consumable registerConsumable(String operatorId, String customCode, String name,
                                         String spec, String unit, int initialStock,
                                         int safetyThreshold, boolean hazardous, String location) {
        authService.requireRole(operatorId, Role.LAB_MANAGER, Role.ADMIN);
        String code = (customCode == null || customCode.trim().isEmpty())
                ? consumableDao.nextCode()
                : customCode.trim();
        if (consumableDao.findByCode(code).isPresent()) {
            throw new IllegalArgumentException("耗材编码已存在: " + code);
        }
        Consumable consumable = new Consumable(
                code, name, spec, unit, initialStock, safetyThreshold, hazardous, location
        );
        consumableDao.save(consumable);
        auditLogDao.append(operatorId, "CONSUMABLE_REGISTER", code,
                String.format("建档耗材: %s(%s), 初始库存=%d%s, 预警阈值=%d%s",
                        name, spec, initialStock, unit, safetyThreshold, unit));
        return consumable;
    }

    /**
     * 耗材补充入库
     */
    public Consumable inboundStock(String operatorId, String code, int quantity, String remark) {
        authService.requireRole(operatorId, Role.LAB_MANAGER, Role.ADMIN);
        if (quantity <= 0) {
            throw new IllegalArgumentException("入库数量必须大于0");
        }
        Consumable consumable = getByCodeOrThrow(code);
        int oldStock = consumable.getCurrentStock();
        int newStock = oldStock + quantity;
        consumable.setCurrentStock(newStock);
        consumableDao.save(consumable);

        auditLogDao.append(operatorId, "CONSUMABLE_INBOUND", code,
                String.format("耗材[%s]入库 +%d%s，库存 %d -> %d%s，备注：%s",
                        consumable.getName(), quantity, consumable.getUnit(),
                        oldStock, newStock, consumable.getUnit(),
                        remark == null ? "常规补货" : remark));
        return consumable;
    }

    /**
     * 耗材领用出库（若库存不足则抛出 IllegalStateException）
     */
    public Consumable outboundStock(String operatorId, String code, int quantity, String purpose) {
        authService.getActiveUserOrThrow(operatorId);
        if (quantity <= 0) {
            throw new IllegalArgumentException("领用出库数量必须大于0");
        }
        Consumable consumable = getByCodeOrThrow(code);
        int currentStock = consumable.getCurrentStock();
        if (currentStock < quantity) {
            throw new IllegalStateException(String.format(
                    "耗材库存不足：[%s-%s]当前库存为 %d%s，申请领用 %d%s",
                    consumable.getCode(), consumable.getName(),
                    currentStock, consumable.getUnit(),
                    quantity, consumable.getUnit()));
        }
        int newStock = currentStock - quantity;
        consumable.setCurrentStock(newStock);
        consumableDao.save(consumable);

        String alertMsg = consumable.isLowStock()
                ? String.format("【触发低库存预警：当前%d < 阈值%d】", newStock, consumable.getSafetyThreshold())
                : "";
        auditLogDao.append(operatorId, "CONSUMABLE_OUTBOUND", code,
                String.format("耗材[%s]领用出库 -%d%s，库存 %d -> %d%s，用途：%s %s",
                        consumable.getName(), quantity, consumable.getUnit(),
                        currentStock, newStock, consumable.getUnit(),
                        purpose == null ? "实验教学消耗" : purpose, alertMsg));
        return consumable;
    }

    /**
     * 查询所有低于安全库存阈值的预警耗材列表（currentStock < safetyThreshold）
     */
    public List<Consumable> getLowStockWarnings() {
        return consumableDao.findLowStock();
    }

    public Consumable getByCodeOrThrow(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("耗材编码不能为空");
        }
        return consumableDao.findByCode(code.trim())
                .orElseThrow(() -> new IllegalArgumentException("未找到耗材: " + code));
    }

    public List<Consumable> findAll() {
        return consumableDao.findAll();
    }
}
