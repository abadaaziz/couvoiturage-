
@echo off
setlocal

set "CATALINA_HOME=%CD%\.tools\apache-tomcat-10.1.31"
set "CATALINA_BASE=%CATALINA_HOME%"

REM Stop any existing Tomcat java process for this workspace.
powershell -NoProfile -ExecutionPolicy Bypass -Command "$tomcat=(Resolve-Path '.tools\apache-tomcat-10.1.31').Path;Get-CimInstance Win32_Process ^| Where-Object { $_.Name -eq 'java.exe' -and $_.CommandLine -like ('*'+$tomcat+'*') } ^| ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }" >nul 2>nul

for /f "tokens=5" %%p in ('netstat -ano ^| findstr /R /C:":8080 .*LISTENING"') do taskkill /F /PID %%p >nul 2>nul
for /f "tokens=5" %%p in ('netstat -ano ^| findstr /R /C:":8005 .*LISTENING"') do taskkill /F /PID %%p >nul 2>nul

set "MVN_CMD=%LOCALAPPDATA%\Programs\Apache\Maven\bin\mvn.cmd"
if not exist "%MVN_CMD%" if defined M2_HOME set "MVN_CMD=%M2_HOME%\bin\mvn.cmd"
if not exist "%MVN_CMD%" set "MVN_CMD=mvn"

call "%MVN_CMD%" clean package -DskipTests || goto :eof

copy /Y target\covoiturage.war .tools\apache-tomcat-10.1.31\webapps\ROOT.war >nul
if exist .tools\apache-tomcat-10.1.31\webapps\ROOT rmdir /S /Q .tools\apache-tomcat-10.1.31\webapps\ROOT

REM Start Tomcat in background (more stable than foreground run for repeated restarts).
call "%CATALINA_HOME%\bin\startup.bat"

echo CovoitApp started on http://localhost:8080
