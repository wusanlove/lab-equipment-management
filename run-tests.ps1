$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$ClassesDir = Join-Path $ProjectRoot "out\classes"
$TestClassesDir = Join-Path $ProjectRoot "out\test-classes"
$OutFile = Join-Path $ProjectRoot "out\test-output.txt"

if (-not (Test-Path $TestClassesDir)) {
    & (Join-Path $ProjectRoot "build.ps1")
}

$Cp = "$ClassesDir;$TestClassesDir"
$OutputLines = & java "-Dfile.encoding=UTF-8" -cp $Cp cn.edu.ctgu.labms.AllTests
if ($LASTEXITCODE -ne 0) {
    throw "Test suite failed with exit code $LASTEXITCODE"
}

$OutputLines | ForEach-Object { Write-Host $_ }
[System.IO.File]::WriteAllLines($OutFile, $OutputLines, (New-Object System.Text.UTF8Encoding($false)))
Write-Host "Saved test output to $OutFile"
