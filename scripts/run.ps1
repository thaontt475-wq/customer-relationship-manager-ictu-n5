<#
.SYNOPSIS
    Triển khai và khởi động máy chủ Apache Tomcat 10.1 cho NovaCRM.
.DESCRIPTION
    - Kiểm tra và xác định máy chủ Apache Tomcat 10.1 và Java 21.
    - Cấu hình biến môi trường kết nối database an toàn qua setenv.bat của Tomcat.
    - Kiểm tra trạng thái cổng 8080, dừng an toàn phiên bản cũ trước khi triển khai bản mới.
    - Deploy tệp WAR (backend/target/crm.war) vào thư mục webapps của Tomcat.
    - Khởi động Tomcat và hiển thị đường dẫn đăng nhập hệ thống.
#>
param(
    [string]$DbUser = $env:CRM_DB_USERNAME,
    [string]$DbPassword = $env:CRM_DB_PASSWORD,
    [string]$DbHost = "127.0.0.1",
    [int]$DbPort = 3306,
    [string]$TomcatDir = $env:CATALINA_HOME,
    [switch]$ForceRestart,
    [switch]$OpenBrowser,
    [switch]$NonInteractive
)

$ErrorActionPreference = "Stop"

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "   NOVACRM - KHỞI CHẠY HỆ THỐNG TRÊN TOMCAT 10.1" -ForegroundColor Cyan
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
$warFile = Join-Path $backendDir "target\crm.war"

# 1. Tìm kiếm và kiểm tra Tomcat 10.1
if ([string]::IsNullOrWhiteSpace($TomcatDir) -or !(Test-Path (Join-Path $TomcatDir "bin\catalina.bat"))) {
    $searchPatterns = @(
        $env:CATALINA_HOME,
        "C:\Users\*\Tools\apache-tomcat-10.1*",
        "C:\Tools\apache-tomcat-10.1*",
        "C:\Program Files\Apache Software Foundation\Tomcat 10.1*",
        "C:\Users\*\Tools\apache-tomcat-*",
        "C:\Tools\apache-tomcat-*"
    )
    foreach ($pat in $searchPatterns) {
        if ([string]::IsNullOrWhiteSpace($pat)) { continue }
        $item = Get-Item $pat -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($item -and (Test-Path (Join-Path $item.FullName "bin\catalina.bat"))) {
            $TomcatDir = $item.FullName
            break
        }
    }
}

if (!$TomcatDir -or !(Test-Path (Join-Path $TomcatDir "bin\catalina.bat"))) {
    throw "Không tìm thấy thư mục cài đặt Apache Tomcat 10.1! Vui lòng đặt biến CATALINA_HOME hoặc truyền tham số -TomcatDir."
}
Write-Host "Tìm thấy Apache Tomcat: $TomcatDir" -ForegroundColor Green

# 2. Tìm Java 21
$javaPath = Find-Tool "java.exe" @(
    "$env:JAVA_HOME\bin\java.exe",
    "C:\Program Files\Eclipse Adoptium\jdk-21*\bin\java.exe",
    "C:\Program Files\Java\jdk-21*\bin\java.exe",
    "C:\Program Files\Microsoft\jdk-21*\bin\java.exe"
)
if (!$javaPath) {
    throw "Không tìm thấy Java 21! Vui lòng cài đặt JDK 21 hoặc cấu hình JAVA_HOME."
}
$javaHome = (Resolve-Path (Join-Path (Split-Path $javaPath -Parent) "..")).Path
Write-Host "Sử dụng JAVA_HOME:     $javaHome" -ForegroundColor Green

# 3. Kiểm tra file WAR
if (!(Test-Path $warFile)) {
    Write-Host "Chưa có file WAR tại: $warFile. Đang tự động gọi build.ps1..." -ForegroundColor Yellow
    & "$scriptRoot\build.ps1"
    if (!(Test-Path $warFile)) {
        throw "Build WAR thất bại! Vui lòng kiểm tra lại mã nguồn."
    }
}

# 4. Xác nhận thông tin kết nối CSDL Development
if ([string]::IsNullOrWhiteSpace($DbUser)) {
    if ($NonInteractive) {
        $DbUser = "crm_user"
    } else {
        $DbUser = Read-Host "Nhập MySQL Username cho Tomcat (mặc định: crm_user)"
        if ([string]::IsNullOrWhiteSpace($DbUser)) { $DbUser = "crm_user" }
    }
}

