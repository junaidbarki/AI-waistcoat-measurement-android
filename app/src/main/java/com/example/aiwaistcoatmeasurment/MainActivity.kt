package com.example.aiwaistcoatmeasurment

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.aiwaistcoatmeasurment.ui.theme.AIWaistCoatMeasurmentTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {
    private lateinit var poseLandmarker: PoseLandmarker
    private lateinit var processor: PoseLandmarkProcessor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Initialize MediaPipe Pose Landmarker
        initializePoseLandmarker()

        // Initialize Processor
        processor = PoseLandmarkProcessor(
            poseLandmarker = poseLandmarker,
            userHeightCm = 172f // Default height, can be made dynamic
        )
        setContent {
            AIWaistCoatMeasurmentTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                ) {
                    BodyScannerApp(processor = processor)
                }
            }
        }
    }

    private fun initializePoseLandmarker() {
        try {
            // Copy model from assets to cache
            val modelFile = File(cacheDir, "pose_landmarker_full.task")
            if (!modelFile.exists()) {
                assets.open("pose_landmarker_full.task").use { input ->
                    FileOutputStream(modelFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }

            // Create PoseLandmarker options
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath(modelFile.absolutePath)
                .build()

            val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.IMAGE)
                .setNumPoses(1)
                .setMinPoseDetectionConfidence(0.5f)
                .setMinPosePresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .build()

            // Initialize PoseLandmarker
            poseLandmarker = PoseLandmarker.createFromOptions(this, options)

        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException("Failed to initialize PoseLandmarker: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::poseLandmarker.isInitialized) {
            poseLandmarker.close()
        }
    }
}


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BodyScannerApp(processor: PoseLandmarkProcessor) {
    val navController = rememberNavController()
    val viewModel: BodyScanViewModel = viewModel()

    val cameraPermissionState = rememberPermissionState(
        android.Manifest.permission.CAMERA
    )

    if (!cameraPermissionState.status.isGranted) {
        // Request Camera Permission
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "📸 Camera Permission Required",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                text = "We need camera access to scan your body measurements for accurate size recommendations.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                Text("Grant Camera Permission")
            }
        }
    } else {
        // Navigation
        BodyScannerNavigation(
            navController = navController,
            viewModel = viewModel,
            processor = processor
        )
    }
}
@Composable
fun BodyScannerNavigation(
    navController: NavHostController,
    viewModel: BodyScanViewModel,
    processor: PoseLandmarkProcessor
) {
    val scanState by viewModel.scanState.collectAsState()
    val products by viewModel.products.collectAsState()

    // Store measurements and recommendation
    var currentMeasurements by remember { mutableStateOf<BodyMeasurements?>(null) }
    var currentRecommendation by remember { mutableStateOf<SizeRecommendation?>(null) }
    var selectedProduct by remember { mutableStateOf<WaistcoatProduct?>(null) }

    // Update when scan succeeds
    LaunchedEffect(scanState) {
        if (scanState is ScanState.Success) {
            val success = scanState as ScanState.Success
            currentMeasurements = success.measurements
            currentRecommendation = success.recommendation
        }
    }

    NavHost(
        navController = navController,
        startDestination = "scanner"
    ) {
        // Scanner Screen
        composable("scanner") {
            ScannerScreen(
                viewModel = viewModel,
                processor = processor,
                onScanComplete = {
                    navController.navigate("results") {
                        popUpTo("scanner") { inclusive = false }
                    }
                }
            )
        }

        // Results Screen
        composable("results") {
            currentMeasurements?.let { measurements ->
                currentRecommendation?.let { recommendation ->
                    ResultsScreen(
                        measurements = measurements,
                        recommendation = recommendation,
                        onContinue = {
                            navController.navigate("products")
                        }
                    )
                }
            }
        }

        // Products List Screen
        composable("products") {
            ProductsScreen(
                products = products,
                onProductClick = { product ->
                    selectedProduct = product
                    navController.navigate("product_detail")
                }
            )
        }

        // Product Detail Screen
        composable("product_detail") {
            selectedProduct?.let { product ->
                currentMeasurements?.let { measurements ->
                    currentRecommendation?.let { recommendation ->
                        ProductDetailScreen(
                            product = product,
                            measurements = measurements,
                            recommendation = recommendation,
                            onAddToCart = {
                                navController.navigate("cart")
                            }
                        )
                    }
                }
            }
        }

        // Cart Screen
        composable("cart") {
            CartScreen(
                product = selectedProduct,
                onCheckout = {
                    // Handle checkout logic
                }
            )
        }
    }
}
