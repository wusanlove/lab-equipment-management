$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$ClassesDir = Join-Path $ProjectRoot "out\classes"
$OutFile = Join-Path $ProjectRoot "out\demo-output.txt"

if (-not (Test-Path $ClassesDir)) {
    & (Join-Path $ProjectRoot "build.ps1")
}

$OutputLines = & java "-Dfile.encoding=UTF-8" -cp $ClassesDir cn.edu.ctgu.labms.Main
if ($LASTEXITCODE -ne 0) {
    throw "Demo execution failed with exit code $LASTEXITCODE"
}

$OutputLines | ForEach-Object { Write-Host $_ }
[System.IO.File]::WriteAllLines($OutFile, $OutputLines, (New-Object System.Text.UTF8Encoding($false)))
Write-Host "Saved demo output to $OutFile"
