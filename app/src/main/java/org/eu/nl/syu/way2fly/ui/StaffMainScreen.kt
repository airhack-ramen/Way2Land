package org.eu.nl.syu.way2fly.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.eu.nl.syu.way2fly.model.BoardingPassData
import org.eu.nl.syu.way2fly.model.HelpRequest
import org.eu.nl.syu.way2fly.model.InboxMessage
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffMainScreen(
    helpRequests: List<HelpRequest>,
    notifications: List<InboxMessage>,
    passengers: List<BoardingPassData>,
    onSendHelpRequest: (Int, String) -> Unit,
    onSecurityAction: (BoardingPassData, String) -> Unit,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Map, null) },
                    label = { Text("Task Map") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Emergency, null) },
                    label = { Text("Help") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Groups, null) },
                    label = { Text("Users") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { 
                        BadgedBox(badge = { if (notifications.isNotEmpty()) Badge { Text(notifications.size.toString()) } }) {
                            Icon(Icons.Default.Inbox, null)
                        }
                    },
                    label = { Text("Staff Inbox") }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            when (selectedTab) {
                0 -> {
                    Column {
                        BrandedHeader(title = "Task Map", subtitle = "Emergency coordination") {
                            IconButton(onClick = onLogout) { Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Color.White) }
                        }
                        MapScreen(role = "STAFF", helpRequests = helpRequests)
                    }
                }
                1 -> {
                    Column {
                        BrandedHeader(title = "Broadcast", subtitle = "Request terminal assistance") {
                            IconButton(onClick = onLogout) { Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Color.White) }
                        }
                        HelpToolTab(onSendHelpRequest)
                    }
                }
                2 -> PassengerListScreen(passengers = passengers, onSecurityAction = onSecurityAction)
                3 -> InboxScreen(messages = notifications)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpToolTab(onSendHelpRequest: (Int, String) -> Unit) {
    var urgency by remember { mutableFloatStateOf(5f) }
    val locations = listOf("Check-in Island", "Security T4", "Duty Free Shop", "Gate B1", "Gate B2", "Arrivals Hall", "Business Lounge")
    var expanded by remember { mutableStateOf(false) }
    var selectedLocation by remember { mutableStateOf(locations[0]) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("Request Assistance", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Broadcast an urgency alert to all nearby staff members.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Text("Urgency Level: ${urgency.roundToInt()}", fontWeight = FontWeight.Bold, color = if (urgency > 7) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
        Slider(
            value = urgency,
            onValueChange = { urgency = it },
            valueRange = 1f..10f,
            steps = 9,
            colors = SliderDefaults.colors(
                thumbColor = if (urgency > 7) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                activeTrackColor = if (urgency > 7) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedLocation,
                onValueChange = {},
                readOnly = true,
                label = { Text("Select Location") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                locations.forEach { location ->
                    DropdownMenuItem(
                        text = { Text(location) },
                        onClick = {
                            selectedLocation = location
                            expanded = false
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = { onSendHelpRequest(urgency.roundToInt(), selectedLocation) },
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (urgency > 7) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, null)
            Spacer(modifier = Modifier.width(12.dp))
            Text("BROADCAST HELP REQUEST", fontWeight = FontWeight.Bold)
        }
    }
}
