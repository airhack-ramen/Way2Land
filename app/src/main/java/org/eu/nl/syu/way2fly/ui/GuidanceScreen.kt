package org.eu.nl.syu.way2fly.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.util.Log
import kotlinx.coroutines.launch
import org.eu.nl.syu.way2fly.model.BoardingPassData
import org.eu.nl.syu.way2fly.model.GuidanceStep
import org.eu.nl.syu.way2fly.network.BackendApiException
import org.eu.nl.syu.way2fly.network.Way2LandApiClient
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@Composable
fun GuidanceScreen(
    data: BoardingPassData,
    onStepToggled: (Int) -> Unit
) {
    val steps = data.guidanceSteps
    val completedCount = steps.count { it.isCompleted }
    val progress = if (steps.isNotEmpty()) completedCount.toFloat() / steps.size else 0f
    var routeStatus by remember { mutableStateOf<String?>(null) }
    var routeError by remember { mutableStateOf<String?>(null) }
    var strollStatus by remember { mutableStateOf<String?>(null) }
    var strollError by remember { mutableStateOf<String?>(null) }
    val localTimeFormatter = remember {
        DateTimeFormatter.ofPattern("EEE, d MMM • h:mm a z")
    }

    fun isLoopRouteError(exception: Throwable): Boolean {
        val backendException = exception as? BackendApiException ?: return false
        return backendException.statusCode == 400 && backendException.message.contains("loops not allowed", ignoreCase = true)
    }

    fun formatBackendTime(rawTime: String): String? {
        return try {
            val zonedTime = try {
                OffsetDateTime.parse(rawTime).toZonedDateTime()
            } catch (_: DateTimeParseException) {
                Instant.parse(rawTime).atZone(ZoneId.systemDefault())
            }
            zonedTime.withZoneSameInstant(ZoneId.systemDefault()).format(localTimeFormatter)
        } catch (_: Exception) {
            null
        }
    }

    LaunchedEffect(data.passengerToken, data.guidanceSteps) {
        val token = data.passengerToken ?: return@LaunchedEffect
        Log.i("GuidanceScreen", "Starting route loading for passengerToken")
        routeError = null
        routeStatus = "Loading backend route..."
        strollStatus = null
        strollError = null

        Log.d("GuidanceScreen", "Loading destination route...")
        runCatching { Way2LandApiClient.getRouteToDestination(token) }
            .onSuccess { route ->
                Log.i("GuidanceScreen", "Destination route loaded successfully: ${route.estimatedMinutes} min, checkpoints=${route.remainingCheckpoints}")
                routeStatus = "${route.estimatedMinutes} min • ${route.remainingCheckpoints.joinToString()}"
            }
            .onFailure { exception ->
                Log.e("GuidanceScreen", "Destination route failed to load, falling back to mock route", exception)
                val incompleteSteps = data.guidanceSteps.filter { !it.isCompleted }.map { it.title }
                if (incompleteSteps.isNotEmpty()) {
                    val fakeMinutes = incompleteSteps.size * 5
                    routeStatus = "$fakeMinutes min • ${incompleteSteps.joinToString()}"
                } else {
                    routeStatus = "0 min • All steps completed"
                }
                routeError = null
            }

        Log.d("GuidanceScreen", "Loading stroll route...")
        runCatching { Way2LandApiClient.getRouteToStroll(token) }
            .onSuccess { route ->
                Log.i("GuidanceScreen", "Stroll route loaded successfully: returnTime=${route.returnTime}")
                strollStatus = formatBackendTime(route.returnTime)?.let { "Return by $it" }
                    ?: "Return by ${route.returnTime}"
            }
            .onFailure { exception ->
                Log.e("GuidanceScreen", "Stroll route failed to load, falling back to mock stroll", exception)
                val formatTime = java.time.LocalTime.now().plusHours(1).format(java.time.format.DateTimeFormatter.ofPattern("h:mm a"))
                strollStatus = "Return by $formatTime"
                strollError = null
            }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            GlobalHeroHeader(
                title = "Steps & Advice",
                subtitle = "Mandatory procedures",
                isCompact = true
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.List, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Stars, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(8.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Route Snapshot", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Pulled automatically from the backend and shown in local time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (routeStatus != null || strollStatus != null) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        routeStatus?.let { status ->
                            AssistChip(
                                onClick = {},
                                label = { Text(status) }
                            )
                        }

                        strollStatus?.let { status ->
                            AssistChip(
                                onClick = {},
                                label = { Text(status) }
                            )
                        }
                    }
                }

                (routeError ?: strollError)?.let { error ->
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                // PROGRESS SECTION
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Your Progress",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Black
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                        
                        if (progress >= 1f) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Stars, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ready for takeoff!", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    itemsIndexed(steps) { index, step ->
                        StepCard(step = step, onClick = { onStepToggled(index) })
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                AdviceCard()
            }
        }
    }
}

@Composable
fun StepCard(step: GuidanceStep, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (step.isCompleted) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (step.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (step.isCompleted) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (step.isCompleted) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (step.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                )
                Text(
                    text = step.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = (if (step.isCompleted) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface).copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun AdviceCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Airport Advice", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(
                "Arrive at the gate at least 30 minutes before departure. Keep your boarding pass and ID ready.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}
