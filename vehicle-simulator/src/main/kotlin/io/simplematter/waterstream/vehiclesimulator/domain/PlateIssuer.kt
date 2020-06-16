package io.simplematter.waterstream.vehiclesimulator.domain

import java.lang.RuntimeException
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ConcurrentSkipListSet
import kotlin.random.Random
import org.slf4j.LoggerFactory

class PlateIssuer(seed: Long) {
    private val log = LoggerFactory.getLogger(PlateIssuer::class.java)

    private val generatedPlates = ConcurrentSkipListSet<String>()
    private val returnedPlates = ConcurrentLinkedQueue<String>()

    private val rng = Random(seed)

    private fun generatePlate(remainingAttempts: Int): String {
        val begin: String = listOf(1, 2).map { CharRange('A', 'F').random(rng) }.joinToString("")
        val end: String = listOf(1, 2).map { CharRange('A', 'Z').random(rng) }.joinToString("")
        val middle: String = listOf(1, 2, 3).map { IntRange(0, 9).random(rng) }.joinToString("")
        val plate = "$begin$middle$end"
        if (generatedPlates.add(plate))
            return plate
        else if (remainingAttempts > 0)
            return generatePlate(remainingAttempts - 1)
        else
            throw RuntimeException("Unable to generate a plate")
    }

    fun issuePlate(): String {
        val plateFromReturned = returnedPlates.poll()
        if (plateFromReturned == null) {
            val newPlate = generatePlate(1000)
            log.debug("Issued newly generated plate {}", newPlate)
            return newPlate
        } else {
            log.debug("Issued reused plate {}", plateFromReturned)
            return plateFromReturned
        }
    }

    fun returnPlate(plate: String) {
        returnedPlates.add(plate)
        log.info("Plate {} returned", plate)
    }

    companion object {
        val default = PlateIssuer(20200331)
    }
}
