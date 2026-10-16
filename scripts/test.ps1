<#
.SYNOPSIS
    Chạy bộ kiểm thử Maven (Maven clean test) trên môi trường database test cô lập (crm_sprint1_test).
.DESCRIPTION
    - Kiểm tra và đảm bảo chỉ chạy trên database test crm_sprint1_test, TUYỆT ĐỐI KHÔNG CHẠY TRÊN crm_db.
    - Kiểm tra kết nối và schema database test trước khi kích hoạt.
    - Chạy toàn bộ 322/322 Maven tests.
    - Tổng hợp và hiển thị chi tiết kết quả PASS/FAIL cùng nguyên nhân lỗi nếu có.
#>
param(
    [string]$DbUser = $env:CRM_DB_USERNAME,
    [string]$DbPassword = $env:CRM_DB_PASSWORD,
    [string]$DbHost = "127.0.0.1",
    [int]$DbPort = 3306,
    [string]$SingleTest,
    [switch]$NonInteractive
)

$ErrorActionPreference = "Stop"

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "   NOVACRM - CHẠY KIỂM THỬ TỰ ĐỘNG (MAVEN TEST)" -ForegroundColor Cyan
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

# 1. Tìm kiếm Maven và MySQL CLI
$mvnPath = Find-Tool "mvn.cmd" @(
    "$env:MAVEN_HOME\bin\mvn.cmd",
    "$env:M2_HOME\bin\mvn.cmd",
    "C:\Users\*\Tools\apache-maven-*\bin\mvn.cmd",
    "C:\Tools\apache-maven-*\bin\mvn.cmd",
    "C:\Program Files\apache-maven-*\bin\mvn.cmd"
)
if (!$mvnPath) { $mvnPath = Find-Tool "mvn" @() }
if (!$mvnPath) { throw "Không tìm thấy Maven CLI! Xem SETUP_TEAM.md." }

$mysqlPath = Find-Tool "mysql.exe" @(
    "$env:MYSQL_HOME\bin\mysql.exe",
    "C:\Program Files\MySQL\MySQL Server *\bin\mysql.exe",
    "C:\Program Files (x86)\MySQL\MySQL Server *\bin\mysql.exe",
    "C:\Tools\mysql-*\bin\mysql.exe"
)

# 2. Chuẩn bị thông tin tài khoản kết nối Database test
if ([string]::IsNullOrWhiteSpace($DbUser)) {
    if ($NonInteractive) {
        throw "Thiếu thông tin user database test (CRM_DB_USERNAME hoặc tham số -DbUser)!"
    }
    $DbUser = Read-Host "Nhập MySQL Username cho Database Test (mặc định: crm_user)"
    if ([string]::IsNullOrWhiteSpace($DbUser)) { $DbUser = "crm_user" }
}

