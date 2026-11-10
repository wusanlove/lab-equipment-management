$ErrorActionPreference = 'Stop'
$root = 'D:\BaiduSyncdisk\课内\软件工程\分组作业'
$uml = Join-Path $root 'docs\uml'
$outDir = Join-Path $root '05-项目答辩材料'
$outFile = Join-Path $outDir '第01组-202410120301-王胜运-实验室设备管理系统答辩PPT.pptx'
$tmpFile = 'C:\Users\Public\labms_ppt.pptx'
Remove-Item $tmpFile -Force -ErrorAction SilentlyContinue

$ppt = New-Object -ComObject PowerPoint.Application
$pres = $ppt.Presentations.Add($true)
try { $pres.PageSetup.SlideWidth = 960 } catch { Write-Host "width set skipped" }
try { $pres.PageSetup.SlideHeight = 540 } catch { Write-Host "height set skipped" }

$NAVY = 1576774
$BLUE = 11563306
$GRAY = 6710886
$LIGHT = 16316664

function Add-Slide([string]$title, [int]$idx) {
    $s = $pres.Slides.Add($idx, 12)
    $bar = $s.Shapes.AddShape(1, 0, 0, 960, 78)
    $bar.Fill.ForeColor.RGB = $NAVY
    $bar.Line.Visible = 0
    $tb = $s.Shapes.AddTextbox(1, 46, 16, 868, 48)
    $tf = $tb.TextFrame
    $tf.TextRange.Text = $title
    $tf.TextRange.Font.Name = '微软雅黑'
    $tf.TextRange.Font.Size = 28
    $tf.TextRange.Font.Bold = -1
    $tf.TextRange.Font.Color.RGB = 16777215
    $tf.VerticalAnchor = 3
    $tf.WordWrap = -1
    return $s
}

function Add-Body($s, [string]$text, [int]$size, [int]$left, [int]$top, [int]$width, [int]$height) {
    $tb = $s.Shapes.AddTextbox(1, $left, $top, $width, $height)
    $tf = $tb.TextFrame
    $tf.TextRange.Text = $text
    $tf.TextRange.Font.Name = '微软雅黑'
    $tf.TextRange.Font.Size = $size
    $tf.TextRange.Font.Color.RGB = 3223604
    $tf.TextRange.ParagraphFormat.SpaceWithin = 1.35
    $tf.WordWrap = -1
    $tf.AutoSize = 0
    return $tb
}

function Set-Notes($s, [string]$text) {
    $s.NotesPage.Shapes.Placeholders.Item(2).TextFrame.TextRange.Text = $text
}

function Add-Pic($s, [string]$path, [int]$left, [int]$top, [int]$width) {
    $pic = $s.Shapes.AddPicture($path, 0, -1, $left, $top, -1, -1)
    $ratio = $pic.Height / $pic.Width
    $pic.Width = $width
    $pic.Height = [int]($width * $ratio)
    if ($pic.Height -gt 380) {
        $pic.Height = 380
        $pic.Width = [int](380 / $ratio)
    }
    return $pic
}

# ---------- Slide 1: Cover ----------
$s = $pres.Slides.Add(1, 12)
$bg = $s.Shapes.AddShape(1, 0, 0, 960, 540)
$bg.Fill.ForeColor.RGB = $NAVY
$bg.Line.Visible = 0
$t = $s.Shapes.AddTextbox(1, 80, 170, 800, 90)
$t.TextFrame.TextRange.Text = '实验室设备管理系统'
$t.TextFrame.TextRange.Font.Name = '微软雅黑'
$t.TextFrame.TextRange.Font.Size = 48
$t.TextFrame.TextRange.Font.Bold = -1
$t.TextFrame.TextRange.Font.Color.RGB = 16777215
$t.TextFrame.TextRange.ParagraphFormat.Alignment = 2
$st = $s.Shapes.AddTextbox(1, 80, 275, 800, 40)
$st.TextFrame.TextRange.Text = '高校实验室设备与耗材管理 · 软件工程团队项目'
$st.TextFrame.TextRange.Font.Name = '微软雅黑'
$st.TextFrame.TextRange.Font.Size = 20
$st.TextFrame.TextRange.Font.Color.RGB = 14211053
$st.TextFrame.TextRange.ParagraphFormat.Alignment = 2
$info = $s.Shapes.AddTextbox(1, 80, 390, 800, 70)
$info.TextFrame.TextRange.Text = "第 01 组    组长：王胜运（202410120301）`r小组成员：答辩前补充"
$info.TextFrame.TextRange.Font.Name = '微软雅黑'
$info.TextFrame.TextRange.Font.Size = 18
$info.TextFrame.TextRange.Font.Color.RGB = 16777215
$info.TextFrame.TextRange.ParagraphFormat.Alignment = 2
Set-Notes $s "开场：各位老师好，我们是第01组，项目名称是《实验室设备管理系统》。我先简单介绍一下我们做了什么，然后由组员分别讲各个部分。"

