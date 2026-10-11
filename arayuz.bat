@echo off
REM README duzenleyicinin arayuzunu acar. Kod guncellendiyse once build.bat calistirin.
cd /d "%~dp0"
if not exist target\marmara-readme-araci-0.1.0.jar call mvnw.cmd -q -B -DskipTests package
if errorlevel 1 (pause & exit /b 1)
java -jar target\marmara-readme-araci-0.1.0.jar
