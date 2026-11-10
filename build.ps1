$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$ClassesDir = Join-Path $ProjectRoot "out\classes"
$TestClassesDir = Join-Path $ProjectRoot "out\test-classes"

if (Test-Path $ClassesDir) {
    Remove-Item -Recurse -Force $ClassesDir
}
if (Test-Path $TestClassesDir) {
    Remove-Item -Recurse -Force $TestClassesDir
}

New-Item -ItemType Directory -Force -Path $ClassesDir | Out-Null
New-Item -ItemType Directory -Force -Path $TestClassesDir | Out-Null

$MainSources = Get-ChildItem -Path (Join-Path $ProjectRoot "src\main\java") -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
Write-Host "Compiling $($MainSources.Count) main source files..."
& javac -encoding UTF-8 -d $ClassesDir $MainSources
if ($LASTEXITCODE -ne 0) {
    throw "Main compilation failed with exit code $LASTEXITCODE"
}

$TestSources = Get-ChildItem -Path (Join-Path $ProjectRoot "src\test\java") -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
Write-Host "Compiling $($TestSources.Count) test source files..."
& javac -encoding UTF-8 -cp $ClassesDir -d $TestClassesDir $TestSources
if ($LASTEXITCODE -ne 0) {
    throw "Test compilation failed with exit code $LASTEXITCODE"
}

Write-Host "Build succeeded: classes output to out\classes and out\test-classes."
