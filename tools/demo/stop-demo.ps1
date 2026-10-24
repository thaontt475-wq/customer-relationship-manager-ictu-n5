<#
.SYNOPSIS  Dung Tomcat demo. Chi dung tien trinh thuoc CATALINA_BASE cua demo; khong dong MySQL va khong dung tien trinh khac.
#>
[CmdletBinding()]
param([int]$TimeoutSec = 30)

. (Join-Path $PSScriptRoot 'demo-common.ps1')
try {
    $owner = Get-PortOwner $script:HttpPort
    if (-not $owner) { Write-Ok 'Tomcat demo khong chay.'; exit 0 }
    if (-not (Test-IsDemoTomcat $owner)) {
        throw "Cong $($script:HttpPort) thuoc tien trinh khac (PID $($owner.ProcessId)). Khong dung."
    }
    $javaCmd = Get-Command java -ErrorAction Stop
    $env:JAVA_HOME = Split-Path (Split-Path $javaCmd.Source -Parent) -Parent
    $env:CATALINA_HOME = $script:CatalinaHome
    $env:CATALINA_BASE = $script:CatalinaBase
    Write-Step "Dung Tomcat demo (PID $($owner.ProcessId))..."
    & "$($script:CatalinaHome)\bin\catalina.bat" stop *> $null
    $sw = [Diagnostics.Stopwatch]::StartNew()
    while ($sw.Elapsed.TotalSeconds -lt $TimeoutSec -and (Get-PortOwner $script:HttpPort)) { Start-Sleep 1 }
    $left = Get-PortOwner $script:HttpPort
    if ($left -and (Test-IsDemoTomcat $left)) {
        Write-Warn2 'Tomcat chua dung theo shutdown port, dung tien trinh demo.'
        Stop-Process -Id $left.ProcessId -Force
        Start-Sleep 2
    }
    if (Get-PortOwner $script:HttpPort) { throw 'Cong van con bi chiem.' }
    Write-Ok 'Da dung Tomcat demo.'
    exit 0
} catch {
    Write-Fail $_.Exception.Message
    exit 1
}
