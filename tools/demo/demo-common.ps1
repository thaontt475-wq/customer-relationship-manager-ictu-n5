# NovaCRM Demo - shared settings and helpers
# Dung chung boi start-demo.ps1, check-demo.ps1 va stop-demo.ps1.
# Khong luu mat khau that trong repository.

$ErrorActionPreference = 'Stop'

# ============================================================
# 1. CAU HINH CHUNG
# ============================================================

$script:RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path

# Thu muc local nam ngoai repository.
$script:LocalRoot = Join-Path (Split-Path $script:RepoRoot -Parent) 'NovaCRM-Demo-Local'

$script:CatalinaBase = Join-Path $script:LocalRoot 'tomcat-base'
$script:LogDir       = Join-Path $script:LocalRoot 'logs'
$script:ToolsDir     = Join-Path $script:LocalRoot 'tools'
$script:DownloadDir  = Join-Path $script:LocalRoot 'downloads'

$script:TomcatVersion = '10.1.60'

$script:HttpPort     = 8090
$script:ShutdownPort = 8095

$script:MySqlService = 'MySQL84'
$script:MySqlPort    = 3306
$script:DbName       = 'crm_demo'

$script:AppUrl   = "http://localhost:$($script:HttpPort)/crm/login.html"
$script:HealthUrl = "http://localhost:$($script:HttpPort)/crm/api/leads/health"

$script:WarSource = Join-Path $script:RepoRoot 'backend\target\crm.war'

# Duong dan Tomcat va MySQL se duoc tu tim.
$script:CatalinaHome = $null
$script:MySqlExe = $null


# ============================================================
# 2. HAM HIEN THI
# ============================================================

function Write-Step([string]$Message) {
    Write-Host "[DEMO] $Message" -ForegroundColor Cyan
}

function Write-Ok([string]$Message) {
    Write-Host "[ OK ] $Message" -ForegroundColor Green
}

function Write-Warn2([string]$Message) {
    Write-Host "[WARN] $Message" -ForegroundColor Yellow
}

function Write-Fail([string]$Message) {
    Write-Host "[FAIL] $Message" -ForegroundColor Red
}


# ============================================================
# 3. TU TIM TOMCAT
# ============================================================

function Find-TomcatHome {
    $candidates = @()

    # Uu tien duong dan do nguoi dung cau hinh.
    if ($env:NOVACRM_TOMCAT_HOME) {
        $candidates += $env:NOVACRM_TOMCAT_HOME
    }

    # Cac duong dan thuong gap.
    $candidates += @(
        "C:\Tools\apache-tomcat-$($script:TomcatVersion)"
        "$env:USERPROFILE\Tools\apache-tomcat-$($script:TomcatVersion)"
        "$env:USERPROFILE\Downloads\apache-tomcat-$($script:TomcatVersion)"
        "$env:USERPROFILE\apache-tomcat-$($script:TomcatVersion)"
        "$env:ProgramFiles\Apache Software Foundation\Tomcat 10.1"
        "$env:ProgramFiles\Apache Software Foundation\Tomcat 10.1.60"
        "$($script:ToolsDir)\apache-tomcat-$($script:TomcatVersion)"
    )

    # Tim cac ban Tomcat 10.1 trong Tools, Downloads va Program Files.
    $patterns = @(
        "$env:USERPROFILE\Tools\apache-tomcat-*"
        "$env:USERPROFILE\Downloads\apache-tomcat-*"
        "C:\Tools\apache-tomcat-*"
        "$env:ProgramFiles\Apache Software Foundation\Tomcat*"
        "$($script:ToolsDir)\apache-tomcat-*"
    )

    foreach ($pattern in $patterns) {
        if ([string]::IsNullOrWhiteSpace($pattern)) {
            continue
        }

        try {
            $folders = Get-ChildItem -Path $pattern -Directory -ErrorAction SilentlyContinue

            foreach ($folder in $folders) {
                $candidates += $folder.FullName
            }
        }
        catch {
            # Bo qua thu muc khong ton tai.
        }
    }

    foreach ($candidate in ($candidates | Select-Object -Unique)) {
        if ([string]::IsNullOrWhiteSpace($candidate)) {
            continue
        }

        $catalina = Join-Path $candidate 'bin\catalina.bat'

        if (Test-Path $catalina) {
            return (Resolve-Path $candidate).Path
        }
    }

    return $null
}


