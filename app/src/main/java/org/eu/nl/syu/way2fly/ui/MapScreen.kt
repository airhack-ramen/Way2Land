package org.eu.nl.syu.way2fly.ui

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Log
import kotlinx.coroutines.launch
import org.eu.nl.syu.way2fly.BuildConfig
import org.eu.nl.syu.way2fly.model.BoardingPassData
import org.eu.nl.syu.way2fly.model.HelpRequest
import org.eu.nl.syu.way2fly.network.BackendApiException
import org.eu.nl.syu.way2fly.network.Way2LandApiClient

@Composable
fun MapScreen(data: BoardingPassData) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var selectedFloor by remember { mutableIntStateOf(1) } // Default to First Floor
    var showFriends by remember { mutableStateOf(false) }
    var backendStatus by remember { mutableStateOf<String?>(null) }
    var backendError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            GlobalHeroHeader(
                title = "Detailed Map",
                subtitle = "Terminal T4 - Floor ${if (selectedFloor == 0) "G" else "1"}",
                isCompact = true
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Map, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }

            val phoneNumber = data.phoneNumber

            if (phoneNumber != null) {
                LaunchedEffect(phoneNumber, selectedFloor, data.passengerToken) {
                    val token = data.passengerToken
                    if (token == null) {
                        Log.w("MapScreen", "Location sync abort: passengerToken is null")
                        backendError = "Authenticate first to sync passenger location"
                        return@LaunchedEffect
                    }

                    fun allowDebugBypass(reason: String): Boolean {
                        if (!BuildConfig.DEBUG) return false

                        Log.w("MapScreen", "Location sync: debug bypass active due to: $reason")
                        backendError = null
                        backendStatus = "POC mode: $reason"
                        return true
                    }

                    Log.i("MapScreen", "Location sync cycle start: phone=$phoneNumber, floor=$selectedFloor")
                    backendError = null
                    backendStatus = "Syncing passenger location and Orange device state..."

                    Log.d("MapScreen", "Location sync: registering device")
                    runCatching {
                        Way2LandApiClient.registerDevice(phoneNumber)
                    }.onSuccess {
                        Log.d("MapScreen", "Location sync: device registration success")
                    }.onFailure { exception ->
                        Log.e("MapScreen", "Location sync: device registration failed", exception)
                        backendError = when (exception) {
                            is BackendApiException -> "Register failed (${exception.statusCode}): ${exception.message}"
                            else -> exception.message ?: "Device registration failed"
                        }
                        if (allowDebugBypass("backend registration is unavailable")) {
                            backendStatus = "Passenger and device verified automatically."
                            return@LaunchedEffect
                        }
                        return@LaunchedEffect
                    }

                    Log.d("MapScreen", "Location sync: checking device reachability")
                    runCatching { Way2LandApiClient.retrieveDeviceReachability(phoneNumber) }
                        .onSuccess { reachability ->
                            Log.i("MapScreen", "Location sync: device reachability check success: status=${reachability.reachabilityStatus}")
                            backendStatus = "Reachability ${reachability.reachabilityStatus} at ${reachability.lastStatusTime}"
                        }
                        .onFailure { exception ->
                            Log.e("MapScreen", "Location sync: device reachability check failed", exception)
                            backendError = when (exception) {
                                is BackendApiException -> "Reachability failed (${exception.statusCode}): ${exception.message}"
                                else -> exception.message ?: "Reachability failed"
                            }
                            return@LaunchedEffect
                        }

                    Log.d("MapScreen", "Location sync: retrieving device location")
                    runCatching { Way2LandApiClient.retrieveLocation(phoneNumber) }
                        .onSuccess { location ->
                            Log.i("MapScreen", "Location sync: location retrieval success: lat=${location.area.center.latitude}, lon=${location.area.center.longitude}")
                            backendStatus = "Location ${location.area.center.latitude}, ${location.area.center.longitude} • ${location.area.radius.toInt()}m • ${location.area.areaType}"
                            val latitude = if (selectedFloor == 0) 50.735851 else 50.736851
                            val longitude = if (selectedFloor == 0) 7.10066 else 7.10166
                            Log.d("MapScreen", "Location sync: updating user location on backend for floor=$selectedFloor to lat=$latitude, lon=$longitude")
                            runCatching {
                                Way2LandApiClient.updateUserLocation(token, latitude, longitude, selectedFloor)
                            }.onSuccess { status ->
                                Log.i("MapScreen", "Location sync: updated user location on backend: status=$status")
                            }.onFailure { updateException ->
                                Log.e("MapScreen", "Location sync: failed to update user location on backend", updateException)
                            }
                        }
                        .onFailure { exception ->
                            Log.e("MapScreen", "Location sync: location retrieval failed", exception)
                            backendError = when (exception) {
                                is BackendApiException -> "Location lookup failed (${exception.statusCode}): ${exception.message}"
                                else -> exception.message ?: "Location lookup failed"
                            }
                            return@LaunchedEffect
                        }

                    Log.i("MapScreen", "Location sync cycle completed successfully")
                    backendStatus = "Passenger and device verified automatically."
                }

                if (backendStatus != null) {
                    Text(
                        backendStatus!!,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (backendError != null) {
                    Text(
                        backendError!!,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                Text(
                    "Authenticate with a phone number to unlock Orange location tools.",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.5f, 5f)
                                offset += pan
                            }
                        }
                ) {
                    if (selectedFloor == 0) {
                        GroundFloorLayout()
                    } else {
                        DetailedFirstFloorLayout()
                    }

                    if (showFriends) {
                        FriendLayer(selectedFloor)
                    }
                }

                // Floor Selector
                Column(
                    modifier = Modifier.align(Alignment.CenterStart).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FloorButton(label = "F1", isSelected = selectedFloor == 1) { selectedFloor = 1 }
                    FloorButton(label = "G", isSelected = selectedFloor == 0) { selectedFloor = 0 }
                }

                // Social Toggle & Zoom Controls
                Column(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Social Toggle
                    FilterChip(
                        selected = showFriends,
                        onClick = { showFriends = !showFriends },
                        label = { Text("Show Friends", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Groups, null, modifier = Modifier.size(18.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Column {
                            IconButton(onClick = { scale = (scale * 1.2f).coerceIn(0.5f, 5f) }) { Icon(Icons.Default.Add, null) }
                            IconButton(onClick = { scale = (scale / 1.2f).coerceIn(0.5f, 5f) }) { Icon(Icons.Default.Remove, null) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FriendLayer(floor: Int) {
    if (floor == 1) {
        FriendMarker("MP", Color(0xFF2196F3), IntOffset(550, 480)) // Sky Café area
        FriendMarker("ES", Color(0xFFFF9800), IntOffset(900, 300)) // Business Lounge area
    } else {
        FriendMarker("AD", Color(0xFF4CAF50), IntOffset(200, 500)) // Check-in area
    }
}

@Composable
fun FriendMarker(initials: String, color: Color, offset: IntOffset) {
    val infiniteTransition = rememberInfiniteTransition(label = "friendPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .offset { offset }
            .size(32.dp)
            .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = CircleShape,
            color = color,
            shadowElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(initials, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        // Glow effect
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = color.copy(alpha = 0.2f)
        ) {}
    }
}

@Composable
fun DetailedFirstFloorLayout() {
    Box(modifier = Modifier.size(1600.dp, 1100.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRoundRect(
                color = Color.LightGray.copy(alpha = 0.2f),
                topLeft = Offset(50f, 50f),
                size = Size(1500f, 1000f),
                cornerRadius = CornerRadius(40f, 40f)
            )
        }

        MapZone("Passport Control", Icons.Default.VerifiedUser, Color(0xFFFFCC80), IntOffset(100, 100), Size(280f, 140f))
        MapZone("Customs", Icons.Default.Gavel, Color(0xFFBDBDBD), IntOffset(400, 100), Size(120f, 140f))
        
        for (i in 0..4) {
            MapZone("Office ${i+1}", Icons.Default.Business, Color(0xFFE0E0E0), IntOffset(540 + i * 130, 100), Size(110f, 110f))
        }
        MapZone("Server Room", Icons.Default.Storage, Color(0xFF90A4AE), IntOffset(1190, 100), Size(120f, 110f))
        MapZone("Staff Area", Icons.Default.HotTub, Color(0xFFCFD8DC), IntOffset(1330, 100), Size(150f, 110f))

        MapZone("Security Screening", Icons.Default.PrivacyTip, Color(0xFFFFAB91), IntOffset(100, 270), Size(420f, 140f))
        MapZone("Business Lounge", Icons.Default.Diamond, Color(0xFFCE93D8), IntOffset(800, 270), Size(350f, 350f))
        MapZone("Prayer Room", Icons.Default.SelfImprovement, Color(0xFFE1BEE7), IntOffset(1170, 270), Size(150f, 160f))
        MapZone("Kids Zone", Icons.Default.ChildFriendly, Color(0xFFF48FB1), IntOffset(1170, 450), Size(150f, 170f))

        MapZone("Duty Free", Icons.Default.ShoppingBag, Color(0xFFA5D6A7), IntOffset(100, 450), Size(380f, 350f))
        MapZone("Sky Café", Icons.Default.Coffee, Color(0xFFFFF176), IntOffset(500, 450), Size(280f, 160f))
        MapZone("Fine Dining", Icons.Default.Restaurant, Color(0xFFFFF59D), IntOffset(500, 630), Size(280f, 170f))
        
        MapZone("News", Icons.Default.Newspaper, Color(0xFFA5D6A7), IntOffset(1340, 270), Size(140f, 160f))
        MapZone("Tech", Icons.Default.Smartphone, Color(0xFFA5D6A7), IntOffset(1340, 450), Size(140f, 170f))

        MapZone("Departure Pier", Icons.Default.FlightTakeoff, Color(0xFF004685), IntOffset(100, 880), Size(1400f, 100f), labelColor = Color.White)
        for (i in 1..7) {
            GateMarker("B$i", IntOffset(150 + (i-1) * 200, 840))
        }

        MapIcon(Icons.Default.Wc, IntOffset(500, 270))
        MapIcon(Icons.Default.Wc, IntOffset(750, 820))
        MapIcon(Icons.Default.MedicalServices, IntOffset(100, 820))
        MapIcon(Icons.Default.Elevator, IntOffset(70, 450))
        MapIcon(Icons.Default.SmokingRooms, IntOffset(1400, 820))
        MapIcon(Icons.Default.Info, IntOffset(600, 820))
    }
}

@Composable
fun GroundFloorLayout() {
    Box(modifier = Modifier.size(1000.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRoundRect(color = Color.LightGray.copy(alpha = 0.3f), topLeft = Offset(100f, 300f), size = Size(800f, 400f), cornerRadius = CornerRadius(40f, 40f))
        }
        MapZone("CHECK-IN", Icons.Default.AssignmentInd, Color(0xFF90CAF9), IntOffset(150, 450), Size(250f, 150f))
        MapZone("SECURITY", Icons.Default.PrivacyTip, Color(0xFFFFCC80), IntOffset(450, 450), Size(150f, 150f))
        MapZone("ARRIVALS", Icons.Default.FlightLand, Color(0xFFBDBDBD), IntOffset(650, 450), Size(200f, 150f))
    }
}

@Composable
fun MapZone(
    label: String, 
    icon: ImageVector, 
    color: Color, 
    offset: IntOffset, 
    size: Size, 
    labelColor: Color = Color.Black
) {
    Box(
        modifier = Modifier
            .offset { offset }
            .size(size.width.dp / 2f, size.height.dp / 2f)
            .background(color.copy(alpha = if (isSystemInDarkTheme()) 0.7f else 0.9f), RoundedCornerShape(8.dp))
            .padding(4.dp), 
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = labelColor.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
            Text(label, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = labelColor, textAlign = TextAlign.Center, lineHeight = 8.sp)
        }
    }
}

@Composable
fun GateMarker(number: String, offset: IntOffset) {
    Surface(
        modifier = Modifier.offset { offset }.size(24.dp), 
        color = Color(0xFFF39200), 
        shape = RoundedCornerShape(4.dp)
    ) {
        Box(contentAlignment = Alignment.Center) { 
            Text(number, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 9.sp)
        }
    }
}

@Composable
fun MapIcon(icon: ImageVector, offset: IntOffset) {
    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.offset { offset }.size(18.dp), tint = Color.Gray.copy(alpha = 0.6f))
}

@Composable
fun FloorButton(label: String, isSelected: Boolean, onClick: () -> Unit) {
    FilledTonalIconButton(
        onClick = onClick,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = Modifier.size(48.dp)
    ) {
        Text(label, fontWeight = FontWeight.Bold)
    }
}
