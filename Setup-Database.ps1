param(
    [Parameter(Mandatory=$true)]
    [string]$Database
)

$ErrorActionPreference = "Stop"

$root = $PSScriptRoot
$folder = Join-Path $root "backend\database"
$qa = Join-Path $root ".local-qa"

if ($Database -notmatch '^(crm_db|crm_sprint1_test|crm_install_qa_[a-f0-9]{8})$') {
    throw "TEN DATABASE KHONG HOP LE"
}

if (!$env:CRM_DB_USERNAME -or !$env:CRM_DB_PASSWORD) {
    throw "CAN CAU HINH CRM_DB_USERNAME VA CRM_DB_PASSWORD"
}

$mysql = "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe"

if (!(Test-Path $mysql)) {
    $command = Get-Command mysql.exe -ErrorAction SilentlyContinue
    if (!$command) {
        throw "KHONG TIM THAY MYSQL CLI"
    }
    $mysql = $command.Source
}

$ordered = @(
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
    "020_products_prices.sql"
)

foreach ($name in $ordered) {
    if (!(Test-Path (Join-Path $folder $name))) {
        throw "THIEU MIGRATION: $name"
    }
}

New-Item -ItemType Directory -Force $qa | Out-Null

git -C $root check-ignore -q -- ".local-qa/mysql-client.cnf"
if ($LASTEXITCODE -ne 0) {
    throw ".local-qa CHUA DUOC GITIGNORE - DUNG"
}

$cfg = Join-Path $qa "mysql-client.cnf"

if (Test-Path $cfg) {
    throw "FILE CAU HINH TAM DA TON TAI"
}

$run = Join-Path $qa ("install-" + $Database)

if (Test-Path $run) {
    throw "THU MUC KIEM THU DA TON TAI"
}

$escaped = $env:CRM_DB_PASSWORD.Replace('\','\\').Replace('"','\"')
$user = $env:CRM_DB_USERNAME

try {
    New-Item -ItemType Directory $run | Out-Null

    $config = @"
[client]
host=127.0.0.1
port=3306
protocol=tcp
user="$user"
password="$escaped"
"@

    [System.IO.File]::WriteAllText(
        $cfg,
        $config,
        [System.Text.UTF8Encoding]::new($false)
    )

    $exists = & $mysql "--defaults-extra-file=$cfg" `
        --batch --skip-column-names `
        --execute="SELECT COUNT(*) FROM information_schema.SCHEMATA WHERE SCHEMA_NAME='$Database';"

    if ($LASTEXITCODE -ne 0) {
        throw "KHONG KIEM TRA DUOC DATABASE"
    }

    if (($exists | Out-String).Trim() -ne "0") {
        throw "DATABASE DA TON TAI - KHONG GHI DE"
    }

    # Chuan bi tat ca SQL truoc khi chay
    $prepared = @()

    foreach ($name in $ordered) {
        $sql = Get-Content (Join-Path $folder $name) -Raw -Encoding UTF8

        $sql = [regex]::Replace(
            $sql,
            '(?i)(?<![a-z0-9_])crm_db(?![a-z0-9_])',
            $Database
        )

        if ($sql -match '(?i)\bDROP\s+DATABASE\b') {
            throw "PHAT HIEN DROP DATABASE: $name"
        }

        if ($Database -ne "crm_db" -and
            $sql -match '(?i)(?<![a-z0-9_])crm_db(?![a-z0-9_])') {
            throw "PHAT HIEN THAM CHIEU DATABASE CHINH: $name"
        }

        $file = Join-Path $run $name

        [System.IO.File]::WriteAllText(
            $file,
            $sql,
            [System.Text.UTF8Encoding]::new($false)
        )

        $prepared += $file
    }

    Write-Host "`nDATABASE: $Database" -ForegroundColor Cyan
    Write-Host "MIGRATIONS: $($prepared.Count)"
    Write-Host "CHI CAI DAT NEU DATABASE CHUA TON TAI"

    $confirm = Read-Host "Nhap INSTALL de thuc hien"

    if ($confirm -cne "INSTALL") {
        Write-Host "DA HUY"
        return
    }

    for ($i = 0; $i -lt $prepared.Count; $i++) {
        $file = $prepared[$i]
        $name = Split-Path $file -Leaf

        Write-Host "[$($i+1)/$($prepared.Count)] $name" -ForegroundColor Cyan

        $cmd = "`"$mysql`" --defaults-extra-file=`"$cfg`" --database=$Database < `"$file`""

        # Migration 001 tao database, nen chua the dung --database
        if ($i -eq 0) {
            $cmd = "`"$mysql`" --defaults-extra-file=`"$cfg`" < `"$file`""
        }

        cmd.exe /d /c $cmd

        if ($LASTEXITCODE -ne 0) {
            throw "MIGRATION FAILED: $name - KHONG CHAY LAI TREN DB NAY"
        }
    }

    $query = "SELECT CONCAT((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()), ',', (SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('quotes','quote_items') AND REFERENCED_TABLE_NAME IS NOT NULL));"

    $check = & $mysql "--defaults-extra-file=$cfg" `
        "--database=$Database" --batch --skip-column-names `
        "--execute=$query"

    if ($LASTEXITCODE -ne 0) {
        throw "SCHEMA VERIFICATION FAILED"
    }

    Write-Host "TABLES, QUOTES_FK: $check"

    if (($check | Out-String).Trim() -ne "34,5") {
        throw "SCHEMA KHAC KET QUA QA DA KIEM THU"
    }

    Write-Host "`nNOVACRM DATABASE INSTALL PASS" -ForegroundColor Green
}
finally {
    if (Test-Path $cfg) {
        Remove-Item $cfg -Force
    }
    $escaped = $null
    $config = $null
    Write-Host "MYSQL TEMP CONFIG REMOVED"
}