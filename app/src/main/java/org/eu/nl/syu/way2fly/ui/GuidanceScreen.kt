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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.eu.nl.syu.way2fly.model.BoardingPassData
import org.eu.nl.syu.way2fly.model.GuidanceStep
import org.eu.nl.syu.way2fly.network.BackendApiException
import org.eu.nl.syu.way2fly.network.Way2LandApiClient

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
    val scope = rememberCoroutineScope()

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

            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Backend Routes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Route planning now comes from the backend, using the passenger session token instead of local-only advice.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                val token = data.passengerToken
                                if (token == null) {
                                    routeError = "Authenticate first to load destination routes"
                                    return@Button
                                }

                                routeError = null
                                routeStatus = null
                                scope.launch {
                                    try {
                                        val route = Way2LandApiClient.getRouteToDestination(token)
                                        routeStatus = "${route.estimatedMinutes} min • ${route.remainingCheckpoints.joinToString()}"
                                    } catch (exception: BackendApiException) {
                                        routeError = "Destination route failed (${exception.statusCode}): ${exception.message}"
                                    } catch (exception: Exception) {
                                        routeError = exception.message ?: "Destination route failed"
                                    }
                                }
                            }
                        ) {
                            Text("Load destination route")
                        }

                        OutlinedButton(
                            onClick = {
                                val token = data.passengerToken
                                if (token == null) {
                                    routeError = "Authenticate first to load stroll routes"
                                    return@OutlinedButton
                                }

                                routeError = null
                                strollStatus = null
                                scope.launch {
                                    try {
                                        val route = Way2LandApiClient.getRouteToStroll(token)
                                        strollStatus = "Return by ${route.returnTime}"
                                    } catch (exception: BackendApiException) {
                                        routeError = "Stroll route failed (${exception.statusCode}): ${exception.message}"
                                    } catch (exception: Exception) {
                                        routeError = exception.message ?: "Stroll route failed"
                                    }
                                }
                            }
                        ) {
                            Text("Load stroll route")
                        }
                    }

                    routeStatus?.let { status ->
                        Text(status, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                    }

                    strollStatus?.let { status ->
                        Text(status, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
                    }

                    routeError?.let { error ->
                        Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
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
