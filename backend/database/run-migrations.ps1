<#
.SYNOPSIS
  Apply every NovaCRM schema migration, in dependency order, to ONE target database.

.DESCRIPTION
  Migrations in this folder contain `USE crm_db;`. This runner rewrites that
  database name to -Database on the fly so a fresh QA/demo database can be built
  without touching crm_db. All scripts are idempotent (CREATE TABLE IF NOT EXISTS,
  guarded ALTERs), so re-running on an existing database is safe.

  Password is read from $env:CRM_DB_PASSWORD (never pass it on the command line).

.EXAMPLE
  $env:CRM_DB_PASSWORD = '<your local password>'
  ./backend/database/run-migrations.ps1 -Database novacrm_demo -User root
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$Database,
    [string]$DbHost = '127.0.0.1',
    [int]$Port = 3306,
    [string]$User = $(if ($env:CRM_DB_USERNAME) { $env:CRM_DB_USERNAME } else { 'root' }),
    [string]$MysqlExe = 'mysql',
    # Required to target a database whose name looks like production (crm_db, *prod*).
    [switch]$AllowProductionName
)

$ErrorActionPreference = 'Stop'

if ($Database -notmatch '^[A-Za-z0-9_]{1,64}$') {
    throw "Database name '$Database' is invalid (only letters, digits and underscore)."
}
if (-not $AllowProductionName -and ($Database -eq 'crm_db' -or $Database -match 'prod')) {
    throw "Refusing to run against '$Database'. Use a dedicated QA/demo database or pass -AllowProductionName explicitly."
}

# Dependency order (validated on a fresh MySQL 8.4 database). 012 and 019 must run
# before 009-011 because those compatibility scripts alter customer/quote tables.
$ordered = @(
    '001_sprint1_sprint2.sql',
    '002_permissions.sql',
    '003_auth_extensions.sql',
    '004_sprint1_management.sql',
    '005_profile_audit.sql',
    '006_sprint2_catalogs.sql',
    '007_sprint1_roles_scope.sql',
    '008_audit_logs_compatibility.sql',
    '012_sprint3_customer_foundation.sql',
    '019_quotes_v2.sql',
    '009_operational_entities_complete.sql',
    '010_schema_field_compatibility.sql',
    '011_profile_and_field_fixes.sql',
    '013_sprint3_permissions.sql',
    '014_s3_02_contacts.sql',
    '014_s3_04_customer_merge.sql',
    '015_s3_06_customer_import.sql',
    '016_s3_08_support_risk.sql',
    '017_s3_07_customer_search_filters.sql',
    '018_sprint3_contact_role_permissions.sql',
    '020_contracts_crud.sql',
    '020_products_prices.sql',
    '021_fix_roles_teams_encoding.sql',
    '022_seed_master_data.sql',
    '023_teams_leader_region.sql',
    '024_s4_02_lead_manual_excel.sql',
    '024_s4_07_lead_sla.sql',
    '024_sprint4_campaigns_leads.sql',
    '024_sprint4_web_form_leads.sql',
    '025_s4_04_lead_duplicate_merge.sql',
    '025_s4_05_lead_scoring.sql',
    '025_sprint4_lead_assignment_rules.sql',
    '026_s4_08_lead_conversion.sql',
    '026_sprint4_lead_filters_sla.sql',
    '027_s4_03_campaign_opportunities.sql',
    '028_s4_01_lead_capture.sql',
    '029_s4_06_assignment_sla_integration.sql',
    '030_fix_win_loss.sql'
)

$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$present = Get-ChildItem -Path $here -Filter '0*.sql' | Select-Object -ExpandProperty Name
$unlisted = $present | Where-Object { $ordered -notcontains $_ }
if ($unlisted) {
    Write-Warning ("Migrations present but not in the ordered list (NOT applied): " + ($unlisted -join ', '))
}

$previousPwd = $env:MYSQL_PWD
if ($env:CRM_DB_PASSWORD) { $env:MYSQL_PWD = $env:CRM_DB_PASSWORD }
try {
    $common = @("--host=$DbHost", "--port=$Port", "--user=$User", '--protocol=TCP', '--default-character-set=utf8mb4')
    & $MysqlExe @common -e "CREATE DATABASE IF NOT EXISTS ``$Database`` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    if ($LASTEXITCODE -ne 0) { throw "Cannot create/access database $Database" }

    foreach ($file in $ordered) {
        $path = Join-Path $here $file
        if (-not (Test-Path $path)) { throw "Missing migration $file" }
        Write-Host "-> $file"
        $sql = [System.IO.File]::ReadAllText($path, [System.Text.Encoding]::UTF8)
        $sql = [regex]::Replace($sql, '\bcrm_db\b', $Database)
        $tmp = [System.IO.Path]::GetTempFileName()
        try {
            [System.IO.File]::WriteAllText($tmp, $sql, (New-Object System.Text.UTF8Encoding($false)))
            # `source` lets mysql read the UTF-8 file itself (PowerShell pipes would re-encode it).
            & $MysqlExe @common $Database -e ("source " + ($tmp -replace '\\', '/'))
            if ($LASTEXITCODE -ne 0) { throw "Migration $file failed (exit $LASTEXITCODE)" }
        } finally {
            Remove-Item -Force $tmp -ErrorAction SilentlyContinue
        }
    }
    Write-Host "All $($ordered.Count) migrations applied to $Database."
} finally {
    $env:MYSQL_PWD = $previousPwd
}
