@echo off
REM README duzenleyicinin arayuzunu acar. Cift tiklamak yeterli.
REM Her acilista kodu derler (guncellendiyse yeni hali calissin). Ilk acilis internetten
REM Maven'i ve kutuphaneleri indirdigi icin birkac dakika surebilir.
cd /d "%~dp0"
call java_bul.bat
if errorlevel 1 (pause & exit /b 1)
echo Derleniyor, lutfen bekleyin...
call mvnw.cmd -q -B -DskipTests package
if errorlevel 1 (echo. & echo Derleme basarisiz, yukaridaki hataya bakin. & pause & exit /b 1)
echo Arayuz aciliyor. Bu pencereyi kapatirsan arayuz de kapanir.
java -jar target\marmara-readme-araci.jar
if errorlevel 1 pause
