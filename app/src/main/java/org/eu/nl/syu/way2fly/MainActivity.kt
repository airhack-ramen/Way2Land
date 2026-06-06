package org.eu.nl.syu.way2fly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.eu.nl.syu.way2fly.model.*
import org.eu.nl.syu.way2fly.ui.*
import org.eu.nl.syu.way2fly.ui.theme.Way2FlyTheme
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Way2FlyTheme {
                val navController = rememberNavController()
                var boardingPassData by remember { mutableStateOf<BoardingPassData?>(null) }
                var helpRequests by remember { mutableStateOf(listOf<HelpRequest>()) }
                var staffNotifications by remember { mutableStateOf(listOf<InboxMessage>()) }
                var passengerDatabase by remember { mutableStateOf(generateRandomPassengers(50)) }

                NavHost(
                    navController = navController,
                    startDestination = "auth"
                ) {
                    composable("auth") {
                        AuthScreen(
                            onPassengerAuthenticated = { data ->
                                boardingPassData = data
                                if (!passengerDatabase.any { it.pnr == data.pnr }) {
                                    passengerDatabase = passengerDatabase + data
                                }
                                navController.navigate("passenger_main") { popUpTo("auth") { inclusive = true } }
                                sendPassengerNotification(
                                    "Gate Proximity Alert",
                                    "You are 8 minutes away from Gate B12. Boarding starts in 20 minutes.",
                                    boardingPassData
                                ) { boardingPassData = it }
                            },
                            onStaffAuthenticated = {
                                navController.navigate("staff_main") { popUpTo("auth") { inclusive = true } }
                            }
                        )
                    }
                    
                    composable("passenger_main") {
                        boardingPassData?.let { data ->
                            PassengerMainScreen(
                                data = data,
                                onStepToggled = { index ->
                                    val newSteps = data.guidanceSteps.toMutableList()
                                    val step = newSteps[index]
                                    newSteps[index] = step.copy(isCompleted = !step.isCompleted)
                                    boardingPassData = data.copy(guidanceSteps = newSteps)
                                },
                                onOpenDev = { navController.navigate("dev") },
                                onLogout = {
                                    boardingPassData = null
                                    navController.navigate("auth") { popUpTo(0) }
                                }
                            )
                        }
                    }

                    composable("staff_main") {
                        StaffMainScreen(
                            helpRequests = helpRequests,
                            notifications = staffNotifications,
                            passengers = passengerDatabase,
                            onSendHelpRequest = { urgency, location ->
                                val id = UUID.randomUUID().toString()
                                val req = HelpRequest(id, urgency, "General", location, System.currentTimeMillis(), "Help at $location")
                                helpRequests = helpRequests + req
                                staffNotifications = staffNotifications + InboxMessage(id, "Help Requested (Level $urgency)", "Location: $location", System.currentTimeMillis())
                            },
                            onSecurityAction = { passenger, action ->
                                val id = UUID.randomUUID().toString()
                                staffNotifications = staffNotifications + InboxMessage(
                                    id = id,
                                    title = "SECURITY ALERT: $action",
                                    body = "Action taken for ${passenger.passengerName} (TS: ${passenger.threatScore}). PNR: ${passenger.pnr}",
                                    timestamp = System.currentTimeMillis()
                                )
                            },
                            onLogout = { 
                                navController.navigate("auth") { popUpTo(0) }
                            }
                        )
                    }

                    composable("dev") {
                        boardingPassData?.let { data ->
                            DevToolScreen(
                                currentData = data,
                                onDataChanged = { boardingPassData = it },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun sendPassengerNotification(title: String, body: String, data: BoardingPassData?, onUpdate: (BoardingPassData) -> Unit) {
        data?.let {
            val newMessage = InboxMessage(UUID.randomUUID().toString(), title, body, System.currentTimeMillis())
            onUpdate(it.copy(notifications = it.notifications + newMessage))
        }
    }

    private fun generateRandomPassengers(count: Int): List<BoardingPassData> {
        val firstNames = listOf("Mihai", "Elena", "Andrei", "Maria", "Stefan", "Ioana", "Radu", "Cristina", "Alexandru", "Anca")
        val lastNames = listOf("Popescu", "Ionescu", "Dumitru", "Stan", "Gheorghe", "Rusu", "Matei", "Vasile", "Constantin", "Dinu")
        val cities = listOf("OTP", "CLJ", "TSR", "IAS", "LHR", "FRA", "CDG", "AMS")
        
        return List(count) {
            val name = "${lastNames.random()}/${firstNames.random()}"
            BoardingPassData(
                passengerName = name,
                pnr = UUID.randomUUID().toString().substring(0, 6).uppercase(),
                from = cities.random(),
                to = cities.random(),
                carrier = "RO",
                flightNumber = String.format("%05d", (1..99999).random()),
                date = "123",
                seat = "${(1..30).random()}${('A'..'F').random()}",
                threatScore = (0..100).random()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassengerMainScreen(
    data: BoardingPassData,
    onStepToggled: (Int) -> Unit,
    onOpenDev: () -> Unit,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Way2Fly", fontWeight = FontWeight.Bold, color = Color.White) },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Switch Account", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Map, null) },
                    label = { Text("Map") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, null) },
                    label = { Text("Steps") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { 
                        BadgedBox(badge = { if (data.notifications.isNotEmpty()) Badge { Text(data.notifications.size.toString()) } }) {
                            Icon(Icons.Default.Inbox, null)
                        }
                    },
                    label = { Text("Inbox") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Description, null) },
                    label = { Text("Data") }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> MapScreen()
                1 -> GuidanceScreen(data.guidanceSteps, onStepToggled)
                2 -> InboxScreen(data.notifications)
                3 -> {
                    Column {
                        IconButton(onClick = onOpenDev, modifier = Modifier.align(Alignment.End)) {
                            Icon(Icons.Default.Settings, null)
                        }
                        DetailsScreen(data)
                    }
                }
            }
        }
    }
}
