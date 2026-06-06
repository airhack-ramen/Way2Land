package org.eu.nl.syu.way2fly.ui

import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview as ComposePreview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import org.eu.nl.syu.way2fly.model.BoardingPassData
import org.eu.nl.syu.way2fly.ui.theme.Way2FlyTheme
import org.eu.nl.syu.way2fly.util.BCBPParser

@Composable
fun AuthScreen(
    onPassengerAuthenticated: (BoardingPassData) -> Unit,
    onStaffAuthenticated: () -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: Passenger, 1: Staff
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            Surface(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.AirplanemodeActive,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Way2Fly",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // ROLE IDENTIFIER TEXT
            Surface(
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (selectedTabIndex == 0) "LOGIN AS PASSENGER" else "LOGIN AS STAFF",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            SecondaryTabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
                contentColor = Color.White,
                divider = {}
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("PASSENGER", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("STAFF", fontWeight = FontWeight.Bold) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (selectedTabIndex == 0) {
                PassengerAuthCard(onPassengerAuthenticated)
            } else {
                StaffAuthCard(onStaffAuthenticated)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "By entering, you agree to our Terms of Service",
                style = MaterialTheme.typography.labelSmall,
                color = if (selectedTabIndex == 0) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PassengerAuthCard(onAuthenticated: (BoardingPassData) -> Unit) {
    var phoneNumber by remember { mutableStateOf("") }
    var scannedData by remember { mutableStateOf<BoardingPassData?>(null) }
    var scanError by remember { mutableStateOf<String?>(null) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
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
                OutlinedButton(
                    onClick = {
                        val testRaw = "M1DOE/JOHN            EABCDEFJFKLAXDL 00123123Y001A00001 1"
                        scannedData = BCBPParser.parse(testRaw)
                    },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Simulation Scan", color = MaterialTheme.colorScheme.secondary)
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
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone)
            )

            Spacer(modifier = Modifier.height(24.dp))
            
            // VALIDATION LOGIC
            val isPhoneValid = phoneNumber == "0" || (phoneNumber.length >= 10 && phoneNumber.all { it.isDigit() })

            Button(
                onClick = { scannedData?.let { onAuthenticated(it.copy(phoneNumber = phoneNumber)) } },
                enabled = scannedData != null && isPhoneValid,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = Color.White)
            ) {
                Text("ENTER APP", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            
            if (phoneNumber.isNotEmpty() && !isPhoneValid) {
                Text(
                    "Invalid phone (Enter 10 digits or '0')",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun StaffAuthCard(onAuthenticated: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Staff Credentials", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 24.dp))
            
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
            
            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (username == "admin" && password == "admin") {
                        onAuthenticated()
                    } else {
                        error = "Invalid admin credentials"
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("STAFF LOGIN", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ScannerOverlay() {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        val color = MaterialTheme.colorScheme.secondary
        Surface(modifier = Modifier.size(30.dp, 2.dp).align(Alignment.TopStart), color = color) {}
        Surface(modifier = Modifier.size(2.dp, 30.dp).align(Alignment.TopStart), color = color) {}
        Surface(modifier = Modifier.size(30.dp, 2.dp).align(Alignment.TopEnd), color = color) {}
        Surface(modifier = Modifier.size(2.dp, 30.dp).align(Alignment.TopEnd), color = color) {}
        Surface(modifier = Modifier.size(30.dp, 2.dp).align(Alignment.BottomStart), color = color) {}
        Surface(modifier = Modifier.size(2.dp, 30.dp).align(Alignment.BottomStart), color = color) {}
        Surface(modifier = Modifier.size(30.dp, 2.dp).align(Alignment.BottomEnd), color = color) {}
        Surface(modifier = Modifier.size(2.dp, 30.dp).align(Alignment.BottomEnd), color = color) {}
    }
}

@Composable
fun SuccessScanView(data: BoardingPassData, onRescan: () -> Unit) {
    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(48.dp))
    Text("Success!", style = MaterialTheme.typography.titleLarge, color = Color(0xFF4CAF50))
    Spacer(modifier = Modifier.height(8.dp))
    Text(data.passengerName, fontWeight = FontWeight.Bold)
    Text("Flight ${data.carrier}${data.flightNumber}")
    TextButton(onClick = onRescan) { Text("Rescan", color = MaterialTheme.colorScheme.primary) }
}

@OptIn(ExperimentalGetImage::class)
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
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
                } catch (exc: Exception) {}
            }, executor)
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@ComposePreview(showBackground = true)
@Composable
fun AuthScreenPreview() {
    Way2FlyTheme { AuthScreen(onPassengerAuthenticated = {}, onStaffAuthenticated = {}) }
}
