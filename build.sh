#!/bin/bash
# Aracı derler: target/marmara-readme-araci-0.1.0.jar (arayüz ve komut satırı tek dosyada).
# Gereksinim: JDK 21. Maven kurmak gerekmez, mvnw kendisi indirir.
cd "$(dirname "$0")" || exit 1
source ./java_bul.sh || exit 1
./mvnw -q -B -DskipTests package && echo "Derlendi: target/marmara-readme-araci-0.1.0.jar"
