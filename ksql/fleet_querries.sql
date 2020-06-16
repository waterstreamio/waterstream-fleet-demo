CREATE TABLE fleet_table
(plate STRING,
current STRUCT<lat DOUBLE, lng DOUBLE>,
waypoint STRUCT<lat DOUBLE, lng DOUBLE>,
speed DOUBLE,
distance DOUBLE,
updateTimestamp BIGINT) WITH (KAFKA_TOPIC='waterstream_fleet_demo', VALUE_FORMAT='JSON', KEY='plate');

CREATE TABLE fleet_directions AS
    SELECT plate,
        CASE
            WHEN waypoint->lat >= current->lat AND waypoint->lng >= current->lng THEN 'NE'
            WHEN waypoint->lat >= current->lat AND waypoint->lng < current->lng THEN 'NW'
            WHEN waypoint->lat < current->lat AND waypoint->lng <= current->lng THEN 'SW'
            ELSE 'SE'
        END AS direction FROM fleet_table
    EMIT CHANGES;


CREATE TABLE directions_vehicles AS
    SELECT direction, COUNT(*) as vehicles_count FROM fleet_directions
    GROUP BY direction EMIT CHANGES;

CREATE STREAM directions_vehicles_stream
    (direction STRING, vehicles_count INTEGER)
    WITH (KAFKA_TOPIC='DIRECTIONS_VEHICLES', VALUE_FORMAT='JSON', KEY='direction');

CREATE STREAM directions_vehicles_mqtt
    AS SELECT direction,
        vehicles_count,
        'waterstream-fleet-demo/direction-stats/' + direction AS mqtt_topic
       FROM directions_vehicles_stream
    PARTITION BY mqtt_topic;

