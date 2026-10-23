# Launcher chay Tomcat demo (foreground) - duoc start-demo.ps1 khoi dong tach roi. Khong chua mat khau.
. (Join-Path $PSScriptRoot 'demo-common.ps1')
Import-DemoEnv
$javaCmd = Get-Command java -ErrorAction Stop
$env:JAVA_HOME = Split-Path (Split-Path $javaCmd.Source -Parent) -Parent
$env:CATALINA_HOME = $script:CatalinaHome
$env:CATALINA_BASE = $script:CatalinaBase
$env:CATALINA_OPTS = '-Dfile.encoding=UTF-8 -Duser.timezone=Asia/Ho_Chi_Minh'
New-Item -ItemType Directory -Force $script:LogDir | Out-Null
& "$($script:CatalinaHome)\bin\catalina.bat" run *>> (Join-Path $script:LogDir 'tomcat-demo.out.log')


