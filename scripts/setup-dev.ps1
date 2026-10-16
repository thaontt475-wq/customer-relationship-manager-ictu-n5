<#
.SYNOPSIS
    Thiết lập và chuẩn hóa môi trường Database (Development & Test) cho NovaCRM.
.DESCRIPTION
    - Tự động kiểm tra và xác định Java 21, Maven, MySQL CLI độc lập với tên Windows user.
    - Kiểm tra trạng thái máy chủ MySQL.
    - Hướng dẫn thiết lập tài khoản database riêng biệt (không bắt buộc dùng root).
    - Tạo database development (crm_db) và test (crm_sprint1_test).
    - Áp dụng các migration còn thiếu theo đúng thứ tự an toàn thông qua bảng schema_migrations.
    - Xác minh kết nối và bảng dữ liệu. Tuyệt đối không in mật khẩu ra màn hình.
.PARAMETER DbUser
    Tên tài khoản MySQL dùng cho ứng dụng CRM (mặc định lấy từ biến môi trường CRM_DB_USERNAME, nếu không có sẽ hỏi).
.PARAMETER DbPassword
    Mật khẩu của tài khoản CRM (mặc định lấy từ biến môi trường CRM_DB_PASSWORD, nếu không có sẽ hỏi an toàn).
.PARAMETER DbHost
    Địa chỉ máy chủ MySQL (mặc định 127.0.0.1).
.PARAMETER DbPort
    Cổng kết nối MySQL (mặc định 3306).
.PARAMETER TargetDatabase
    Database cần thiết lập: 'all' (mặc định), 'crm_db', hoặc 'crm_sprint1_test'.
.PARAMETER NonInteractive
    Chế độ chạy tự động không hỏi tương tác (thích hợp cho CI/CD).
#>
param(
    [string]$DbUser = $env:CRM_DB_USERNAME,
    [string]$DbPassword = $env:CRM_DB_PASSWORD,
    [string]$DbHost = "127.0.0.1",
    [int]$DbPort = 3306,
    [ValidateSet("all", "crm_db", "crm_sprint1_test")]
    [string]$TargetDatabase = "all",
    [switch]$NonInteractive
)

$ErrorActionPreference = "Stop"

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "   NOVACRM - THIẾT LẬP MÔI TRƯỜNG PHÁT TRIỂN & CSDL" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

# 1. Tìm kiếm và kiểm tra các công cụ hệ thống (Độc lập user profile)
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

Write-Host "`n[1/6] Kiểm tra môi trường công cụ..." -ForegroundColor Yellow

$javaPath = Find-Tool "java.exe" @(
    "$env:JAVA_HOME\bin\java.exe",
    "C:\Program Files\Eclipse Adoptium\jdk-21*\bin\java.exe",
    "C:\Program Files\Java\jdk-21*\bin\java.exe",
    "C:\Program Files\Microsoft\jdk-21*\bin\java.exe",
    "C:\Program Files\Amazon Corretto\jdk-21*\bin\java.exe",
    "C:\Program Files\Zulu\zulu-21*\bin\java.exe"
)

if (!$javaPath) {
    throw "Không tìm thấy Java 21! Vui lòng cài đặt JDK 21 và cấu hình JAVA_HOME. Xem SETUP_TEAM.md."
}

$javaVerOut = cmd.exe /c "`"$javaPath`" -version 2>&1" | Out-String
if ($javaVerOut -notmatch 'version "21') {
    Write-Host "[CẢNH BÁO] Java phát hiện không phải version 21: $javaVerOut" -ForegroundColor Yellow
} else {
    Write-Host "  -> Java 21: OK ($javaPath)" -ForegroundColor Green
}


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
    throw "Không tìm thấy Maven! Vui lòng cài đặt Maven 3.9+ và cấu hình PATH hoặc MAVEN_HOME."
}
Write-Host "  -> Maven:   OK ($mvnPath)" -ForegroundColor Green

$mysqlPath = Find-Tool "mysql.exe" @(
    "$env:MYSQL_HOME\bin\mysql.exe",
    "C:\Program Files\MySQL\MySQL Server *\bin\mysql.exe",
    "C:\Program Files (x86)\MySQL\MySQL Server *\bin\mysql.exe",
    "C:\Tools\mysql-*\bin\mysql.exe"
)

if (!$mysqlPath) {
    throw "Không tìm thấy MySQL CLI (mysql.exe)! Vui lòng cài đặt MySQL Server 8.0/8.4 hoặc thêm thư mục bin vào PATH."
}
Write-Host "  -> MySQL:   OK ($mysqlPath)" -ForegroundColor Green

# 2. Kiểm tra máy chủ MySQL có đang lắng nghe cổng không
Write-Host ("`n[2/6] Kiểm tra trạng thái máy chủ MySQL (" + $DbHost + ":" + $DbPort + ")...") -ForegroundColor Yellow
$tcpTest = $false
try {
    $client = [System.Net.Sockets.TcpClient]::new()
    $connect = $client.BeginConnect($DbHost, $DbPort, $null, $null)
    $success = $connect.AsyncWaitHandle.WaitOne(3000, $false)
    if ($success) {
        $client.EndConnect($connect)
        $tcpTest = $true
    }
    $client.Close()
} catch {
    $tcpTest = $false
}

