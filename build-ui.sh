#!/bin/sh
set -e

SCRIPT_DIR=`realpath $(dirname "$0")`

cd $SCRIPT_DIR/vehicle-simulator-ui/
./gradlew clean shadowJar
docker build . -t io.simplematter/fleet-ui


