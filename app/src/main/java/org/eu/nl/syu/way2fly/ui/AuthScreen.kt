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
import org.eu.nl.syu.way2fly.BuildConfig
import org.eu.nl.syu.way2fly.model.BoardingPassData
import org.eu.nl.syu.way2fly.network.BackendApiException
import android.util.Log
import org.eu.nl.syu.way2fly.network.PassengerSessionStore
import org.eu.nl.syu.way2fly.network.Way2LandApiClient
import org.eu.nl.syu.way2fly.ui.theme.Way2FlyTheme
import org.eu.nl.syu.way2fly.util.BCBPParser
import org.eu.nl.syu.way2fly.util.BackendClient
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    onPassengerAuthenticated: (BoardingPassData) -> Unit,
    statusMessage: String? = null,
    statusIsError: Boolean = false,
    statusIsWorking: Boolean = false
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

            if (statusMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    statusMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (statusIsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (statusIsWorking) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            
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
    var permissionState by remember { mutableStateOf<PermissionState>(PermissionState.Initial) }
    var authError by remember { mutableStateOf<String?>(null) }
    var isAuthenticating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun isValidE164PhoneNumber(value: String): Boolean {
        return value == "0" || value.matches(Regex("^\\+[1-9]\\d{1,14}$"))
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
                            Log.d("AuthScreen", "Scanned barcode data length: ${barcode.length}")
                            val data = BCBPParser.parse(barcode)
                            if (data != null) {
                                Log.i("AuthScreen", "Successfully parsed boarding pass for: ${data.passengerName}")
                                scannedData = data
                                scanError = null
                            } else {
                                Log.w("AuthScreen", "Failed to parse boarding pass barcode")
                                scanError = "Invalid boarding pass format"
                            }
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
                        Log.i("AuthScreen", "Using test boarding pass")
                        scannedData = BCBPParser.parse(testRaw)
                    }
                ) {
                    Text("Use test boarding pass", color = MaterialTheme.colorScheme.primary)
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
                            onBack = { permissionState = PermissionState.Initial },
                            isValidE164PhoneNumber = ::isValidE164PhoneNumber
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

                val normalizedPhoneNumber = phoneNumber.trim()
                val isPhoneValid = isValidE164PhoneNumber(normalizedPhoneNumber)

                Button(
                    onClick = { 
                        scannedData?.let { data -> 
                            authError = null
                            isAuthenticating = true
                            scope.launch {
                                try {
                                    // Default test mock data if "0" is used, otherwise real phone
                                    val phoneToUse = if (normalizedPhoneNumber == "0") "+33612345678" else normalizedPhoneNumber
                                    // For the demo bypass, if using test boarding pass PNR is "ABCDEF"
                                    val pnrToUse = if (normalizedPhoneNumber == "0") "ABCDEF" else data.pnr
                                    
                                    Log.i("AuthScreen", "Exchanging passenger token: phone=$phoneToUse, PNR=$pnrToUse")
                                    val tokenResponse = Way2LandApiClient.exchangePassengerToken(
                                        phoneNumber = phoneToUse,
                                        pnr = pnrToUse
                                    )
                                    Log.i("AuthScreen", "Passenger token exchange success, saving session")
                                    isAuthenticating = false
                                    onAuthenticated(
                                        data.copy(
                                            phoneNumber = phoneToUse,
                                            pnr = pnrToUse,
                                            passengerToken = tokenResponse.passengerToken
                                        )
                                    )
                                    PassengerSessionStore.save(context, phoneToUse, tokenResponse.passengerToken)
                                } catch (exception: BackendApiException) {
                                    Log.e("AuthScreen", "Backend token exchange failed: status=${exception.statusCode}", exception)
                                    authError = "Backend auth failed (${exception.statusCode}): ${exception.message}"
                                    isAuthenticating = false
                                } catch (exception: Exception) {
                                    Log.e("AuthScreen", "Unexpected token exchange failure", exception)
                                    authError = exception.message ?: "Authentication failed"
                                    isAuthenticating = false
                                }
                            }
                        } 
                    },
                    enabled = scannedData != null && isPhoneValid && !isAuthenticating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    if (isAuthenticating) {
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
                        text = "Invalid format (use E.164 like +33612345678 or '0')",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                if (authError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = authError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, icon: ImageVector) {
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
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_NUMBERS) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                val number = runCatching { telephonyManager.line1Number }.getOrNull()
                if (!number.isNullOrBlank()) {
                    onPermissionGranted(number.filter { it.isDigit() || it == '+' })
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
    onBack: () -> Unit,
    isValidE164PhoneNumber: (String) -> Boolean
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
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        if (phoneNumber.isNotEmpty()) {
            val isPhoneValid = isValidE164PhoneNumber(phoneNumber.trim())
            if (!isPhoneValid) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Invalid format (use E.164 like +33612345678 or '0')",
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
                    Log.d("AuthScreen", "CameraX: fetching camera provider instance")
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            Log.d("AuthScreen", "CameraX: binding preview and barcode analyzer use cases")
                            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                            val scanner = BarcodeScanning.getClient()
                            val imageAnalysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                            imageAnalysis.setAnalyzer(executor) { imageProxy ->
                                val mediaImage = imageProxy.image
                                if (mediaImage != null) {
                                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                    scanner.process(image).addOnSuccessListener { barcodes ->
                                        for (barcode in barcodes) {
                                            barcode.rawValue?.let {
                                                Log.i("AuthScreen", "CameraX: barcode scanned")
                                                onBarcodeScanned(it)
                                            }
                                        }
                                    }.addOnCompleteListener { imageProxy.close() }
                                } else { imageProxy.close() }
                            }
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
                            Log.i("AuthScreen", "CameraX: successfully bound camera to lifecycle")
                        } catch (exc: Exception) {
                            Log.e("AuthScreen", "CameraX: camera binding failed", exc)
                        }
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
