@echo off
REM JDK 21 ya da ustunu bulur, JAVA_HOME ve PATH'i ona gore ayarlar.
REM Diger .bat dosyalari "call java_bul.bat" ile kullanir; dogrudan calistirilmaz.
REM Sirayla bakilan yerler: JAVA_HOME, PATH'teki javac, Program Files altindaki bilinen JDK klasorleri.

if defined JAVA_HOME (
    call :dene "%JAVA_HOME%"
    if not errorlevel 1 goto :bulundu
)
for /f "delims=" %%j in ('where javac 2^>nul') do (
    call :dene "%%~dpj.."
    if not errorlevel 1 goto :bulundu
)
REM ProgramW6432: 32 bitlik bir komut isteminde de 64 bitlik "Program Files" klasoru.
for %%p in ("%ProgramW6432%" "%ProgramFiles%") do for %%k in ("Eclipse Adoptium" "Java" "Microsoft" "Zulu") do (
    for /f "delims=" %%d in ('dir /b /ad "%%~p\%%~k" 2^>nul') do (
        call :dene "%%~p\%%~k\%%d"
        if not errorlevel 1 goto :bulundu
    )
)

echo.
echo  ============================================================
echo   JDK 21 bulunamadi.
echo.
echo   1. https://adoptium.net adresinden "Temurin 21 (LTS)" indir
echo      (Windows, x64, .msi).
echo   2. Kurulumda "Set JAVA_HOME variable" secenegini ac.
echo   3. Bu pencereyi kapatip arayuz.bat'a tekrar cift tikla.
echo  ============================================================
echo.
exit /b 1

:bulundu
set "PATH=%JAVA_HOME%\bin;%PATH%"
exit /b 0

REM Verilen klasor JDK 21+ ise JAVA_HOME'u ona ayarlar (basari: errorlevel 0).
:dene
if not exist "%~1\bin\javac.exe" exit /b 1
REM "java -version"un ilk satiri: openjdk version "21.0.4" ... (ya da java version "1.8.0_401" ...)
set "JB_SURUM="
for /f "tokens=3" %%v in ('call "%~1\bin\java.exe" -version 2^>^&1') do if not defined JB_SURUM set "JB_SURUM=%%~v"
if not defined JB_SURUM exit /b 1
set "JB_ANA="
for /f "delims=." %%m in ("%JB_SURUM%") do set "JB_ANA=%%m"
REM Sayi degilse 0 olur; karsilastirma metin olarak yapilmasin.
set /a JB_ANA_SAYI=JB_ANA 2>nul
if %JB_ANA_SAYI% LSS 21 exit /b 1
for %%h in ("%~1") do set "JAVA_HOME=%%~fh"
exit /b 0
