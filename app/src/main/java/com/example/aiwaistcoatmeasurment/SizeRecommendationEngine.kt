package com.example.aiwaistcoatmeasurment

class SizeRecommendationEngine {

    fun recommendSize(measurements: BodyMeasurements): SizeRecommendation {
        val chestCm = measurements.chestCm

        // Standard waistcoat sizing chart
        val size = when {
            chestCm < 90 -> 36
            chestCm < 95 -> 38
            chestCm < 100 -> 40
            chestCm < 105 -> 42
            chestCm < 110 -> 44
            else -> 46
        }

        // Determine fit type based on body shape
        val fitType = when (measurements.bodyShape) {
            "Athletic" -> "Slim Fit"
            "Fit" -> "Slim Fit"
            else -> "Regular Fit"
        }

        // Confidence level based on detection quality
        val confidence = if (measurements.confidence > 0.8f) "High" else "Medium"

        return SizeRecommendation(size, fitType, confidence)
    }
}