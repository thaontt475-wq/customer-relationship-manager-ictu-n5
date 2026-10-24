@echo off
setlocal EnableExtensions
title NovaCRM Demo

REM 1. Uu tien duong dan Tomcat da duoc cau hinh neu hop le
set "DEMO_TOMCAT_HOME="

if defined NOVACRM_TOMCAT_HOME if exist "%NOVACRM_TOMCAT_HOME%\bin\catalina.bat" (
    set "DEMO_TOMCAT_HOME=%NOVACRM_TOMCAT_HOME%"
)

REM 2. Tim trong thu muc Tools cua user hien tai
if not defined DEMO_TOMCAT_HOME (
    if exist "%USERPROFILE%\Tools\apache-tomcat-10.1.60\bin\catalina.bat" (
        set "DEMO_TOMCAT_HOME=%USERPROFILE%\Tools\apache-tomcat-10.1.60"
    )
)

REM 3. Thu duong dan C:\Tools
if not defined DEMO_TOMCAT_HOME (
    if exist "C:\Tools\apache-tomcat-10.1.60\bin\catalina.bat" (
        set "DEMO_TOMCAT_HOME=C:\Tools\apache-tomcat-10.1.60"
    )
)

REM 4. Dung ro rang neu khong tim thay
if not defined DEMO_TOMCAT_HOME (
    echo.
    echo [FAIL] Khong tim thay Apache Tomcat 10.1.60.
    echo Hay cai dat Tomcat hoac cau hinh bien NOVACRM_TOMCAT_HOME.
    echo.
    pause
    exit /b 1
)

set "NOVACRM_TOMCAT_HOME=%DEMO_TOMCAT_HOME%"

REM Chay tu thu muc chua file CMD
cd /d "%~dp0"
set "START_SCRIPT=%~dp0tools\demo\start-demo.ps1"

echo.
echo ==========================================
echo              NovaCRM Demo
echo ==========================================
echo.
echo [INFO] Tomcat: %NOVACRM_TOMCAT_HOME%
echo.

if not exist "%START_SCRIPT%" (
    echo [FAIL] Khong tim thay script:
    echo "%START_SCRIPT%"
    echo.
    pause
    exit /b 1
)

echo [INFO] Dang khoi dong NovaCRM Demo...
echo.

powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%START_SCRIPT%"
set "RC=%ERRORLEVEL%"

if not "%RC%"=="0" (
    echo.
    echo [FAIL] KHOI DONG NOVACRM THAT BAI. Ma loi: %RC%
    echo.
    pause
    exit /b %RC%
)

echo.
echo [OK] LENH KHOI DONG NOVACRM DA HOAN TAT
echo URL: http://localhost:8090/crm/login.html
echo.

timeout /t 2 /nobreak >nul
pause
exit /b 0