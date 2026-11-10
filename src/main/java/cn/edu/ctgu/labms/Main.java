package cn.edu.ctgu.labms;

import cn.edu.ctgu.labms.controller.LabSystemFacade;
import cn.edu.ctgu.labms.domain.AuditLog;
import cn.edu.ctgu.labms.domain.BorrowRecord;
import cn.edu.ctgu.labms.domain.Consumable;
import cn.edu.ctgu.labms.domain.Equipment;
import cn.edu.ctgu.labms.domain.EquipmentStatus;
import cn.edu.ctgu.labms.domain.MaintenanceScrapRecord;
import cn.edu.ctgu.labms.domain.PurchaseOrder;
import cn.edu.ctgu.labms.domain.Role;
import cn.edu.ctgu.labms.domain.User;
import cn.edu.ctgu.labms.service.EquipmentService;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

/**
 * 《实验室设备管理系统》端到端全流程演示入口
 */
public class Main {

    public static void main(String[] args) throws Exception {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8.name()));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8.name()));

        LabSystemFacade system = new LabSystemFacade();

        printBanner("三峡大学 · 实验室设备与耗材全生命周期管理系统 (LabMS v1.0)");

        // =========================================================================
        // 1. 用户注册、登录认证与 RBAC 权限控制演示
        // =========================================================================
        printSection("模块一：用户管理与 RBAC 角色权限控制");
        User admin = system.registerUser("admin", "Admin@2026", "张伟(系统管理员)", Role.ADMIN, "信息化办公室");
        User labMgr = system.registerUser("lab_li", "Lab@2026", "李华(实验员)", Role.LAB_MANAGER, "计算机实验中心");
        User teacher = system.registerUser("prof_wang", "Wang@2026", "王建国(教授)", Role.TEACHER, "计算机与信息学院");
        User student1 = system.registerUser("stu_chen", "Chen@2026", "陈明(本科生)", Role.STUDENT, "软件工程2301班");
        User student2 = system.registerUser("stu_zhao", "Zhao@2026", "赵敏(研究生)", Role.STUDENT, "智能科学实验室");

        System.out.println("已初始化注册 5 名系统用户：");
        for (User u : system.listAllUsers()) {
            System.out.println("  -> " + u);
        }

        User loggedInMgr = system.login("lab_li", "Lab@2026");
        System.out.println("登录认证成功：" + loggedInMgr.getRealName() + "，当前角色：" + loggedInMgr.getRole().getDisplayName());

        // 演示越权操作拦截（学生尝试直接登记设备台账）
        try {
            system.registerEquipment(student1.getId(), "EQ-ILLEGAL", "非法设备", "X1", "测试", "厂商",
                    1000.0, LocalDate.of(2026, 3, 1), LocalDate.of(2029, 3, 1), "信息技术楼301");
        } catch (SecurityException ex) {
            System.out.println("【RBAC安全拦截演示】" + ex.getMessage());
        }

        // =========================================================================
        // 2. 初始设备台账建档与多条件检索演示
        // =========================================================================
        printSection("模块二：实验设备台账登记与多维条件检索");
        Equipment eqOsc1 = system.registerEquipment(
                labMgr.getId(), "EQ-2026-0001", "数字存储示波器", "TBS2102B (100MHz)",
                "电子测量仪器", "泰克(Tektronix)", 8600.00,
                LocalDate.of(2025, 9, 10), LocalDate.of(2028, 9, 10), "电工电子实验室A201"
        );
        Equipment eqGpu1 = system.registerEquipment(
                labMgr.getId(), "EQ-2026-0002", "深度学习图形工作站", "RTX-4090-Dual (128GB)",
                "计算服务器", "浪潮信息", 42000.00,
                LocalDate.of(2025, 11, 15), LocalDate.of(2028, 11, 15), "人工智能实验室B405"
        );
        Equipment eqOld = system.registerEquipment(
                labMgr.getId(), "EQ-2026-0003", "高精度稳压电源", "IT6322A",
                "电子测量仪器", "艾德克斯(ITECH)", 3200.00,
                LocalDate.of(2019, 5, 20), LocalDate.of(2022, 5, 20), "电工电子实验室A201"
        );

        System.out.println("按分类【电子测量仪器】检索设备台账：");
        List<Equipment> measureEqs = system.queryEquipment(null, "电子测量仪器", null, null);
        for (Equipment eq : measureEqs) {
            System.out.println("  -> " + eq);
        }

        // =========================================================================
        // 3. 设备申购、审批与到货自动批量建账入库演示
        // =========================================================================
        printSection("模块三：设备申购审批与自动批量建账入库联动");
        PurchaseOrder order1 = system.submitPurchaseOrder(
                teacher.getId(), "嵌入式开发实验箱", "STM32H743-Pro",
                "嵌入式教学设备", "正点原子", 3, 2400.00,
                "《嵌入式系统设计》课程实验教学扩充工位"
        );
        System.out.println("教师发起申购单：" + order1);

        List<Equipment> inboundBatch = system.approveAndInboundPurchaseOrder(
                order1.getOrderId(), labMgr.getId(),
                "符合本学期教学大纲预算，批准采购并验收合格",
                "嵌入式系统实验室C302",
                LocalDate.of(2026, 3, 15), 3
        );
        System.out.println("申购单审批通过并完成到货验收，自动批量生成 " + inboundBatch.size() + " 台在库设备：");
        for (Equipment eq : inboundBatch) {
            System.out.println("  -> " + eq);
        }

        // =========================================================================
        // 4. 设备借用、完好归还、受损归还自动触发维修工单与超期检测演示
        // =========================================================================
        printSection("模块四：设备借用、受损归还自动转修与超期预警");
        // 场景4.1：学生1借用第一台嵌入式实验箱并按期完好归还
        Equipment box1 = inboundBatch.get(0);
        BorrowRecord br1 = system.borrowEquipment(
                box1.getCode(), student1.getId(), labMgr.getId(),
                "全国大学生嵌入式芯片与系统设计竞赛备赛",
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 15)
        );
        System.out.println("借出登记 1：" + br1 + " | 当前设备状态：" + box1.getStatus().getDisplayName());

        system.returnEquipment(br1.getRecordId(), labMgr.getId(),
                LocalDate.of(2026, 4, 12), true, null);
        System.out.println("完好归还后：" + br1 + " | 当前设备状态：" + box1.getStatus().getDisplayName());

        // 场景4.2：学生2借用示波器，归还时探头通道损坏 -> 自动流转至 MAINTENANCE 并生成维修工单
        BorrowRecord br2 = system.borrowEquipment(
                eqOsc1.getCode(), student2.getId(), labMgr.getId(),
                "高频信号滤波采集实验",
                LocalDate.of(2026, 4, 2), LocalDate.of(2026, 4, 10)
        );
        System.out.println("借出登记 2：" + br2 + " | 当前设备状态：" + eqOsc1.getStatus().getDisplayName());

        system.returnEquipment(br2.getRecordId(), labMgr.getId(),
                LocalDate.of(2026, 4, 9), false, "CH1通道BNC接口接触不良，波形失真");
        System.out.println("受损归还后：" + br2 + " | 设备自动流转状态：" + eqOsc1.getStatus().getDisplayName());

        // 场景4.3：学生1借用第二台实验箱，模拟超期未还
        Equipment box2 = inboundBatch.get(1);
        BorrowRecord br3 = system.borrowEquipment(
                box2.getCode(), student1.getId(), labMgr.getId(),
                "课程设计硬件联调",
                LocalDate.of(2026, 3, 20), LocalDate.of(2026, 4, 5)
        );
        List<BorrowRecord> overdues = system.listOverdueBorrows(LocalDate.of(2026, 4, 20));
        System.out.println("截至 2026-04-20 超期未还记录数：" + overdues.size());
        for (BorrowRecord r : overdues) {
            System.out.println("  -> 【超期催还】" + r);
        }

        // =========================================================================
        // 5. 设备维修完工恢复在库与老旧设备报废核销演示
        // =========================================================================
        printSection("模块五：设备维修闭环与报废终态核销管理");
        List<MaintenanceScrapRecord> tickets = system.getMaintenanceScrapService().findAllRecords();
        MaintenanceScrapRecord autoTicket = tickets.get(0);
        System.out.println("发现由受损归还自动生成的待处理维修工单：" + autoTicket);

        // 修复示波器，恢复在库可用
        system.completeRepair(autoTicket.getRecordId(), labMgr.getId(), "更换CH1通道BNC高频接头并通过校准测试");
        System.out.println("维修完工后工单状态：" + autoTicket);
        System.out.println("示波器当前状态：" + eqOsc1.getStatus().getDisplayName());

        // 对老旧稳压电源发起故障报修，经检测主板烧毁无维修价值，执行报废审批
        MaintenanceScrapRecord oldFaultTicket = system.reportFault(
                eqOld.getCode(), teacher.getId(), "变压器老旧击穿，输出电压严重漂移"
        );
        System.out.println("老旧设备报修：" + oldFaultTicket + " | 设备状态：" + eqOld.getStatus().getDisplayName());

        MaintenanceScrapRecord scrapRecord = system.approveScrap(
                eqOld.getCode(), labMgr.getId(), admin.getId(),
                "已超质保期且核心控制板停产无法修复",
                "同意按国有资产报废规程核销下账"
        );
        System.out.println("设备报废审批完成：" + scrapRecord + " | 设备最终状态：" + eqOld.getStatus().getDisplayName());

        // 验证已报废设备绝不允许再次借用
        try {
            system.borrowEquipment(eqOld.getCode(), student1.getId(), labMgr.getId(),
                    "尝试借用报废设备", LocalDate.of(2026, 4, 21), LocalDate.of(2026, 4, 25));
        } catch (IllegalStateException ex) {
            System.out.println("【状态机终态保护演示】" + ex.getMessage());
        }

        // =========================================================================
        // 6. 仓库库存盘点统计与完好率分析演示
        // =========================================================================
        printSection("模块六：仓库设备盘点汇总与完好率统计");
        EquipmentService.InventoryStatistics stats = system.getInventoryStatistics();
        System.out.println("【当前仓库盘点大盘】" + stats);
        System.out.println("当前全部设备台账明细：");
        for (Equipment eq : system.listAllEquipment()) {
            System.out.println("  -> " + eq);
        }

        // =========================================================================
        // 7. 实验耗材出入库管理与低库存安全阈值预警演示
        // =========================================================================
        printSection("模块七：实验耗材出入库与低库存自动预警");
        Consumable cSolder = system.registerConsumable(
                labMgr.getId(), "CS-2026-001", "无铅焊锡丝", "0.8mm/500g", "卷",
                30, 15, false, "耗材库房A区-01架"
        );
        Consumable cEthanol = system.registerConsumable(
                labMgr.getId(), "CS-2026-002", "无水乙醇(分析纯)", "500mL/瓶", "瓶",
                12, 10, true, "危化品防爆柜-02号"
        );
        Consumable cResistors = system.registerConsumable(
                labMgr.getId(), "CS-2026-003", "贴片电阻实验包", "0805全系列", "包",
                50, 20, false, "耗材库房A区-03架"
        );

        // 领用出库导致无水乙醇库存跌破安全阈值 (12 - 5 = 7 < 10)
        system.outboundConsumable(teacher.getId(), cEthanol.getCode(), 5, "精密光学镜片清洗实验");
        // 领用出库导致无铅焊锡丝跌破安全阈值 (30 - 18 = 12 < 15)
        system.outboundConsumable(student1.getId(), cSolder.getCode(), 18, "电子工艺实训课程焊接");

        System.out.println("领用后触发【低库存预警】的耗材清单：");
        List<Consumable> warnings = system.listLowStockConsumables();
        for (Consumable c : warnings) {
            System.out.println("  -> " + c);
        }

        // 及时补货入库无水乙醇 (+20瓶 -> 27瓶)，解除该项预警
        system.inboundConsumable(labMgr.getId(), cEthanol.getCode(), 20, "危化品定点采购补库");
        System.out.println("无水乙醇补货 +20瓶 后最新状态：" + cEthanol);

        // =========================================================================
        // 8. 审计日志全链路追踪展示
        // =========================================================================
        printSection("安全审计：系统关键操作全链路日志 (最近 10 条)");
        List<AuditLog> allLogs = system.listAuditLogs();
        int startIdx = Math.max(0, allLogs.size() - 10);
        for (int i = startIdx; i < allLogs.size(); i++) {
            System.out.println("  " + allLogs.get(i));
        }
        System.out.println("\n系统累计记录安全审计日志总数：" + allLogs.size() + " 条。演示圆满结束！");
    }

    private static void printBanner(String title) {
        System.out.println("================================================================================");
        System.out.println("   " + title);
        System.out.println("================================================================================");
    }

    private static void printSection(String sectionTitle) {
        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.println("▶ " + sectionTitle);
        System.out.println("--------------------------------------------------------------------------------");
    }
}
