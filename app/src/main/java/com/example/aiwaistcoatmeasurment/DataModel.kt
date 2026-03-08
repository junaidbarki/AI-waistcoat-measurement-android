package com.example.aiwaistcoatmeasurment
data class BodyMeasurements(
    val chestCm: Float,
    val waistCm: Float,
    val shoulderCm: Float,
    val torsoLengthCm: Float,
    val bodyShape: String,
    val confidence: Float
)

data class SizeRecommendation(
    val size: Int,
    val fitType: String,
    val confidence: String
)

data class WaistcoatProduct(
    val id: String,
    val name: String,
    val price: Double,
    val imageUrl: String,
    val recommendedSize: Int,
    val fitType: String
)
