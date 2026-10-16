<#
.SYNOPSIS
    Đóng gói ứng dụng NovaCRM thành tệp WAR (backend/target/crm.war).
.DESCRIPTION
    - Không yêu cầu kết nối cơ sở dữ liệu MySQL (bỏ qua integration tests).
    - Tự động đóng gói toàn bộ Backend (Servlets, DAOs, Services) và Frontend (HTML, CSS, JS).
    - Tạo tệp backend/target/crm.war sẵn sàng triển khai lên Tomcat 10.1.
#>
param(
    [switch]$Quiet
)

$ErrorActionPreference = "Stop"

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "   NOVACRM - ĐÓNG GÓI ỨNG DỤNG (BUILD WAR)" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

function Find-Tool {
    param([string]$CommandName, [string[]]$SearchPatterns)
    $cmd = Get-Command $CommandName -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    foreach ($pat in $SearchPatterns) {
        if ([string]::IsNullOrWhiteSpace($pat)) { continue }
        $found = Get-Item $pat -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($found) { return $found.FullName }
    }
    return $null
}

$scriptRoot = $PSScriptRoot
$repoRoot = (Resolve-Path (Join-Path $scriptRoot "..")).Path
$backendDir = Join-Path $repoRoot "backend"
$frontendDir = Join-Path $repoRoot "frontend"

if (!(Test-Path (Join-Path $backendDir "pom.xml"))) {
    throw "Không tìm thấy backend\pom.xml tại $backendDir!"
}

if (!(Test-Path (Join-Path $frontendDir "login.html"))) {
    throw "Không tìm thấy thư mục frontend tại $frontendDir!"
}

# 1. Kiểm tra Maven và Java 21
$mvnPath = Find-Tool "mvn.cmd" @(
    "$env:MAVEN_HOME\bin\mvn.cmd",
    "$env:M2_HOME\bin\mvn.cmd",
    "C:\Users\*\Tools\apache-maven-*\bin\mvn.cmd",
    "C:\Tools\apache-maven-*\bin\mvn.cmd",
    "C:\Program Files\apache-maven-*\bin\mvn.cmd"
)
if (!$mvnPath) {
    $mvnPath = Find-Tool "mvn" @()
}

if (!$mvnPath) {
    throw "Không tìm thấy Maven CLI! Vui lòng cài đặt Maven 3.9+ hoặc cấu hình PATH."
}

Write-Host "Sử dụng Maven: $mvnPath" -ForegroundColor Gray
Write-Host "Đang thực hiện biên dịch và đóng gói WAR (bỏ qua tests, không cần MySQL)..." -ForegroundColor Yellow

$buildArgs = @("clean", "package", "-DskipTests")
if ($Quiet) {
    $buildArgs += "-q"
}

Push-Location $backendDir
try {
    & $mvnPath @buildArgs
    if ($LASTEXITCODE -ne 0) {
        throw "Lỗi đóng gói Maven! Quá trình build thất bại với exit code $LASTEXITCODE."
    }
} finally {
    Pop-Location
}

$warFile = Join-Path $backendDir "target\crm.war"
if (!(Test-Path $warFile)) {
    throw "Không tìm thấy file WAR kết quả tại: $warFile"
}

$warItem = Get-Item $warFile
$sizeMb = [math]::Round($warItem.Length / 1MB, 2)

Write-Host "`n============================================================" -ForegroundColor Green
Write-Host "   BUILD HOÀN TẤT THÀNH CÔNG (BUILD SUCCESS)" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green
Write-Host ("File WAR kết quả: " + $warFile + " (" + $sizeMb + " MB)") -ForegroundColor Green
Write-Host "Bao gồm đầy đủ:" -ForegroundColor Cyan
Write-Host "  - Backend: Jakarta EE 10 Servlets & Business Services" -ForegroundColor Gray
Write-Host "  - Frontend: Giao diện người dùng CRM hoàn chỉnh (HTML/CSS/JS)" -ForegroundColor Gray
Write-Host "`nBạn có thể chạy thử nghiệm bằng: .\scripts\run.ps1" -ForegroundColor Cyan