if ($DbPassword -eq $null -or ($DbPassword -eq "" -and !$NonInteractive)) {
    if (!$NonInteractive) {
        $secPass = Read-Host "Nhập MySQL Password cho user '$DbUser'" -AsSecureString
        $BSTR = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($secPass)
        $DbPassword = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($BSTR)
        [System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($BSTR)
    }
}

# 3. An toàn schema: Đảm bảo URL kết nối chỉ trỏ vào crm_sprint1_test
$testDbName = "crm_sprint1_test"
$testDbUrl = "jdbc:mysql://" + $DbHost + ":" + $DbPort + "/" + $testDbName + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8"

if ($testDbUrl -notmatch "crm_sprint1_test") {
    throw "CẢNH BÁO BẢO VỆ DỮ LIỆU: URL kết nối không chứa database test 'crm_sprint1_test'. Không được chạy test trên database khác!"
}

# 4. Kiểm tra sự tồn tại của database test và kết nối MySQL
if ($mysqlPath) {
    Write-Host "Kiểm tra cơ sở dữ liệu test [$testDbName]..." -ForegroundColor Yellow
    $tempExtraCnf = [System.IO.Path]::GetTempFileName()
    try {
        $escapedPass = if ($DbPassword) { $DbPassword.Replace('\', '\\').Replace('"', '\"') } else { "" }
        $cnfLines = @(
            "[client]",
            "host=$DbHost",
            "port=$DbPort",
            "user=`"$DbUser`"",
            "password=`"$escapedPass`"",
            "default-character-set=utf8mb4"
        )
        [System.IO.File]::WriteAllLines($tempExtraCnf, $cnfLines, [System.Text.UTF8Encoding]::new($false))
        
        $tableCount = cmd.exe /c "`"$mysqlPath`" --defaults-extra-file=`"$tempExtraCnf`" --database=$testDbName --batch --skip-column-names --execute=`"SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='$testDbName';`" 2>&1"
        if ($LASTEXITCODE -ne 0) {

            Write-Host "[!] KHÔNG THỂ KẾT NỐI TỚI DATABASE TEST '$testDbName'!" -ForegroundColor Red
            Write-Host "Lỗi: $tableCount" -ForegroundColor Yellow
            Write-Host "Vui lòng chạy script thiết lập trước:" -ForegroundColor Cyan
            Write-Host "  powershell -ExecutionPolicy Bypass -File scripts\setup-dev.ps1 -TargetDatabase crm_sprint1_test" -ForegroundColor White
            throw "Database test chưa sẵn sàng."
        }
        
        $countNum = [int]($tableCount | Out-String).Trim()
        if ($countNum -lt 10) {
            Write-Host "[!] Database test '$testDbName' chỉ có $countNum bảng (thiếu schema!)." -ForegroundColor Red
            Write-Host "Hãy chạy script khởi tạo schema test: .\scripts\setup-dev.ps1 -TargetDatabase crm_sprint1_test" -ForegroundColor Cyan
            throw "Schema test chưa đầy đủ."
        }
        Write-Host "  -> Database test '$testDbName' hợp lệ ($countNum bảng)." -ForegroundColor Green
    } finally {
        if (Test-Path $tempExtraCnf) { Remove-Item $tempExtraCnf -Force }
    }
}

# 5. Cấu hình biến môi trường cho tiến trình test
$env:CRM_DB_URL = $testDbUrl
$env:CRM_DB_USERNAME = $DbUser
$env:CRM_DB_PASSWORD = $DbPassword

Write-Host "`nĐang kích hoạt Maven Clean Test trên database: $testDbName..." -ForegroundColor Yellow
Write-Host "  CRM_DB_URL:      $testDbUrl" -ForegroundColor Gray
Write-Host "  CRM_DB_USERNAME: $DbUser" -ForegroundColor Gray
Write-Host "  CRM_DB_PASSWORD: [ĐÃ BẢO MẬT]" -ForegroundColor Gray

$mvnTestArgs = @("clean", "test")
if ($SingleTest) {
    $mvnTestArgs += "-Dtest=$SingleTest"
    Write-Host "  -> Chỉ chạy test: $SingleTest" -ForegroundColor Cyan
}

$testPassed = $false
Push-Location $backendDir
try {
    & $mvnPath @mvnTestArgs
    if ($LASTEXITCODE -eq 0) {
        $testPassed = $true
    }
} finally {
    Pop-Location
}

# 6. Đọc kết quả từ surefire-reports để lập báo cáo
$reportsDir = Join-Path $backendDir "target\surefire-reports"
$totalRun = 0
$totalFail = 0
$totalErr = 0
$totalSkip = 0
$failureDetails = @()

if (Test-Path $reportsDir) {
    $reportFiles = Get-ChildItem -Path $reportsDir -Filter "*.txt"
    foreach ($rf in $reportFiles) {
        $content = Get-Content $rf.FullName -Raw -ErrorAction SilentlyContinue
        if ($content -match 'Tests run:\s*(\d+),\s*Failures:\s*(\d+),\s*Errors:\s*(\d+),\s*Skipped:\s*(\d+)') {
            $totalRun += [int]$matches[1]
            $totalFail += [int]$matches[2]
            $totalErr += [int]$matches[3]
            $totalSkip += [int]$matches[4]
            if ([int]$matches[2] -gt 0 -or [int]$matches[3] -gt 0) {
                $failureDetails += ("File: " + $rf.Name + "`n" + (Get-Content $rf.FullName -Tail 20 | Out-String))
            }
        }
    }
}

$failColor = if ($totalFail -gt 0) { "Red" } else { "Gray" }
$errColor = if ($totalErr -gt 0) { "Red" } else { "Gray" }
$skipColor = if ($totalSkip -gt 0) { "Yellow" } else { "Gray" }

Write-Host "`n============================================================" -ForegroundColor Cyan
Write-Host "   KẾT QUẢ KIỂM THỬ (TEST SUMMARY)" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "Tổng số tests đã chạy: $totalRun" -ForegroundColor White
Write-Host "Thành công (Passed):   $($totalRun - $totalFail - $totalErr)" -ForegroundColor Green
Write-Host "Thất bại (Failures):   $totalFail" -ForegroundColor $failColor
Write-Host "Lỗi (Errors):          $totalErr" -ForegroundColor $errColor
Write-Host "Bỏ qua (Skipped):      $totalSkip" -ForegroundColor $skipColor

if ($testPassed -and $totalFail -eq 0 -and $totalErr -eq 0) {
    Write-Host "`n>>> TẤT CẢ TESTS ĐỀU ĐẠT CHUẨN (ALL TESTS PASSED)! <<<" -ForegroundColor Green
    exit 0
} else {
    Write-Host "`n>>> PHÁT HIỆN TESTS THẤT BẠI HOẶC GẶP LỖI! <<<" -ForegroundColor Red
    if ($failureDetails.Count -gt 0) {
        Write-Host "`nChi tiết lỗi gần nhất:" -ForegroundColor Yellow
        foreach ($d in $failureDetails) {
            Write-Host $d -ForegroundColor DarkRed
        }
    }
    exit 1
}
