#!/bin/zsh
set -e
cd "$(dirname "$0")"
mvn package -DskipTests -q
java -jar target/hirelens-1.0.0.jar
