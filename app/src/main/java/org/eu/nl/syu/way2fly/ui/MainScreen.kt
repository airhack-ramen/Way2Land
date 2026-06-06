package org.eu.nl.syu.way2fly.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import org.eu.nl.syu.way2fly.model.BoardingPassData

@Composable
fun MainScreen(data: BoardingPassData) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.LocationOn, contentDescription = "Map") },
                    label = { Text("Map") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Info, contentDescription = "Details") },
                    label = { Text("Details") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Notifications, contentDescription = "Status") },
                    label = { Text("Status") }
                )
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (selectedTab) {
                0 -> MapScreen()
                1 -> DetailsScreen(data)
                2 -> StatusScreen(data)
            }
        }
    }
}

@Composable
fun DetailsScreen(data: BoardingPassData) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Boarding Pass Details", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        DetailRow("Passenger", data.passengerName)
        DetailRow("PNR", data.pnr)
        DetailRow("Flight", "${data.carrier}${data.flightNumber}")
        DetailRow("Route", "${data.from} -> ${data.to}")
        DetailRow("Seat", data.seat)
        DetailRow("Phone", data.phoneNumber ?: "Not provided")
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(text = "$label:", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.width(100.dp))
        Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
    }
}

@Composable
fun StatusScreen(data: BoardingPassData) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Flight Status", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Flight ${data.carrier}${data.flightNumber} is ON TIME", color = MaterialTheme.colorScheme.primary)
        Text("Gate: B12")
        Text("Boarding: 14:30")
    }
}