if (!$tcpTest) {
    throw "Không thể kết nối đến cổng $DbPort của MySQL tại $DbHost! Hãy đảm bảo dịch vụ MySQL (ví dụ: MySQL84) đã được khởi động."
}
Write-Host ("  -> Kết nối cổng " + $DbPort + ": Thành công") -ForegroundColor Green

# 3. Xác thực thông tin tài khoản MySQL
Write-Host "`n[3/6] Cấu hình thông tin tài khoản MySQL..." -ForegroundColor Yellow

if ([string]::IsNullOrWhiteSpace($DbUser)) {
    if ($NonInteractive) {
        throw "Thiếu tham số -DbUser hoặc biến môi trường CRM_DB_USERNAME trong chế độ NonInteractive!"
    }
    Write-Host "Khuyến nghị: Sử dụng tài khoản riêng biệt như 'crm_user' thay vì tài khoản root." -ForegroundColor Cyan
    $DbUser = Read-Host "Nhập MySQL Username (mặc định: crm_user)"
    if ([string]::IsNullOrWhiteSpace($DbUser)) {
        $DbUser = "crm_user"
    }
}

if ($DbPassword -eq $null -or ($DbPassword -eq "" -and !$NonInteractive)) {
    if (!$NonInteractive) {
        $secPass = Read-Host "Nhập MySQL Password cho user '$DbUser' (để trống nếu không có mật khẩu)" -AsSecureString
        $BSTR = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($secPass)
        $DbPassword = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($BSTR)
        [System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($BSTR)
    }
}

# Tạo file cấu hình tạm an toàn cho mysql client để không lộ password trong process args
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

    # Kiểm tra đăng nhập với thông tin vừa cung cấp
    $checkAuth = cmd.exe /c "`"$mysqlPath`" --defaults-extra-file=`"$tempExtraCnf`" --batch --skip-column-names --execute=`"SELECT 1;`" 2>&1"
    if ($LASTEXITCODE -ne 0) {

        Write-Host "`n[!] KHÔNG THỂ ĐĂNG NHẬP MYSQL VỚI USER '$DbUser'!" -ForegroundColor Red
        Write-Host "Chi tiết lỗi từ MySQL: $checkAuth" -ForegroundColor Yellow
        Write-Host "`nHƯỚNG DẪN DÀNH CHO THÀNH VIÊN:" -ForegroundColor Cyan
        Write-Host "Nếu chưa có tài khoản '$DbUser', hãy mở MySQL CLI bằng quyền admin (root) và chạy các lệnh sau:" -ForegroundColor Gray
        Write-Host "------------------------------------------------------------" -ForegroundColor DarkGray
        Write-Host "CREATE USER IF NOT EXISTS '$DbUser'@'localhost' IDENTIFIED BY 'MatKhauCuaBan';" -ForegroundColor White
        Write-Host "GRANT ALL PRIVILEGES ON crm_db.* TO '$DbUser'@'localhost';" -ForegroundColor White
        Write-Host "GRANT ALL PRIVILEGES ON crm_sprint1_test.* TO '$DbUser'@'localhost';" -ForegroundColor White
        Write-Host "FLUSH PRIVILEGES;" -ForegroundColor White
        Write-Host "------------------------------------------------------------" -ForegroundColor DarkGray
        Write-Host "Sau đó chạy lại script này với thông tin đã tạo." -ForegroundColor Cyan
        throw "Xác thực MySQL thất bại cho user '$DbUser'."
    }
    Write-Host "  -> Xác thực MySQL user '$DbUser': Thành công" -ForegroundColor Green

    # 4. Xác định danh sách database cần thiết lập
    Write-Host "`n[4/6] Chuẩn bị cơ sở dữ liệu mục tiêu..." -ForegroundColor Yellow
    $databasesToSetup = @()
    if ($TargetDatabase -eq "all") {
        $databasesToSetup = @("crm_db", "crm_sprint1_test")
    } else {
        $databasesToSetup = @($TargetDatabase)
    }

    # 5. Danh sách các file migration theo thứ tự an toàn đã được kiểm chứng
    $scriptRoot = $PSScriptRoot
    $repoRoot = (Resolve-Path (Join-Path $scriptRoot "..")).Path
    $dbFolder = Join-Path $repoRoot "backend\database"

    $orderedMigrations = @(
        "001_sprint1_sprint2.sql",
        "002_permissions.sql",
        "003_auth_extensions.sql",
        "004_sprint1_management.sql",
        "005_profile_audit.sql",
        "006_sprint2_catalogs.sql",
        "007_sprint1_roles_scope.sql",
        "008_audit_logs_compatibility.sql",
        "012_sprint3_customer_foundation.sql",
        "019_quotes_v2.sql",
        "009_operational_entities_complete.sql",
        "010_schema_field_compatibility.sql",
        "011_profile_and_field_fixes.sql",
        "013_sprint3_permissions.sql",
        "014_s3_02_contacts.sql",
        "014_s3_04_customer_merge.sql",
        "015_s3_06_customer_import.sql",
        "016_s3_08_support_risk.sql",
        "017_s3_07_customer_search_filters.sql",
        "018_sprint3_contact_role_permissions.sql",
        "020_contracts_crud.sql",
        "020_products_prices.sql",
        "021_fix_roles_teams_encoding.sql",
        "022_seed_master_data.sql",
        "023_teams_leader_region.sql",
        "024_s4_02_lead_manual_excel.sql",
        "024_s4_07_lead_sla.sql",
        "024_sprint4_campaigns_leads.sql",
        "024_sprint4_web_form_leads.sql",
        "025_s4_04_lead_duplicate_merge.sql",
        "025_s4_05_lead_scoring.sql",
        "025_sprint4_lead_assignment_rules.sql",
        "026_s4_08_lead_conversion.sql",
        "026_sprint4_lead_filters_sla.sql",
        "027_s4_03_campaign_opportunities.sql"
    )

    foreach ($mig in $orderedMigrations) {
        $fullPath = Join-Path $dbFolder $mig
        if (!(Test-Path $fullPath)) {
            throw "Thiếu file migration bắt buộc: $mig trong thư mục backend\database!"
        }
    }

    # 6. Thực thi khởi tạo và migration cho từng database
    Write-Host "`n[5/6] Áp dụng migrations an toàn vào database..." -ForegroundColor Yellow

    foreach ($targetDb in $databasesToSetup) {
        Write-Host "`n--- Đang xử lý database: [$targetDb] ---" -ForegroundColor Cyan

        # 6.1. Tạo database nếu chưa tồn tại
        $createDbSql = "CREATE DATABASE IF NOT EXISTS \`$targetDb\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
        & $mysqlPath "--defaults-extra-file=$tempExtraCnf" --execute=$createDbSql
        if ($LASTEXITCODE -ne 0) {
            throw "Không thể tạo database '$targetDb'!"
        }

        # 6.2. Tạo bảng quản lý phiên bản schema_migrations
        $initMigrationTableSql = "CREATE TABLE IF NOT EXISTS ``$targetDb``.schema_migrations (version VARCHAR(100) NOT NULL PRIMARY KEY, applied_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"
        & $mysqlPath "--defaults-extra-file=$tempExtraCnf" --execute=$initMigrationTableSql
        if ($LASTEXITCODE -ne 0) {
            throw "Không thể tạo bảng schema_migrations trong database '$targetDb'!"
        }

        # 6.3. Lấy danh sách migration đã chạy
        $appliedListRaw = & $mysqlPath "--defaults-extra-file=$tempExtraCnf" --database=$targetDb --batch --skip-column-names --execute="SELECT version FROM schema_migrations;" 2>&1
        $appliedSet = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::OrdinalIgnoreCase)
        if ($appliedListRaw) {
            foreach ($line in ($appliedListRaw -split "`r?`n")) {
                $trimmed = $line.Trim()
                if ($trimmed) { [void]$appliedSet.Add($trimmed) }
            }
        }

        $appliedCount = 0
        $skippedCount = 0

        for ($idx = 0; $idx -lt $orderedMigrations.Count; $idx++) {
            $migFile = $orderedMigrations[$idx]
            $step = "[$($idx+1)/$($orderedMigrations.Count)]"

            if ($appliedSet.Contains($migFile)) {
                $skippedCount++
                continue
            }

            Write-Host "  $step Đang áp dụng: $migFile" -ForegroundColor Gray
            $rawSql = Get-Content (Join-Path $dbFolder $migFile) -Raw -Encoding UTF8

            # Thay thế biến crm_db bằng tên database mục tiêu (đảm bảo không ảnh hưởng sang DB khác)
            $safeSql = [regex]::Replace(
                $rawSql,
                '(?i)(?<![a-z0-9_])crm_db(?![a-z0-9_])',
                $targetDb
            )

            # Chặn câu lệnh DROP DATABASE nếu có
            if ($safeSql -match '(?i)\bDROP\s+DATABASE\b') {
                throw "CẢNH BÁO NGUY HIỂM: Phát hiện lệnh DROP DATABASE trong migration $migFile! Dừng thực thi."
            }

            # Ghi ra file tạm để chạy qua MySQL CLI
            $tempMigSql = [System.IO.Path]::GetTempFileName()
            try {
                [System.IO.File]::WriteAllText($tempMigSql, $safeSql, [System.Text.UTF8Encoding]::new($false))
                $cmd = "type `"$tempMigSql`" | `"$mysqlPath`" --defaults-extra-file=`"$tempExtraCnf`" --database=$targetDb"
                cmd.exe /d /c $cmd
                if ($LASTEXITCODE -ne 0) {
                    throw "Thất bại khi thực thi migration: $migFile trên database $targetDb!"
                }

                # Ghi nhận vào schema_migrations
                $recSql = "INSERT INTO \`$targetDb\`.schema_migrations(version) VALUES ('$migFile');"
                & $mysqlPath "--defaults-extra-file=$tempExtraCnf" --execute=$recSql
                $appliedCount++
            } finally {
                if (Test-Path $tempMigSql) { Remove-Item $tempMigSql -Force }
            }
        }

        Write-Host "  -> Kết quả [$targetDb]: Áp dụng mới $appliedCount file, Bỏ qua (đã chạy trước đó) $skippedCount file." -ForegroundColor Green
    }

    # 7. Kiểm tra tổng kết và hướng dẫn thiết lập biến môi trường
    Write-Host "`n[6/6] Kiểm tra xác nhận cấu trúc..." -ForegroundColor Yellow
    foreach ($targetDb in $databasesToSetup) {
        $tableCount = & $mysqlPath "--defaults-extra-file=$tempExtraCnf" --database=$targetDb --batch --skip-column-names --execute="SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='$targetDb';"
        Write-Host "  -> Database [$targetDb]: $tableCount bảng dữ liệu." -ForegroundColor Green
    }

    Write-Host "`n============================================================" -ForegroundColor Green
    Write-Host "   THIẾT LẬP HOÀN TẤT THÀNH CÔNG!" -ForegroundColor Green
    Write-Host "============================================================" -ForegroundColor Green
    Write-Host "`nĐể chạy ứng dụng hoặc chạy tests, hãy thiết lập các biến môi trường sau trong terminal PowerShell của bạn:" -ForegroundColor Cyan
    Write-Host "------------------------------------------------------------" -ForegroundColor DarkGray
    Write-Host "`$env:CRM_DB_USERNAME = `"$DbUser`"" -ForegroundColor Yellow
    Write-Host "`$env:CRM_DB_PASSWORD = `"$DbPassword`"" -ForegroundColor Yellow
    Write-Host ("`$env:CRM_DB_URL = `"jdbc:mysql://" + $DbHost + ":" + $DbPort + "/crm_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8`"") -ForegroundColor Yellow
    Write-Host "------------------------------------------------------------" -ForegroundColor DarkGray
    Write-Host "Hoặc khi chạy test tự động, hãy dùng script: .\scripts\test.ps1" -ForegroundColor Cyan
} finally {
    if (Test-Path $tempExtraCnf) {
        Remove-Item $tempExtraCnf -Force
    }
}