# ---------- Slide 2: 一句话介绍 ----------
$s = Add-Slide '一、我们做的是什么' 2
Add-Body $s "要解决的问题：`r实验室的设备（仪器、工具）和耗材（试剂、配件）现在大多靠 Excel 和纸质本子记录，谁借走了、坏了没修、库存还剩多少，都很难查清楚。`r`r我们做的系统：`r把设备的「买 → 入库 → 借出 → 归还/报修 → 报废」整条流程，以及耗材的「入库 → 领用 → 低库存提醒」放到一个系统里统一管理，不同角色登录后看到的功能不一样。" 17 50 110 860 300 | Out-Null
Set-Notes $s "这一页就讲一句：我们做了一个管实验室设备和耗材的系统。以前靠本子记，容易乱；现在在系统里点几下就能查清楚。重点是设备从买到报废的整个流程都在系统里留痕。"

# ---------- Slide 3: 七大功能 ----------
$s = Add-Slide '二、系统有哪些功能' 3
Add-Pic $s (Join-Path $uml 'fig1_system_boundary.png') 40 100 880 | Out-Null
Set-Notes $s "这张图是我们系统的七个功能模块。从上往下看：最左边是权限，谁能干什么；中间是设备从建档、采购、借还到维修报废；右边是库存统计和耗材管理。答辩时不念每一条，只说：一共七块，覆盖了设备从买来到报废的全过程。"

# ---------- Slide 4: 七模块表格 ----------
$s = Add-Slide '三、七个模块分别干什么' 4
$rows = @(
 @('模块','做什么','谁在用'),
 @('安全与权限','登录、分角色、记操作日志','全部用户'),
 @('设备信息','登记设备档案、按条件查设备','实验室管理员'),
 @('设备采购','提采购申请 → 审批 → 到货自动入库','教师、管理员'),
 @('设备使用','申请借用 → 审批 → 归还验收','师生、管理员'),
 @('维修与报废','报修 → 修复，或申请报废','管理员'),
 @('仓库库存','统计在库、借出、维修、报废数量','管理员'),
 @('物资耗材','入库、领用、低于安全库存自动提醒','教师、管理员')
)
$shp = $s.Shapes.AddTable(8, 3, 50, 105, 860, 330)
for ($r = 0; $r -lt 8; $r++) {
    for ($c = 0; $c -lt 3; $c++) {
        $cell = $shp.Table.Cell($r+1, $c+1)
        $cell.Shape.TextFrame.TextRange.Text = $rows[$r][$c]
        $cell.Shape.TextFrame.TextRange.Font.Name = '微软雅黑'
        $cell.Shape.TextFrame.TextRange.Font.Size = 14
        $cell.Shape.TextFrame.TextRange.Font.Color.RGB = 3223604
        $cell.Shape.Fill.ForeColor.RGB = 16777215
        if ($r -eq 0) {
            $cell.Shape.TextFrame.TextRange.Font.Bold = -1
            $cell.Shape.TextFrame.TextRange.Font.Color.RGB = 16777215
            $cell.Shape.Fill.ForeColor.RGB = $NAVY
        }
    }
}
$shp.Table.Columns.Item(1).Width = 150
$shp.Table.Columns.Item(2).Width = 450
$shp.Table.Columns.Item(3).Width = 260
Set-Notes $s "这一页用表格把七个模块讲清楚，一行一个模块。可以指着说：第一个是权限，第二到第五是设备的一生，第六是库存统计，第七是耗材。讲课老师最关心的其实就是「功能是否完整、能不能对应上需求」，这张表就能对上。"

# ---------- Slide 5: 怎么做的 ----------
$s = Add-Slide '四、系统是怎么搭起来的' 5
Add-Pic $s (Join-Path $uml 'fig7_layered_arch.png') 30 100 900 | Out-Null
Set-Notes $s "这张是分层图。说白了就是分四层：最上面是界面，用户点的按钮；中间是业务逻辑，判断能不能借、库存够不够；下面是数据层，负责存和取；最底下是数据模型，就是设备、用户这些对象。这样分层的好处是：改界面不影响业务，改业务不影响存数据。"

