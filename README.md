Waterstream Fleet Demo
======================

Demo project for MQTT + Kafka integration. See https://fleetdemo.waterstream.io for 
this demo running with [Waterstream](https://waterstream.io) - Kafka-native MQTT broker.

Trucks are moving along the randomly assigned routes, reporting their location and nearest waypoint
to the MQTT broker. Small subset of the fleet is displayed on the map.  
As the data ends up in Kafka [ksqlDB](https://ksqldb.io/) is used to build some aggregates - 
in this example, a statistics of the vehicle moving directions. The results of aggregations
are retrieved back through MQTT (remember - Waterstream is a Kafka-native MQTT broker)
directly into UI using MQTT WebSocket transport.

## Pre-requisites

MQTT broker running with Kafka integration on (for example, https://waterstream.io),
must be accessible from `mqttd-demo` network.

Here are the mappings between Kafka and MQTT topic required for this demo to work:

| Kafka topic                   | MQTT topic pattern                               | Direction |
|-------------------------------|--------------------------------------------------|-----------|
|waterstream_fleet_demo_visible | waterstream-fleet-demo/visible_vehicle_updates/# | M <-> K   |
|DIRECTIONS_VEHICLES_MQTT       | waterstream-fleet-demo/direction-stats/#         | K -> M    |
|waterstream_fleet_demo         | waterstream-fleet-demo/#                         | M -> K    |

To properly protect the demo writing to all MQTT topics should be restricted to the authenticated
users. Reading from `waterstream-fleet-demo/visible_vehicle_updates` and 
`waterstream-fleet-demo/direction-stats/#` MQTT topics should be allowed for anonymous users. 

## Open Route Service

Get the source code from https://github.com/GIScience/openrouteservice/ into the sibling directory:

    cd ..
    git checkout git@github.com:GIScience/openrouteservice.git

Back to this project, create data directory:

    cd waterstream-fleet-demo
    mkdir -p volumes/ors/data/
    
Download `italy-latest.osm.pbf` into the data directory from https://download.geofabrik.de/europe/italy.html:

    ./download-data.sh
    
## Configure

Copy `.env.example` to `.env`, specify username and password which `vehiclesimulator` will use
to connect to MQTT broker. 

## Run

    ./build-route-service.sh
    ./build-simulator.sh
    docker-compose up -d
    
Keep in mind that `openrouteservice` may take a few hours to build the routes, during this time 
the requests for retrieving the route will be failing.

Open http://your.host:8082 in your browser to see the UI with the map over which the vehicles 
are moving.
    
## Stop 

    docker-compose down


