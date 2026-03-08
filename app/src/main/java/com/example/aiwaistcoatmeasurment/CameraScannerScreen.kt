package com.example.aiwaistcoatmeasurment

import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun ScannerScreen(
    viewModel: BodyScanViewModel,
    processor: PoseLandmarkProcessor,
    onScanComplete: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scanState by viewModel.scanState.collectAsState()

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var useFrontCamera by remember { mutableStateOf(false) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    // Recompose when camera switch changes
    LaunchedEffect(useFrontCamera) {
        previewView?.let { preview ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            val cameraProvider = cameraProviderFuture.get()

            val previewUseCase = Preview.Builder().build().also {
                it.setSurfaceProvider(preview.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = if (useFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    previewUseCase,
                    imageCapture
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    previewView = this
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Visual Guide Overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val centerX = canvasWidth / 2

            // Draw body outline guide
            drawCircle(
                color = Color.White.copy(alpha = 0.5f),
                radius = 40f,
                center = androidx.compose.ui.geometry.Offset(centerX, canvasHeight * 0.15f),
                style = Stroke(width = 3f)
            )

            // Shoulders line
            drawLine(
                color = Color.White.copy(alpha = 0.5f),
                start = androidx.compose.ui.geometry.Offset(centerX - 150f, canvasHeight * 0.3f),
                end = androidx.compose.ui.geometry.Offset(centerX + 150f, canvasHeight * 0.3f),
                strokeWidth = 3f
            )

            // Torso rectangle
            drawRect(
                color = Color.White.copy(alpha = 0.3f),
                topLeft = androidx.compose.ui.geometry.Offset(centerX - 100f, canvasHeight * 0.3f),
                size = androidx.compose.ui.geometry.Size(200f, canvasHeight * 0.35f),
                style = Stroke(width = 3f)
            )
        }

        // Overlay Instructions
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (useFrontCamera) "👤 Front Camera" else "📸 Back Camera (Recommended)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Stand 6-8 feet away. Full body visible.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Camera Switch Button
        FloatingActionButton(
            onClick = { useFrontCamera = !useFrontCamera },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Text("🔄", style = MaterialTheme.typography.titleLarge)
        }

        // Status Display
        when (scanState) {
            is ScanState.Capturing -> {
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = "Capturing...",
                        modifier = Modifier.padding(24.dp),
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }
            is ScanState.Processing -> {
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Processing your scan...",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
            is ScanState.Success -> {
                LaunchedEffect(Unit) {
                    onScanComplete()
                }
            }
            is ScanState.Error -> {
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = (scanState as ScanState.Error).message,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.resetScan() }) {
                            Text("Try Again")
                        }
                    }
                }
            }
            else -> {}
        }

        // Capture Button
        if (scanState is ScanState.Idle) {
            FloatingActionButton(
                onClick = {
                    imageCapture?.let { capture ->
                        capture.takePicture(
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(image: ImageProxy) {
                                    val bitmap = image.toBitmap()
                                    viewModel.processCapturedImage(bitmap, processor)
                                    image.close()
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    exception.printStackTrace()
                                }
                            }
                        )
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp)
                    .size(72.dp)
            ) {
                Text("📸", style = MaterialTheme.typography.headlineMedium)
            }
        }
    }
}

// Helper extension to convert ImageProxy to Bitmap
fun ImageProxy.toBitmap(): Bitmap {
    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}

@Composable
fun ResultsScreen(
    measurements: BodyMeasurements,
    recommendation: SizeRecommendation,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize().verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Your AI Scan Results",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Debug Info Card - Shows detection quality
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    measurements.confidence > 0.8f -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                    measurements.confidence > 0.6f -> Color(0xFFFFC107).copy(alpha = 0.2f)
                    else -> Color(0xFFF44336).copy(alpha = 0.2f)
                }
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("🔍 Detection Quality", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Confidence Score:", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "${(measurements.confidence * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyLarge,
                        color = when {
                            measurements.confidence > 0.8f -> Color(0xFF4CAF50)
                            measurements.confidence > 0.6f -> Color(0xFFFFC107)
                            else -> Color(0xFFF44336)
                        }
                    )
                }
                Text(
                    text = when {
                        measurements.confidence > 0.8f -> "✓ Excellent detection quality"
                        measurements.confidence > 0.6f -> "⚠ Good, but could be better"
                        else -> "❌ Poor quality - retake recommended"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Measurements Card with Validation
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("📏 Body Measurements", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(12.dp))
                MeasurementRowWithValidation("Chest", measurements.chestCm, 85f..130f)
                MeasurementRowWithValidation("Waist", measurements.waistCm, 70f..120f)
                MeasurementRowWithValidation("Shoulder", measurements.shoulderCm, 38f..60f)
                MeasurementRowWithValidation("Torso", measurements.torsoLengthCm, 45f..75f)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Body Shape Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("🎯 Body Shape Analysis", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = measurements.bodyShape,
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Chest/Waist Ratio: ${
                        String.format(
                            "%.2f",
                            measurements.chestCm / measurements.waistCm
                        )
                    }",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Recommendation Card with Logic Explanation
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("🧥 Size Recommendation", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Size ${recommendation.size}",
                    style = MaterialTheme.typography.displaySmall
                )
                Text(
                    text = recommendation.fitType,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "AI Confidence: ${recommendation.confidence}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                // Explain the logic
                Text(
                    text = "Why this size?",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                )
                Text(
                    text = getSizeExplanation(measurements.chestCm, recommendation.size),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = measurements.confidence > 0.5f
        ) {
            Text("View Recommended Products", style = MaterialTheme.typography.titleMedium)
        }

        if (measurements.confidence <= 0.5f) {
            Text(
                text = "Low confidence - consider retaking photo",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun MeasurementRowWithValidation(
    label: String,
    value: Float,
    expectedRange: ClosedFloatingPointRange<Float>
) {
    val isValid = value in expectedRange
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Row {
            Text(
                text = "${value.toInt()} cm",
                style = MaterialTheme.typography.bodyLarge,
                color = if (isValid) MaterialTheme.colorScheme.primary else Color(0xFFF44336)
            )
            Text(
                text = if (isValid) " ✓" else " ⚠",
                style = MaterialTheme.typography.bodyLarge,
                color = if (isValid) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
        }
    }
    if (!isValid) {
        Text(
            text = "Expected: ${expectedRange.start.toInt()}-${expectedRange.endInclusive.toInt()} cm",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFF44336).copy(alpha = 0.7f),
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

fun getSizeExplanation(chestCm: Float, size: Int): String {
    return when {
        chestCm < 90 -> "Chest ${chestCm.toInt()}cm suggests size 36 for slim fit"
        chestCm < 95 -> "Chest ${chestCm.toInt()}cm typically fits size 38"
        chestCm < 100 -> "Chest ${chestCm.toInt()}cm indicates size 40 is ideal"
        chestCm < 105 -> "Chest ${chestCm.toInt()}cm corresponds to size 42"
        chestCm < 110 -> "Chest ${chestCm.toInt()}cm matches size 44"
        else -> "Chest ${chestCm.toInt()}cm requires size 46 or larger"
    }
}

// ============================================================================
// 7. PRODUCTS SCREEN
// ============================================================================

@Composable
fun ProductsScreen(
    products: List<WaistcoatProduct>,
    onProductClick: (WaistcoatProduct) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Recommended Waistcoats",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        products.forEach { product ->
            ProductCard(product, onClick = { onProductClick(product) })
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun ProductCard(product: WaistcoatProduct, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${product.price}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = "Recommended Size: ${product.recommendedSize} (${product.fitType})",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

// ============================================================================
// 8. PRODUCT DETAIL SCREEN
// ============================================================================

@Composable
fun ProductDetailScreen(
    product: WaistcoatProduct,
    measurements: BodyMeasurements,
    recommendation: SizeRecommendation,
    onAddToCart: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = product.name,
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        // AI Measurements
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("📏 Your AI Measurements", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(12.dp))
                MeasurementRow("Chest", "${measurements.chestCm.toInt()} cm")
                MeasurementRow("Waist", "${measurements.waistCm.toInt()} cm")
                MeasurementRow("Shoulder", "${measurements.shoulderCm.toInt()} cm")
                MeasurementRow("Torso", "${measurements.torsoLengthCm.toInt()} cm")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // AI Recommendation
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("🎯 AI Recommendation", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Your Best Size: ${recommendation.size}",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Fit Type: ${recommendation.fitType}",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Add to Cart Button
        Button(
            onClick = onAddToCart,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                "Add to Cart — Size ${product.recommendedSize}",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun MeasurementRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

// ============================================================================
// 9. CART SCREEN
// ============================================================================

@Composable
fun CartScreen(
    product: WaistcoatProduct?,
    onCheckout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "🛒 Your Cart",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        product?.let {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = it.name,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Size: ${it.recommendedSize}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Fit: ${it.fitType}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "✓ AI Verified",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "${it.price}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onCheckout,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Proceed to Checkout", style = MaterialTheme.typography.titleMedium)
        }
    }
}
