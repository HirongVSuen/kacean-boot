@echo off
setlocal EnableDelayedExpansion
rem Standalone application startup script (Windows)
rem Note: keep this file pure ASCII, cmd parses UTF-8/GBK Chinese comments incorrectly.
set APP_NAME=standalone-runner
for %%I in ("%~dp0..") do set APP_HOME=%%~fI
set APP_HOME_UNIX=%APP_HOME:\=/%
set JAR_FILE=%APP_HOME%\lib\%APP_NAME%.jar
set PID_FILE=%APP_HOME%\run\%APP_NAME%.pid
set LOG_DIR=%APP_HOME%\.logs
set LOG_FILE=%LOG_DIR%\%APP_NAME%.log
set ERR_FILE=%LOG_DIR%\%APP_NAME%-error.log
set MAIN_CLASS=com.kacean.boot.KaceanBootApplication

rem 1. Check the executable jar under lib directory
if not exist "%JAR_FILE%" (
    echo [ERROR] Executable jar not found: %JAR_FILE%
    echo         Please make sure the distribution is complete, lib\%APP_NAME%.jar is required.
    exit /b 1
)

rem 2. Prevent duplicate startup
if exist "%PID_FILE%" (
    set /p PID=<"%PID_FILE%"
    tasklist /fi "PID eq !PID!" 2>nul | findstr /i "java" >nul
    if !errorlevel!==0 (
        echo Application is already running, PID: !PID!
        exit /b 0
    )
)

rem 3. Archive logs older than 30 days, then start in background via PowerShell and record PID
call "%~dp0archive-logs.bat"
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"
if not exist "%APP_HOME%\run" mkdir "%APP_HOME%\run"
powershell -NoProfile -Command "$p = Start-Process -FilePath 'java' -ArgumentList '-cp', ('\"' + $env:APP_HOME + '\lib\*' + '\"'), $env:MAIN_CLASS, ('--logging.config=file:' + $env:APP_HOME_UNIX + '/conf/logback-spring.xml'), ('\"--spring.config.additional-location=file:' + $env:APP_HOME_UNIX + '/conf/\"') -WorkingDirectory $env:APP_HOME -RedirectStandardOutput $env:LOG_FILE -RedirectStandardError $env:ERR_FILE -WindowStyle Hidden -PassThru; Set-Content -Path $env:PID_FILE -Value $p.Id -Encoding ascii"
echo Started, PID file: %PID_FILE%
echo Log file: %LOG_FILE%
endlocal