# ---------- Slide 6: 状态机 ----------
$s = Add-Slide '五、设备的四个状态' 6
Add-Pic $s (Join-Path $uml 'fig10_state_diagram.png') 40 95 470 | Out-Null
Add-Body $s "在库：可以借`r`r借出：别人不能重复借`r`r维修中：暂停外借`r`r已报废：终点，不能再变`r`r`r为什么要定死状态？`r防止出现「已经报废的设备还能借出去」这种错误数据。" 16 545 140 380 300 | Out-Null
Set-Notes $s "这页讲设备只有四种状态：在库、借出、维修中、报废。规矩是：报废了就不能再借，借出去了别人就借不了。系统里写死了这个规则，输入错误的操作会直接被拒绝。这是我们项目比较有亮点的一块。"

# ---------- Slide 7: 数据库 ----------
$s = Add-Slide '六、数据怎么存' 7
Add-Pic $s (Join-Path $uml 'fig11_er_diagram.png') 20 95 920 | Out-Null
Set-Notes $s "数据库一共七张表，核心就三块：用户表管人和权限，设备表管每一台设备的档案，借还记录表和维修报废表管发生过的事。表和表之间用编号关联，这样查一台设备的全部历史很容易。"

# ---------- Slide 8: 测试 ----------
$s = Add-Slide '七、测试结果' 8
$shp = $s.Shapes.AddTable(5, 4, 60, 115, 840, 200)
$tdata = @(
 @('测试类型','数量','通过','通过率'),
 @('自动化单元测试','40 项','40 项','100%'),
 @('功能测试用例','36 项','36 项','100%'),
 @('状态机非法操作拦截','11 项','11 项','100%'),
 @('合计','87 项','87 项','100%')
)
for ($r = 0; $r -lt 5; $r++) {
    for ($c = 0; $c -lt 4; $c++) {
        $cell = $shp.Table.Cell($r+1, $c+1)
        $cell.Shape.TextFrame.TextRange.Text = $tdata[$r][$c]
        $cell.Shape.TextFrame.TextRange.Font.Name = '微软雅黑'
        $cell.Shape.TextFrame.TextRange.Font.Size = 16
        $cell.Shape.TextFrame.TextRange.Font.Color.RGB = 3223604
        $cell.Shape.TextFrame.TextRange.ParagraphFormat.Alignment = 2
        if ($r -eq 0) {
            $cell.Shape.TextFrame.TextRange.Font.Bold = -1
            $cell.Shape.TextFrame.TextRange.Font.Color.RGB = 16777215
            $cell.Shape.Fill.ForeColor.RGB = $NAVY
        }
    }
}
Add-Body $s "测试重点：`r1. 正常流程能不能走通`r2. 乱操作会不会被拦住（比如借已报废的设备、耗材领超）`r3. 归还时发现损坏，能不能自动转成维修单" 16 60 350 840 150 | Out-Null
Set-Notes $s "测试这块我们做了两件事：一是写了 40 个自动化单元测试，跑一次就知道有没有改坏；二是按功能写了 36 条测试用例，一条条手工验证。全部通过。测试的重点不是「能不能正常运行」，而是「乱操作会不会被拦住」。"

# ---------- Slide 9: 亮点 ----------
$s = Add-Slide '八、项目的两个亮点' 9
$box1 = $s.Shapes.AddShape(5, 50, 120, 400, 290)
$box1.Fill.ForeColor.RGB = $LIGHT
$box1.Line.ForeColor.RGB = 14277081
$h1 = $s.Shapes.AddTextbox(1, 70, 140, 360, 50)
$h1.TextFrame.TextRange.Text = '亮点一：归还即验收'
$h1.TextFrame.TextRange.Font.Name = '微软雅黑'
$h1.TextFrame.TextRange.Font.Size = 22
$h1.TextFrame.TextRange.Font.Bold = -1
$h1.TextFrame.TextRange.Font.Color.RGB = $NAVY
Add-Body $s "归还时如果勾选「设备有损坏」，`r系统不用人工再填一张报修单，`r会自动把设备改成「维修中」，`r同时生成维修工单。`r`r好处：不会出现「还回来了但没人管」的设备。" 15 70 195 360 200 | Out-Null
$box2 = $s.Shapes.AddShape(5, 510, 120, 400, 290)
$box2.Fill.ForeColor.RGB = $LIGHT
$box2.Line.ForeColor.RGB = 14277081
$h2 = $s.Shapes.AddTextbox(1, 530, 140, 360, 50)
$h2.TextFrame.TextRange.Text = '亮点二：库存自动预警'
$h2.TextFrame.TextRange.Font.Name = '微软雅黑'
$h2.TextFrame.TextRange.Font.Size = 22
$h2.TextFrame.TextRange.Font.Bold = -1
$h2.TextFrame.TextRange.Font.Color.RGB = $NAVY
Add-Body $s "每种耗材都设一个「安全库存」，`r低于这个数就自动标红提醒。`r领用时如果库存不够，`r直接拒绝，不会出现负数。`r`r好处：不会到用的时候才发现没货。" 15 530 195 360 200 | Out-Null
Set-Notes $s "两个小亮点，都是很朴素的功能。第一个是借还联动：还的时候发现坏了，系统自动转维修，省一道手续。第二个是耗材低于安全库存自动提醒，领超了会拦住。老师问「你们有什么创新」，就说这两个点，不要吹太大。"

