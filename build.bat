@echo off
REM Araci derler: target\marmara-readme-araci.jar (arayuz ve komut satiri tek dosyada).
REM Gereksinim: JDK 21 (JAVA_HOME ya da PATH'te). Maven kurmak gerekmez, mvnw.cmd kendisi indirir.
cd /d "%~dp0"
call mvnw.cmd -q -B -DskipTests package
if errorlevel 1 (pause & exit /b 1)
echo Derlendi: target\marmara-readme-araci.jar
pause
