# NovaCRM demo - shared settings and helpers (dot-sourced by start/check/stop scripts).
# KHONG luu mat khau that trong file nay. Mat khau MySQL doc tu file local ngoai repo:
#   <repo>\..\NovaCRM-Demo-Local\demo.env   (dong dang KEY=VALUE, vi du CRM_DB_PASSWORD=...)
# hoac tu bien moi truong CRM_DB_PASSWORD co san.

$ErrorActionPreference = 'Stop'

$script:RepoRoot     = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$script:LocalRoot    = Join-Path (Split-Path $script:RepoRoot -Parent) 'NovaCRM-Demo-Local'
$script:CatalinaHome = if ($env:NOVACRM_TOMCAT_HOME) { $env:NOVACRM_TOMCAT_HOME } else { 'C:\Users\Windows\Tools\apache-tomcat-10.1.60' }
$script:CatalinaBase = Join-Path $script:LocalRoot 'tomcat-base'
$script:LogDir       = Join-Path $script:LocalRoot 'logs'
$script:HttpPort     = 8090
$script:ShutdownPort = 8095
$script:MySqlService = 'MySQL84'
$script:MySqlPort    = 3306
$script:DbName       = 'crm_demo'
$script:AppUrl       = "http://localhost:$($script:HttpPort)/crm/login.html"
$script:HealthUrl    = "http://localhost:$($script:HttpPort)/crm/api/leads/health"
$script:WarSource    = Join-Path $script:RepoRoot 'backend\target\crm.war'
$script:MySqlExe     = 'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe'

function Write-Step([string]$m)  { Write-Host "[DEMO] $m" -ForegroundColor Cyan }
function Write-Ok([string]$m)    { Write-Host "[ OK ] $m" -ForegroundColor Green }
function Write-Warn2([string]$m) { Write-Host "[WARN] $m" -ForegroundColor Yellow }
function Write-Fail([string]$m)  { Write-Host "[FAIL] $m" -ForegroundColor Red }

function Import-DemoEnv {
    $file = Join-Path $script:LocalRoot 'demo.env'
    if (Test-Path $file) {
        foreach ($line in Get-Content $file) {
            if ($line -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*?)\s*$' -and -not $line.TrimStart().StartsWith('#')) {
                if (-not [Environment]::GetEnvironmentVariable($Matches[1], 'Process')) {
                    [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2], 'Process')
                }
            }
        }
    }
    if (-not $env:CRM_DB_USERNAME) { $env:CRM_DB_USERNAME = 'root' }
    if (-not $env:CRM_DB_PASSWORD) {
        throw "Thieu CRM_DB_PASSWORD. Tao file $file voi dong CRM_DB_PASSWORD=<mat khau MySQL> (file nay nam ngoai repo)."
    }
    $env:CRM_DB_URL = "jdbc:mysql://localhost:$($script:MySqlPort)/$($script:DbName)?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8"
}

function Test-MySqlRunning {
    $svc = Get-Service $script:MySqlService -ErrorAction SilentlyContinue
    if (-not $svc -or $svc.Status -ne 'Running') { return $false }
    $c = New-Object Net.Sockets.TcpClient
    try { $c.Connect('127.0.0.1', $script:MySqlPort); return $true } catch { return $false } finally { $c.Close() }
}

function Invoke-DemoSql([string]$Sql) {
    $old = $env:MYSQL_PWD
    try {
        $env:MYSQL_PWD = $env:CRM_DB_PASSWORD
        & $script:MySqlExe -u $env:CRM_DB_USERNAME --protocol=TCP -h 127.0.0.1 -P $script:MySqlPort --default-character-set=utf8mb4 -N -B $script:DbName -e $Sql 2>&1
    } finally { $env:MYSQL_PWD = $old }
}

# Tra ve process dang nghe cong (hoac $null).
function Get-PortOwner([int]$Port) {
    $conn = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $conn) { return $null }
    Get-CimInstance Win32_Process -Filter "ProcessId=$($conn.OwningProcess)"
}

# $true neu process thuoc Tomcat demo (CATALINA_BASE cua demo).
function Test-IsDemoTomcat($proc) {
    return [bool]($proc -and $proc.CommandLine -and $proc.CommandLine.Contains($script:CatalinaBase))
}

function Initialize-TomcatBase([switch]$Redeploy) {
    if (-not (Test-Path "$($script:CatalinaHome)\bin\catalina.bat")) { throw "Khong thay Tomcat tai $($script:CatalinaHome)" }
    foreach ($d in 'conf', 'logs', 'temp', 'work', 'webapps') {
        New-Item -ItemType Directory -Force (Join-Path $script:CatalinaBase $d) | Out-Null
    }
    New-Item -ItemType Directory -Force $script:LogDir | Out-Null
    $serverXml = Join-Path $script:CatalinaBase 'conf\server.xml'
    if (-not (Test-Path $serverXml)) {
        Write-Step 'Tao Tomcat base rieng cho demo (copy conf, doi cong)...'
        Copy-Item "$($script:CatalinaHome)\conf\*" (Join-Path $script:CatalinaBase 'conf') -Recurse -Force
        $x = Get-Content $serverXml -Raw
        $x = $x -replace '<Server port="8005"', "<Server port=`"$($script:ShutdownPort)`""
        $x = $x -replace '(<Connector port=)"8080"', "`$1`"$($script:HttpPort)`""
        Set-Content $serverXml $x -Encoding UTF8
    }
    # Tomcat dung chung co bin\setenv.bat tro vao crm_db. catalina.bat uu tien setenv.bat cua CATALINA_BASE,
    # nen tao file rieng de DAM BAO demo chi dung crm_demo (mat khau lay tu bien moi truong, khong ghi vao file).
    New-Item -ItemType Directory -Force (Join-Path $script:CatalinaBase 'bin') | Out-Null
    $setenv = "@echo off`r`nrem NovaCRM demo - tu sinh boi demo-common.ps1. Khong chua mat khau.`r`nset `"CRM_DB_URL=$($env:CRM_DB_URL)`"`r`nset `"CRM_DB_USERNAME=$($env:CRM_DB_USERNAME)`"`r`n"
    Set-Content (Join-Path $script:CatalinaBase 'bin\setenv.bat') $setenv -Encoding ASCII
    $warDest = Join-Path $script:CatalinaBase 'webapps\crm.war'
    if ($Redeploy -or -not (Test-Path $warDest)) {
        if (-not (Test-Path $script:WarSource)) { throw "Chua co WAR: $($script:WarSource). Chay: mvn -f backend\pom.xml clean package -DskipTests" }
        Write-Step 'Trien khai WAR vao Tomcat demo...'
        Remove-Item (Join-Path $script:CatalinaBase 'webapps\crm') -Recurse -Force -ErrorAction SilentlyContinue
        Copy-Item $script:WarSource $warDest -Force
    }
}

function Wait-DemoHealth([int]$TimeoutSec = 120) {
    $sw = [Diagnostics.Stopwatch]::StartNew()
    while ($sw.Elapsed.TotalSeconds -lt $TimeoutSec) {
        try {
            $r = Invoke-WebRequest -Uri $script:HealthUrl -UseBasicParsing -TimeoutSec 5
            if ($r.StatusCode -eq 200) { return $true }
        } catch { }
        Start-Sleep -Seconds 2
    }
    return $false
}
