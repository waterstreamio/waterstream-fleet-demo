#!/bin/sh
set -e

SCRIPT_DIR=`realpath $(dirname "$0")`

cd $SCRIPT_DIR/vehicle-simulator/
./gradlew clean shadowJar
docker build . -t io.simplematter/fleet-service

cd ..

./build-ui.sh