if ($DbPassword -eq $null -or ($DbPassword -eq "" -and !$NonInteractive)) {
    if (!$NonInteractive) {
        $secPass = Read-Host "Nhập MySQL Password cho user '$DbUser'" -AsSecureString
        $BSTR = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($secPass)
        $DbPassword = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($BSTR)
        [System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($BSTR)
    }
}

# 5. Kiểm tra an toàn tiến trình Tomcat đang chạy
$activePort = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
if ($activePort) {
    Write-Host "[!] Phát hiện Tomcat (hoặc tiến trình khác) đang lắng nghe tại cổng 8080." -ForegroundColor Yellow
    if (!$ForceRestart -and !$NonInteractive) {
        $confirm = Read-Host "Bạn có muốn dừng phiên bản cũ để triển khai bản mới không? (Y/n)"
        if ($confirm -match '^[Nn]') {
            Write-Host "Đã hủy thao tác chạy Tomcat." -ForegroundColor Gray
            return
        }
    }
    
    Write-Host "Đang dừng máy chủ cũ..." -ForegroundColor Yellow
    $stopScript = Join-Path $TomcatDir "bin\shutdown.bat"
    if (Test-Path $stopScript) {
        & cmd.exe /c "set `"JAVA_HOME=$javaHome`" && `"$stopScript`"" 2>&1 | Out-Null
    }
    
    # Chờ giải phóng cổng 8080 (tối đa 10 giây)
    $waited = 0
    while ((Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue) -and ($waited -lt 10)) {
        Start-Sleep -Seconds 1
        $waited++
    }
}

# 6. Cấu hình biến môi trường an toàn vào bin/setenv.bat của Tomcat
$setenvPath = Join-Path $TomcatDir "bin\setenv.bat"
$devDbUrl = "jdbc:mysql://" + $DbHost + ":" + $DbPort + "/crm_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8"

$setenvLines = @(
    "@echo off",
    "set `"JAVA_HOME=$javaHome`"",
    "set `"CATALINA_HOME=$TomcatDir`"",
    "set `"CRM_DB_URL=$devDbUrl`"",
    "set `"CRM_DB_USERNAME=$DbUser`"",
    "set `"CRM_DB_PASSWORD=$DbPassword`""
)
[System.IO.File]::WriteAllLines($setenvPath, $setenvLines, [System.Text.UTF8Encoding]::new($false))
Write-Host "Cập nhật cấu hình môi trường: $setenvPath" -ForegroundColor Green


# 7. Triển khai WAR an toàn vào webapps
$webappsDir = Join-Path $TomcatDir "webapps"
$targetWar = Join-Path $webappsDir "crm.war"
$explodedDir = Join-Path $webappsDir "crm"

if (Test-Path $explodedDir) {
    Write-Host "Dọn dẹp thư mục triển khai cũ: $explodedDir" -ForegroundColor Gray
    Remove-Item $explodedDir -Recurse -Force -ErrorAction SilentlyContinue
}

Write-Host "Sao chép tệp WAR mới vào webapps..." -ForegroundColor Yellow
Copy-Item $warFile $targetWar -Force

# 8. Khởi động Tomcat trong cửa sổ riêng
Write-Host "Khởi động máy chủ Apache Tomcat..." -ForegroundColor Yellow
$startupScript = Join-Path $TomcatDir "bin\startup.bat"
Start-Process -FilePath $startupScript -WorkingDirectory (Join-Path $TomcatDir "bin")

# 9. Chờ đợi máy chủ khởi động và kiểm tra HTTP 200
Write-Host "Đang chờ ứng dụng triển khai..." -ForegroundColor Yellow
$loginUrl = "http://localhost:8080/crm/login.html"
$isReady = $false

for ($i = 1; $i -le 15; $i++) {
    Start-Sleep -Seconds 2
    try {
        $res = Invoke-WebRequest -Uri $loginUrl -UseBasicParsing -TimeoutSec 3 -ErrorAction SilentlyContinue
        if ($res.StatusCode -eq 200) {
            $isReady = $true
            break
        }
    } catch {
        # Chưa sẵn sàng, tiếp tục chờ
    }
    Write-Host "  ... chờ máy chủ sẵn sàng ($($i*2)s)" -ForegroundColor Gray
}

Write-Host "`n============================================================" -ForegroundColor Green
Write-Host "   MÁY CHỦ TOMCAT ĐÃ SẴN SÀNG ĐỂ CHẠY DEMO!" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green
Write-Host "URL Đăng nhập CRM: $loginUrl" -ForegroundColor Cyan
Write-Host "URL Dashboard:    http://localhost:8080/crm/dashboard.html" -ForegroundColor Cyan

if ($OpenBrowser) {
    Start-Process $loginUrl
}
