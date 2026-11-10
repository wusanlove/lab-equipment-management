$ErrorActionPreference = 'Stop'

$projectRoot = 'D:\BaiduSyncdisk\课内\软件工程\分组作业'
$htmlSrcDir = Join-Path $projectRoot 'docs\html_src'
$umlDir = Join-Path $projectRoot 'docs\uml'
$stageDir = 'C:\Users\Public\labms_word_build'

if (Test-Path -LiteralPath $stageDir) {
    Remove-Item -LiteralPath $stageDir -Recurse -Force -ErrorAction SilentlyContinue
}
New-Item -ItemType Directory -Path $stageDir -Force | Out-Null

# Copy all UML PNGs and HTML sources to ASCII staging directory
Get-ChildItem -Path $umlDir -Filter '*.png' | ForEach-Object {
    Copy-Item -LiteralPath $_.FullName -Destination (Join-Path $stageDir $_.Name) -Force
}
Get-ChildItem -Path $htmlSrcDir -Filter '*.html' | ForEach-Object {
    Copy-Item -LiteralPath $_.FullName -Destination (Join-Path $stageDir $_.Name) -Force
}

$docsConfig = @(
    @{
        Id       = 'doc1'
        HtmlName = 'doc1_proposal.html'
        OutDir   = Join-Path $projectRoot '01-项目立项说明书'
        BaseName = '第01组-202410120301-王胜运-实验室设备管理系统项目立项说明书'
    },
    @{
        Id       = 'doc2'
        HtmlName = 'doc2_srs.html'
        OutDir   = Join-Path $projectRoot '02-需求分析与系统原型'
        BaseName = '第01组-202410120301-王胜运-实验室设备管理系统项目需求分析规格说明书'
    },
    @{
        Id       = 'doc3'
        HtmlName = 'doc3_design.html'
        OutDir   = Join-Path $projectRoot '03-系统设计说明书'
        BaseName = '第01组-202410120301-王胜运-实验室设备管理系统系统设计说明书'
    },
    @{
        Id       = 'doc4'
        HtmlName = 'doc4_test_report.html'
        OutDir   = Join-Path $projectRoot '04-测试用例与测试报告'
        BaseName = '第01组-202410120301-王胜运-实验室设备管理系统测试报告'
    }
)

foreach ($cfg in $docsConfig) {
    if (-not (Test-Path -LiteralPath $cfg.OutDir)) {
        New-Item -ItemType Directory -Path $cfg.OutDir -Force | Out-Null
    }
}

