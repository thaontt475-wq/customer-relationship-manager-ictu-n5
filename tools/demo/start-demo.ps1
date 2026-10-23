<#
.SYNOPSIS  Khoi dong NovaCRM demo mot lan nhap (MySQL -> Tomcat demo -> health -> mo trinh duyet).
.PARAMETER NoBrowser   Khong mo trinh duyet.
.PARAMETER Redeploy    Trien khai lai WAR tu backend\target\crm.war (chi khi Tomcat demo dang dung).
.PARAMETER TimeoutSec  Thoi gian toi da cho health API (mac dinh 120s).
#>
[CmdletBinding()]
param([switch]$NoBrowser, [switch]$Redeploy, [int]$TimeoutSec = 120)

. (Join-Path $PSScriptRoot 'demo-common.ps1')
try {
    Write-Step "NovaCRM demo - commit: $(git -C $script:RepoRoot log -1 --oneline 2>$null)"
    Import-DemoEnv

    # 1. MySQL
    if (-not (Test-MySqlRunning)) {
        throw "MySQL ($($script:MySqlService)) khong chay o cong $($script:MySqlPort). Hay khoi dong service truoc (Start-Service $($script:MySqlService), can quyen Admin)."
    }
    Write-Ok "MySQL dang chay (cong $($script:MySqlPort))"
    $dbs = Invoke-DemoSql 'SELECT COUNT(*) FROM customers'
    if ($LASTEXITCODE -ne 0) { throw "Khong truy cap duoc database $($script:DbName): $dbs" }
    Write-Ok "Database $($script:DbName) san sang (customers=$dbs)"

    # 2. Tomcat demo da chay chua?
    $owner = Get-PortOwner $script:HttpPort
    if ($owner -and -not (Test-IsDemoTomcat $owner)) {
        throw "Cong $($script:HttpPort) dang bi tien trinh khac su dung (PID $($owner.ProcessId), $($owner.Name)). Khong tu dong dung. Hay giai phong cong hoac doi HttpPort."
    }
    if ($owner) {
        Write-Ok "Tomcat demo da chay (PID $($owner.ProcessId)) - khong khoi dong lai"
        if ($Redeploy) { Write-Warn2 'Bo qua -Redeploy vi Tomcat dang chay. Chay stop-demo.ps1 truoc.' }
    } else {
        Initialize-TomcatBase -Redeploy:$Redeploy
        Write-Step "Khoi dong Tomcat demo (cong $($script:HttpPort))..."
        # Tao qua WMI de Tomcat song doc lap voi cua so/tien trinh khoi chay (khong bi dong theo).
        $psExe = (Get-Command powershell.exe).Source
        $cl = "`"$psExe`" -NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File `"$(Join-Path $PSScriptRoot 'run-tomcat.ps1')`""
        $res = Invoke-CimMethod -ClassName Win32_Process -MethodName Create -Arguments @{ CommandLine = $cl; CurrentDirectory = $script:RepoRoot }
        if ($res.ReturnValue -ne 0) { throw "Khong khoi dong duoc Tomcat (WMI ma $($res.ReturnValue))." }
    }

    # 3. Health
    Write-Step "Cho health API ($($script:HealthUrl)) toi da $TimeoutSec giay..."
    if (-not (Wait-DemoHealth $TimeoutSec)) {
        throw "Health API khong san sang sau $TimeoutSec giay. Xem log: $($script:LogDir)\tomcat-demo.out.log va $($script:CatalinaBase)\logs"
    }
    Write-Ok 'Health API san sang'

    # 4. Mo trinh duyet
    if (-not $NoBrowser) { Start-Process $script:AppUrl; Write-Ok "Da mo $($script:AppUrl)" }
    else { Write-Ok "URL: $($script:AppUrl)" }
    exit 0
} catch {
    Write-Fail $_.Exception.Message
    exit 1
}

