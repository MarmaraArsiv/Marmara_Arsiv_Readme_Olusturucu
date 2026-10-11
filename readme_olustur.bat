@echo off
REM Arayuzsuz README uretimi.
REM   readme_olustur.bat <arsiv klasoru>            README'leri JSON'dan uretir
REM   readme_olustur.bat <arsiv klasoru> --kontrol  hicbir sey yazmaz, README'ler JSON ile uyumlu mu bakar
REM   (--altyapi-kontrol, --altyapi-guncelle ve --yeni-arsiv icin README.md'ye bakin)
set "ARSIV=%~f1"
cd /d "%~dp0"
call mvnw.cmd -q -B -DskipTests package
if errorlevel 1 (pause & exit /b 1)
java -cp target\marmara-readme-araci.jar edu.marmara.readme.cli.ReadmeOlusturCli "%ARSIV%" %2
pause
