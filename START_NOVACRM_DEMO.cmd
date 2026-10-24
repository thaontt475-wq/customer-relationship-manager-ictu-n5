@echo off
set "NOVACRM_TOMCAT_HOME=C:\Tools\apache-tomcat-10.1.60"
setlocal
title NovaCRM Demo

REM Luon chay tu thu muc chua file BAT
cd /d "%~dp0"

set "START_SCRIPT=%~dp0tools\demo\start-demo.ps1"

echo.
echo ==========================================
echo              NovaCRM Demo
echo ==========================================
echo.

REM Kiem tra script khoi dong co ton tai khong
if not exist "%START_SCRIPT%" (
    echo [FAIL] Khong tim thay script khoi dong:
    echo "%START_SCRIPT%"
    echo.
    pause
    exit /b 1
)

echo [INFO] Dang khoi dong NovaCRM Demo...
echo.

REM Chay script PowerShell
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%START_SCRIPT%"
set "RC=%ERRORLEVEL%"

REM Xu ly neu khoi dong that bai
if not "%RC%"=="0" (
    echo.
    echo ==========================================
    echo [FAIL] KHOI DONG NOVACRM THAT BAI
    echo Ma loi: %RC%
    echo Hay kiem tra thong bao [FAIL] o tren.
    echo ==========================================
    echo.
    pause
    exit /b %RC%
)

REM Xu ly neu script ket thuc thanh cong
echo.
echo ==========================================
echo [OK] LENH KHOI DONG NOVACRM DA HOAN TAT
echo ==========================================
echo.
echo Neu can kiem tra trang thai, chay:
echo powershell -NoProfile -ExecutionPolicy Bypass -File ".\tools\demo\check-demo.ps1"
echo.

timeout /t 2 /nobreak >nul
pause

exit /b 0
