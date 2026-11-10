$ErrorActionPreference = 'Stop'
$projectRoot = 'D:\BaiduSyncdisk\课内\软件工程\分组作业'
$stageDir = 'C:\Users\Public\labms_word_build'

$jobs = @(
    @{ Html = 'doc3_design.html'; OutDir = (Join-Path $projectRoot '03-系统设计说明书'); Base = '第01组-202410120301-王胜运-实验室设备管理系统系统设计说明书' },
    @{ Html = 'doc4_test_report.html'; OutDir = (Join-Path $projectRoot '04-测试用例与测试报告'); Base = '第01组-202410120301-王胜运-实验室设备管理系统测试报告' }
)

foreach ($job in $jobs) {
    if (-not (Test-Path $job.OutDir)) { New-Item -ItemType Directory -Path $job.OutDir -Force | Out-Null }
    $word = $null
    $doc = $null
    try {
        $word = New-Object -ComObject Word.Application
        $word.Visible = $false
        $word.DisplayAlerts = 0
# options skipped
# options skipped
# options skipped

        $html = Join-Path $stageDir $job.Html
        $doc = $word.Documents.Open([string]$html, [ref]$false, [ref]$true)

        $doc.PageSetup.PaperSize = 7
        $doc.PageSetup.TopMargin = 62.35
        $doc.PageSetup.BottomMargin = 62.35
        $doc.PageSetup.LeftMargin = 62.35
        $doc.PageSetup.RightMargin = 62.35

        # Normal style: 宋体 12pt, exact 20pt line spacing, 2-char first line indent
        $n = $doc.Styles.Item(-1)
        $n.Font.NameFarEast = '宋体'
        $n.Font.NameAscii = 'Times New Roman'
        $n.Font.NameOther = 'Times New Roman'
        $n.Font.Size = 12
        $n.ParagraphFormat.LineSpacingRule = 4
        $n.ParagraphFormat.LineSpacing = 20
        $n.ParagraphFormat.CharacterUnitFirstLineIndent = 2
        $n.ParagraphFormat.SpaceBefore = 0
        $n.ParagraphFormat.SpaceAfter = 3

        # Embed images: break links, scale to page width
        $cnt = $doc.InlineShapes.Count
        for ($s = 1; $s -le $cnt; $s++) {
            try {
                $shp = $doc.InlineShapes.Item($s)
                $shp.LockAspectRatio = -1
                if ($shp.Width -gt 420) { $shp.Width = 420 }
                if ($shp.Height -gt 260) { $shp.Height = 260 }
                if ($shp.LinkFormat -ne $null) {
                    $shp.LinkFormat.SavePictureWithDocument = -1
                    $shp.LinkFormat.BreakLink()
                }
                $shp.Range.ParagraphFormat.Alignment = 1
                $shp.Range.ParagraphFormat.CharacterUnitFirstLineIndent = 0
                $shp.Range.ParagraphFormat.FirstLineIndent = 0
                $shp.Range.ParagraphFormat.LineSpacingRule = 0
                $shp.Range.ParagraphFormat.KeepWithNext = -1
            } catch {}
        }

        # Code containers: 1x1 tables -> no indent, single tight spacing
        for ($t = 1; $t -le $doc.Tables.Count; $t++) {
            try {
                $tbl = $doc.Tables.Item($t)
                if ($tbl.Rows.Count -eq 1 -and $tbl.Columns.Count -eq 1) {
                    $tbl.Rows.AllowBreakAcrossPages = 0
                    $rng = $tbl.Range
                    $rng.ParagraphFormat.CharacterUnitFirstLineIndent = 0
                    $rng.ParagraphFormat.FirstLineIndent = 0
                    $rng.ParagraphFormat.LineSpacingRule = 4
                    $rng.ParagraphFormat.LineSpacing = 12
                    $rng.ParagraphFormat.SpaceBefore = 0
                    $rng.ParagraphFormat.SpaceAfter = 0
                } else {
                    $tbl.Rows.AllowBreakAcrossPages = 0
                    try { $tbl.Rows.Item(1).HeadingFormat = -1 } catch {}
                    $tbl.Range.ParagraphFormat.CharacterUnitFirstLineIndent = 0
                    $tbl.Range.ParagraphFormat.FirstLineIndent = 0
                    $tbl.Range.ParagraphFormat.LineSpacingRule = 4
                    $tbl.Range.ParagraphFormat.LineSpacing = 15
                    $tbl.Range.ParagraphFormat.SpaceBefore = 1
                    $tbl.Range.ParagraphFormat.SpaceAfter = 1
                }
            } catch {}
        }

        # Headings + page breaks via Find (cheap, no per-paragraph COM walk)
        $doc.Repaginate()

        $tmpDocx = Join-Path $stageDir ($job.Base + '.docx')
        $tmpDoc = Join-Path $stageDir ($job.Base + '.doc')
        $tmpPdf = Join-Path $stageDir ($job.Base + '.pdf')

        $doc.SaveAs2([string]$tmpDocx, 16)
        $doc.SaveAs2([string]$tmpDoc, 0)
        $doc.ExportAsFixedFormat([string]$tmpPdf, 17)
        $doc.Close(0)
        $doc = $null

        Copy-Item -LiteralPath $tmpDocx -Destination (Join-Path $job.OutDir ($job.Base + '.docx')) -Force
        Copy-Item -LiteralPath $tmpDoc -Destination (Join-Path $job.OutDir ($job.Base + '.doc')) -Force
        Copy-Item -LiteralPath $tmpPdf -Destination (Join-Path $job.OutDir ($job.Base + '.pdf')) -Force
        Write-Host "OK: $($job.Base)"
    } catch {
        Write-Host "FAILED: $($job.Base) -> $($_.Exception.Message)"
    } finally {
        if ($doc -ne $null) { try { $doc.Close(0) } catch {} }
        if ($word -ne $null) { try { $word.Quit() } catch {} ; try { [Runtime.InteropServices.Marshal]::ReleaseComObject($word) | Out-Null } catch {} }
        [GC]::Collect(); [GC]::WaitForPendingFinalizers()
        Start-Sleep -Seconds 2
    }
}
Write-Host '=== FAST BUILD DONE ==='