# ============================================================
# 4. TU CAI TOMCAT NEU CHUA CO
# ============================================================

function Install-DemoTomcat {
    $version = $script:TomcatVersion

    $java = Get-Command java.exe -ErrorAction SilentlyContinue

    if (-not $java) {
        throw @"
Khong tim thay Java tren may nay.

Can cai JDK 21 va cau hinh PATH/JAVA_HOME truoc khi chay NovaCRM.
Sau khi cai Java, dong va mo lai PowerShell roi chay lai launcher.
"@
    }

    $installRoot = $script:ToolsDir
    $targetHome = Join-Path $installRoot "apache-tomcat-$version"
    $zipFile = Join-Path $script:DownloadDir "apache-tomcat-$version-windows-x64.zip"

    $downloadUrl = "https://archive.apache.org/dist/tomcat/tomcat-10/v$version/bin/apache-tomcat-$version-windows-x64.zip"

    New-Item -ItemType Directory -Path $installRoot -Force | Out-Null
    New-Item -ItemType Directory -Path $script:DownloadDir -Force | Out-Null

    if (Test-Path (Join-Path $targetHome 'bin\catalina.bat')) {
        $script:CatalinaHome = (Resolve-Path $targetHome).Path
        return $script:CatalinaHome
    }

    Write-Step "Chua tim thay Tomcat. Thu tai Tomcat $version..."

    if (-not (Test-Path $zipFile)) {
        try {
            Invoke-WebRequest `
                -Uri $downloadUrl `
                -OutFile $zipFile `
                -UseBasicParsing
        }
        catch {
            Remove-Item $zipFile -Force -ErrorAction SilentlyContinue

            throw @"
Khong tai duoc Tomcat tu may chu Apache.

URL: $downloadUrl

Hay kiem tra ket noi Internet hoac cai Tomcat 10.1 thu cong.
Sau khi cai, dat bien NOVACRM_TOMCAT_HOME den thu muc Tomcat.
Loi goc: $($_.Exception.Message)
"@
        }
    }

    Write-Step 'Giai nen Tomcat vao thu muc local cua demo...'

    try {
        Expand-Archive -Path $zipFile -DestinationPath $installRoot -Force
    }
    catch {
        throw "Giai nen Tomcat that bai: $($_.Exception.Message)"
    }

    if (-not (Test-Path (Join-Path $targetHome 'bin\catalina.bat'))) {
        throw "Khong tim thay catalina.bat sau khi giai nen: $targetHome"
    }

    $script:CatalinaHome = (Resolve-Path $targetHome).Path

    Write-Ok "Tomcat san sang tai $($script:CatalinaHome)"

    return $script:CatalinaHome
}


# Tim Tomcat ngay khi nap file helper.
$script:CatalinaHome = Find-TomcatHome


# ============================================================
# 5. TU TIM MYSQL CLIENT
# ============================================================

function Find-MySqlClient {
    $candidates = @(
        "$env:ProgramFiles\MySQL\MySQL Server 8.4\bin\mysql.exe"
        "$env:ProgramFiles\MySQL\MySQL Server 8.0\bin\mysql.exe"
        "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe"
        "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
        "C:\xampp\mysql\bin\mysql.exe"
        "C:\wamp64\bin\mysql\mysql.exe"
    )

    # Tim cac ban MySQL Server da cai.
    $patterns = @(
        "$env:ProgramFiles\MySQL\MySQL Server*\bin\mysql.exe"
        "${env:ProgramFiles(x86)}\MySQL\MySQL Server*\bin\mysql.exe"
    )

    foreach ($pattern in $patterns) {
        if ([string]::IsNullOrWhiteSpace($pattern)) {
            continue
        }

        try {
            $found = Get-ChildItem -Path $pattern -File -ErrorAction SilentlyContinue

            foreach ($item in $found) {
                $candidates += $item.FullName
            }
        }
        catch {
            # Bo qua thu muc khong ton tai.
        }
    }

    $pathCommand = Get-Command mysql.exe -ErrorAction SilentlyContinue

    if ($pathCommand) {
        $candidates += $pathCommand.Source
    }

    foreach ($candidate in ($candidates | Select-Object -Unique)) {
        if (
            -not [string]::IsNullOrWhiteSpace($candidate) -and
            (Test-Path $candidate)
        ) {
            return (Resolve-Path $candidate).Path
        }
    }

    return $null
}

$script:MySqlExe = Find-MySqlClient


# ============================================================
# 6. DOC CAU HINH LOCAL
# ============================================================

function Import-DemoEnv {
    $envFile = Join-Path $script:LocalRoot 'demo.env'

    # Doc cau hinh local neu ton tai.
    if (Test-Path $envFile) {
        foreach ($line in Get-Content $envFile) {
            $trimmed = $line.Trim()

            if (
                [string]::IsNullOrWhiteSpace($trimmed) -or
                $trimmed.StartsWith('#')
            ) {
                continue
            }

            if ($trimmed -match '^([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*?)\s*$') {
                $key = $Matches[1]
                $value = $Matches[2]

                # Bo dau nhay bao quanh gia tri, neu co.
                if (
                    $value.Length -ge 2 -and
                    (
                        ($value.StartsWith('"') -and $value.EndsWith('"')) -or
                        ($value.StartsWith("'") -and $value.EndsWith("'"))
                    )
                ) {
                    $value = $value.Substring(1, $value.Length - 2)
                }

                $current = [Environment]::GetEnvironmentVariable($key, 'Process')

                if ([string]::IsNullOrEmpty($current)) {
                    [Environment]::SetEnvironmentVariable($key, $value, 'Process')
                }
            }
        }
    }

    if ([string]::IsNullOrWhiteSpace($env:CRM_DB_USERNAME)) {
        $env:CRM_DB_USERNAME = 'root'
    }

    # Khong ghi mat khau vao repository.
    # Neu chua co password, yeu cau nhap tai may local.
    if ($null -eq $env:CRM_DB_PASSWORD) {
        $securePassword = Read-Host `
            -Prompt "Nhap mat khau MySQL cho tai khoan $($env:CRM_DB_USERNAME)" `
            -AsSecureString

        $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)

        try {
            $env:CRM_DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
        }
        finally {
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
        }
    }

    # Co dinh database demo; khong dung crm_db.
    $env:CRM_DB_URL = "jdbc:mysql://127.0.0.1:$($script:MySqlPort)/$($script:DbName)?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8"

    if (-not $script:MySqlExe) {
        $script:MySqlExe = Find-MySqlClient
    }

    if (-not $script:MySqlExe) {
        throw @"
Khong tim thay mysql.exe.

Hay cai MySQL Server/MySQL Client, hoac them thu muc bin cua MySQL vao PATH.
Script khong tu dong reset hay xoa database tren may thanh vien.
"@
    }
}


# ============================================================
# 7. KIEM TRA MYSQL
# ============================================================

function Test-MySqlRunning {
    $client = New-Object System.Net.Sockets.TcpClient

    try {
        $task = $client.BeginConnect('127.0.0.1', $script:MySqlPort, $null, $null)

        if (-not $task.AsyncWaitHandle.WaitOne(2000, $false)) {
            return $false
        }

        $client.EndConnect($task)
        return $true
    }
    catch {
        return $false
    }
    finally {
        $client.Close()
    }
}


# ============================================================
# 8. CHAY SQL
# ============================================================

function Invoke-DemoSql([string]$Sql) {
    if (-not $script:MySqlExe -or -not (Test-Path $script:MySqlExe)) {
        throw 'Khong tim thay mysql.exe. Hay kiem tra MySQL Client.'
    }

    if (-not $env:CRM_DB_USERNAME) {
        throw 'Thieu CRM_DB_USERNAME.'
    }

    if ($null -eq $env:CRM_DB_PASSWORD) {
        throw 'Thieu CRM_DB_PASSWORD. Hay chay Import-DemoEnv truoc.'
    }

    $oldMysqlPassword = $env:MYSQL_PWD

    try {
        # Chi dat password trong environment cua process hien tai.
        $env:MYSQL_PWD = $env:CRM_DB_PASSWORD

        $output = @(
            & $script:MySqlExe `
                -u $env:CRM_DB_USERNAME `
                --protocol=TCP `
                -h 127.0.0.1 `
                -P $script:MySqlPort `
                --default-character-set=utf8mb4 `
                -N -B `
                $script:DbName `
                -e $Sql 2>&1
        )

        $exitCode = $LASTEXITCODE

        if ($exitCode -ne 0) {
            throw "Lenh SQL that bai (exit code $exitCode): $($output -join "`n")"
        }

        return $output
    }
    finally {
        $env:MYSQL_PWD = $oldMysqlPassword
    }
}


