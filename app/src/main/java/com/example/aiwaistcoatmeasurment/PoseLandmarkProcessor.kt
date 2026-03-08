package com.example.aiwaistcoatmeasurment
import android.graphics.Bitmap
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.google.mediapipe.framework.image.BitmapImageBuilder
import kotlin.math.sqrt

class PoseLandmarkProcessor(
    private val poseLandmarker: PoseLandmarker,
    private val userHeightCm: Float = 172f
) {

    fun processImage(bitmap: Bitmap): BodyMeasurements? {
        val mpImage = BitmapImageBuilder(bitmap).build()
        val result = poseLandmarker.detect(mpImage)

        return if (result.landmarks().isNotEmpty()) {
            calculateMeasurements(result, bitmap.width, bitmap.height)
        } else {
            null
        }
    }

    private fun calculateMeasurements(
        result: PoseLandmarkerResult,
        imageWidth: Int,
        imageHeight: Int
    ): BodyMeasurements {
        val landmarks = result.landmarks()[0]

        // Get key landmarks (MediaPipe Pose has 33 landmarks)
        val leftShoulder = landmarks[11]
        val rightShoulder = landmarks[12]
        val leftHip = landmarks[23]
        val rightHip = landmarks[24]
        val nose = landmarks[0]
        val leftAnkle = landmarks[27]

        // Calculate pixel distances
        val shoulderWidthPx = distance(
            leftShoulder.x() * imageWidth,
            leftShoulder.y() * imageHeight,
            rightShoulder.x() * imageWidth,
            rightShoulder.y() * imageHeight
        )

        val hipWidthPx = distance(
            leftHip.x() * imageWidth,
            leftHip.y() * imageHeight,
            rightHip.x() * imageWidth,
            rightHip.y() * imageHeight
        )

        val torsoLengthPx = distance(
            (leftShoulder.x() + rightShoulder.x()) / 2 * imageWidth,
            (leftShoulder.y() + rightShoulder.y()) / 2 * imageHeight,
            (leftHip.x() + rightHip.x()) / 2 * imageWidth,
            (leftHip.y() + rightHip.y()) / 2 * imageHeight
        )

        // Calculate full body height in pixels (nose to ankle)
        val bodyHeightPx = distance(
            nose.x() * imageWidth,
            nose.y() * imageHeight,
            leftAnkle.x() * imageWidth,
            leftAnkle.y() * imageHeight
        )

        // Scale factor: cm per pixel
        val scaleFactor = userHeightCm / bodyHeightPx

        // Convert to cm
        val shoulderCm = shoulderWidthPx * scaleFactor
        val waistCm = hipWidthPx * scaleFactor * 1.8f // Hip to waist estimation
        val chestCm = shoulderWidthPx * scaleFactor * 2.1f // Shoulder to chest circumference
        val torsoLengthCm = torsoLengthPx * scaleFactor

        // Calculate body shape based on chest/waist ratio
        val chestWaistRatio = chestCm / waistCm
        val bodyShape = when {
            chestWaistRatio > 1.15 -> "Athletic"
            chestWaistRatio > 1.05 -> "Fit"
            else -> "Regular"
        }

        // Get confidence from visibility score
        val confidence = result.landmarks()[0].first().visibility().orElse(0f)

        return BodyMeasurements(
            chestCm = chestCm,
            waistCm = waistCm,
            shoulderCm = shoulderCm,
            torsoLengthCm = torsoLengthCm,
            bodyShape = bodyShape,
            confidence = confidence
        )
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1))
    }
}
