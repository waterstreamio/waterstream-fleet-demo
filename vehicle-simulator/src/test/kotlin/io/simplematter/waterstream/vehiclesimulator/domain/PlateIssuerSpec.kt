package io.simplematter.waterstream.vehiclesimulator.domain

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

class PlateIssuerSpec : StringSpec() {
    init {
        "issue stable sequence" {
            val issuer1 = PlateIssuer(2020)
            val issuer2 = PlateIssuer(2020)

            val seq1 = (1..10).map { issuer1.issuePlate() }
            val seq2 = (1..10).map { issuer2.issuePlate() }

            seq1 shouldBe seq2
        }

        "issue unique plates" {
            val platesCount = 20000
            val plates: Set<String> = (1..platesCount).map { PlateIssuer.default.issuePlate() }.toSet()
            plates.size shouldBe platesCount
        }

        "re-issue returned plate" {
            val plates: Set<String> = (1..10).map { PlateIssuer.default.issuePlate() }.toSet()
            plates.size shouldBe 10
            val plate1 = plates.first()
            PlateIssuer.default.returnPlate(plate1)
            val plateN = PlateIssuer.default.issuePlate()
            plateN shouldBe plate1
        }
    }
}