$word = $null
try {
    $word = New-Object -ComObject Word.Application
    $word.Visible = $false
    $word.DisplayAlerts = 0

    foreach ($cfg in $docsConfig) {
        Write-Host ">>> Building $($cfg.BaseName) ..."
        $stagedHtml = Join-Path $stageDir $cfg.HtmlName
        $tempDocx   = Join-Path $stageDir "$($cfg.Id).docx"
        $tempDoc    = Join-Path $stageDir "$($cfg.Id).doc"
        $tempPdf    = Join-Path $stageDir "$($cfg.Id).pdf"

        $doc = $word.Documents.Open([string]$stagedHtml)

        # 1. Page Setup (A4, 2.2cm margins = 62.35 pt)
        $doc.PageSetup.PaperSize = 7 # wdPaperA4
        $doc.PageSetup.TopMargin = 62.35
        $doc.PageSetup.BottomMargin = 62.35
        $doc.PageSetup.LeftMargin = 62.35
        $doc.PageSetup.RightMargin = 62.35

        # 2. Process InlineShapes (break external links so PNGs embed into word/media/ & scale to fit)
        $shapeCount = $doc.InlineShapes.Count
        for ($s = 1; $s -le $shapeCount; $s++) {
            $shape = $doc.InlineShapes.Item($s)
            try {
                $shape.LockAspectRatio = -1
                if ($shape.Width -gt 420) {
                    $shape.Width = 420
                }
                if ($shape.Height -gt 265) {
                    $shape.Height = 265
                }
                if ($shape.LinkFormat -ne $null) {
                    $shape.LinkFormat.SavePictureWithDocument = -1
                    $shape.LinkFormat.BreakLink()
                }
            } catch {}
            try {
                $pFmt = $shape.Range.ParagraphFormat
                $pFmt.Alignment = 1 # Center
                $pFmt.FirstLineIndent = 0
                $pFmt.CharacterUnitFirstLineIndent = 0
                $pFmt.LineSpacingRule = 0 # Single spacing so image is never clipped!
                $pFmt.KeepWithNext = -1
                $pFmt.SpaceBefore = 6
                $pFmt.SpaceAfter = 2
            } catch {}
        }

        # 3. Process Tables (Code boxes, TOC table, Cover tables, Data tables)
        $tocTableIndex = -1
        for ($t = 1; $t -le $doc.Tables.Count; $t++) {
            $tbl = $doc.Tables.Item($t)
            $rowCount = $tbl.Rows.Count
            $colCount = $tbl.Columns.Count

            $tbl.PreferredWidthType = 2 # wdPreferredWidthPercent
            $tbl.PreferredWidth = 100

            if ($rowCount -eq 1 -and $colCount -eq 1) {
                # 1x1 IDE Code Block Container
                $tbl.Rows.AllowBreakAcrossPages = 0
                $rng = $tbl.Range
                $rng.ParagraphFormat.FirstLineIndent = 0
                $rng.ParagraphFormat.CharacterUnitFirstLineIndent = 0
                $rng.ParagraphFormat.LineSpacingRule = 4 # wdLineSpaceExactly
                $rng.ParagraphFormat.LineSpacing = 12
                $rng.ParagraphFormat.SpaceBefore = 0
                $rng.ParagraphFormat.SpaceAfter = 0
                continue
            }

            # Check if this is the TOC table (2 columns, first cell starts with '1. ')
            if ($colCount -eq 2) {
                $firstCellTxt = $tbl.Cell(1, 1).Range.Text.Replace([string][char]13, '').Replace([string][char]7, '').Trim()
                if ($firstCellTxt.StartsWith('1. ')) {
                    $tocTableIndex = $t
                    $tbl.Borders.Enable = 0
                    $tbl.Range.ParagraphFormat.FirstLineIndent = 0
                    $tbl.Range.ParagraphFormat.CharacterUnitFirstLineIndent = 0
                    $tbl.Range.ParagraphFormat.LineSpacingRule = 4
                    $tbl.Range.ParagraphFormat.LineSpacing = 19
                    $tbl.Range.ParagraphFormat.SpaceBefore = 1
                    $tbl.Range.ParagraphFormat.SpaceAfter = 1
                    continue
                }
            }

            # Check if this is the Cover Metadata table (table #1)
            if ($t -eq 1) {
                $tbl.PreferredWidth = 88
                $tbl.Rows.Alignment = 1 # Center table
                $tbl.Range.ParagraphFormat.FirstLineIndent = 0
                $tbl.Range.ParagraphFormat.CharacterUnitFirstLineIndent = 0
                $tbl.Range.ParagraphFormat.LineSpacingRule = 4
                $tbl.Range.ParagraphFormat.LineSpacing = 20
                continue
            }

            # Standard Data Tables (including approval/sign tables)
            $tbl.Rows.AllowBreakAcrossPages = 0
            try {
                $tbl.Rows.Item(1).HeadingFormat = -1
            } catch {}
            $rng = $tbl.Range
            $rng.ParagraphFormat.FirstLineIndent = 0
            $rng.ParagraphFormat.CharacterUnitFirstLineIndent = 0
            $rng.ParagraphFormat.LineSpacingRule = 4 # wdLineSpaceExactly
            $rng.ParagraphFormat.LineSpacing = 15
            $rng.ParagraphFormat.SpaceBefore = 1
            $rng.ParagraphFormat.SpaceAfter = 1
        }

        # 4. Process Paragraphs outside tables
        $inCover = $true
        $pCount = $doc.Paragraphs.Count
        for ($i = 1; $i -le $pCount; $i++) {
            $p = $doc.Paragraphs.Item($i)
            # Skip paragraphs inside tables (12 = wdWithInTable)
            if ($p.Range.Information(12)) {
                continue
            }
            # Skip paragraphs containing images
            if ($p.Range.InlineShapes.Count -gt 0) {
                continue
            }

            $txt = $p.Range.Text.Replace([string][char]13, '').Replace([string][char]7, '').Trim()
            if ([string]::IsNullOrWhiteSpace($txt)) {
                continue
            }

            if ($txt -eq '目　　录') {
                $inCover = $false
                $p.Format.PageBreakBefore = -1
                $p.Format.Alignment = 1 # Center
                $p.Format.FirstLineIndent = 0
                $p.Format.CharacterUnitFirstLineIndent = 0
                $p.Format.KeepWithNext = -1
                $p.Format.LineSpacingRule = 4
                $p.Format.LineSpacing = 24
                $p.Format.SpaceBefore = 8
                $p.Format.SpaceAfter = 12
                continue
            }

            if ($inCover) {
                # Cover page paragraphs
                $p.Format.Alignment = 1
                $p.Format.FirstLineIndent = 0
                $p.Format.CharacterUnitFirstLineIndent = 0
                continue
            }

            # Major Chapter Heading 1 (e.g., "1. 项目提出")
            if ($txt -match '^[1-9]\.\s+\S+') {
                $p.Format.PageBreakBefore = -1
                $p.Format.KeepWithNext = -1
                $p.Format.FirstLineIndent = 0
                $p.Format.CharacterUnitFirstLineIndent = 0
                $p.Format.LineSpacingRule = 4
                $p.Format.LineSpacing = 24
                $p.Format.SpaceBefore = 10
                $p.Format.SpaceAfter = 8
                continue
            }

            # Sub-headings Heading 2 / Heading 3 (e.g., "1.1 ...", "4.5.1 ...")
            if ($txt -match '^[1-9]\.[1-9](\.[1-9])?\s+\S+') {
                $p.Format.PageBreakBefore = 0
                $p.Format.KeepWithNext = -1
                $p.Format.FirstLineIndent = 0
                $p.Format.CharacterUnitFirstLineIndent = 0
                $p.Format.LineSpacingRule = 4
                $p.Format.LineSpacing = 22
                $p.Format.SpaceBefore = 8
                $p.Format.SpaceAfter = 4
                continue
            }

            # Figure Caption (below image: "图 X-X ...")
            if ($txt -match '^图\s*\d+-\d+\s+') {
                $p.Format.Alignment = 1 # Center
                $p.Format.FirstLineIndent = 0
                $p.Format.CharacterUnitFirstLineIndent = 0
                $p.Format.LineSpacingRule = 4
                $p.Format.LineSpacing = 18
                $p.Format.SpaceBefore = 2
                $p.Format.SpaceAfter = 8
                continue
            }

            # Table Caption (above table: "表 X-X ...")
            if ($txt -match '^表\s*\d+-\d+\s+') {
                $p.Format.Alignment = 1 # Center
                $p.Format.FirstLineIndent = 0
                $p.Format.CharacterUnitFirstLineIndent = 0
                $p.Format.KeepWithNext = -1
                $p.Format.LineSpacingRule = 4
                $p.Format.LineSpacing = 18
                $p.Format.SpaceBefore = 6
                $p.Format.SpaceAfter = 3
                continue
            }

            # Standard Body Paragraph: 宋体 小四 (12pt), Times New Roman, 2-char indent (24pt), Exact 20pt line spacing
            $p.Format.Alignment = 3 # wdAlignParagraphJustify
            $p.Format.CharacterUnitFirstLineIndent = 0
            $p.Format.FirstLineIndent = 24
            $p.Format.LineSpacingRule = 4 # wdLineSpaceExactly
            $p.Format.LineSpacing = 20
            $p.Format.SpaceBefore = 0
            $p.Format.SpaceAfter = 3
            $p.Range.Font.NameFarEast = '宋体'
            $p.Range.Font.NameAscii = 'Times New Roman'
            $p.Range.Font.NameOther = 'Times New Roman'
            $p.Range.Font.Size = 12
        }

        # 5. Repaginate and dynamically populate accurate page numbers in TOC table
        $doc.Repaginate()
        $headingPages = @{}
        for ($i = 1; $i -le $pCount; $i++) {
            $p = $doc.Paragraphs.Item($i)
            if ($p.Range.Information(12)) { continue }
            $txt = $p.Range.Text.Replace([string][char]13, '').Replace([string][char]7, '').Trim()
            if ($txt -match '^[1-9](\.[1-9])*\.?\s+\S+') {
                $pgNum = $p.Range.Information(3) # wdActiveEndAdjustedPageNumber
                $headingPages[$txt] = $pgNum
            }
        }

        if ($tocTableIndex -gt 0) {
            $tocTbl = $doc.Tables.Item($tocTableIndex)
            for ($r = 1; $r -le $tocTbl.Rows.Count; $r++) {
                $leftTxt = $tocTbl.Cell($r, 1).Range.Text.Replace([string][char]13, '').Replace([string][char]7, '').Trim()
                if ($headingPages.ContainsKey($leftTxt)) {
                    $pg = $headingPages[$leftTxt]
                    $rightCell = $tocTbl.Cell($r, 2)
                    $rightCell.Range.Text = "............... $pg"
                    $rightCell.Range.ParagraphFormat.Alignment = 2 # Right align
                }
            }
        }

        # 6. Add Page Numbers in Footer
        try {
            $section = $doc.Sections.Item(1)
            $footer = $section.Footers.Item(1) # wdHeaderFooterPrimary
            $footer.PageNumbers.Add(1, $false) | Out-Null # 1 = wdAlignPageNumberCenter, $false = FirstPage
        } catch {}

        # 7. Save as .docx (16), .doc (0), and .pdf (17)
        $doc.SaveAs2([string]$tempDocx, 16)
        $doc.SaveAs2([string]$tempDoc, 0)
        $doc.ExportAsFixedFormat([string]$tempPdf, 17)
        $doc.Close(0)
        [Runtime.InteropServices.Marshal]::ReleaseComObject($doc) | Out-Null

        # Copy to target output folder
        $outDocx = Join-Path $cfg.OutDir "$($cfg.BaseName).docx"
        $outDoc  = Join-Path $cfg.OutDir "$($cfg.BaseName).doc"
        $outPdf  = Join-Path $cfg.OutDir "$($cfg.BaseName).pdf"

        Copy-Item -LiteralPath $tempDocx -Destination $outDocx -Force
        Copy-Item -LiteralPath $tempDoc  -Destination $outDoc  -Force
        Copy-Item -LiteralPath $tempPdf  -Destination $outPdf  -Force

        Write-Host "    Saved: $outDocx"
        Write-Host "    Saved: $outDoc"
        Write-Host "    Saved: $outPdf"
    }
}
finally {
    if ($word -ne $null) {
        try { $word.Quit() } catch {}
        [Runtime.InteropServices.Marshal]::ReleaseComObject($word) | Out-Null
    }
    [GC]::Collect()
    [GC]::WaitForPendingFinalizers()
}
Write-Host "=== ALL 4 DOCUMENTS GENERATED SUCCESSFULLY ==="
