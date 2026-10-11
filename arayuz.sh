#!/bin/bash
# README düzenleyicinin arayüzünü açar; kaynak kod değiştiyse önce derler.
cd "$(dirname "$0")" || exit 1
source ./java_bul.sh || exit 1
JAR=target/marmara-readme-araci-0.1.0.jar
if [ ! -f "$JAR" ] || [ -n "$(find src pom.xml -newer "$JAR" -print -quit)" ]; then
    echo "Derleniyor..."
    ./mvnw -q -B -DskipTests package || exit 1
fi
exec java -jar "$JAR"
