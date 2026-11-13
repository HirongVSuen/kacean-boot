@echo off
rem Standalone application restart script (Windows): stop then start
rem Note: keep this file pure ASCII, cmd parses UTF-8/GBK Chinese comments incorrectly.
call "%~dp0stop.bat"
call "%~dp0startup.bat"
