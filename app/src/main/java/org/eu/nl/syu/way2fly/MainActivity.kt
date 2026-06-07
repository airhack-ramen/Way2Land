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

import org.eu.nl.syu.way2fly.util.BackendClient
import kotlinx.coroutines.delay

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
                var passengerDatabase by remember { mutableStateOf(generateRandomPassengers(100)) }

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
                            
                            // Poll backend for real-time notifications
                            LaunchedEffect(Unit) {
                                while(true) {
                                    val newNotifications = BackendClient.fetchNotifications()
                                    if (newNotifications.isNotEmpty()) {
                                        // Update state with newly fetched notifications (avoiding exact duplicates by ID)
                                        val existingIds = boardingPassData?.notifications?.map { it.body } ?: emptyList()
                                        val filtered = newNotifications.filter { it.body !in existingIds }
                                        if (filtered.isNotEmpty()) {
                                            boardingPassData = boardingPassData?.copy(
                                                notifications = boardingPassData!!.notifications + filtered
                                            )
                                        }
                                    }
                                    delay(5000) // Poll every 5 seconds
                                }
                            }
                            
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
        
        val passengers = mutableListOf<BoardingPassData>()
        var i = 0
        while (i < count) {
            val isGroup = Math.random() > 0.6 // 40% chance to be in a group
            val groupSize = if (isGroup) (2..5).random() else 1
            val groupId = if (isGroup) UUID.randomUUID().toString().substring(0, 8) else null
            
            val toAdd = minOf(groupSize, count - i)
            val lastName = lastNames.random() // Families usually share a last name
            val flightNum = String.format("%05d", (1..99999).random())
            val fromCity = cities.random()
            val toCity = cities.random()

            for (j in 0 until toAdd) {
                val name = "${lastName}/${firstNames.random()}"
                passengers.add(
                    BoardingPassData(
                        passengerName = name,
                        pnr = UUID.randomUUID().toString().substring(0, 6).uppercase(),
                        from = fromCity,
                        to = toCity,
                        carrier = "RO",
                        flightNumber = flightNum,
                        date = "123",
                        seat = "${(1..30).random()}${('A'..'F').random()}",
                        threatScore = (0..100).random(),
                        groupId = groupId,
                        companionCount = toAdd - 1
                    )
                )
                i++
            }
        }
        return passengers
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
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Map, null) },
                    label = { Text("Map") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, null) },
                    label = { Text("Steps") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { 
                        BadgedBox(badge = { if (data.notifications.isNotEmpty()) Badge { Text(data.notifications.size.toString()) } }) {
                            Icon(Icons.Default.Inbox, null)
                        }
                    },
                    label = { Text("Inbox") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Description, null) },
                    label = { Text("Data") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> MapScreen()
                1 -> GuidanceScreen(data.guidanceSteps, onStepToggled)
                2 -> InboxScreen(data.notifications, onStepToggled)
                3 -> {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onOpenDev) {
                                Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = onLogout) {
                                Icon(Icons.AutoMirrored.Filled.Logout, null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        DetailsScreen(data)
                    }
                }
            }
        }
    }
}
