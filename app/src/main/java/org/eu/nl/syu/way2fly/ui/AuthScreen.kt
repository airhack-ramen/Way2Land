package org.eu.nl.syu.way2fly.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.telephony.TelephonyManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview as ComposePreview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import org.eu.nl.syu.way2fly.model.BoardingPassData
import org.eu.nl.syu.way2fly.ui.theme.Way2FlyTheme
import org.eu.nl.syu.way2fly.util.BCBPParser
import org.eu.nl.syu.way2fly.util.BackendClient
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    onPassengerAuthenticated: (BoardingPassData) -> Unit,
    onStaffAuthenticated: () -> Unit // Kept for backwards compatibility if needed elsewhere
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // App logo / brand
            Box(
                modifier = Modifier.size(96.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 0.dp
                ) {
                    Icon(
                        imageVector = Icons.Default.AirplanemodeActive,
                        contentDescription = null,
                        modifier = Modifier.size(52.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Way2Fly",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Your journey starts here",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(40.dp))

            PassengerAuthCard(onPassengerAuthenticated)

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "By continuing, you agree to our Terms of Service & Privacy Policy",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                textAlign = TextAlign.Center,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// Removed RoleSelector and StaffAuthCard as they are deprecated.

@Composable
fun PassengerAuthCard(onAuthenticated: (BoardingPassData) -> Unit) {
    var phoneNumber by remember { mutableStateOf("") }
    var scannedData by remember { mutableStateOf<BoardingPassData?>(null) }
    var scanError by remember { mutableStateOf<String?>(null) }
    var permissionState by remember { mutableStateOf<PermissionState>(PermissionState.Initial) }
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (scannedData == null) {
                SectionLabel(
                    text = "Boarding pass",
                    icon = Icons.Default.CreditCard
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                ) {
                    if (LocalInspectionMode.current) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    } else {
                        CameraPreview(onBarcodeScanned = { barcode ->
                            val data = BCBPParser.parse(barcode)
                            if (data != null) {
                                scannedData = data
                                scanError = null
                            } else {
                                scanError = "Invalid boarding pass format"
                            }
                        })
                    }
                    ScannerOverlay()
                }

                if (scanError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = scanError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = {
                    val testRaw = "M1DOE/JOHN            EABCDEFJFKLAXDL 00123123Y001A00001 1"
                    scannedData = BCBPParser.parse(testRaw)
                }) {
                    Text(
                        text = "Use test boarding pass",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            } else {
                SuccessScanView(scannedData!!) { scannedData = null }
            }

            if (scannedData != null) {
                Spacer(modifier = Modifier.height(4.dp))

                SectionLabel(
                    text = "Phone number",
                    icon = Icons.Default.Phone
                )

                Spacer(modifier = Modifier.height(12.dp))

                when (permissionState) {
                    PermissionState.Initial -> {
                        PhonePermissionRequestCard(
                            onGrantPermission = { permissionState = PermissionState.Requesting },
                            onEnterManually = { permissionState = PermissionState.ManualEntry }
                        )
                    }
                    PermissionState.Requesting -> {
                        PermissionRequestScreen(
                            onPermissionGranted = { phone ->
                                phoneNumber = phone
                                permissionState = PermissionState.Completed
                            },
                            onPermissionDenied = { permissionState = PermissionState.ManualEntry },
                            onGoBack = { permissionState = PermissionState.Initial }
                        )
                    }
                    PermissionState.ManualEntry -> {
                        PhoneNumberInput(
                            phoneNumber = phoneNumber,
                            onPhoneNumberChange = { phoneNumber = it },
                            onBack = { permissionState = PermissionState.Initial }
                        )
                    }
                    PermissionState.Completed -> {
                        PhoneNumberConfirmed(
                            phoneNumber = phoneNumber,
                            onEdit = { permissionState = PermissionState.ManualEntry }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                val isPhoneValid = phoneNumber == "0" || (phoneNumber.length >= 10 && phoneNumber.all { it.isDigit() })

                Button(
                    onClick = { 
                        scannedData?.let { data -> 
                            isLoading = true
                            coroutineScope.launch {
                                // Default test mock data if "0" is used, otherwise real phone
                                val phoneToUse = if (phoneNumber == "0") "+33612345678" else phoneNumber
                                // For the demo bypass, if using test boarding pass PNR is "ABCDEF"
                                val pnrToUse = if (phoneNumber == "0") "ABCDEF" else data.pnr
                                
                                val success = BackendClient.exchangeToken(phoneToUse, pnrToUse)
                                isLoading = false
                                if (success) {
                                    onAuthenticated(data.copy(phoneNumber = phoneNumber))
                                } else {
                                    scanError = "Backend authentication failed. Is server running?"
                                }
                            }
                        } 
                    },
                    enabled = scannedData != null && isPhoneValid && !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            text = "Enter app",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (phoneNumber.isNotEmpty() && !isPhoneValid) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Enter 10 digits or '0'",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private enum class PermissionState {
    Initial,
    Requesting,
    ManualEntry,
    Completed
}

@Composable
private fun PhonePermissionRequestCard(
    onGrantPermission: () -> Unit,
    onEnterManually: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Smartphone,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Auto-fill phone number?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "We can automatically read your number from your SIM card.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onEnterManually,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    Text(
                        text = "Enter manually",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Button(
                    onClick = onGrantPermission,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    Text(
                        text = "Auto-fill",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionRequestScreen(
    onPermissionGranted: (String) -> Unit,
    onPermissionDenied: () -> Unit,
    onGoBack: () -> Unit
) {
    val context = LocalContext.current
    var isProcessing by remember { mutableStateOf(true) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val phoneGranted = permissions[Manifest.permission.READ_PHONE_NUMBERS] == true ||
                permissions[Manifest.permission.READ_PHONE_STATE] == true

        if (phoneGranted) {
            @Suppress("DEPRECATION")
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_NUMBERS) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                @Suppress("DEPRECATION")
                val number = runCatching { telephonyManager.line1Number }.getOrNull()
                if (!number.isNullOrBlank()) {
                    onPermissionGranted(number.filter { it.isDigit() })
                } else {
                    onPermissionDenied()
                }
            } else {
                onPermissionDenied()
            }
        } else {
            onPermissionDenied()
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.READ_PHONE_NUMBERS,
                Manifest.permission.READ_PHONE_STATE
            )
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Requesting permission...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    tint = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Permission required",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Please enable phone permission in Settings to auto-fill your number.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onGoBack,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        Text(text = "Back", style = MaterialTheme.typography.labelLarge)
                    }

                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        Text(text = "Open Settings", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun PhoneNumberInput(
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Enter phone number",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = phoneNumber,
            onValueChange = onPhoneNumberChange,
            label = { Text("Phone Number") },
            leadingIcon = {
                Icon(
                    Icons.Default.Phone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        if (phoneNumber.isNotEmpty()) {
            val isPhoneValid = phoneNumber == "0" || (phoneNumber.length >= 10 && phoneNumber.all { it.isDigit() })
            if (!isPhoneValid) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Enter 10 digits or '0'",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun PhoneNumberConfirmed(phoneNumber: String, onEdit: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Phone number saved",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = phoneNumber,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TextButton(onClick = onEdit) {
                Text(
                    text = "Edit",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

// StaffAuthCard removed

@Composable
fun ScannerOverlay() {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        val color = MaterialTheme.colorScheme.primary
        Surface(
            modifier = Modifier.size(28.dp, 3.dp).align(Alignment.TopStart),
            color = color,
            shape = RoundedCornerShape(2.dp)
        ) {}
        Surface(
            modifier = Modifier.size(3.dp, 28.dp).align(Alignment.TopStart),
            color = color,
            shape = RoundedCornerShape(2.dp)
        ) {}
        Surface(
            modifier = Modifier.size(28.dp, 3.dp).align(Alignment.TopEnd),
            color = color,
            shape = RoundedCornerShape(2.dp)
        ) {}
        Surface(
            modifier = Modifier.size(3.dp, 28.dp).align(Alignment.TopEnd),
            color = color,
            shape = RoundedCornerShape(2.dp)
        ) {}
        Surface(
            modifier = Modifier.size(28.dp, 3.dp).align(Alignment.BottomStart),
            color = color,
            shape = RoundedCornerShape(2.dp)
        ) {}
        Surface(
            modifier = Modifier.size(3.dp, 28.dp).align(Alignment.BottomStart),
            color = color,
            shape = RoundedCornerShape(2.dp)
        ) {}
        Surface(
            modifier = Modifier.size(28.dp, 3.dp).align(Alignment.BottomEnd),
            color = color,
            shape = RoundedCornerShape(2.dp)
        ) {}
        Surface(
            modifier = Modifier.size(3.dp, 28.dp).align(Alignment.BottomEnd),
            color = color,
            shape = RoundedCornerShape(2.dp)
        ) {}
    }
}

@Composable
fun SuccessScanView(data: BoardingPassData, onRescan: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(26.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Board pass scanned",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = data.passengerName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Flight ${data.carrier}${data.flightNumber}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onRescan) {
            Text(
                text = "Rescan",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge
            )
        }
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
    ) { granted ->
        cameraPermissionGranted = granted
    }

    LaunchedEffect(cameraPermissionGranted) {
        if (!cameraPermissionGranted) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
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
                            val preview = Preview.Builder().build().also { it.surfaceProvider =
                                previewView.surfaceProvider }
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Camera permission required",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@ComposePreview(showBackground = true)
@Composable
fun AuthScreenPreview() {
    Way2FlyTheme { AuthScreen(onPassengerAuthenticated = {}, onStaffAuthenticated = {}) }
}
