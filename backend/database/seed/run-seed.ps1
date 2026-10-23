<#
.SYNOPSIS
  Nạp dữ liệu mẫu (Seed Data) chuẩn kiểm thử & demo cho hệ thống NovaCRM.

.DESCRIPTION
  Chạy các tệp seed SQL theo đúng thứ tự phụ thuộc (01 -> 06) và chạy kiểm tra (07_verify_seed.sql).
  Mật khẩu MySQL được đọc an toàn từ $env:CRM_DB_PASSWORD.

.EXAMPLE
  $env:CRM_DB_PASSWORD = "your_mysql_password"
  ./backend/database/seed/run-seed.ps1 -Database crm_sprint1_test_final_20261023
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$Database,
    [string]$DbHost = '127.0.0.1',
    [int]$Port = 3306,
    [string]$User = $(if ($env:CRM_DB_USERNAME) { $env:CRM_DB_USERNAME } else { 'root' }),
    [string]$MysqlExe = 'mysql',
    [switch]$CleanFirst,
    [switch]$VerifyOnly
)

$ErrorActionPreference = 'Stop'

if ($Database -notmatch '^[A-Za-z0-9_]{1,64}$') {
    throw "Tên database '$Database' không hợp lệ."
}

if ($Database -eq 'crm_db' -or $Database -match 'prod') {
    throw "CHẶN AN TOÀN: Không được nạp seed data lên database Production '$Database'."
}

$seedDir = $PSScriptRoot
$mysqlArgsCommon = @(
    "--host=$DbHost",
    "--port=$Port",
    "--user=$User",
    "--default-character-set=utf8mb4",
    $Database
)

function Invoke-SqlFile([string]$filePath) {
    $fileName = Split-Path $filePath -Leaf
    Write-Host "[SEED] Đang thực thi: $fileName ..." -ForegroundColor Cyan
    
    # Sử dụng biến môi trường MYSQL_PWD để truyền mật khẩu an toàn
    $oldPwd = $env:MYSQL_PWD
    try {
        if ($env:CRM_DB_PASSWORD) {
            $env:MYSQL_PWD = $env:CRM_DB_PASSWORD
        }
        
        $output = & $MysqlExe @mysqlArgsCommon -e "source $filePath" 2>&1
        if ($LASTEXITCODE -ne 0) {
            throw "Lỗi khi chạy $fileName : $output"
        }
        if ($output) {
            Write-Host $output
        }
    }
    finally {
        $env:MYSQL_PWD = $oldPwd
    }
}

if ($CleanFirst) {
    Write-Host "[SEED] Dọn dẹp dữ liệu cũ trước khi nạp..." -ForegroundColor Yellow
    Invoke-SqlFile (Join-Path $seedDir '08_cleanup_seed.sql')
}

if (-not $VerifyOnly) {
    $seedFiles = @(
        '01_seed_master_data.sql',
        '02_seed_users_roles.sql',
        '03_seed_customers_contacts.sql',
        '04_seed_products_opportunities.sql',
        '05_seed_leads_campaigns.sql',
        '06_seed_activities.sql'
    )

    foreach ($file in $seedFiles) {
        $fullPath = Join-Path $seedDir $file
        if (-not (Test-Path $fullPath)) {
            throw "Không tìm thấy tệp seed: $fullPath"
        }
        Invoke-SqlFile $fullPath
    }
    Write-Host "[SEED] Hoàn tất nạp dữ liệu mẫu!" -ForegroundColor Green
}

# Chạy kiểm tra kết quả
Invoke-SqlFile (Join-Path $seedDir '07_verify_seed.sql')
