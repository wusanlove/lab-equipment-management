$ErrorActionPreference = 'Continue'
$root = 'D:\BaiduSyncdisk\课内\软件工程\分组作业'

$map = @(
    @('陈宇航', '组员A（待定）'),
    @('林诗涵', '组员B（待定）'),
    @('张明哲', '组员C（待定）'),
    @('刘思源', '组员D（待定）'),
    @('202410120305', '待定'),
    @('202410120312', '待定'),
    @('202410120318', '待定'),
    @('202410120326', '待定')
)

$docs = Get-ChildItem $root -Recurse -Filter '*.docx' -File | Where-Object { $_.Name -like '第01组-*' }
$word = $null
try {
    $word = New-Object -ComObject Word.Application
    $word.Visible = $false
    $word.DisplayAlerts = 0
    foreach ($d in $docs) {
        Write-Host ">>> fixing $($d.Name)"
        $doc = $word.Documents.Open([string]$d.FullName)
        $doc.TrackRevisions = $false
        foreach ($pair in $map) {
            $find = $doc.Content.Find
            $find.ClearFormatting()
            $find.Replacement.ClearFormatting()
            $find.Text = $pair[0]
            $find.Replacement.Text = $pair[1]
            $find.Forward = $true
            $find.Wrap = 1
            $find.Execute([ref]$pair[0], [ref]$true, [ref]$false, [ref]$false, [ref]$false, [ref]$false, [ref]$true, [ref]1, [ref]$false, [ref]$pair[1], [ref]2) | Out-Null
        }
        $doc.Save()
        $pdf = [IO.Path]::ChangeExtension($d.FullName, '.pdf')
        $doc.ExportAsFixedFormat([string]$pdf, 17)
        $doc.Close(0)
        [Runtime.InteropServices.Marshal]::ReleaseComObject($doc) | Out-Null
        Start-Sleep -Milliseconds 400
    }
}
finally {
    if ($word) { try { $word.Quit() } catch {} ; try { [Runtime.InteropServices.Marshal]::ReleaseComObject($word) | Out-Null } catch {} }
    [GC]::Collect(); [GC]::WaitForPendingFinalizers()
}

# Fix Excel defense sheet
$xlFile = Join-Path $root '05-项目答辩材料\第01组-202410120301-王胜运-实验室设备管理系统答辩记录单.xlsx'
if (Test-Path $xlFile) {
    $xl = New-Object -ComObject Excel.Application
    $xl.Visible = $false
    $xl.DisplayAlerts = $false
    $wb = $xl.Workbooks.Open($xlFile)
    foreach ($ws in $wb.Worksheets) {
        $used = $ws.UsedRange
        foreach ($cell in $used.Cells) {
            $v = $cell.Value2
            if ($v -is [string]) {
                $nv = $v
                foreach ($pair in $map) { $nv = $nv.Replace($pair[0], $pair[1]) }
                if ($nv -ne $v) { $cell.Value2 = $nv }
            }
        }
    }
    $wb.Save()
    $wb.Close($false)
    $xl.Quit()
    [GC]::Collect(); [GC]::WaitForPendingFinalizers()
    Write-Host "xlsx fixed"
}
Write-Host "DONE"
