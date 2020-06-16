package io.simplematter.waterstream.vehiclesimulator.monitoring

import io.prometheus.client.Counter
import io.prometheus.client.Gauge

object VehicleSimCounters {

    val mqttMessagesSent = Counter.build("vehicle_simulator_mqtt_messages_sent", "Number of PUBLISH messages sent to MQTT broker").register()

    val mqttCurrentConections = Gauge.build("vehicle_simulator_mqtt_connect_current", "Number of current MQTT broker connections").register()
    val mqttConnectAttempts = Counter.build("vehicle_simulator_mqtt_connect_attempts", "Number of attempts to connect to MQTT broker").register()
    val mqttConnectSuccess = Counter.build("vehicle_simulator_mqtt_connect_success", "Number of successful MQTT broker connect attempts").register()
    val mqttConnectFailed = Counter.build("vehicle_simulator_mqtt_connect_failed", "Number of failed MQTT broker connect attempts").register()
    val mqttConnectionsClosed = Counter.build("vehicle_simulator_mqtt_connections_closed", "Number of closed MQTT broker connections").register()
    val mqttDisconnectSent = Counter.build("vehicle_simulator_mqtt_disconnect_sent", "Number of DISCONNECT messages sent").register()

    val vehiclesCurrent = Gauge.build("vehicle_simulator_vehicles_current", "Number of current vehicles").register()
    val vehiclesCreated = Counter.build("vehicle_simulator_vehicles_created", "Number of vehicles created").register()
    val vehiclesRemoved = Counter.build("vehicle_simulator_vehicles_removed", "Number of vehicles removed").register()
    val vehiclesUndeployRequested = Counter.build("vehicle_simulator_vehicles_undeploy_requested", "Number of vehicle undeploy requests").register()
    val vehiclesUnsolicitedRemovals = Counter.build("vehicle_simulator_vehicles_unsolicited_removals", "Number of vehicles removed without undeploy request").register()
    val vehiclesRegistryInserts = Counter.build("vehicle_simulator_vehicles_registry_inserts", "Number of vehicle registry inserts").register()
    val vehiclesRegistryUpdates = Counter.build("vehicle_simulator_vehicles_registry_updates", "Number of vehicle registry updates").register()
    val vehiclesRegistryDeletes = Counter.build("vehicle_simulator_vehicles_registry_deletes", "Number of vehicle registry deletes").register()
    val vehiclesRegistryNonexistingDeletes = Counter.build("vehicle_simulator_vehicles_registry_nonexisting_deletes", "Number of attempts to delete non-existing vehicle from the registry").register()
    val vehiclesRegistrySize = Gauge.build("vehicle_simulator_vehicles_registry_size", "Number of vehicles in registry").register()
    val vehiclesRegistryDuplicateMapEntries = Counter.build("vehicle_simulator_vehicles_registry_duplicate_map_entries", "Number of vehicles inserts in registry with duplicate keys (deployment IDs)").register()
    val vehiclesRegistryUnexpectedInserts = Counter.build("vehicle_simulator_vehicles_registry_unexpected_inserts", "Number of vehicles inserts in registry where update was actually expected").register()

    val vehicleInitErrors = Counter.build("vehicle_simulator_vehicle_init_errors", "Number of vehicle initialization errors").register()
    val vehicleUpdates = Counter.build("vehicle_simulator_vehicle_updates", "Number of vehicle update events").register()
    val vehicleArriveEvents = Counter.build("vehicle_simulator_vehicle_arrive_events", "Number of events when vehicle has arrived").register()
    val vehicleNoRoutesEvents = Counter.build("vehicle_simulator_vehicle_no_routes_events", "Number of events when vehicle has no available routes").register()

    val routeRequests = Counter.build("vehicle_simulator_route_requests", "Number of requests to route service").register()

    val kafkaRecordsRead = Counter.build("vehicle_simulator_kafka_records_read", "Number of records read from Kafka").register()
}
