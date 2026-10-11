@echo off
REM Arayuzsuz README uretimi (komut isteminden):
REM   readme_olustur.bat <arsiv klasoru>                    README'leri JSON'dan uretir
REM   readme_olustur.bat <arsiv klasoru> --kontrol          hicbir sey yazmaz, README'ler JSON ile uyumlu mu bakar
REM   readme_olustur.bat <arsiv klasoru> --altyapi-kontrol  GitHub betikleri aracin sablonuyla ayni mi bakar
REM   readme_olustur.bat <arsiv klasoru> --altyapi-guncelle GitHub betiklerini sablonla esitler
REM Yeni bolum arsivi icin arayuzdeki "Yeni Bolum Arsivi Olustur" dugmesini kullanin.
set "ARSIV=%~f1"
cd /d "%~dp0"
call java_bul.bat
if errorlevel 1 (pause & exit /b 1)
call mvnw.cmd -q -B -DskipTests package
if errorlevel 1 (pause & exit /b 1)
java -cp target\marmara-readme-araci.jar edu.marmara.readme.cli.ReadmeOlusturCli "%ARSIV%" %2
pause
