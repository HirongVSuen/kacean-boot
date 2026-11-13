@echo off
setlocal EnableDelayedExpansion
rem Standalone application stop script (Windows)
rem Note: keep this file pure ASCII, cmd parses UTF-8/GBK Chinese comments incorrectly.
set APP_NAME=standalone-runner
for %%I in ("%~dp0..") do set APP_HOME=%%~fI
set PID_FILE=%APP_HOME%\run\%APP_NAME%.pid

if not exist "%PID_FILE%" (
    echo PID file not found: %PID_FILE%, application is not running or not started by script.
    exit /b 0
)

set /p PID=<"%PID_FILE%"
tasklist /fi "PID eq !PID!" 2>nul | findstr /i "java" >nul
if !errorlevel!==0 (
    taskkill /f /pid !PID! >nul
    echo Application stopped, PID: !PID!
) else (
    echo Process does not exist, PID: !PID!
)
del /f /q "%PID_FILE%" >nul 2>&1
endlocal
