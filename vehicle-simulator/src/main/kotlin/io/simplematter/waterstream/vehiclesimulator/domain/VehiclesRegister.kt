package io.simplematter.waterstream.vehiclesimulator.domain

import io.simplematter.waterstream.vehiclesimulator.monitoring.VehicleSimCounters
import java.util.concurrent.locks.ReentrantLock

class VehiclesRegister(private val visibleVehicles: Int) {

  fun <T> ReentrantLock.withLock(lambda: () -> T): T {
    this.lock();  // block until condition holds
    return try {
      lambda.invoke()
    } finally {
      this.unlock()
    }
  }

  private val mutex = ReentrantLock()

  private val registry: MutableMap<String, Vehicle> = mutableMapOf()

  fun add(vehicle: Vehicle, isNew: Boolean) {
    mutex.withLock {
      val prev = registry.put(vehicle.id, vehicle)
      if (prev != null && isNew)
        VehicleSimCounters.vehiclesRegistryDuplicateMapEntries.inc()
      if (prev == null && !isNew)
        VehicleSimCounters.vehiclesRegistryUnexpectedInserts.inc()
      VehicleSimCounters.vehiclesRegistrySize.set(registry.size.toDouble())
      if (isNew) {
        VehicleSimCounters.vehiclesRegistryInserts.inc()
      } else {
        VehicleSimCounters.vehiclesRegistryUpdates.inc()
      }
    }
  }


  fun remove(vehicle: Vehicle) {
    mutex.withLock {
      val prev = registry.remove(vehicle.id)
      if (prev == null)
        VehicleSimCounters.vehiclesRegistryNonexistingDeletes.inc()
      else
        VehicleSimCounters.vehiclesRegistryDeletes.inc()
      VehicleSimCounters.vehiclesRegistrySize.set(registry.size.toDouble())
    }
  }

  fun visibleVehicles(): List<Vehicle> {
    return registry.values.filter { it.visible }
  }

  fun size(): Int = registry.size
}
