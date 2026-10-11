#!/bin/bash
# Arayüzsüz README üretimi; kaynak kod değiştiyse önce derler.
#   ./readme_olustur.sh [arşiv klasörü]                    README'leri JSON'dan üretir, değişenleri yazar
#   ./readme_olustur.sh [arşiv klasörü] --kontrol          hiçbir şey yazmaz; README'ler JSON ile uyumlu mu bakar
#   ./readme_olustur.sh [arşiv klasörü] --altyapi-kontrol  GitHub betikleri aracın şablonuyla aynı mı bakar
#   ./readme_olustur.sh [arşiv klasörü] --altyapi-guncelle GitHub betiklerini şablonla eşitler
#   ./readme_olustur.sh --yeni-arsiv <klasör> --bolum ... --fakulte ... --github ...   yeni bölüm arşivi
# Arşiv klasörü verilmezse aracın yanındaki klasörlerde bulunan tek arşiv kullanılır.
ARAC="$(cd "$(dirname "$0")" && pwd)"
ARGS=()
if [ $# -gt 0 ] && [ "${1#--}" = "$1" ]; then
    ARGS+=("$(realpath "$1")")
    shift
fi
if [ "$1" = "--yeni-arsiv" ] && [ $# -gt 1 ]; then
    ARGS+=("$1" "$(realpath -m "$2")")
    shift 2
fi
cd "$ARAC" || exit 1
source ./java_bul.sh || exit 1
JAR=target/marmara-readme-araci.jar
if [ ! -f "$JAR" ] || [ -n "$(find src pom.xml -newer "$JAR" -print -quit)" ]; then
    echo "Derleniyor..."
    ./mvnw -q -B -DskipTests package || exit 1
fi
exec java -cp "$JAR" edu.marmara.readme.cli.ReadmeOlusturCli "${ARGS[@]}" "$@"
