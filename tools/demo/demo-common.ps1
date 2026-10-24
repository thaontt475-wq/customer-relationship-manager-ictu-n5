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

# icacls.exe duoc tu tim khi can (co the gan truoc de kiem thu).
$script:IcaclsExe = $null


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

# Chi cho phep hoi nguoi dung khi co console that. run-tomcat.ps1 chay an (WMI) khong bao gio duoc hoi.
function Test-CanPromptUser {
    $rootFrame = Get-PSCallStack | Select-Object -Last 1

    if (
        $rootFrame.ScriptName -and
        (Split-Path $rootFrame.ScriptName -Leaf) -ieq 'run-tomcat.ps1'
    ) {
        return $false
    }

    return ([Environment]::UserInteractive -and -not [Console]::IsInputRedirected)
}

# Ghi byte vao stream (tach rieng de co the kiem thu loi ghi).
function Write-DemoEnvBytes($Stream, [byte[]]$Bytes) {
    $Stream.Write($Bytes, 0, $Bytes.Length)
    $Stream.Flush($true)
}

# Kiem tra ACL THUC TE cua file (khong chi dua vao exit code cua icacls).
# Tra ve danh sach van de; mang rong = an toan.
function Test-DemoEnvAcl([string]$Path, $UserSid) {
    $problems = @()

    try {
        $acl = Get-Acl -LiteralPath $Path
    }
    catch {
        return @('khong doc duoc ACL cua file')
    }

    if (-not $acl.AreAccessRulesProtected) {
        $problems += 'file van ke thua quyen tu thu muc cha'
    }

    $targetSid = if ($UserSid -is [System.Security.Principal.SecurityIdentifier]) {
        $UserSid.Value
    }
    elseif ($UserSid -is [string]) {
        $UserSid
    }
    elseif ($UserSid -and $UserSid.Value) {
        $UserSid.Value
    }
    else {
        [System.Security.Principal.WindowsIdentity]::GetCurrent().User.Value
    }

    $adminSid = 'S-1-5-32-544'

    try {
        $owner = $acl.GetOwner([System.Security.Principal.SecurityIdentifier]).Value

        if ($owner -ne $targetSid -and $owner -ne $adminSid) {
            $problems += "chu so huu file la $owner (khong phai tai khoan hien tai)"
        }
    }
    catch {
        $problems += 'khong xac dinh duoc chu so huu file'
    }

    $rules = @($acl.GetAccessRules($true, $true, [System.Security.Principal.SecurityIdentifier]))

    if ($rules.Count -eq 0) {
        $problems += 'khong co quyen truy cap nao'
    }
    elseif ($rules.Count -gt 1) {
        $problems += "co $($rules.Count) quyen truy cap; chi cho phep dung 1 quyen Allow cho tai khoan hien tai"
    }

    $rights = 0

    foreach ($rule in $rules) {
        if ($rule.IdentityReference.Value -ne $targetSid) {
            $problems += "co quyen cho tai khoan khac: $($rule.IdentityReference.Value)"
            continue
        }

        if ($rule.AccessControlType -ne [System.Security.AccessControl.AccessControlType]::Allow) {
            $problems += 'co rule Deny khong mong doi'
            continue
        }

        $rights = $rights -bor [int]$rule.FileSystemRights
    }

    $needRead = [int][System.Security.AccessControl.FileSystemRights]::ReadData
    $needWrite = [int][System.Security.AccessControl.FileSystemRights]::WriteData

    if (($rights -band $needRead) -ne $needRead -or ($rights -band $needWrite) -ne $needWrite) {
        $problems += 'tai khoan hien tai khong co du quyen doc/ghi'
    }

    return $problems
}

