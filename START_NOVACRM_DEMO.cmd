@echo off
title NovaCRM Demo
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\demo\start-demo.ps1"
if errorlevel 1 (
  echo.
  echo === KHOI DONG THAT BAI - xem thong bao [FAIL] o tren ===
  pause
  exit /b 1
)
ping 127.0.0.1 -n 3 >nul
