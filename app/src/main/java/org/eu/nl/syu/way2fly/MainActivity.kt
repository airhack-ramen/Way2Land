package org.eu.nl.syu.way2fly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.eu.nl.syu.way2fly.model.*
import org.eu.nl.syu.way2fly.network.PassengerSessionStore
import org.eu.nl.syu.way2fly.network.Way2LandApiClient
import org.eu.nl.syu.way2fly.network.BackendApiException
import org.eu.nl.syu.way2fly.ui.*
import org.eu.nl.syu.way2fly.ui.theme.Way2FlyTheme
import java.util.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Way2FlyTheme {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                val navController = rememberNavController()
                var boardingPassData by remember { mutableStateOf<BoardingPassData?>(null) }

                NavHost(
                    navController = navController,
                    startDestination = "auth",
                    enterTransition = { fadeIn(tween(400)) + slideInHorizontally(tween(400)) { it } },
                    exitTransition = { fadeOut(tween(400)) + slideOutHorizontally(tween(400)) { -it } }
                ) {
                    composable("auth") {
                        AuthScreen(
                            onPassengerAuthenticated = { data ->
                                // Initialize with some dummy groups for demonstration
                                val initialGroups = listOf(
                                    FriendGroup(UUID.randomUUID().toString(), "Family Vacation", 4, "Nearby"),
                                    FriendGroup(UUID.randomUUID().toString(), "Project Team", 3, "20m")
                                )
                                boardingPassData = data.copy(activeGroups = initialGroups)
                                navController.navigate("passenger_main") { popUpTo("auth") { inclusive = true } }
                                sendPassengerNotification(
                                    "Gate Proximity Alert",
                                    "You are 8 minutes away from Gate B12. Boarding starts in 20 minutes.",
                                    boardingPassData
                                ) { boardingPassData = it }

                                data.passengerToken?.let { token ->
                                    scope.launch {
                                        runCatching {
                                            Way2LandApiClient.syncUserTags(
                                                passengerToken = token,
                                                tags = listOf(
                                                    "PASSENGER",
                                                    "FLIGHT_${data.carrier}_${data.flightNumber}",
                                                    "PNR_${data.pnr}"
                                                ),
                                                replace = false
                                            )
                                        }

                                        runCatching { Way2LandApiClient.getUserNotifications(token) }
                                            .onSuccess { backendMessages ->
                                                boardingPassData = boardingPassData?.copy(notifications = backendMessages + (boardingPassData?.notifications.orEmpty()))
                                            }
                                    }
                                }
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
                                onDeleteNotification = { id ->
                                    boardingPassData = data.copy(notifications = data.notifications.filter { it.id != id })
                                },
                                onCreateGroup = { name ->
                                    val newGroup = FriendGroup(UUID.randomUUID().toString(), name, 1, "0m")
                                    boardingPassData = data.copy(activeGroups = data.activeGroups + newGroup)
                                },
                                onDeleteGroup = { id ->
                                    val token = boardingPassData?.passengerToken
                                    if (token == null) {
                                        boardingPassData = data.copy(activeGroups = data.activeGroups.filter { it.id != id })
                                    } else {
                                        scope.launch {
                                            runCatching { Way2LandApiClient.leaveGroup(token, id) }
                                        }
                                        boardingPassData = data.copy(activeGroups = data.activeGroups.filter { it.id != id })
                                    }
                                },
                                onRenameGroup = { id, newName ->
                                    boardingPassData = data.copy(activeGroups = data.activeGroups.map { 
                                        if (it.id == id) it.copy(name = newName) else it 
                                    })
                                },
                                onJoinGroup = { groupId ->
                                    val newGroup = FriendGroup(groupId, "Joined Group", 1, "0m")
                                    boardingPassData = data.copy(activeGroups = data.activeGroups + newGroup)
                                },
                                onLogout = {
                                    PassengerSessionStore.clear(context)
                                    boardingPassData = null
                                    navController.navigate("auth") { popUpTo(0) }
                                }
                            )
                        }
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassengerMainScreen(
    data: BoardingPassData,
    onStepToggled: (Int) -> Unit,
    onDeleteNotification: (String) -> Unit,
    onCreateGroup: (String) -> Unit,
    onDeleteGroup: (String) -> Unit,
    onRenameGroup: (String, String) -> Unit,
    onJoinGroup: (String) -> Unit,
    onOpenDev: () -> Unit,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 8.dp) {
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
                    icon = { Icon(Icons.Default.Groups, null) },
                    label = { Text("Groups") }
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Default.Description, null) },
                    label = { Text("Data") }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn(tween(300)) togetherWith fadeOut(tween(300))
                },
                label = "tab_switch"
            ) { targetTab ->
                when (targetTab) {
                    0 -> MapScreen(data)
                    1 -> GuidanceScreen(data, onStepToggled)
                    2 -> InboxScreen(data.notifications, onStepToggled, onDeleteNotification)
                    3 -> GroupsScreen(
                        data = data,
                        groups = data.activeGroups,
                        onCreateGroup = onCreateGroup,
                        onDeleteGroup = onDeleteGroup,
                        onRenameGroup = onRenameGroup,
                        onJoinGroup = onJoinGroup
                    )
                    4 -> DetailsScreen(data, onLogout)
                }
            }
        }
    }
}