# Luu demo.env AN TOAN. Tra ve $true chi khi: file moi duoc tao (CreateNew, khong ghi de),
# ACL da duoc gioi han va XAC MINH, noi dung da ghi xong. Nguoc lai tra ve $false va
# khong de lai file chua mat khau. Khong bao gio dua mat khau vao thong bao.
function Save-DemoEnvFile {
    param(
        [string]$Path,
        [string]$Username,
        [string]$Password
    )

    if (
        [string]::IsNullOrWhiteSpace($Username) -or
        [string]::IsNullOrWhiteSpace($Password) -or
        $Username -match '[\r\n]' -or
        $Password -match '[\r\n]' -or
        $Username -ne $Username.Trim() -or
        $Password -ne $Password.Trim() -or
        $Password -match '^["'']' -or
        $Password -match '["'']$'
    ) {
        Write-Warn2 'Tai khoan/mat khau co ky tu khong luu duoc an toan vao demo.env (xuong dong, khoang trang hoac dau nhay o dau/cuoi). Khong luu.'
        return $false
    }

    $icacls = $script:IcaclsExe

    if (-not $icacls) {
        $found = Get-Command icacls.exe -ErrorAction SilentlyContinue
        if ($found) { $icacls = $found.Source }
    }

    if (-not $icacls -or -not (Test-Path -LiteralPath $icacls)) {
        Write-Warn2 'Khong tim thay icacls.exe nen khong the gioi han quyen demo.env. Khong luu mat khau.'
        return $false
    }

    try {
        $userSid = [System.Security.Principal.WindowsIdentity]::GetCurrent().User
        New-Item -ItemType Directory -Path (Split-Path $Path -Parent) -Force | Out-Null
    }
    catch {
        Write-Warn2 "Khong chuan bi duoc thu muc cho demo.env ($($_.Exception.GetType().Name)). Khong luu mat khau."
        return $false
    }

    $stream = $null
    $created = $false
    $saved = $false

    try {
        # CreateNew: that bai neu file da ton tai (ke ca file vua xuat hien dong thoi) -> khong bao gio ghi de.
        # FileShare.ReadWrite (khong co Delete): giu handle mo de file khong bi doi ten/xoa/thay the
        # trong luc icacls va buoc kiem tra ACL chay; file van RONG nen chua co bi mat nao bi lo.
        try {
            $stream = [System.IO.FileStream]::new(
                $Path,
                [System.IO.FileMode]::CreateNew,
                [System.IO.FileAccess]::Write,
                [System.IO.FileShare]::ReadWrite
            )
            $created = $true
        }
        catch {
            if (Test-Path -LiteralPath $Path) {
                Write-Warn2 'demo.env da ton tai (hoac vua duoc tao boi tien trinh khac). KHONG ghi de va KHONG luu mat khau.'
            }
            else {
                Write-Warn2 "Khong tao duoc demo.env ($($_.Exception.GetType().Name)). Khong luu mat khau."
            }
            return $false
        }

        # Gioi han quyen: bo ke thua, chi cap quyen doc/ghi cho SID cua tai khoan hien tai.
        $exitCode = $null

        try {
            $exitCode = & {
                $ErrorActionPreference = 'Continue'
                & $icacls $Path /inheritance:r /grant:r ("*{0}:(R,W)" -f $userSid.Value) *> $null
                $LASTEXITCODE
            }
        }
        catch {
            $exitCode = -1
        }

        if ($exitCode -ne 0) {
            Write-Warn2 "icacls that bai (ma $exitCode). Khong luu mat khau."
            return $false
        }

        # Khong tin exit code: doc lai ACL thuc te.
        $aclProblems = @(Test-DemoEnvAcl -Path $Path -UserSid $userSid)

        if ($aclProblems.Count -gt 0) {
            Write-Warn2 "Khong xac minh duoc ACL an toan cho demo.env: $($aclProblems -join '; '). Khong luu mat khau."
            return $false
        }

        # Chi ghi noi dung SAU KHI ACL da duoc thiet lap va xac minh.
        $encoding = [System.Text.UTF8Encoding]::new($true)
        $body = "CRM_DB_USERNAME=$Username`r`nCRM_DB_PASSWORD=$Password`r`n"
        $bytes = [byte[]]($encoding.GetPreamble() + $encoding.GetBytes($body))

        try {
            Write-DemoEnvBytes -Stream $stream -Bytes $bytes
        }
        finally {
            [Array]::Clear($bytes, 0, $bytes.Length)
            $body = $null
        }

        $saved = $true
        return $true
    }
    catch {
        # Khong dua noi dung loi/mat khau vao thong bao: chi ten loai ngoai le.
        Write-Warn2 "Loi khi luu demo.env ($($_.Exception.GetType().Name)). Mat khau khong duoc luu."
        return $false
    }
    finally {
        if ($stream) {
            try { if (-not $saved) { $stream.SetLength(0) } } catch { }
            try { $stream.Dispose() } catch { }
        }

        # File nay do chinh ham nay vua tao: neu that bai thi xoa de khong de lai file rong/mot phan.
        if ($created -and -not $saved) {
            Remove-Item -LiteralPath $Path -Force -ErrorAction SilentlyContinue

            if (Test-Path -LiteralPath $Path) {
                Write-Warn2 "Khong xoa duoc file do dang: $Path. Hay kiem tra va xoa thu cong."
            }
        }
    }
}

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

    # Khong ghi mat khau vao repository, setenv.bat hay log.
    # Neu chua co password hoac password chi chua khoang trang, yeu cau nhap an toan tai may local.
    if ([string]::IsNullOrWhiteSpace($env:CRM_DB_PASSWORD)) {
        # run-tomcat.ps1 chay an (WMI, khong co console de nhap) -> khong bao gio hoi, chi bao loi.
        $canPrompt = Test-CanPromptUser

        if ($canPrompt) {
            $securePassword = Read-Host `
                -Prompt "Nhap mat khau MySQL cho tai khoan $($env:CRM_DB_USERNAME)" `
                -AsSecureString

            if ($securePassword -and $securePassword.Length -gt 0) {
                $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)

                try {
                    $env:CRM_DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
                }
                finally {
                    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
                }

                # Chi de xuat luu khi demo.env CHUA ton tai (khong bao gio ghi de file co san).
                # Tomcat chay an (WMI) khong ke thua bien moi truong cua cua so nay, nen can demo.env.
                if (-not (Test-Path -LiteralPath $envFile)) {
                    $answer = Read-Host -Prompt "Luu tai khoan vao $envFile (ngoai repository, chi tai khoan Windows hien tai doc duoc) de Tomcat nen doc duoc? [y/N]"

                    if ($answer -match '^(y|yes)$') {
                        if (Save-DemoEnvFile -Path $envFile -Username $env:CRM_DB_USERNAME -Password $env:CRM_DB_PASSWORD) {
                            Write-Ok "Da luu cau hinh local: $envFile"
                        }
                        else {
                            Write-Warn2 "Mat khau CHUA duoc luu. Tomcat chay an se khong co mat khau; hay tu tao $envFile neu can."
                        }
                    }
                    else {
                        Write-Warn2 "Khong luu demo.env: Tomcat chay an se khong co mat khau. Hay tu tao $envFile neu can."
                    }
                }
                else {
                    Write-Warn2 "demo.env da ton tai nhung thieu mat khau hop le; script KHONG ghi de. Hay sua $envFile."
                }
            }
        }
    }

    if ([string]::IsNullOrWhiteSpace($env:CRM_DB_PASSWORD)) {
        throw @"
Mat khau MySQL (CRM_DB_PASSWORD) khong hop le hoac bi bo trong.

Vui long cung cap mat khau hop le qua bien moi truong hoac tao file cau hinh tai:
$envFile
Noi dung mau:
CRM_DB_USERNAME=$($env:CRM_DB_USERNAME)
CRM_DB_PASSWORD=<mat_khau_mysql>
"@
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

    if ([string]::IsNullOrWhiteSpace($env:CRM_DB_PASSWORD)) {
        throw 'Thieu CRM_DB_PASSWORD hoac mat khau chi chua khoang trang. Hay chay Import-DemoEnv truoc.'
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

    $wrapper = $wrapperCandidates |
        Where-Object { Test-Path $_ } |
        Select-Object -First 1

    $mavenCommand = Get-Command mvn.cmd -ErrorAction SilentlyContinue

    if (-not $mavenCommand) {
        $mavenCommand = Get-Command mvn -ErrorAction SilentlyContinue
    }

    $mvnExe = if ($mavenCommand) { $mavenCommand.Source } else { $null }

    if (-not $wrapper -and -not $mvnExe) {
        $mvnCandidates = @(
            "$env:USERPROFILE\Tools\apache-maven-*\bin\mvn.cmd"
            "C:\Tools\apache-maven-*\bin\mvn.cmd"
            "$env:ProgramFiles\apache-maven-*\bin\mvn.cmd"
            "$env:ProgramFiles\Apache Software Foundation\apache-maven-*\bin\mvn.cmd"
        )
        foreach ($pattern in $mvnCandidates) {
            $found = Get-ChildItem -Path $pattern -File -ErrorAction SilentlyContinue | Select-Object -First 1
            if ($found) {
                $mvnExe = $found.FullName
                break
            }
        }
    }

    $pomPath = Join-Path $script:RepoRoot 'backend\pom.xml'
    if (-not (Test-Path $pomPath)) {
        throw "Khong tim thay pom.xml tai: $pomPath"
    }

    Push-Location $script:RepoRoot

    try {
        $global:LASTEXITCODE = 0

        if ($wrapper) {
            & $wrapper -f $pomPath clean package -DskipTests
        }
        elseif ($mvnExe) {
            & $mvnExe -f $pomPath clean package -DskipTests
        }
        else {
            throw @"
Khong tim thay Maven va Maven Wrapper.

Hay cai Maven hoac cung cap mvnw.cmd.
Sau do chay lai START_NOVACRM_DEMO.cmd.
"@
        }

        $buildExitCode = $LASTEXITCODE

        if ($buildExitCode -ne 0) {
            throw "Build Maven that bai, exit code $buildExitCode."
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

# Liet ke cac the <TagName> CON HIEU LUC (bo qua noi dung trong <!-- comment -->).
function Get-DemoLiveTags([string]$Xml, [string]$TagName) {
    $result = @()
    $pattern = "(?s)<!--.*?-->|(?<tag><$TagName(?=[\s/>])[^>]*>)"

    foreach ($match in [regex]::Matches($Xml, $pattern)) {
        if ($match.Groups['tag'].Success) {
            $result += [pscustomobject]@{
                Index = $match.Groups['tag'].Index
                Tag   = $match.Groups['tag'].Value
            }
        }
    }

    return $result
}

# Doc gia tri thuoc tinh trong mot the (chap nhan khoang trang quanh dau '=' va nhay don/kep).
function Get-DemoTagAttr([string]$Tag, [string]$Name) {
    $match = [regex]::Match($Tag, "(?<![\w.-])$Name\s*=\s*([`"'])(?<v>.*?)\1")

    if ($match.Success) { return $match.Groups['v'].Value }

    return $null
}

# Doi gia tri port trong MOT the. Nhanh literal: the bat dau dung chuoi chuan.
# Nhanh regex: thuoc tinh co khoang trang/thu tu khac. Chi sua dung gia tri cu; tra ve the moi.
function Set-DemoTagPort([string]$Tag, [string]$Literal, [string]$OldPort, [string]$NewPort) {
    $literalNew = $Literal.Replace("`"$OldPort`"", "`"$NewPort`"")

    if ($Tag.StartsWith($Literal)) {
        return $literalNew + $Tag.Substring($Literal.Length)
    }

    $regex = New-Object System.Text.RegularExpressions.Regex(
        "((?<![\w.-])port\s*=\s*)([`"'])$OldPort\2"
    )

    return $regex.Replace($Tag, ('${1}${2}' + $NewPort + '${2}'), 1)
}

# Doi cong shutdown (8005 -> ShutdownPort) va HTTP (8080 -> HttpPort) TRONG BO NHO.
# Khong ghi file. Nem loi neu cau truc khong hop le hoac khong thay duoc dung cong.
function Set-DemoServerXmlPorts([string]$Xml) {
    if ([string]::IsNullOrWhiteSpace($Xml)) {
        throw 'Noi dung server.xml bi rong.'
    }

    $shutdown = "$($script:ShutdownPort)"
    $http = "$($script:HttpPort)"

    # --- Shutdown port: dung 1 the <Server> con hieu luc ---
    $servers = @(Get-DemoLiveTags -Xml $Xml -TagName 'Server')

    if ($servers.Count -ne 1) {
        throw "server.xml phai co dung 1 the <Server> (tim thay $($servers.Count))."
    }

    $serverTag = $servers[0]
    $currentShutdown = Get-DemoTagAttr -Tag $serverTag.Tag -Name 'port'

    if ($currentShutdown -eq '8005') {
        $newTag = Set-DemoTagPort -Tag $serverTag.Tag -Literal '<Server port="8005"' -OldPort '8005' -NewPort $shutdown

        if ($newTag -ceq $serverTag.Tag) {
            throw 'Khong thay duoc cong shutdown 8005 trong server.xml.'
        }

        $Xml = $Xml.Substring(0, $serverTag.Index) + $newTag + $Xml.Substring($serverTag.Index + $serverTag.Tag.Length)
    }
    elseif ($currentShutdown -ne $shutdown) {
        throw "Khong tim thay cong shutdown 8005 de thay (hien tai: '$currentShutdown')."
    }

    # --- HTTP port: the <Connector> HTTP/1.1 con hieu luc dau tien co port 8080 ---
    $connectors = @(Get-DemoLiveTags -Xml $Xml -TagName 'Connector')
    $httpConnectors = @(
        $connectors | Where-Object {
            $protocol = Get-DemoTagAttr -Tag $_.Tag -Name 'protocol'
            (-not $protocol) -or ($protocol -match '^(HTTP/1\.1|org\.apache\.coyote\.http11\.Http11\w*Protocol)$')
        }
    )

    $target = $httpConnectors |
        Where-Object { (Get-DemoTagAttr -Tag $_.Tag -Name 'port') -eq '8080' } |
        Select-Object -First 1

    if ($target) {
        $newTag = Set-DemoTagPort -Tag $target.Tag -Literal '<Connector port="8080"' -OldPort '8080' -NewPort $http

        if ($newTag -ceq $target.Tag) {
            throw 'Khong thay duoc cong HTTP 8080 trong server.xml.'
        }

        $Xml = $Xml.Substring(0, $target.Index) + $newTag + $Xml.Substring($target.Index + $target.Tag.Length)
    }
    elseif (-not ($httpConnectors | Where-Object { (Get-DemoTagAttr -Tag $_.Tag -Name 'port') -eq $http })) {
        throw 'Khong tim thay connector HTTP cong 8080 de thay.'
    }

    return $Xml
}

# Xac nhan NOI DUNG server.xml la XML hop le, shutdown = ShutdownPort (8095), HTTP = HttpPort (8090).
function Assert-DemoServerXmlContent([string]$Xml, [string]$Source) {
    try {
        [xml]$parsedXml = $Xml
    }
    catch {
        throw "Cau hinh server.xml khong phai XML hop le ($Source)."
    }

    $serverPort = $parsedXml.Server.port
    if ($serverPort -ne "$($script:ShutdownPort)") {
        throw "server.xml sai cong shutdown: '$serverPort', mong doi '$($script:ShutdownPort)'. Sua $Source hoac xoa tomcat-base\conf de tao lai (script khong tu ghi de)."
    }

    $httpConnector = $parsedXml.Server.Service.Connector |
        Where-Object { $_.protocol -eq 'HTTP/1.1' -or [string]::IsNullOrWhiteSpace($_.protocol) } |
        Select-Object -First 1

    if (-not $httpConnector -or $httpConnector.port -ne "$($script:HttpPort)") {
        $foundPort = if ($httpConnector) { $httpConnector.port } else { 'khong tim thay' }
        throw "server.xml sai cong HTTP: '$foundPort', mong doi '$($script:HttpPort)'. Sua $Source hoac xoa tomcat-base\conf de tao lai (script khong tu ghi de)."
    }
}

# Chi doc file; khong sua cau hinh co san cua nguoi dung.
function Assert-DemoServerXml([string]$Path) {
    Assert-DemoServerXmlContent -Xml (Get-Content -Path $Path -Raw) -Source $Path
}

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

        try {
            $xml = Get-Content -Path $serverXml -Raw

            # Thay cong va xac nhan HOAN TOAN TRONG BO NHO; chi ghi file khi ket qua hop le.
            $newXml = Set-DemoServerXmlPorts -Xml $xml
            Assert-DemoServerXmlContent -Xml $newXml -Source $serverXml

            Set-Content -Path $serverXml -Value $newXml -Encoding UTF8
        }
        catch {
            # server.xml nay vua duoc copy o buoc tren (truoc do chua ton tai): xoa de lan chay sau
            # tao lai thay vi coi mot file sai la 'da ton tai'.
            Remove-Item -LiteralPath $serverXml -Force -ErrorAction SilentlyContinue
            throw
        }
    }

    # Luon kiem tra (ca khi server.xml da ton tai tu lan chay truoc). Chi doc, khong ghi de.
    Assert-DemoServerXml -Path $serverXml

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