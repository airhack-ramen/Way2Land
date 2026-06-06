package org.eu.nl.syu.way2fly.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview as ComposePreview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import androidx.compose.ui.platform.LocalContext
import org.eu.nl.syu.way2fly.model.BoardingPassData
import org.eu.nl.syu.way2fly.network.BackendApiException
import org.eu.nl.syu.way2fly.network.PassengerSessionStore
import org.eu.nl.syu.way2fly.network.Way2LandApiClient
import org.eu.nl.syu.way2fly.ui.theme.Way2FlyTheme
import org.eu.nl.syu.way2fly.util.BCBPParser
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    onPassengerAuthenticated: (BoardingPassData) -> Unit
) {
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            GlobalHeroHeader(
                title = "Way2Fly",
                subtitle = "Passenger Portal",
                isCompact = false
            )

            Spacer(modifier = Modifier.height(32.dp))

            PassengerAuthCard(onPassengerAuthenticated)
            
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "By entering, you agree to our Terms of Service",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PassengerAuthCard(onAuthenticated: (BoardingPassData) -> Unit) {
    val context = LocalContext.current
    var phoneNumber by remember { mutableStateOf("") }
    var scannedData by remember { mutableStateOf<BoardingPassData?>(null) }
    var scanError by remember { mutableStateOf<String?>(null) }
    var authError by remember { mutableStateOf<String?>(null) }
    var isAuthenticating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun isValidE164PhoneNumber(value: String): Boolean {
        return value.matches(Regex("^\\+[1-9]\\d{1,14}$"))
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (scannedData == null) {
                Text("Scan Boarding Pass", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))
                
                Box(modifier = Modifier.fillMaxWidth().height(250.dp).clip(RoundedCornerShape(16.dp)).background(Color.Black)) {
                    if (LocalInspectionMode.current) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
                        }
                    } else {
                        CameraPreview(onBarcodeScanned = { barcode ->
                            val data = BCBPParser.parse(barcode)
                            if (data != null) { scannedData = data; scanError = null }
                            else { scanError = "Invalid boarding pass format" }
                        })
                    }
                    ScannerOverlay()
                }
                
                if (scanError != null) {
                    Text(scanError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))
                TextButton(
                    onClick = {
                        val testRaw = "M1DOE/JOHN            EABCDEFJFKLAXDL 00123123Y001A00001 1"
                        scannedData = BCBPParser.parse(testRaw)
                    }
                ) {
                    Text("Use test boarding pass", color = MaterialTheme.colorScheme.primary)
                }
            } else {
                SuccessScanView(scannedData!!) { scannedData = null }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp).alpha(0.1f))

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Phone Number") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )

            Spacer(modifier = Modifier.height(24.dp))
            
            val normalizedPhoneNumber = phoneNumber.trim()
            val isPhoneValid = isValidE164PhoneNumber(normalizedPhoneNumber)

            Button(
                onClick = {
                    val scannedPass = scannedData
                    if (scannedPass == null) {
                        authError = "Scan a boarding pass first"
                        return@Button
                    }

                    if (!isPhoneValid) {
                        authError = "Enter an E.164 phone number, for example +33612345678"
                        return@Button
                    }

                    authError = null
                    isAuthenticating = true
                    scope.launch {
                        try {
                            val tokenResponse = Way2LandApiClient.exchangePassengerToken(
                                phoneNumber = normalizedPhoneNumber,
                                pnr = scannedPass.pnr
                            )

                            onAuthenticated(
                                scannedPass.copy(
                                    phoneNumber = normalizedPhoneNumber,
                                    passengerToken = tokenResponse.passengerToken
                                )
                            )
                            PassengerSessionStore.save(context, normalizedPhoneNumber, tokenResponse.passengerToken)
                        } catch (exception: BackendApiException) {
                            authError = "Backend auth failed (${exception.statusCode}): ${exception.message}"
                        } catch (exception: Exception) {
                            authError = exception.message ?: "Authentication failed"
                        } finally {
                            isAuthenticating = false
                        }
                    }
                },
                enabled = scannedData != null && isPhoneValid && !isAuthenticating,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = Color.White)
            ) {
                Text(
                    if (isAuthenticating) "CONNECTING..." else "ENTER APP",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            if (phoneNumber.isNotEmpty() && !isPhoneValid) {
                Text(
                    "Invalid phone (use E.164 format, such as +33612345678)",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (authError != null) {
                Text(
                    authError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
fun ScannerOverlay() {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        val color = MaterialTheme.colorScheme.primary
        Surface(modifier = Modifier.size(28.dp, 3.dp).align(Alignment.TopStart), color = color, shape = RoundedCornerShape(2.dp)) {}
        Surface(modifier = Modifier.size(3.dp, 28.dp).align(Alignment.TopStart), color = color, shape = RoundedCornerShape(2.dp)) {}
        Surface(modifier = Modifier.size(28.dp, 3.dp).align(Alignment.TopEnd), color = color, shape = RoundedCornerShape(2.dp)) {}
        Surface(modifier = Modifier.size(3.dp, 28.dp).align(Alignment.TopEnd), color = color, shape = RoundedCornerShape(2.dp)) {}
        Surface(modifier = Modifier.size(28.dp, 3.dp).align(Alignment.BottomStart), color = color, shape = RoundedCornerShape(2.dp)) {}
        Surface(modifier = Modifier.size(3.dp, 28.dp).align(Alignment.BottomStart), color = color, shape = RoundedCornerShape(2.dp)) {}
        Surface(modifier = Modifier.size(28.dp, 3.dp).align(Alignment.BottomEnd), color = color, shape = RoundedCornerShape(2.dp)) {}
        Surface(modifier = Modifier.size(3.dp, 28.dp).align(Alignment.BottomEnd), color = color, shape = RoundedCornerShape(2.dp)) {}
    }
}

@Composable
fun SuccessScanView(data: BoardingPassData, onRescan: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primaryContainer).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text("Boarding pass scanned", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(data.passengerName, style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = onRescan) { Text("Rescan") }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraPreview(onBarcodeScanned: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var cameraPermissionGranted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> cameraPermissionGranted = granted }

    LaunchedEffect(cameraPermissionGranted) {
        if (!cameraPermissionGranted) { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (cameraPermissionGranted) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val executor = ContextCompat.getMainExecutor(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                            val scanner = BarcodeScanning.getClient()
                            val imageAnalysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                            imageAnalysis.setAnalyzer(executor) { imageProxy ->
                                val mediaImage = imageProxy.image
                                if (mediaImage != null) {
                                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                    scanner.process(image).addOnSuccessListener { barcodes ->
                                        for (barcode in barcodes) { barcode.rawValue?.let { onBarcodeScanned(it) } }
                                    }.addOnCompleteListener { imageProxy.close() }
                                } else { imageProxy.close() }
                            }
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
                        } catch (exc: Exception) {}
                    }, executor)
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(Color.DarkGray), contentAlignment = Alignment.Center) {
                Text("Camera permission required", color = Color.White)
            }
        }
    }
}

@ComposePreview(showBackground = true)
@Composable
fun AuthScreenPreview() {
    Way2FlyTheme { AuthScreen(onPassengerAuthenticated = {}) }
}
