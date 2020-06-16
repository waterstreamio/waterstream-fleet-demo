#!/bin/sh
set -e

SCRIPT_DIR=`realpath $(dirname "$0")`

cd $SCRIPT_DIR/../openrouteservice
git pull
docker build . -t io.simplematter/openrouteservice

