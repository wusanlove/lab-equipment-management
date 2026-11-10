package cn.edu.ctgu.labms.dao;

/**
 * 内存数据库聚合容器，统一管理各模块DAO实例
 */
public class InMemoryDatabase {
    private final UserDao userDao = new UserDao();
    private final EquipmentDao equipmentDao = new EquipmentDao();
    private final PurchaseOrderDao purchaseOrderDao = new PurchaseOrderDao();
    private final BorrowRecordDao borrowRecordDao = new BorrowRecordDao();
    private final MaintenanceScrapDao maintenanceScrapDao = new MaintenanceScrapDao();
    private final ConsumableDao consumableDao = new ConsumableDao();
    private final AuditLogDao auditLogDao = new AuditLogDao();

    public UserDao getUserDao() {
        return userDao;
    }

    public EquipmentDao getEquipmentDao() {
        return equipmentDao;
    }

    public PurchaseOrderDao getPurchaseOrderDao() {
        return purchaseOrderDao;
    }

    public BorrowRecordDao getBorrowRecordDao() {
        return borrowRecordDao;
    }

    public MaintenanceScrapDao getMaintenanceScrapDao() {
        return maintenanceScrapDao;
    }

    public ConsumableDao getConsumableDao() {
        return consumableDao;
    }

    public AuditLogDao getAuditLogDao() {
        return auditLogDao;
    }

    public void clearAll() {
        userDao.clear();
        equipmentDao.clear();
        purchaseOrderDao.clear();
        borrowRecordDao.clear();
        maintenanceScrapDao.clear();
        consumableDao.clear();
        auditLogDao.clear();
    }
}
