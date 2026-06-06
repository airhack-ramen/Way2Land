package org.eu.nl.syu.way2fly.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import org.eu.nl.syu.way2fly.model.HelpRequest
import org.eu.nl.syu.way2fly.model.InboxMessage
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffMainScreen(
    helpRequests: List<HelpRequest>,
    notifications: List<InboxMessage>,
    onSendHelpRequest: (Int, String) -> Unit,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Way2Fly - STAFF", fontWeight = FontWeight.Bold, color = Color.White) },
                actions = {
                    IconButton(onClick = onLogout) { Icon(Icons.Default.Logout, contentDescription = "Logout", tint = Color.White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.error)
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Map, contentDescription = "Map") },
                    label = { Text("Task Map") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Emergency, contentDescription = "Help") },
                    label = { Text("Request Help") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { 
                        BadgedBox(badge = { if (notifications.isNotEmpty()) Badge { Text(notifications.size.toString()) } }) {
                            Icon(Icons.Default.Inbox, contentDescription = "Inbox")
                        }
                    },
                    label = { Text("Staff Inbox") }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            when (selectedTab) {
                0 -> MapScreen(role = "STAFF", helpRequests = helpRequests)
                1 -> HelpToolTab(onSendHelpRequest)
                2 -> InboxScreen(messages = notifications)
            }
        }
    }
}

@Composable
fun HelpToolTab(onSendHelpRequest: (Int, String) -> Unit) {
    var urgency by remember { mutableFloatStateOf(5f) }
    var details by remember { mutableStateOf("") }

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
        
        OutlinedTextField(
            value = details,
            onValueChange = { details = it },
            label = { Text("Location/Type of help") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = { onSendHelpRequest(urgency.roundToInt(), details); details = "" },
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (urgency > 7) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Send, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text("BROADCAST HELP REQUEST", fontWeight = FontWeight.Bold)
        }
    }
}

import kotlin.math.roundToInt
