#!/bin/sh

set -e

SCRIPTDIR=`realpath $(dirname "$0")`

cd $SCRIPTDIR

curl https://download.geofabrik.de/europe/italy-latest.osm.pbf -o volumes/ors/data/italy-latest.osm.pbf

