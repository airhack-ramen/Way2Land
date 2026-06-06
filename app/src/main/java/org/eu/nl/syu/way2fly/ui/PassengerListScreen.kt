package org.eu.nl.syu.way2fly.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.eu.nl.syu.way2fly.model.BoardingPassData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassengerListScreen(
    passengers: List<BoardingPassData>,
    onSecurityAction: (BoardingPassData, String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredPassengers = passengers.filter {
        it.passengerName.contains(searchQuery, ignoreCase = true) || 
        it.pnr.contains(searchQuery, ignoreCase = true)
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            BrandedHeader(
                title = "Users",
                subtitle = "Security & Monitoring",
                isCompact = true
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Groups, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search by Name or PNR") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredPassengers) { passenger ->
                        PassengerCard(passenger, onSecurityAction)
                    }
                }
            }
        }
    }
}

@Composable
fun PassengerCard(
    passenger: BoardingPassData,
    onSecurityAction: (BoardingPassData, String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    val threatColor = when {
        passenger.threatScore > 80 -> Color.Red
        passenger.threatScore > 40 -> MaterialTheme.colorScheme.secondary // Orange
        else -> Color(0xFF4CAF50) // Green
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = passenger.passengerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Flight ${passenger.carrier}${passenger.flightNumber} • PNR ${passenger.pnr}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                
                Surface(
                    color = threatColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, threatColor)
                ) {
                    Text(
                        text = "TS: ${passenger.threatScore}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = threatColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box {
                    Button(
                        onClick = { expanded = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Security, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SECURITY ACTION", style = MaterialTheme.typography.labelMedium)
                    }
                    
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Call Airport Security") },
                            leadingIcon = { Icon(Icons.Default.Shield, contentDescription = null, tint = Color.Blue) },
                            onClick = { 
                                onSecurityAction(passenger, "Airport Security Called")
                                expanded = false 
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Call Police") },
                            leadingIcon = { Icon(Icons.Default.LocalPolice, contentDescription = null, tint = Color.Red) },
                            onClick = { 
                                onSecurityAction(passenger, "Police Dispatched")
                                expanded = false 
                            }
                        )
                    }
                }
            }
        }
    }
}
