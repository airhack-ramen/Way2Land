package org.eu.nl.syu.way2fly.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview as ComposePreview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import org.eu.nl.syu.way2fly.model.BoardingPassData
import org.eu.nl.syu.way2fly.network.BackendApiException
import org.eu.nl.syu.way2fly.network.PassengerSessionStore
import org.eu.nl.syu.way2fly.network.Way2LandApiClient
import org.eu.nl.syu.way2fly.ui.theme.Way2FlyTheme
import org.eu.nl.syu.way2fly.util.BCBPParser
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

    AnimatedContent(
        targetState = scannedData != null,
        transitionSpec = {
            fadeIn(animationSpec = tween(220, delayMillis = 30)).togetherWith(fadeOut(animationSpec = tween(180)))
        },
        label = "postScanContent"
    ) { hasData ->
        if (!hasData) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SectionLabel(
                        text = "Boarding pass",
                        icon = Icons.Default.CreditCard
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(16.dp))
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
                                    modifier = Modifier.size(44.dp)
                                )
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
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = scanError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    TextButton(onClick = {
                        val testRaw = "M1DOE/JOHN            EABCDEFJFKLAXDL 00123123Y001A00001 1"
                        Log.i("AuthScreen", "Using test boarding pass")
                        scannedData = BCBPParser.parse(testRaw)
                    }) {
                        Text(
                            text = "Use test boarding pass",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        } else {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (scannedData != null) {
                        SuccessScanView(scannedData!!) { scannedData = null }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    SectionLabel(
                        text = "Phone number",
                        icon = Icons.Default.Phone
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    when (permissionState) {
                        PermissionState.Initial -> {
                            PhonePermissionRequestCard(
                                onGrantPermission = {
                                    authError = null
                                    permissionState = PermissionState.Requesting
                                },
                                onEnterManually = { permissionState = PermissionState.ManualEntry }
                            )
                        }
                        PermissionState.Requesting -> {
                            PermissionRequestScreen(
                                onPermissionGranted = { phone ->
                                    phoneNumber = phone
                                    authError = null
                                    permissionState = PermissionState.Completed
                                },
                                onUnavailable = { reason ->
                                    authError = reason
                                    permissionState = PermissionState.ManualEntry
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

                    Spacer(modifier = Modifier.height(20.dp))

                    val normalizedPhoneNumber = phoneNumber.trim()
                    val isPhoneValid = isValidE164PhoneNumber(normalizedPhoneNumber)

                    Button(
                        onClick = {
                            scannedData?.let { data ->
                                authError = null
                                isAuthenticating = true
                                scope.launch {
                                    try {
                                        val phoneToUse = if (normalizedPhoneNumber == "0" || normalizedPhoneNumber == "+33612345678") "+33612345678" else normalizedPhoneNumber
                                        val pnrToUse = if (normalizedPhoneNumber == "0" || normalizedPhoneNumber == "+33612345678") "ABCDEF" else data.pnr

                                        Log.i("AuthScreen", "Exchanging passenger token: phone=$phoneToUse, PNR=$pnrToUse")
                                        val tokenResponse = runCatching {
                                            Way2LandApiClient.exchangePassengerToken(
                                                phoneNumber = phoneToUse,
                                                pnr = pnrToUse
                                            )
                                        }.getOrElse { error ->
                                            if (phoneToUse == "+33612345678") {
                                                Log.w("AuthScreen", "Backend token exchange failed for bypass number, using mock token", error)
                                                org.eu.nl.syu.way2fly.network.PassengerTokenResponse(passengerToken = "mock_passenger_token", expiresIn = 3600)
                                            } else {
                                                throw error
                                            }
                                        }
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
                            .height(52.dp),
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
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center
                        )
                    }
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
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Smartphone,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Auto-fill phone number?",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "We can read your number from the SIM.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onEnterManually,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text(
                        text = "Enter manually",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Button(
                    onClick = onGrantPermission,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
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
    onUnavailable: (String) -> Unit,
    onPermissionDenied: () -> Unit,
    onGoBack: () -> Unit
) {
    val context = LocalContext.current
    var isProcessing by remember { mutableStateOf(true) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        isProcessing = false
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
                    onPermissionGranted(number.filter { it.isDigit() || it == '+' })
                } else {
                    onUnavailable("Phone number not available on this SIM. Please enter manually.")
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
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Requesting permission...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Permission granted, but we can’t read your number",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Your carrier doesn’t expose the number on this SIM.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onGoBack,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Text(text = "Back", style = MaterialTheme.typography.labelLarge)
                    }

                    Button(
                        onClick = onPermissionDenied,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Text(text = "Enter manually", style = MaterialTheme.typography.labelLarge)
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

        Spacer(modifier = Modifier.height(10.dp))

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
            shape = RoundedCornerShape(14.dp),
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

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
    Box(modifier = Modifier.fillMaxSize().padding(14.dp)) {
        val color = MaterialTheme.colorScheme.primary
        Surface(
            modifier = Modifier.size(24.dp, 3.dp).align(Alignment.TopStart),
            color = color,
            shape = RoundedCornerShape(2.dp)
        ) {}
        Surface(
            modifier = Modifier.size(3.dp, 24.dp).align(Alignment.TopStart),
            color = color,
            shape = RoundedCornerShape(2.dp)
        ) {}
        Surface(
            modifier = Modifier.size(24.dp, 3.dp).align(Alignment.TopEnd),
            color = color,
            shape = RoundedCornerShape(2.dp)
        ) {}
        Surface(
            modifier = Modifier.size(3.dp, 24.dp).align(Alignment.TopEnd),
            color = color,
            shape = RoundedCornerShape(2.dp)
        ) {}
        Surface(
            modifier = Modifier.size(24.dp, 3.dp).align(Alignment.BottomStart),
            color = color,
            shape = RoundedCornerShape(2.dp)
        ) {}
        Surface(
            modifier = Modifier.size(3.dp, 24.dp).align(Alignment.BottomStart),
            color = color,
            shape = RoundedCornerShape(2.dp)
        ) {}
        Surface(
            modifier = Modifier.size(24.dp, 3.dp).align(Alignment.BottomEnd),
            color = color,
            shape = RoundedCornerShape(2.dp)
        ) {}
        Surface(
            modifier = Modifier.size(3.dp, 24.dp).align(Alignment.BottomEnd),
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
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Board pass scanned",
            style = MaterialTheme.typography.titleSmall,
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

        Spacer(modifier = Modifier.height(10.dp))

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
                        modifier = Modifier.size(44.dp),
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
    Way2FlyTheme { AuthScreen(onPassengerAuthenticated = {}) }
}
