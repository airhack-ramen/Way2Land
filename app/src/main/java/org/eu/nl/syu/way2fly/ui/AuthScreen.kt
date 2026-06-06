package org.eu.nl.syu.way2fly.ui

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import org.eu.nl.syu.way2fly.model.BoardingPassData
import org.eu.nl.syu.way2fly.util.BCBPParser

@Composable
fun AuthScreen(
    onAuthenticated: (BoardingPassData) -> Unit
) {
    var phoneNumber by remember { mutableStateOf("") }
    var scannedData by remember { mutableStateOf<BoardingPassData?>(null) }
    var scanError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Welcome to Way2Fly", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        if (scannedData == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                CameraPreview(onBarcodeScanned = { barcode ->
                    val data = BCBPParser.parse(barcode)
                    if (data != null) {
                        scannedData = data
                        scanError = null
                    } else {
                        scanError = "Invalid boarding pass format"
                    }
                })
                
                // Overlay for scanner
                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = scanError ?: "Scan your boarding pass",
                        modifier = Modifier.padding(8.dp),
                        color = if (scanError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(onClick = {
                // Simulate scan for testing
                val testRaw = "M1DOE/JOHN            EABCDEFJFKLAXDL 00123123Y001A00001 1"
                scannedData = BCBPParser.parse(testRaw)
            }) {
                Text("Simulate Scan (Test)")
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Boarding Pass Scanned!", style = MaterialTheme.typography.titleMedium)
                    Text("Passenger: ${scannedData?.passengerName}")
                    Text("Flight: ${scannedData?.carrier}${scannedData?.flightNumber}")
                    Text("Seat: ${scannedData?.seat}")
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { scannedData = null }) {
                        Text("Rescan")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = { Text("Phone Number") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                scannedData?.let {
                    onAuthenticated(it.copy(phoneNumber = phoneNumber))
                }
            },
            enabled = scannedData != null && phoneNumber.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Enter App")
        }
    }
}

@Composable
fun CameraPreview(onBarcodeScanned: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val executor = ContextCompat.getMainExecutor(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val scanner = BarcodeScanning.getClient()
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(executor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                for (barcode in barcodes) {
                                    barcode.rawValue?.let { onBarcodeScanned(it) }
                                }
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    } else {
                        imageProxy.close()
                    }
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (exc: Exception) {
                    // Handle errors
                }
            }, executor)
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}