# ---------- Slide 10: 分工 ----------
$s = Add-Slide '九、小组分工' 10
$shp = $s.Shapes.AddTable(6, 3, 90, 115, 780, 280)
$pdata = @(
 @('成员','主要工作','贡献率'),
 @('王胜运（组长）','总体设计、核心业务编码、集成与答辩组织','待定'),
 @('组员 A','需求分析与原型设计','待定'),
 @('组员 B','数据库与界面实现','待定'),
 @('组员 C','采购、维修模块编码','待定'),
 @('组员 D','测试用例与文档整理','待定')
)
for ($r = 0; $r -lt 6; $r++) {
    for ($c = 0; $c -lt 3; $c++) {
        $cell = $shp.Table.Cell($r+1, $c+1)
        $cell.Shape.TextFrame.TextRange.Text = $pdata[$r][$c]
        $cell.Shape.TextFrame.TextRange.Font.Name = '微软雅黑'
        $cell.Shape.TextFrame.TextRange.Font.Size = 15
        $cell.Shape.TextFrame.TextRange.Font.Color.RGB = 3223604
        if ($r -eq 0) {
            $cell.Shape.TextFrame.TextRange.Font.Bold = -1
            $cell.Shape.TextFrame.TextRange.Font.Color.RGB = 16777215
            $cell.Shape.Fill.ForeColor.RGB = $NAVY
        }
    }
}
$shp.Table.Columns.Item(1).Width = 220
$shp.Table.Columns.Item(2).Width = 430
$shp.Table.Columns.Item(3).Width = 130
Add-Body $s "注：组员姓名、学号与贡献率答辩前按实际情况填写。" 13 90 415 780 30 | Out-Null
Set-Notes $s "这页讲团队分工。每个人负责一块，最后集成。贡献率按课程要求填写，间隔至少 1%，答辩前再定。如果老师问到具体做了什么，就按表格里写的说。"

# ---------- Slide 11: 总结 ----------
$s = Add-Slide '十、总结与收获' 11
Add-Body $s "做完这个项目，我们比较深的体会：`r`r1. 先想清楚再写代码，比写完再改省事得多。需求和设计文档不是走形式，是真的能少返工。`r`r2. 规则写在代码里才靠得住。设备状态这种约束，靠人记一定会出错，靠程序判断就不会。`r`r3. 小团队也要用 Git 分支，不然改乱了自己都找不回来。`r`r不足：功能还是比较基础，界面偏简单，没做真实的数据库，数据存在内存里。" 17 60 110 840 340 | Out-Null
Set-Notes $s "最后总结三条体会，再主动说一句不足：我们没接真实数据库，数据是存在内存里的，重启就没了；界面也比较简单。主动承认不足比被老师问出来好。"

# ---------- Slide 12: 谢谢 ----------
$s = $pres.Slides.Add(12, 12)
$bg2 = $s.Shapes.AddShape(1, 0, 0, 960, 540)
$bg2.Fill.ForeColor.RGB = $NAVY
$bg2.Line.Visible = 0
$tt = $s.Shapes.AddTextbox(1, 80, 220, 800, 90)
$tt.TextFrame.TextRange.Text = '谢谢各位老师'
$tt.TextFrame.TextRange.Font.Name = '微软雅黑'
$tt.TextFrame.TextRange.Font.Size = 40
$tt.TextFrame.TextRange.Font.Bold = -1
$tt.TextFrame.TextRange.Font.Color.RGB = 16777215
$tt.TextFrame.TextRange.ParagraphFormat.Alignment = 2
Set-Notes $s "讲完了，谢谢老师。请老师提问。"

$pres.SaveAs($tmpFile)
$pres.Close()
$ppt.Quit()
[GC]::Collect(); [GC]::WaitForPendingFinalizers()

Copy-Item -LiteralPath $tmpFile -Destination $outFile -Force
Write-Output "PPT generated: $outFile"
