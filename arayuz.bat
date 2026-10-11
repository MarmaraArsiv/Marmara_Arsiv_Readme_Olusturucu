@echo off
REM README duzenleyicinin arayuzunu acar; her acilista derler (kod guncellendiyse yeni hali calissin).
cd /d "%~dp0"
call mvnw.cmd -q -B -DskipTests package
if errorlevel 1 (pause & exit /b 1)
java -jar target\marmara-readme-araci.jar
