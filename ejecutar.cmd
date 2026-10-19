@echo off
setlocal
cd /d "%~dp0"
set "JAVA_HOME=C:\Program Files\Java\jdk-21.0.10"
set "PATH=%JAVA_HOME%\bin;%PATH%"
call "%~dp0.tools\apache-maven-3.10.0\bin\mvn.cmd" "-Dmaven.repo.local=%~dp0.tools\repository" clean compile exec:exec
if errorlevel 1 pause
endlocal