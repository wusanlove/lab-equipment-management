package cn.edu.ctgu.labms;

import cn.edu.ctgu.labms.controller.LabSystemFacade;
import cn.edu.ctgu.labms.domain.AuditLog;
import cn.edu.ctgu.labms.domain.BorrowRecord;
import cn.edu.ctgu.labms.domain.Consumable;
import cn.edu.ctgu.labms.domain.Equipment;
import cn.edu.ctgu.labms.domain.EquipmentStatus;
import cn.edu.ctgu.labms.domain.MaintenanceScrapRecord;
import cn.edu.ctgu.labms.domain.OrderStatus;
import cn.edu.ctgu.labms.domain.PurchaseOrder;
import cn.edu.ctgu.labms.domain.Role;
import cn.edu.ctgu.labms.domain.User;
import cn.edu.ctgu.labms.service.AuthService;
import cn.edu.ctgu.labms.service.EquipmentService;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

/**
 * 《实验室设备管理系统》零依赖综合单元测试套件（共 40 个测试用例）
 * 覆盖七大业务模块正向流程、状态机非法转移拦截、RBAC越权拦截与边界条件校验
 */
public class AllTests {

    private static int passed = 0;
    private static int failed = 0;

    @FunctionalInterface
    private interface TestCase {
        void run() throws Exception;
    }

