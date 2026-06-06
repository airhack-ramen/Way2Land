package org.eu.nl.syu.way2fly.ui

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import org.eu.nl.syu.way2fly.model.HelpRequest

@Composable
fun MapScreen(
    role: String = "PASSENGER", // PASSENGER or STAFF
    helpRequests: List<HelpRequest> = emptyList()
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var selectedFloor by remember { mutableIntStateOf(0) } // 0: Ground, 1: Floor 1

    val locationCoords = mapOf(
        "Check-in Island" to (Offset(275f, 525f) to 0),
        "Security T4" to (Offset(525f, 525f) to 0),
        "Arrivals Hall" to (Offset(750f, 525f) to 0),
        "Duty Free Shop" to (Offset(775f, 475f) to 1),
        "Business Lounge" to (Offset(250f, 550f) to 1),
        "Gate B1" to (Offset(160f, 210f) to 1),
        "Gate B2" to (Offset(250f, 210f) to 1),
        "Gate B3" to (Offset(340f, 210f) to 1),
        "Gate B4" to (Offset(430f, 210f) to 1),
        "Gate B5" to (Offset(520f, 210f) to 1)
    )

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            BrandedHeader(
                title = if (role == "STAFF") "Task Map" else "Floor Map",
                subtitle = "Terminal T4 - Floor ${if (selectedFloor == 0) "G" else "1"}"
            ) {
                Surface(
                    modifier = Modifier.size(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Map, null, tint = Color.White)
                    }
                }
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
                        GroundFloorLayout(helpRequests)
                    } else {
                        FirstFloorLayout(helpRequests)
                    }
                    
                    if (role == "STAFF") {
                        helpRequests.forEach { request ->
                            val coordInfo = locationCoords[request.location]
                            if (coordInfo != null && coordInfo.second == selectedFloor) {
                                StaffHelpPin(request, coordInfo.first)
                            }
                        }
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

                // Zoom Controls
                Card(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
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

@Composable
fun GroundFloorLayout(helpRequests: List<HelpRequest>) {
    Box(modifier = Modifier.size(1000.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRoundRect(color = Color.LightGray.copy(alpha = 0.5f), topLeft = Offset(100f, 300f), size = Size(800f, 400f), cornerRadius = CornerRadius(40f, 40f))
        }
        MapZone("CHECK-IN", Icons.Default.AssignmentInd, Color(0xFF90CAF9), IntOffset(150, 450), Size(250f, 150f), 
            isHighlighted = helpRequests.any { it.location == "Check-in Island" })
        MapZone("SECURITY", Icons.Default.PrivacyTip, Color(0xFFFFCC80), IntOffset(450, 450), Size(150f, 150f),
            isHighlighted = helpRequests.any { it.location == "Security T4" })
        MapZone("ARRIVALS", Icons.Default.FlightLand, Color(0xFFBDBDBD), IntOffset(650, 450), Size(200f, 150f),
            isHighlighted = helpRequests.any { it.location == "Arrivals Hall" })
    }
}

@Composable
fun FirstFloorLayout(helpRequests: List<HelpRequest>) {
    Box(modifier = Modifier.size(1000.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRoundRect(color = Color.LightGray.copy(alpha = 0.5f), topLeft = Offset(100f, 200f), size = Size(800f, 500f), cornerRadius = CornerRadius(40f, 40f))
        }
        MapZone("BOARDING PIER", Icons.Default.FlightTakeoff, Color(0xFF004685), IntOffset(150, 250), Size(500f, 100f), labelColor = Color.White)
        for (i in 1..5) { 
            GateMarker("B$i", IntOffset(160 + (i-1)*90, 210), 
                isHighlighted = helpRequests.any { it.location == "Gate B$i" }) 
        }
        MapZone("DUTY FREE", Icons.Default.ShoppingBag, Color(0xFFA5D6A7), IntOffset(700, 350), Size(150f, 250f),
            isHighlighted = helpRequests.any { it.location == "Duty Free Shop" })
        MapZone("BUSINESS LOUNGE", Icons.Default.Laptop, Color(0xFFCE93D8), IntOffset(150, 500), Size(200f, 100f),
            isHighlighted = helpRequests.any { it.location == "Business Lounge" })
    }
}

@Composable
fun MapZone(
    label: String, 
    icon: ImageVector, 
    color: Color, 
    offset: IntOffset, 
    size: Size, 
    labelColor: Color = Color.Black,
    isHighlighted: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val borderColor by infiniteTransition.animateColor(
        initialValue = Color.Transparent,
        targetValue = if (isHighlighted) Color.Red else Color.Transparent,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseColor"
    )

    Box(
        modifier = Modifier
            .offset { offset }
            .size(size.width.dp / 2f, size.height.dp / 2f)
            .background(color.copy(alpha = if (isSystemInDarkTheme()) 0.8f else 1f), RoundedCornerShape(8.dp))
            .border(width = if (isHighlighted) 4.dp else 0.dp, color = borderColor, shape = RoundedCornerShape(8.dp))
            .padding(4.dp), 
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = labelColor.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                if (isHighlighted) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.Star, null, tint = Color.Red, modifier = Modifier.size(18.dp))
                }
            }
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = labelColor, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun GateMarker(number: String, offset: IntOffset, isHighlighted: Boolean = false) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isHighlighted) 1.3f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Surface(
        modifier = Modifier.offset { offset }.size(28.dp).graphicsLayer(scaleX = scale, scaleY = scale), 
        color = if (isHighlighted) Color.Red else Color(0xFFF39200), 
        shape = RoundedCornerShape(4.dp),
        shadowElevation = if (isHighlighted) 12.dp else 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(number, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp)
                if (isHighlighted) Icon(Icons.Default.Star, null, tint = Color.White, modifier = Modifier.size(10.dp))
            }
        }
    }
}

@Composable
fun StaffHelpPin(request: HelpRequest, position: Offset) {
    val infiniteTransition = rememberInfiniteTransition(label = "pinPulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pinScale"
    )

    Box(
        modifier = Modifier.offset(x = position.x.dp, y = position.y.dp).size(60.dp).graphicsLayer(scaleX = scale, scaleY = scale),
        contentAlignment = Alignment.Center
    ) {
        Surface(modifier = Modifier.size(40.dp), shape = RoundedCornerShape(20.dp), color = Color.Red.copy(alpha = 0.4f)) {}
        Icon(Icons.Default.LocationOn, null, tint = Color.Red, modifier = Modifier.size(48.dp))
        Surface(
            modifier = Modifier.align(Alignment.TopEnd).offset(x = 2.dp, y = (-2).dp),
            color = Color.Black,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(text = request.urgency.toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
        }
    }
}