# ============================================================
# 9. TIM PROCESS DANG NGHE CONG
# ============================================================

function Get-PortOwner([int]$Port) {
    $connection = Get-NetTCPConnection `
        -State Listen `
        -LocalPort $Port `
        -ErrorAction SilentlyContinue |
        Select-Object -First 1

    if (-not $connection) {
        return $null
    }

    return Get-CimInstance Win32_Process `
        -Filter "ProcessId=$($connection.OwningProcess)" `
        -ErrorAction SilentlyContinue
}


# ============================================================
# 10. XAC DINH TOMCAT CO THUOC DEMO KHONG
# ============================================================

function Test-IsDemoTomcat($ProcessInfo) {
    if (-not $ProcessInfo -or -not $ProcessInfo.CommandLine) {
        return $false
    }

    if ([string]::IsNullOrWhiteSpace($script:CatalinaBase)) {
        return $false
    }

    return $ProcessInfo.CommandLine.IndexOf(
        $script:CatalinaBase,
        [StringComparison]::OrdinalIgnoreCase
    ) -ge 0
}


# ============================================================
# 11. BUILD WAR NEU CHUA CO
# ============================================================

function Build-DemoWar {
    if (Test-Path $script:WarSource) {
        return
    }

    Write-Step 'Chua co crm.war. Thu build backend...'

    $wrapperCandidates = @(
        (Join-Path $script:RepoRoot 'mvnw.cmd')
        (Join-Path $script:RepoRoot 'backend\mvnw.cmd')
    )

    $mavenCommand = Get-Command mvn.cmd -ErrorAction SilentlyContinue

    if (-not $mavenCommand) {
        $mavenCommand = Get-Command mvn -ErrorAction SilentlyContinue
    }

    $wrapper = $wrapperCandidates |
        Where-Object { Test-Path $_ } |
        Select-Object -First 1

    Push-Location $script:RepoRoot

    try {
        if ($wrapper) {
            & $wrapper -f (Join-Path $script:RepoRoot 'backend\pom.xml') clean package -DskipTests
        }
        elseif ($mavenCommand) {
            & $mavenCommand.Source -f (Join-Path $script:RepoRoot 'backend\pom.xml') clean package -DskipTests
        }
        else {
            throw @"
Khong tim thay Maven va Maven Wrapper.

Hay cai Maven hoac cung cap mvnw.cmd.
Sau do chay lai START_NOVACRM_DEMO.cmd.
"@
        }

        if ($LASTEXITCODE -ne 0) {
            throw "Build Maven that bai, exit code $LASTEXITCODE."
        }
    }
    finally {
        Pop-Location
    }

    if (-not (Test-Path $script:WarSource)) {
        throw "Build xong nhung khong tim thay WAR: $($script:WarSource)"
    }

    Write-Ok 'Build backend WAR thanh cong.'
}


# ============================================================
# 12. KHOI TAO TOMCAT BASE RIENG CHO DEMO
# ============================================================

function Initialize-TomcatBase([switch]$Redeploy) {
    # Tu tim hoac tu tai Tomcat neu chua tim thay.
    if (
        -not $script:CatalinaHome -or
        -not (Test-Path (Join-Path $script:CatalinaHome 'bin\catalina.bat'))
    ) {
        $script:CatalinaHome = Find-TomcatHome
    }

    if (-not $script:CatalinaHome) {
        $script:CatalinaHome = Install-DemoTomcat
    }

    Write-Ok "Tomcat: $($script:CatalinaHome)"

    # Tao thu muc local rieng, khong sua Tomcat dung chung.
    foreach ($directory in @('conf', 'logs', 'temp', 'work', 'webapps', 'bin')) {
        New-Item `
            -ItemType Directory `
            -Path (Join-Path $script:CatalinaBase $directory) `
            -Force | Out-Null
    }

    New-Item -ItemType Directory -Path $script:LogDir -Force | Out-Null

    $serverXml = Join-Path $script:CatalinaBase 'conf\server.xml'

    if (-not (Test-Path $serverXml)) {
        Write-Step 'Tao cau hinh Tomcat rieng cho NovaCRM Demo...'

        $sourceConf = Join-Path $script:CatalinaHome 'conf'

        if (-not (Test-Path $sourceConf)) {
            throw "Khong tim thay thu muc conf: $sourceConf"
        }

        Copy-Item `
            (Join-Path $sourceConf '*') `
            (Join-Path $script:CatalinaBase 'conf') `
            -Recurse -Force

        if (-not (Test-Path $serverXml)) {
            throw "Khong tao duoc server.xml: $serverXml"
        }

        $xml = Get-Content $serverXml -Raw

        # Shutdown port rieng.
        $xml = $xml.Replace(
            '<Server port="8005"',
            "<Server port=`"$($script:ShutdownPort)`""
        )

        # HTTP port rieng.
        $xml = $xml.Replace(
            '<Connector port="8080"',
            "<Connector port=`"$($script:HttpPort)`""
        )

        Set-Content -Path $serverXml -Value $xml -Encoding UTF8
    }

    # Cau hinh environment rieng cho Tomcat demo.
    # Password KHONG duoc ghi vao setenv.bat.
    $setenvFile = Join-Path $script:CatalinaBase 'bin\setenv.bat'

    $setenvContent = @"
@echo off
rem NovaCRM Demo - generated automatically.
rem Do not store database passwords in this file.
set "CRM_DB_URL=$($env:CRM_DB_URL)"
set "CRM_DB_USERNAME=$($env:CRM_DB_USERNAME)"
"@

    Set-Content -Path $setenvFile -Value $setenvContent -Encoding ASCII

    # Neu WAR chua co thi tu build.
    Build-DemoWar

    $warDestination = Join-Path $script:CatalinaBase 'webapps\crm.war'
    $explodedApp = Join-Path $script:CatalinaBase 'webapps\crm'

    if ($Redeploy -or -not (Test-Path $warDestination)) {
        Write-Step 'Trien khai crm.war vao Tomcat demo...'

        Remove-Item $explodedApp -Recurse -Force -ErrorAction SilentlyContinue

        Copy-Item `
            -Path $script:WarSource `
            -Destination $warDestination `
            -Force
    }

    Write-Ok 'Tomcat base va WAR da san sang.'
}


# ============================================================
# 13. DOI HEALTH API SAN SANG
# ============================================================

function Wait-DemoHealth([int]$TimeoutSec = 120) {
    $stopwatch = [Diagnostics.Stopwatch]::StartNew()

    Write-Step "Cho Health API toi da $TimeoutSec giay..."

    while ($stopwatch.Elapsed.TotalSeconds -lt $TimeoutSec) {
        try {
            $response = Invoke-WebRequest `
                -Uri $script:HealthUrl `
                -UseBasicParsing `
                -TimeoutSec 5

            if ($response.StatusCode -eq 200) {
                return $true
            }
        }
        catch {
            # Tomcat co the dang khoi dong; thu lai.
        }

        Start-Sleep -Seconds 2
    }

    return $false
}