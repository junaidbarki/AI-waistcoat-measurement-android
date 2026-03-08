package com.example.aiwaistcoatmeasurment
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ScanState {
    object Idle : ScanState()
    object Capturing : ScanState()
    object Processing : ScanState()
    data class Success(
        val measurements: BodyMeasurements,
        val recommendation: SizeRecommendation
    ) : ScanState()
    data class Error(val message: String) : ScanState()
}

class BodyScanViewModel : ViewModel() {

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState

    private val _products = MutableStateFlow<List<WaistcoatProduct>>(emptyList())
    val products: StateFlow<List<WaistcoatProduct>> = _products

    private val sizeEngine = SizeRecommendationEngine()

    fun processCapturedImage(bitmap: Bitmap, processor: PoseLandmarkProcessor) {
        viewModelScope.launch {
            _scanState.value = ScanState.Processing

            try {
                val measurements = processor.processImage(bitmap)

                if (measurements != null) {
                    val recommendation = sizeEngine.recommendSize(measurements)
                    _scanState.value = ScanState.Success(measurements, recommendation)
                    generateProducts(recommendation)
                } else {
                    _scanState.value = ScanState.Error("No body detected. Please ensure full body is visible.")
                }
            } catch (e: Exception) {
                _scanState.value = ScanState.Error("Processing failed: ${e.message}")
            }
        }
    }

    private fun generateProducts(recommendation: SizeRecommendation) {
        _products.value = listOf(
            WaistcoatProduct(
                "1",
                "Classic Black Waistcoat",
                49.99,
                "",
                recommendation.size,
                recommendation.fitType
            ),
            WaistcoatProduct(
                "2",
                "Navy Blue Premium",
                59.99,
                "",
                recommendation.size,
                recommendation.fitType
            ),
            WaistcoatProduct(
                "3",
                "Charcoal Grey Formal",
                54.99,
                "",
                recommendation.size,
                recommendation.fitType
            )
        )
    }

    fun resetScan() {
        _scanState.value = ScanState.Idle
    }
}
