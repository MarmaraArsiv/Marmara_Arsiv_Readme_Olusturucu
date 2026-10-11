@echo off
REM Arayuzsuz README uretimi.
REM   readme_olustur.bat [arsiv klasoru]            README'leri JSON'dan uretir
REM   readme_olustur.bat [arsiv klasoru] --kontrol  hicbir sey yazmaz, README'ler JSON ile uyumlu mu bakar
REM Arsiv klasoru verilmezse aracin yanindaki ..\Marmara_Bilgisayar_Muhendisligi_Arsiv kullanilir.
set "ARSIV=%~f1"
if "%~1"=="" set "ARSIV=%~dp0..\Marmara_Bilgisayar_Muhendisligi_Arsiv"
cd /d "%~dp0"
if not exist target\marmara-readme-araci-0.1.0.jar call mvnw.cmd -q -B -DskipTests package
if errorlevel 1 (pause & exit /b 1)
java -cp target\marmara-readme-araci-0.1.0.jar edu.marmara.readme.cli.ReadmeOlusturCli "%ARSIV%" %2
pause
