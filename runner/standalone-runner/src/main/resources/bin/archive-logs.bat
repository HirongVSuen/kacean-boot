@echo off
setlocal
rem Archive logs older than 30 days into .logs\archive as zip files (monthly kept plain, older compressed).
rem startup.bat runs this automatically before start; can also be scheduled monthly via Task Scheduler.
rem Note: keep this file pure ASCII, cmd parses UTF-8/GBK Chinese comments incorrectly.
for %%I in ("%~dp0..") do set APP_HOME=%%~fI
powershell -NoProfile -Command "$logDir = Join-Path $env:APP_HOME '.logs'; if (Test-Path $logDir) { $archive = Join-Path $logDir 'archive'; New-Item -ItemType Directory -Force -Path $archive | Out-Null; Get-ChildItem $logDir -File -Filter 'power-system.*.log' | Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-30) } | ForEach-Object { $zip = Join-Path $archive ($_.BaseName + '.zip'); Compress-Archive -Path $_.FullName -DestinationPath $zip -Force; Remove-Item $_.FullName -Force; Write-Output ('Archived: ' + $_.Name + ' -> archive/' + $_.BaseName + '.zip') } }"
endlocal
