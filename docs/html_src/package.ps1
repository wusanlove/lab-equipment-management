$ErrorActionPreference = 'Continue'
$root = 'D:\BaiduSyncdisk\课内\软件工程\分组作业'

# 02 需求分析包（SRS + 原型）: 课程要求命名为 第XX组-组长学号-组长姓名-XXXX项目需求分析.rar
$srsBase = '第01组-202410120301-王胜运-实验室设备管理系统项目需求分析规格说明书'
$out02 = Join-Path $root '02-需求分析与系统原型'
$pkgDir = Join-Path $env:TEMP 'labms_pkg_02'
if (Test-Path $pkgDir) { Remove-Item $pkgDir -Recurse -Force }
New-Item -ItemType Directory -Path $pkgDir -Force | Out-Null
Copy-Item (Join-Path $out02 "$srsBase.docx") $pkgDir -Force -ErrorAction SilentlyContinue
Copy-Item (Join-Path $out02 "$srsBase.doc") $pkgDir -Force -ErrorAction SilentlyContinue
Copy-Item (Join-Path $out02 'prototype') $pkgDir -Recurse -Force -ErrorAction SilentlyContinue
$zip02 = Join-Path $out02 '第01组-202410120301-王胜运-实验室设备管理系统项目需求分析.zip'
if (Test-Path $zip02) { Remove-Item $zip02 -Force }
Compress-Archive -Path (Join-Path $pkgDir '*') -DestinationPath $zip02 -Force
Copy-Item $zip02 ([IO.Path]::ChangeExtension($zip02, '.rar')) -Force
Write-Host "PKG02: $zip02"

# 全部材料总包（答辩后提交用）：以 组别+项目名 命名
$allDir = Join-Path $env:TEMP 'labms_pkg_all'
if (Test-Path $allDir) { Remove-Item $allDir -Recurse -Force }
New-Item -ItemType Directory -Path $allDir -Force | Out-Null
$folders = @('01-项目立项说明书','02-需求分析与系统原型','03-系统设计说明书','04-测试用例与测试报告','05-项目答辩材料','docs','src','README.md','build.ps1','run-demo.ps1','run-tests.ps1')
foreach ($f in $folders) {
    $src = Join-Path $root $f
    if (Test-Path $src) { Copy-Item $src (Join-Path $allDir $f) -Recurse -Force }
}
$zipAll = Join-Path $root '第01组-实验室设备管理系统-全部项目文档.zip'
if (Test-Path $zipAll) { Remove-Item $zipAll -Force }
Compress-Archive -Path (Join-Path $allDir '*') -DestinationPath $zipAll -Force
Write-Host "PKGALL: $zipAll"
