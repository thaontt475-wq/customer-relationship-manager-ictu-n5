<#
.SYNOPSIS  Kiem tra suc khoe NovaCRM demo: MySQL, du lieu, Tomcat, health, login that, session, API Lead, HikariCP.
.PARAMETER DemoUser  Email tai khoan demo (mac dinh admin@company.com - seed tong hop).
Mat khau demo doc tu bien NOVACRM_DEMO_PASSWORD (hoac demo.env); mac dinh la mat khau tai khoan seed tong hop trong backend\database\seed\README.md.
#>
[CmdletBinding()]
param([string]$DemoUser = 'admin@company.com')

. (Join-Path $PSScriptRoot 'demo-common.ps1')
$fail = 0
function Check([string]$name, [scriptblock]$test) {
    try { $r = & $test; if ($r) { Write-Ok "$name : $r" } else { Write-Fail $name; $script:fail++ } }
    catch { Write-Fail "$name : $($_.Exception.Message)"; $script:fail++ }
}
try { Import-DemoEnv } catch { Write-Fail $_.Exception.Message; exit 1 }
$demoPwd = if ($env:NOVACRM_DEMO_PASSWORD) { $env:NOVACRM_DEMO_PASSWORD } else { 'Password123@' }
$base = "http://localhost:$($script:HttpPort)/crm"

Check 'MySQL service/port'    { if (Test-MySqlRunning) { 'running' } }
Check "Database $($script:DbName)" {
    $o = Invoke-DemoSql "SELECT CONCAT('customers=',(SELECT COUNT(*) FROM customers),' leads=',(SELECT COUNT(*) FROM leads),' users=',(SELECT COUNT(*) FROM users))"
    if ($LASTEXITCODE -eq 0 -and $o -match 'customers=([1-9])') { $o }
}
Check 'Tomcat demo process' {
    $o = Get-PortOwner $script:HttpPort
    if (Test-IsDemoTomcat $o) { "PID $($o.ProcessId) cong $($script:HttpPort)" }
}
Check 'Health API' { (Invoke-WebRequest $script:HealthUrl -UseBasicParsing -TimeoutSec 8).StatusCode }
Check 'Trang dang nhap' { (Invoke-WebRequest "$base/login.html" -UseBasicParsing -TimeoutSec 8).StatusCode }

$sess = New-Object Microsoft.PowerShell.Commands.WebRequestSession
Check "Dang nhap $DemoUser" {
    $body = @{ email = $DemoUser; password = $demoPwd } | ConvertTo-Json
    $r = Invoke-WebRequest "$base/api/auth/login" -Method Post -Body $body -ContentType 'application/json; charset=utf-8' -WebSession $sess -UseBasicParsing -TimeoutSec 10
    $r.StatusCode
}
Check 'Session' { (Invoke-WebRequest "$base/api/auth/session" -WebSession $sess -UseBasicParsing -TimeoutSec 8).StatusCode }
Check 'API Lead' {
    $r = Invoke-WebRequest "$base/api/leads?page=1&size=5" -WebSession $sess -UseBasicParsing -TimeoutSec 10
    "$($r.StatusCode)"
}
Check 'Tomcat chi noi vao crm_demo (khong crm_db)' {
    $o = Invoke-DemoSql "SELECT CONCAT('crm_demo=',SUM(db='crm_demo'),' crm_db=',SUM(db='crm_db')) FROM information_schema.processlist WHERE user='$($env:CRM_DB_USERNAME)' AND host LIKE '%:%' AND db IS NOT NULL"
    if ($o -match 'crm_demo=([1-9]\d*) crm_db=0') { $o }
}
Check 'HikariCP trong log Tomcat' {
    $logs = Get-ChildItem $script:LogDir, (Join-Path $script:CatalinaBase 'logs') -Include *.log, *.txt -Recurse -ErrorAction SilentlyContinue
    $hit = $logs | Select-String -Pattern 'NovaCRM-HikariPool' -List | Select-Object -First 1
    if ($hit) { 'NovaCRM-HikariPool started' }
}
Check 'Dang xuat' { (Invoke-WebRequest "$base/api/auth/logout" -Method Post -WebSession $sess -UseBasicParsing -TimeoutSec 8).StatusCode }

if ($fail -eq 0) { Write-Host "`nTAT CA KIEM TRA DAT" -ForegroundColor Green; exit 0 }
Write-Host "`n$fail kiem tra that bai" -ForegroundColor Red; exit 1



