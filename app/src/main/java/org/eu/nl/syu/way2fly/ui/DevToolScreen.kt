package org.eu.nl.syu.way2fly.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.eu.nl.syu.way2fly.model.BoardingPassData
import org.eu.nl.syu.way2fly.model.InboxMessage
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevToolScreen(
    currentData: BoardingPassData,
    onDataChanged: (BoardingPassData) -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf(currentData.passengerName) }
    var pnr by remember { mutableStateOf(currentData.pnr) }
    var seat by remember { mutableStateOf(currentData.seat) }
    var phone by remember { mutableStateOf(currentData.phoneNumber ?: "") }
    
    var notifTitle by remember { mutableStateOf("Gate Change") }
    var notifBody by remember { mutableStateOf("Flight RO123 gate changed to B15") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Developer Tools", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Boarding Pass State", style = MaterialTheme.typography.titleLarge)
            
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Passenger Name") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = pnr,
                    onValueChange = { pnr = it },
                    label = { Text("PNR") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = seat,
                    onValueChange = { seat = it },
                    label = { Text("Seat") },
                    modifier = Modifier.weight(1f)
                )
            }
            
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Button(
                onClick = {
                    onDataChanged(currentData.copy(
                        passengerName = name,
                        pnr = pnr,
                        seat = seat,
                        phoneNumber = phone
                    ))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Update Boarding Pass Data")
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            Text("Send Notification", style = MaterialTheme.typography.titleLarge)
            
            OutlinedTextField(
                value = notifTitle,
                onValueChange = { notifTitle = it },
                label = { Text("Notification Title") },
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = notifBody,
                onValueChange = { notifBody = it },
                label = { Text("Notification Body") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Button(
                onClick = {
                    val newMessage = InboxMessage(
                        id = UUID.randomUUID().toString(),
                        title = notifTitle,
                        body = notifBody,
                        timestamp = System.currentTimeMillis()
                    )
                    onDataChanged(currentData.copy(
                        notifications = currentData.notifications + newMessage
                    ))
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Default.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Send Test Notification")
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    onDataChanged(currentData.copy(notifications = emptyList()))
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Clear All Notifications")
            }
        }
    }
}
