#!/bin/bash
# Arayüzsüz README üretimi; kaynak kod değiştiyse önce derler.
#   ./readme_olustur.sh [arşiv klasörü]             README'leri JSON'dan üretir, değişenleri yazar
#   ./readme_olustur.sh [arşiv klasörü] --kontrol   hiçbir şey yazmaz; README'ler JSON ile uyumlu mu bakar
# Arşiv klasörü verilmezse aracın yanındaki ../Marmara_Bilgisayar_Muhendisligi_Arsiv kullanılır.
ARAC="$(cd "$(dirname "$0")" && pwd)"
if [ $# -gt 0 ] && [ "${1#--}" = "$1" ]; then
    ARSIV="$(realpath "$1")"
    shift
else
    ARSIV="$ARAC/../Marmara_Bilgisayar_Muhendisligi_Arsiv"
fi
cd "$ARAC" || exit 1
source ./java_bul.sh || exit 1
JAR=target/marmara-readme-araci-0.1.0.jar
if [ ! -f "$JAR" ] || [ -n "$(find src pom.xml -newer "$JAR" -print -quit)" ]; then
    echo "Derleniyor..."
    ./mvnw -q -B -DskipTests package || exit 1
fi
exec java -cp "$JAR" edu.marmara.readme.cli.ReadmeOlusturCli "$ARSIV" "$@"
