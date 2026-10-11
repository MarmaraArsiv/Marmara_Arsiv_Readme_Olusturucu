# JDK 21 ya da üstünü bulur, JAVA_HOME ve PATH'i ona göre ayarlar.
# Diğer betikler "source ./java_bul.sh" ile kullanır; doğrudan çalıştırılmaz.
# Bulamazsa: Ubuntu'da "sudo apt install openjdk-21-jdk" ya da JAVA_HOME'u bir JDK 21 klasörüne ayarlayın.

java_surumu() {
    "$1/bin/java" -version 2>&1 | head -1 | sed -E 's/.*version "([0-9]+).*/\1/'
}

PATHTEKI_JAVA="$(command -v java 2>/dev/null)"
for aday in "$JAVA_HOME" /usr/lib/jvm/java-2[1-9]-* /usr/lib/jvm/*-2[1-9]-* \
        "${PATHTEKI_JAVA:+$(dirname "$(dirname "$(readlink -f "$PATHTEKI_JAVA")")")}"; do
    if [ -n "$aday" ] && [ -x "$aday/bin/javac" ] && [ "$(java_surumu "$aday")" -ge 21 ] 2>/dev/null; then
        export JAVA_HOME="$aday"
        export PATH="$aday/bin:$PATH"
        return 0
    fi
done
echo "JDK 21 bulunamadı. Ubuntu'da: sudo apt install openjdk-21-jdk" >&2
echo "Başka bir yerde kuruluysa: export JAVA_HOME=/jdk21/klasörü" >&2
return 1