    public static void main(String[] args) throws Exception {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8.name()));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8.name()));

        System.out.println("================================================================================");
        System.out.println("   《实验室设备管理系统》自动化单元测试套件 (共 40 项测试)");
        System.out.println("================================================================================");

        // 模块1：用户认证与RBAC权限管理测试 (TC01 - TC08)
        runTest("TC01 - 用户正常注册与SHA-256密码摘要存储验证", AllTests::testUserRegistrationAndPasswordHash);
        runTest("TC02 - 重复用户名注册应抛出IllegalArgumentException", AllTests::testDuplicateUsernameRejected);
        runTest("TC03 - 用户使用正确凭证登录成功并生成审计日志", AllTests::testLoginSuccess);
        runTest("TC04 - 用户登录密码错误应抛出SecurityException拦截", AllTests::testLoginWrongPasswordRejected);
        runTest("TC05 - 管理员禁用用户账号后该用户无法登录", AllTests::testDisabledUserLoginRejected);
        runTest("TC06 - 管理员不允许禁用自身账号（防自锁保护）", AllTests::testAdminCannotDisableSelf);
        runTest("TC07 - 非管理员尝试禁用他人账号应抛出SecurityException", AllTests::testNonAdminCannotDisableUser);
        runTest("TC08 - 管理员正常变更用户角色并记录审计日志", AllTests::testAdminChangeUserRole);

        // 模块2：设备台账与状态机边界测试 (TC09 - TC16)
        runTest("TC09 - 实验室管理员正常登记设备台账（初始状态IN_STOCK）", AllTests::testRegisterEquipmentSuccess);
        runTest("TC10 - 学生尝试越权登记设备台账应抛出SecurityException", AllTests::testStudentRegisterEquipmentRejected);
        runTest("TC11 - 重复设备编码登记应抛出IllegalArgumentException", AllTests::testDuplicateEquipmentCodeRejected);
        runTest("TC12 - 质保到期日早于购置日期应抛出IllegalArgumentException", AllTests::testWarrantyBeforePurchaseDateRejected);
        runTest("TC13 - 多条件组合查询（关键字+分类+状态+位置）精准匹配", AllTests::testMultiCriteriaEquipmentQuery);
        runTest("TC14 - 设备变更存放位置及审计记录验证", AllTests::testRelocateEquipment);
        runTest("TC15 - 状态机校验：SCRAPPED终态严禁流转至IN_STOCK或BORROWED或MAINTENANCE", AllTests::testScrappedTerminalStateProtection);
        runTest("TC16 - 状态机校验：BORROWED状态不可直接流转至SCRAPPED", AllTests::testBorrowedCannotDirectlyScrap);

        // 模块3：设备申购审批与自动入库联动测试 (TC17 - TC22)
        runTest("TC17 - 教师提交设备申购单（初始状态PENDING，总金额计算正确）", AllTests::testTeacherSubmitPurchaseOrder);
        runTest("TC18 - 学生尝试提交设备申购单应被RBAC拦截", AllTests::testStudentSubmitPurchaseOrderRejected);
        runTest("TC19 - 申购数量小于等于0或单价为负数应抛出异常", AllTests::testInvalidPurchaseOrderQuantityOrPrice);
        runTest("TC20 - 申购单审批通过并验收入库后自动批量创建IN_STOCK设备", AllTests::testPurchaseApproveAndBatchAutoInbound);
        runTest("TC21 - 申购单被驳回后状态变为REJECTED且禁止执行入库", AllTests::testRejectedPurchaseOrderCannotInbound);
        runTest("TC22 - 已审批申购单不可重复审批", AllTests::testDuplicatePurchaseOrderReviewRejected);

        // 模块4：设备借用、受损归还自动转修与超期检测测试 (TC23 - TC29)
        runTest("TC23 - 正常借用IN_STOCK设备后设备状态变更为BORROWED", AllTests::testBorrowInStockEquipmentSuccess);
        runTest("TC24 - 尝试重复借用已处于BORROWED状态的设备应抛出异常", AllTests::testBorrowAlreadyBorrowedEquipmentRejected);
        runTest("TC25 - 尝试借用处于MAINTENANCE或SCRAPPED状态的设备应抛出异常", AllTests::testBorrowMaintenanceOrScrappedEquipmentRejected);
        runTest("TC26 - 借用设备完好归还（conditionOk=true）后恢复为IN_STOCK", AllTests::testReturnEquipmentIntactRestoresInStock);
        runTest("TC27 - 借用设备受损归还（conditionOk=false）自动流转至MAINTENANCE并创建维修单", AllTests::testReturnEquipmentDamagedAutoTriggersMaintenance);
        runTest("TC28 - 已归还的借用记录不可重复执行归还", AllTests::testDuplicateReturnRejected);
        runTest("TC29 - 超期未还借用记录检测与已按期归还过滤验证", AllTests::testOverdueBorrowDetection);

        // 模块5：设备报修、维修完工与报废核销测试 (TC30 - TC34)
        runTest("TC30 - 在库设备发起故障报修后状态变更为MAINTENANCE", AllTests::testReportFaultTransitionsToMaintenance);
        runTest("TC31 - 维修工单完工后设备状态由MAINTENANCE恢复为IN_STOCK", AllTests::testCompleteRepairRestoresInStock);
        runTest("TC32 - 已完工的维修工单不可重复执行完工操作", AllTests::testDuplicateCompleteRepairRejected);
        runTest("TC33 - 实验室管理员或系统管理员批准报废后设备永久变为SCRAPPED并自动结案待修单", AllTests::testApproveScrapPermanentAndClosesPendingMaintenance);
        runTest("TC34 - 学生或普通教师无权批准设备报废（RBAC拦截）", AllTests::testUnauthorizedScrapApprovalRejected);

        // 模块6：仓库盘点与完好率统计测试 (TC35 - TC36)
        runTest("TC35 - 仓库库存多状态计数、资产总值与设备完好率公式精确验证", AllTests::testWarehouseInventoryStatisticsAndIntactRate);
        runTest("TC36 - 空仓库边界条件下完好率默认100%且无除零异常", AllTests::testEmptyWarehouseStatisticsBoundary);

        // 模块7：耗材出入库与低库存阈值边界测试 (TC37 - TC40)
        runTest("TC37 - 耗材建档与补充入库后库存准确累加", AllTests::testConsumableRegisterAndInbound);
        runTest("TC38 - 耗材领用出库超过当前库存应抛出IllegalStateException拦截", AllTests::testConsumableOutboundExceedingStockRejected);
        runTest("TC39 - 低库存预警边界测试：currentStock == safetyThreshold 不报警，currentStock < safetyThreshold 报警", AllTests::testLowStockWarningThresholdBoundary);
        runTest("TC40 - 全链路操作安全审计日志完整性与按操作人检索验证", AllTests::testAuditLogIntegrityAndQuery);

        System.out.println("================================================================================");
        System.out.printf("Tests passed: %d, failed: %d%n", passed, failed);
        System.out.println("================================================================================");

        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void runTest(String name, TestCase testCase) {
        try {
            testCase.run();
            passed++;
            System.out.println("[PASS] " + name);
        } catch (Throwable t) {
            failed++;
            System.out.println("[FAIL] " + name + " -> " + t.getClass().getSimpleName() + ": " + t.getMessage());
            t.printStackTrace(System.out);
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null && actual == null) {
            return;
        }
        if (expected == null || !expected.equals(actual)) {
            throw new AssertionError(message + " [expected=" + expected + ", actual=" + actual + "]");
        }
    }

    private static void assertDoubleEquals(double expected, double actual, double delta, String message) {
        if (Math.abs(expected - actual) > delta) {
            throw new AssertionError(message + " [expected=" + expected + ", actual=" + actual + "]");
        }
    }

    private static <T extends Throwable> void assertThrows(Class<T> expectedType, Runnable runnable, String message) {
        try {
            runnable.run();
        } catch (Throwable actual) {
            if (expectedType.isInstance(actual)) {
                return;
            }
            throw new AssertionError(message + " [expected exception " + expectedType.getSimpleName()
                    + " but got " + actual.getClass().getSimpleName() + ": " + actual.getMessage() + "]");
        }
        throw new AssertionError(message + " [expected exception " + expectedType.getSimpleName() + " but nothing was thrown]");
    }

    // ==================== 具体测试用例实现 ====================

    private static void testUserRegistrationAndPasswordHash() {
        LabSystemFacade sys = new LabSystemFacade();
        User u = sys.registerUser("alice", "Secret#123", "爱丽丝", Role.ADMIN, "信息中心");
        assertTrue(u.getId().startsWith("USR-"), "用户ID前缀应为USR-");
        assertEquals("alice", u.getUsername(), "用户名应一致");
        assertTrue(!u.getPasswordHash().equals("Secret#123"), "密码必须经过SHA-256哈希而非明文保存");
        assertEquals(64, u.getPasswordHash().length(), "SHA-256十六进制摘要长度应为64");
        assertEquals(AuthService.hashPassword("Secret#123"), u.getPasswordHash(), "哈希值应匹配");
    }

    private static void testDuplicateUsernameRejected() {
        LabSystemFacade sys = new LabSystemFacade();
        sys.registerUser("bob", "Pass#1", "鲍勃", Role.STUDENT, "计算机学院");
        assertThrows(IllegalArgumentException.class,
                () -> sys.registerUser("BOB", "Pass#2", "鲍勃2", Role.TEACHER, "计算机学院"),
                "不区分大小写的重复用户名注册应被拒绝");
    }

    private static void testLoginSuccess() {
        LabSystemFacade sys = new LabSystemFacade();
        User created = sys.registerUser("lab_mgr", "Mgr@2026", "李管理员", Role.LAB_MANAGER, "实验中心");
        User logged = sys.login("lab_mgr", "Mgr@2026");
        assertEquals(created.getId(), logged.getId(), "登录返回的用户ID应一致");
    }

    private static void testLoginWrongPasswordRejected() {
        LabSystemFacade sys = new LabSystemFacade();
        sys.registerUser("lab_mgr", "Mgr@2026", "李管理员", Role.LAB_MANAGER, "实验中心");
        assertThrows(SecurityException.class,
                () -> sys.login("lab_mgr", "WrongPass"),
                "错误密码登录应抛出SecurityException");
    }

    private static void testDisabledUserLoginRejected() {
        LabSystemFacade sys = new LabSystemFacade();
        User admin = sys.registerUser("admin", "Admin@1", "管理员", Role.ADMIN, "中心");
        User stu = sys.registerUser("stu1", "Stu@1", "学生甲", Role.STUDENT, "软件学院");
        sys.setUserActive(admin.getId(), stu.getId(), false);
        assertTrue(!stu.isActive(), "用户状态应为已禁用");
        assertThrows(SecurityException.class,
                () -> sys.login("stu1", "Stu@1"),
                "已禁用账号登录应抛出SecurityException");
    }

    private static void testAdminCannotDisableSelf() {
        LabSystemFacade sys = new LabSystemFacade();
        User admin = sys.registerUser("admin", "Admin@1", "管理员", Role.ADMIN, "中心");
        assertThrows(IllegalArgumentException.class,
                () -> sys.setUserActive(admin.getId(), admin.getId(), false),
                "管理员禁用自身账号应抛出IllegalArgumentException");
    }

    private static void testNonAdminCannotDisableUser() {
        LabSystemFacade sys = new LabSystemFacade();
        User mgr = sys.registerUser("mgr", "Mgr@1", "实验员", Role.LAB_MANAGER, "中心");
        User stu = sys.registerUser("stu", "Stu@1", "学生", Role.STUDENT, "学院");
        assertThrows(SecurityException.class,
                () -> sys.setUserActive(mgr.getId(), stu.getId(), false),
                "非系统管理员尝试禁用账号应抛出SecurityException");
    }

    private static void testAdminChangeUserRole() {
        LabSystemFacade sys = new LabSystemFacade();
        User admin = sys.registerUser("admin", "Admin@1", "管理员", Role.ADMIN, "中心");
        User teacher = sys.registerUser("t1", "T@1", "王老师", Role.TEACHER, "学院");
        sys.changeUserRole(admin.getId(), teacher.getId(), Role.LAB_MANAGER);
        assertEquals(Role.LAB_MANAGER, teacher.getRole(), "角色应已变更为LAB_MANAGER");
    }

    private static void testRegisterEquipmentSuccess() {
        LabSystemFacade sys = new LabSystemFacade();
        User mgr = sys.registerUser("mgr", "Mgr@1", "实验员", Role.LAB_MANAGER, "中心");
        Equipment eq = sys.registerEquipment(mgr.getId(), "EQ-001", "逻辑分析仪", "LA5016",
                "电子测量", "Kingst", 1500.0, LocalDate.of(2026, 1, 1), LocalDate.of(2029, 1, 1), "A201");
        assertEquals("EQ-001", eq.getCode(), "设备编码应为EQ-001");
        assertEquals(EquipmentStatus.IN_STOCK, eq.getStatus(), "新入账设备初始状态必须为IN_STOCK");
    }

    private static void testStudentRegisterEquipmentRejected() {
        LabSystemFacade sys = new LabSystemFacade();
        User stu = sys.registerUser("stu", "Stu@1", "学生", Role.STUDENT, "学院");
        assertThrows(SecurityException.class,
                () -> sys.registerEquipment(stu.getId(), "EQ-002", "示波器", "DS1102",
                        "电子测量", "普源", 3000.0, LocalDate.of(2026, 1, 1), LocalDate.of(2029, 1, 1), "A201"),
                "学生角色登记设备应抛出SecurityException");
    }

    private static void testDuplicateEquipmentCodeRejected() {
        LabSystemFacade sys = new LabSystemFacade();
        User mgr = sys.registerUser("mgr", "Mgr@1", "实验员", Role.LAB_MANAGER, "中心");
        sys.registerEquipment(mgr.getId(), "EQ-DUP", "设备A", "M1", "类别A", "厂A",
                1000.0, LocalDate.of(2026, 1, 1), LocalDate.of(2028, 1, 1), "A101");
        assertThrows(IllegalArgumentException.class,
                () -> sys.registerEquipment(mgr.getId(), "EQ-DUP", "设备B", "M2", "类别A", "厂A",
                        1200.0, LocalDate.of(2026, 1, 1), LocalDate.of(2028, 1, 1), "A101"),
                "重复设备编码应抛出IllegalArgumentException");
    }

    private static void testWarrantyBeforePurchaseDateRejected() {
        LabSystemFacade sys = new LabSystemFacade();
        User mgr = sys.registerUser("mgr", "Mgr@1", "实验员", Role.LAB_MANAGER, "中心");
        assertThrows(IllegalArgumentException.class,
                () -> sys.registerEquipment(mgr.getId(), "EQ-DATE", "设备", "M1", "类别", "厂商",
                        1000.0, LocalDate.of(2026, 5, 1), LocalDate.of(2025, 5, 1), "A101"),
                "质保日期早于购置日期应抛出IllegalArgumentException");
    }

    private static void testMultiCriteriaEquipmentQuery() {
        LabSystemFacade sys = new LabSystemFacade();
        User mgr = sys.registerUser("mgr", "Mgr@1", "实验员", Role.LAB_MANAGER, "中心");
        sys.registerEquipment(mgr.getId(), "EQ-A1", "泰克示波器", "TBS2000", "测量仪器", "泰克",
                8000.0, LocalDate.of(2026, 1, 1), LocalDate.of(2029, 1, 1), "电工实验室201");
        sys.registerEquipment(mgr.getId(), "EQ-A2", "普源信号源", "DG1022", "测量仪器", "普源",
                2500.0, LocalDate.of(2026, 1, 1), LocalDate.of(2029, 1, 1), "电工实验室201");
        sys.registerEquipment(mgr.getId(), "EQ-B1", "矩阵服务器", "RTX4090", "计算设备", "浪潮",
                45000.0, LocalDate.of(2026, 1, 1), LocalDate.of(2029, 1, 1), "AI实验室405");

        List<Equipment> r1 = sys.queryEquipment("示波器", "测量仪器", EquipmentStatus.IN_STOCK, "201");
        assertEquals(1, r1.size(), "组合条件应精准命中1台示波器");
        assertEquals("EQ-A1", r1.get(0).getCode(), "命中编码应为EQ-A1");

        List<Equipment> r2 = sys.queryEquipment(null, "测量仪器", null, null);
        assertEquals(2, r2.size(), "测量仪器分类应返回2台");
    }

    private static void testRelocateEquipment() {
        LabSystemFacade sys = new LabSystemFacade();
        User mgr = sys.registerUser("mgr", "Mgr@1", "实验员", Role.LAB_MANAGER, "中心");
        sys.registerEquipment(mgr.getId(), "EQ-LOC", "激光测距仪", "LD100", "光学仪器", "徕卡",
                4000.0, LocalDate.of(2026, 1, 1), LocalDate.of(2029, 1, 1), "旧库房101");
        Equipment updated = sys.relocateEquipment(mgr.getId(), "EQ-LOC", "光学精密实验室305");
        assertEquals("光学精密实验室305", updated.getLocation(), "位置应已更新");
    }

    private static void testScrappedTerminalStateProtection() {
        LabSystemFacade sys = new LabSystemFacade();
        User mgr = sys.registerUser("mgr", "Mgr@1", "实验员", Role.LAB_MANAGER, "中心");
        sys.registerEquipment(mgr.getId(), "EQ-SCRAP", "报废测试仪", "S1", "测试", "厂商",
                1000.0, LocalDate.of(2020, 1, 1), LocalDate.of(2023, 1, 1), "A101");
        sys.approveScrap("EQ-SCRAP", mgr.getId(), mgr.getId(), "主板彻底损毁", "同意报废");

        assertThrows(IllegalStateException.class,
                () -> sys.getEquipmentService().transitionStatus("EQ-SCRAP", EquipmentStatus.IN_STOCK, mgr.getId(), "试图复活"),
                "已报废设备不能流转回IN_STOCK");
        assertThrows(IllegalStateException.class,
                () -> sys.getEquipmentService().transitionStatus("EQ-SCRAP", EquipmentStatus.BORROWED, mgr.getId(), "试图借出"),
                "已报废设备不能流转至BORROWED");
        assertThrows(IllegalStateException.class,
                () -> sys.getEquipmentService().transitionStatus("EQ-SCRAP", EquipmentStatus.MAINTENANCE, mgr.getId(), "试图维修"),
                "已报废设备不能流转至MAINTENANCE");
    }

    private static void testBorrowedCannotDirectlyScrap() {
        LabSystemFacade sys = new LabSystemFacade();
        User mgr = sys.registerUser("mgr", "Mgr@1", "实验员", Role.LAB_MANAGER, "中心");
        User stu = sys.registerUser("stu", "Stu@1", "学生", Role.STUDENT, "学院");
        sys.registerEquipment(mgr.getId(), "EQ-B1", "万用表", "FLUKE-17B", "测量", "福禄克",
                900.0, LocalDate.of(2025, 1, 1), LocalDate.of(2028, 1, 1), "A101");
        sys.borrowEquipment("EQ-B1", stu.getId(), mgr.getId(), "实验借用",
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 10));

        assertThrows(IllegalStateException.class,
                () -> sys.approveScrap("EQ-B1", mgr.getId(), mgr.getId(), "借出中直接报废", "不允许"),
                "处于BORROWED借出状态的设备不可未归还直接报废");
    }

    private static void testTeacherSubmitPurchaseOrder() {
        LabSystemFacade sys = new LabSystemFacade();
        User teacher = sys.registerUser("t1", "T@1", "周老师", Role.TEACHER, "智能系");
        PurchaseOrder order = sys.submitPurchaseOrder(teacher.getId(), "无人机飞控开发套件", "PX4-Vision",
                "机器人设备", "大疆/Holybro", 4, 3500.0, "智能无人系统课程建设");
        assertEquals(OrderStatus.PENDING, order.getStatus(), "新申购单状态应为PENDING");
        assertDoubleEquals(14000.0, order.getTotalAmount(), 0.001, "申购总金额应为 4 * 3500 = 14000");
    }

    private static void testStudentSubmitPurchaseOrderRejected() {
        LabSystemFacade sys = new LabSystemFacade();
        User stu = sys.registerUser("stu", "Stu@1", "学生", Role.STUDENT, "学院");
        assertThrows(SecurityException.class,
                () -> sys.submitPurchaseOrder(stu.getId(), "游戏显卡", "RTX4090", "计算", "NVIDIA", 1, 15000.0, "个人使用"),
                "学生不允许提交设备采购申请单");
    }

    private static void testInvalidPurchaseOrderQuantityOrPrice() {
        LabSystemFacade sys = new LabSystemFacade();
        User teacher = sys.registerUser("t1", "T@1", "周老师", Role.TEACHER, "智能系");
        assertThrows(IllegalArgumentException.class,
                () -> sys.submitPurchaseOrder(teacher.getId(), "设备", "M1", "类别", "厂商", 0, 100.0, "数量为0"),
                "申购数量为0应抛出IllegalArgumentException");
        assertThrows(IllegalArgumentException.class,
                () -> sys.submitPurchaseOrder(teacher.getId(), "设备", "M1", "类别", "厂商", 2, -50.0, "负单价"),
                "申购单价为负应抛出IllegalArgumentException");
    }

    private static void testPurchaseApproveAndBatchAutoInbound() {
        LabSystemFacade sys = new LabSystemFacade();
        User teacher = sys.registerUser("t1", "T@1", "周老师", Role.TEACHER, "智能系");
        User mgr = sys.registerUser("mgr", "Mgr@1", "实验员", Role.LAB_MANAGER, "中心");

        PurchaseOrder order = sys.submitPurchaseOrder(teacher.getId(), "物联网传感节点箱", "ZigBee-Pro",
                "物联网设备", "百科荣创", 5, 1800.0, "物联网工程实验");
        List<Equipment> created = sys.approveAndInboundPurchaseOrder(
                order.getOrderId(), mgr.getId(), "批准采购并完成验收",
                "物联网实验室B301", LocalDate.of(2026, 3, 20), 3);

        assertEquals(5, created.size(), "应自动批量生成5台设备台账");
        assertEquals(OrderStatus.COMPLETED, order.getStatus(), "入库完成后申购单状态应为COMPLETED");
        for (Equipment eq : created) {
            assertEquals(EquipmentStatus.IN_STOCK, eq.getStatus(), "自动入库设备状态均应为IN_STOCK");
            assertEquals("物联网实验室B301", eq.getLocation(), "存放位置应一致");
            assertEquals(LocalDate.of(2029, 3, 20), eq.getWarrantyUntil(), "质保期应为购置日+3年");
        }
    }

    private static void testRejectedPurchaseOrderCannotInbound() {
        LabSystemFacade sys = new LabSystemFacade();
        User teacher = sys.registerUser("t1", "T@1", "周老师", Role.TEACHER, "智能系");
        User mgr = sys.registerUser("mgr", "Mgr@1", "实验员", Role.LAB_MANAGER, "中心");

        PurchaseOrder order = sys.submitPurchaseOrder(teacher.getId(), "超算节点", "A100-80G",
                "算力设备", "NVIDIA", 2, 120000.0, "算力扩充");
        sys.reviewPurchaseOrder(order.getOrderId(), mgr.getId(), false, "超出年度专项预算，予以驳回");
        assertEquals(OrderStatus.REJECTED, order.getStatus(), "审批状态应为REJECTED");

        assertThrows(IllegalStateException.class,
                () -> sys.receivePurchaseOrder(order.getOrderId(), mgr.getId(), "B401", LocalDate.now(), 3),
                "已驳回的申购单尝试验收入库应抛出IllegalStateException");
    }

    private static void testDuplicatePurchaseOrderReviewRejected() {
        LabSystemFacade sys = new LabSystemFacade();
        User teacher = sys.registerUser("t1", "T@1", "周老师", Role.TEACHER, "智能系");
        User mgr = sys.registerUser("mgr", "Mgr@1", "实验员", Role.LAB_MANAGER, "中心");

        PurchaseOrder order = sys.submitPurchaseOrder(teacher.getId(), "交换机", "S5735",
                "网络设备", "华为", 2, 4500.0, "网络工程实验");
        sys.reviewPurchaseOrder(order.getOrderId(), mgr.getId(), true, "同意");
        assertThrows(IllegalStateException.class,
                () -> sys.reviewPurchaseOrder(order.getOrderId(), mgr.getId(), false, "反悔驳回"),
                "已审批申购单再次审批应抛出IllegalStateException");
    }

    private static void testBorrowInStockEquipmentSuccess() {
        LabSystemFacade sys = new LabSystemFacade();
        User mgr = sys.registerUser("mgr", "Mgr@1", "实验员", Role.LAB_MANAGER, "中心");
        User stu = sys.registerUser("stu", "Stu@1", "学生", Role.STUDENT, "学院");
        Equipment eq = sys.registerEquipment(mgr.getId(), "EQ-101", "频谱分析仪", "DSA815",
                "射频仪器", "普源", 9800.0, LocalDate.of(2025, 6, 1), LocalDate.of(2028, 6, 1), "射频室102");

        BorrowRecord br = sys.borrowEquipment("EQ-101", stu.getId(), mgr.getId(), "天线驻波比测量",
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 8));
        assertTrue(!br.isReturned(), "新借出记录应处于未归还状态");
        assertEquals(EquipmentStatus.BORROWED, eq.getStatus(), "借出后设备状态应为BORROWED");
    }

    private static void testBorrowAlreadyBorrowedEquipmentRejected() {
        LabSystemFacade sys = new LabSystemFacade();
        User mgr = sys.registerUser("mgr", "Mgr@1", "实验员", Role.LAB_MANAGER, "中心");
        User stu1 = sys.registerUser("stu1", "Stu@1", "学生1", Role.STUDENT, "学院");
        User stu2 = sys.registerUser("stu2", "Stu@2", "学生2", Role.STUDENT, "学院");
        sys.registerEquipment(mgr.getId(), "EQ-102", "信号发生器", "AFG1022",
                "测量仪器", "泰克", 4200.0, LocalDate.of(2025, 6, 1), LocalDate.of(2028, 6, 1), "A201");

        sys.borrowEquipment("EQ-102", stu1.getId(), mgr.getId(), "实验1",
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 8));
        assertThrows(IllegalStateException.class,
                () -> sys.borrowEquipment("EQ-102", stu2.getId(), mgr.getId(), "实验2",
                        LocalDate.of(2026, 4, 2), LocalDate.of(2026, 4, 9)),
                "已被借出的设备在归还前不可被再次借出");
    }

    private static void testBorrowMaintenanceOrScrappedEquipmentRejected() {
        LabSystemFacade sys = new LabSystemFacade();
        User mgr = sys.registerUser("mgr", "Mgr@1", "实验员", Role.LAB_MANAGER, "中心");
        User stu = sys.registerUser("stu", "Stu@1", "学生", Role.STUDENT, "学院");
        sys.registerEquipment(mgr.getId(), "EQ-M1", "故障设备", "M1", "测试", "厂商",
                1000.0, LocalDate.of(2025, 1, 1), LocalDate.of(2028, 1, 1), "A101");
        sys.reportFault("EQ-M1", mgr.getId(), "开机无显示");

        assertThrows(IllegalStateException.class,
                () -> sys.borrowEquipment("EQ-M1", stu.getId(), mgr.getId(), "借用维修中设备",
                        LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 5)),
                "维修中设备不可借出");

        sys.approveScrap("EQ-M1", mgr.getId(), mgr.getId(), "无法修复", "报废");
        assertThrows(IllegalStateException.class,
                () -> sys.borrowEquipment("EQ-M1", stu.getId(), mgr.getId(), "借用已报废设备",
                        LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 5)),
                "已报废设备不可借出");
    }

    private static void testReturnEquipmentIntactRestoresInStock() {
        LabSystemFacade sys = new LabSystemFacade();
        User mgr = sys.registerUser("mgr", "Mgr@1", "实验员", Role.LAB_MANAGER, "中心");
        User stu = sys.registerUser("stu", "Stu@1", "学生", Role.STUDENT, "学院");
        Equipment eq = sys.registerEquipment(mgr.getId(), "EQ-RET1", "红外热像仪", "FLIR-E8",
                "热学仪器", "FLIR", 16000.0, LocalDate.of(2025, 1, 1), LocalDate.of(2028, 1, 1), "A301");

        BorrowRecord br = sys.borrowEquipment("EQ-RET1", stu.getId(), mgr.getId(), "电路板发热分析",
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 10));
        sys.returnEquipment(br.getRecordId(), mgr.getId(), LocalDate.of(2026, 4, 6), true, null);

        assertTrue(br.isReturned(), "借用记录应标记为已归还");
        assertEquals(Boolean.TRUE, br.getReturnConditionOk(), "归还完好标志应为true");
        assertEquals(EquipmentStatus.IN_STOCK, eq.getStatus(), "完好归还后设备应恢复为IN_STOCK");
    }

    private static void testReturnEquipmentDamagedAutoTriggersMaintenance() {
        LabSystemFacade sys = new LabSystemFacade();
        User mgr = sys.registerUser("mgr", "Mgr@1", "实验员", Role.LAB_MANAGER, "中心");
        User stu = sys.registerUser("stu", "Stu@1", "学生", Role.STUDENT, "学院");
        Equipment eq = sys.registerEquipment(mgr.getId(), "EQ-RET2", "机械臂实训台", "DOBOT-Magician",
                "机器人设备", "越疆", 12000.0, LocalDate.of(2025, 1, 1), LocalDate.of(2028, 1, 1), "B202");

        BorrowRecord br = sys.borrowEquipment("EQ-RET2", stu.getId(), mgr.getId(), "视觉抓取实验",
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 10));
        sys.returnEquipment(br.getRecordId(), mgr.getId(), LocalDate.of(2026, 4, 7), false, "末端舵机齿轮卡死");

        assertTrue(br.isReturned(), "借用记录应标记为已归还");
        assertEquals(Boolean.FALSE, br.getReturnConditionOk(), "归还完好标志应为false");
        assertEquals(EquipmentStatus.MAINTENANCE, eq.getStatus(), "受损归还后设备必须自动转入MAINTENANCE状态");

        List<MaintenanceScrapRecord> tickets = sys.getMaintenanceScrapService().findAllRecords();
        assertEquals(1, tickets.size(), "受损归还必须自动生成1张维修工单");
        assertEquals("EQ-RET2", tickets.get(0).getEquipmentCode(), "维修工单关联设备编码应为EQ-RET2");
        assertEquals(MaintenanceScrapRecord.RecordType.MAINTENANCE, tickets.get(0).getType(), "工单类型应为MAINTENANCE");
        assertEquals(OrderStatus.PENDING, tickets.get(0).getStatus(), "新生成维修工单状态应为PENDING");
    }

    private static void testDuplicateReturnRejected() {
        LabSystemFacade sys = new LabSystemFacade();
        User mgr = sys.registerUser("mgr", "Mgr@1", "实验员", Role.LAB_MANAGER, "中心");
        User stu = sys.registerUser("stu", "Stu@1", "学生", Role.STUDENT, "学院");
        sys.registerEquipment(mgr.getId(), "EQ-RET3", "电桥测试仪", "TH2810D",
                "测量", "同惠", 3500.0, LocalDate.of(2025, 1, 1), LocalDate.of(2028, 1, 1), "A201");

        BorrowRecord br = sys.borrowEquipment("EQ-RET3", stu.getId(), mgr.getId(), "电感测量",
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 5));
        sys.returnEquipment(br.getRecordId(), mgr.getId(), LocalDate.of(2026, 4, 4), true, null);

        assertThrows(IllegalStateException.class,
                () -> sys.returnEquipment(br.getRecordId(), mgr.getId(), LocalDate.of(2026, 4, 5), true, null),
                "重复归还同一借用单应抛出IllegalStateException");
    }

    private static void testOverdueBorrowDetection() {
        LabSystemFacade sys = new LabSystemFacade();
        User mgr = sys.registerUser("mgr", "Mgr@1", "实验员", Role.LAB_MANAGER, "中心");
        User stu = sys.registerUser("stu", "Stu@1", "学生", Role.STUDENT, "学院");
        sys.registerEquipment(mgr.getId(), "EQ-OD1", "设备1", "M1", "类别", "厂商",
                1000.0, LocalDate.of(2025, 1, 1), LocalDate.of(2028, 1, 1), "A101");
        sys.registerEquipment(mgr.getId(), "EQ-OD2", "设备2", "M2", "类别", "厂商",
                1000.0, LocalDate.of(2025, 1, 1), LocalDate.of(2028, 1, 1), "A101");

        // EQ-OD1 应还日期 2026-04-05（截至 04-10 已超期）
        sys.borrowEquipment("EQ-OD1", stu.getId(), mgr.getId(), "实验A",
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 5));
        // EQ-OD2 应还日期 2026-04-15（截至 04-10 未超期）
        sys.borrowEquipment("EQ-OD2", stu.getId(), mgr.getId(), "实验B",
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 15));

        List<BorrowRecord> overdueList = sys.listOverdueBorrows(LocalDate.of(2026, 4, 10));
        assertEquals(1, overdueList.size(), "截至2026-04-10应只有1条超期记录");
        assertEquals("EQ-OD1", overdueList.get(0).getEquipmentCode(), "超期设备应为EQ-OD1");
    }

    private static void testReportFaultTransitionsToMaintenance() {
        LabSystemFacade sys = new LabSystemFacade();
        User mgr = sys.registerUser("mgr", "Mgr@1", "实验员", Role.LAB_MANAGER, "中心");
        Equipment eq = sys.registerEquipment(mgr.getId(), "EQ-FLT1", "3D打印机", "Bambu-X1C",
                "制造设备", "拓竹", 9999.0, LocalDate.of(2025, 3, 1), LocalDate.of(2028, 3, 1), "创客空间105");

        MaintenanceScrapRecord ticket = sys.reportFault("EQ-FLT1", mgr.getId(), "挤出机喷头堵塞");
        assertEquals(EquipmentStatus.MAINTENANCE, eq.getStatus(), "报修后设备状态应为MAINTENANCE");
        assertEquals(OrderStatus.PENDING, ticket.getStatus(), "报修单状态应为PENDING");
    }

    private static void testCompleteRepairRestoresInStock() {
        LabSystemFacade sys = new LabSystemFacade();
        User mgr = sys.registerUser("mgr", "Mgr@1", "实验员", Role.LAB_MANAGER, "中心");
        Equipment eq = sys.registerEquipment(mgr.getId(), "EQ-FLT2", "3D打印机", "Bambu-X1C",
                "制造设备", "拓竹", 9999.0, LocalDate.of(2025, 3, 1), LocalDate.of(2028, 3, 1), "创客空间105");

        MaintenanceScrapRecord ticket = sys.reportFault("EQ-FLT2", mgr.getId(), "热床温控传感器断路");
        sys.completeRepair(ticket.getRecordId(), mgr.getId(), "更换NTC热敏电阻并重新校准PID");

        assertEquals(EquipmentStatus.IN_STOCK, eq.getStatus(), "维修完工后设备应恢复为IN_STOCK");
        assertEquals(OrderStatus.COMPLETED, ticket.getStatus(), "维修工单状态应更新为COMPLETED");
    }

    private static void testDuplicateCompleteRepairRejected() {
        LabSystemFacade sys = new LabSystemFacade();
        User mgr = sys.registerUser("mgr", "Mgr@1", "实验员", Role.LAB_MANAGER, "中心");
        sys.registerEquipment(mgr.getId(), "EQ-FLT3", "激光切割机", "LC6040",
                "制造设备", "大族", 18000.0, LocalDate.of(2025, 3, 1), LocalDate.of(2028, 3, 1), "创客空间105");

        MaintenanceScrapRecord ticket = sys.reportFault("EQ-FLT3", mgr.getId(), "激光管功率衰减");
        sys.completeRepair(ticket.getRecordId(), mgr.getId(), "更换CO2激光管");

        assertThrows(IllegalStateException.class,
                () -> sys.completeRepair(ticket.getRecordId(), mgr.getId(), "重复点击完工"),
                "已完工工单不可重复完工");
    }

    private static void testApproveScrapPermanentAndClosesPendingMaintenance() {
        LabSystemFacade sys = new LabSystemFacade();
        User admin = sys.registerUser("admin", "Admin@1", "管理员", Role.ADMIN, "中心");
        User mgr = sys.registerUser("mgr", "Mgr@1", "实验员", Role.LAB_MANAGER, "中心");
        Equipment eq = sys.registerEquipment(mgr.getId(), "EQ-SC1", "老旧服务器", "R720",
                "计算设备", "戴尔", 25000.0, LocalDate.of(2016, 1, 1), LocalDate.of(2019, 1, 1), "机房101");

        MaintenanceScrapRecord faultTicket = sys.reportFault("EQ-SC1", mgr.getId(), "RAID卡与主板连带烧毁");
        MaintenanceScrapRecord scrapRecord = sys.approveScrap(
                "EQ-SC1", mgr.getId(), admin.getId(), "服役超8年无配件更换价值", "核准报废");

        assertEquals(EquipmentStatus.SCRAPPED, eq.getStatus(), "设备状态应变为SCRAPPED终态");
        assertEquals(OrderStatus.COMPLETED, scrapRecord.getStatus(), "报废单状态应为COMPLETED");
        assertEquals(OrderStatus.COMPLETED, faultTicket.getStatus(), "关联的未结案维修工单应自动同步结案");
    }

    private static void testUnauthorizedScrapApprovalRejected() {
        LabSystemFacade sys = new LabSystemFacade();
        User mgr = sys.registerUser("mgr", "Mgr@1", "实验员", Role.LAB_MANAGER, "中心");
        User teacher = sys.registerUser("t1", "T@1", "教师", Role.TEACHER, "学院");
        User stu = sys.registerUser("stu", "Stu@1", "学生", Role.STUDENT, "学院");
        sys.registerEquipment(mgr.getId(), "EQ-SC2", "示波器", "DS1000",
                "测量", "普源", 2800.0, LocalDate.of(2024, 1, 1), LocalDate.of(2027, 1, 1), "A201");

        assertThrows(SecurityException.class,
                () -> sys.approveScrap("EQ-SC2", stu.getId(), stu.getId(), "不想用了", "学生自批"),
                "学生无权批准报废");
        assertThrows(SecurityException.class,
                () -> sys.approveScrap("EQ-SC2", teacher.getId(), teacher.getId(), "老化", "教师自批"),
                "普通指导教师无权直接批准国有资产报废");
    }

    private static void testWarehouseInventoryStatisticsAndIntactRate() {
        LabSystemFacade sys = new LabSystemFacade();
        User mgr = sys.registerUser("mgr", "Mgr@1", "实验员", Role.LAB_MANAGER, "中心");
        User stu = sys.registerUser("stu", "Stu@1", "学生", Role.STUDENT, "学院");

        // 共登记 4 台设备：
        // EQ-1: IN_STOCK (1000元)
        // EQ-2: BORROWED (2000元)
        // EQ-3: MAINTENANCE (3000元)
        // EQ-4: SCRAPPED (4000元)
        sys.registerEquipment(mgr.getId(), "EQ-1", "设备1", "M1", "类别", "厂商",
                1000.0, LocalDate.of(2025, 1, 1), LocalDate.of(2028, 1, 1), "A101");
        sys.registerEquipment(mgr.getId(), "EQ-2", "设备2", "M2", "类别", "厂商",
                2000.0, LocalDate.of(2025, 1, 1), LocalDate.of(2028, 1, 1), "A101");
        sys.registerEquipment(mgr.getId(), "EQ-3", "设备3", "M3", "类别", "厂商",
                3000.0, LocalDate.of(2025, 1, 1), LocalDate.of(2028, 1, 1), "A101");
        sys.registerEquipment(mgr.getId(), "EQ-4", "设备4", "M4", "类别", "厂商",
                4000.0, LocalDate.of(2025, 1, 1), LocalDate.of(2028, 1, 1), "A101");

        sys.borrowEquipment("EQ-2", stu.getId(), mgr.getId(), "借用",
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 10));
        sys.reportFault("EQ-3", mgr.getId(), "故障");
        sys.approveScrap("EQ-4", mgr.getId(), mgr.getId(), "报废", "同意");

        EquipmentService.InventoryStatistics stats = sys.getInventoryStatistics();
        assertEquals(4, stats.getTotalCount(), "总台数应为4");
        assertEquals(1, stats.getInStockCount(), "在库数应为1");
        assertEquals(1, stats.getBorrowedCount(), "借出数应为1");
        assertEquals(1, stats.getMaintenanceCount(), "维修数应为1");
        assertEquals(1, stats.getScrappedCount(), "报废数应为1");
        assertDoubleEquals(10000.0, stats.getTotalAssetValue(), 0.001, "资产总值应为10000.0");
        // 完好率 = (IN_STOCK + BORROWED) / TOTAL = 2 / 4 = 50.0%
        assertDoubleEquals(50.0, stats.getIntactRate(), 0.001, "设备完好率应为50.0%");
    }

    private static void testEmptyWarehouseStatisticsBoundary() {
        LabSystemFacade sys = new LabSystemFacade();
        EquipmentService.InventoryStatistics stats = sys.getInventoryStatistics();
        assertEquals(0, stats.getTotalCount(), "空仓库总台数应为0");
        assertDoubleEquals(100.0, stats.getIntactRate(), 0.001, "空仓库完好率应默认为100.0%");
    }

    private static void testConsumableRegisterAndInbound() {
        LabSystemFacade sys = new LabSystemFacade();
        User mgr = sys.registerUser("mgr", "Mgr@1", "实验员", Role.LAB_MANAGER, "中心");
        Consumable c = sys.registerConsumable(mgr.getId(), "CS-01", "杜邦线", "20cm母对母", "扎",
                20, 10, false, "耗材柜A1");
        assertEquals(20, c.getCurrentStock(), "初始库存应为20");
        sys.inboundConsumable(mgr.getId(), "CS-01", 15, "学期初采购补充");
        assertEquals(35, c.getCurrentStock(), "入库+15后库存应为35");
    }

    private static void testConsumableOutboundExceedingStockRejected() {
        LabSystemFacade sys = new LabSystemFacade();
        User mgr = sys.registerUser("mgr", "Mgr@1", "实验员", Role.LAB_MANAGER, "中心");
        User stu = sys.registerUser("stu", "Stu@1", "学生", Role.STUDENT, "学院");
        sys.registerConsumable(mgr.getId(), "CS-02", "STM32最小系统板", "F103C8T6", "块",
                10, 5, false, "耗材柜A2");

        assertThrows(IllegalStateException.class,
                () -> sys.outboundConsumable(stu.getId(), "CS-02", 11, "超额领用"),
                "领用数量(11)超过当前库存(10)时必须抛出IllegalStateException");
    }

    private static void testLowStockWarningThresholdBoundary() {
        LabSystemFacade sys = new LabSystemFacade();
        User mgr = sys.registerUser("mgr", "Mgr@1", "实验员", Role.LAB_MANAGER, "中心");
        User stu = sys.registerUser("stu", "Stu@1", "学生", Role.STUDENT, "学院");
        sys.registerConsumable(mgr.getId(), "CS-BOUND", "异丙醇清洗剂", "500mL", "瓶",
                15, 10, true, "防爆柜01");

        // 领用 5 瓶 -> 剩余 10 瓶，恰好等于安全阈值 10（currentStock < safetyThreshold 为 false，不触发预警）
        sys.outboundConsumable(stu.getId(), "CS-BOUND", 5, "清洗电路板");
        assertEquals(0, sys.listLowStockConsumables().size(), "库存恰好等于安全阈值(10==10)时不应触发低库存预警");

        // 再领用 1 瓶 -> 剩余 9 瓶，小于安全阈值 10（9 < 10，立即触发低库存预警）
        sys.outboundConsumable(stu.getId(), "CS-BOUND", 1, "再次领用");
        List<Consumable> warnings = sys.listLowStockConsumables();
        assertEquals(1, warnings.size(), "库存低于安全阈值(9<10)时必须触发低库存预警");
        assertEquals("CS-BOUND", warnings.get(0).getCode(), "预警耗材编码应为CS-BOUND");
    }

    private static void testAuditLogIntegrityAndQuery() {
        LabSystemFacade sys = new LabSystemFacade();
        User admin = sys.registerUser("admin", "Admin@1", "管理员", Role.ADMIN, "中心");
        User mgr = sys.registerUser("mgr", "Mgr@1", "实验员", Role.LAB_MANAGER, "中心");
        sys.login("mgr", "Mgr@1");
        sys.registerEquipment(mgr.getId(), "EQ-LOG1", "测试仪", "T1", "测量", "厂商",
                500.0, LocalDate.of(2026, 1, 1), LocalDate.of(2029, 1, 1), "A101");

        List<AuditLog> allLogs = sys.listAuditLogs();
        assertTrue(allLogs.size() >= 4, "至少应记录注册x2、登录x1、设备登记x1共4条以上审计日志");

        List<AuditLog> mgrLogs = sys.getDatabase().getAuditLogDao().findByOperator(mgr.getId());
        assertTrue(mgrLogs.size() >= 3, "按操作人mgr检索应包含其注册、登录及登记设备日志");
    }
}
