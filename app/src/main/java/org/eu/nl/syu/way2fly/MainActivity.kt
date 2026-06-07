package org.eu.nl.syu.way2fly

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import org.eu.nl.syu.way2fly.BuildConfig
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
import org.eu.nl.syu.way2fly.util.BackendClient
import kotlinx.coroutines.delay

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
                var authStatusMessage by remember { mutableStateOf<String?>(null) }
                var authIsError by remember { mutableStateOf(false) }
                var authIsWorking by remember { mutableStateOf(false) }
                var pendingAuthData by remember { mutableStateOf<BoardingPassData?>(null) }

                LaunchedEffect(pendingAuthData?.passengerToken) {
                    val data = pendingAuthData ?: return@LaunchedEffect
                    val phoneNumber = data.phoneNumber
                    val passengerToken = data.passengerToken
                    if (phoneNumber == null || passengerToken == null) {
                        authStatusMessage = "Passenger data is incomplete"
                        authIsError = true
                        authIsWorking = false
                        pendingAuthData = null
                        return@LaunchedEffect
                    }

                    val isBypassNumber = phoneNumber == "+33612345678" || phoneNumber == "0"

                    fun allowDebugBypass(reason: String) {
                        if (BuildConfig.DEBUG) {
                            val initialGroups = listOf(
                                FriendGroup(UUID.randomUUID().toString(), "Family Vacation", 4, "Nearby"),
                                FriendGroup(UUID.randomUUID().toString(), "Project Team", 3, "20m")
                            )

                            BackendClient.jwtToken = passengerToken
                            boardingPassData = data.copy(activeGroups = initialGroups)
                            authStatusMessage = "POC mode: $reason"
                            authIsError = false
                            authIsWorking = false
                            navController.navigate("passenger_main") { popUpTo("auth") { inclusive = true } }
                            pendingAuthData = null
                        }
                    }

                    Log.i("MainActivity", "Auth Flow Start: automatic registration/verification for phoneNumber=$phoneNumber")
                    authIsWorking = true
                    authIsError = false
                    authStatusMessage = "Registering and verifying device automatically..."

                    Log.d("MainActivity", "Auth Flow: registering device for phoneNumber=$phoneNumber")
                    val registrationResult = runCatching { Way2LandApiClient.registerDevice(phoneNumber) }
                    if (registrationResult.isFailure) {
                        val exception = registrationResult.exceptionOrNull()
                        Log.e("MainActivity", "Auth Flow: device registration failed for phoneNumber=$phoneNumber", exception)
                        if (!isBypassNumber) {
                            authStatusMessage = when (exception) {
                                is BackendApiException -> "Register failed (${exception.statusCode}): ${exception.message}"
                                else -> exception?.message ?: "Device registration failed"
                            }
                            allowDebugBypass("backend registration is unavailable")
                            if (boardingPassData != null) {
                                Log.i("MainActivity", "Auth Flow: debug bypass allowed active session")
                                return@LaunchedEffect
                            }
                            authIsError = true
                            authIsWorking = false
                            PassengerSessionStore.clear(context)
                            boardingPassData = null
                            pendingAuthData = null
                            return@LaunchedEffect
                        } else {
                            Log.w("MainActivity", "Auth Flow: device registration failed but test number bypass active, continuing", exception)
                        }
                    } else {
                        Log.i("MainActivity", "Auth Flow: device registered successfully: ${registrationResult.getOrNull()}")
                    }

                    Log.d("MainActivity", "Auth Flow: retrieving device reachability for phoneNumber=$phoneNumber")
                    val reachabilityResult = runCatching { Way2LandApiClient.retrieveDeviceReachability(phoneNumber) }
                    if (reachabilityResult.isFailure) {
                        val exception = reachabilityResult.exceptionOrNull()
                        Log.e("MainActivity", "Auth Flow: device reachability lookup failed for phoneNumber=$phoneNumber", exception)
                        if (!isBypassNumber) {
                            authStatusMessage = when (exception) {
                                is BackendApiException -> "Verification failed (${exception.statusCode}): ${exception.message}"
                                else -> exception?.message ?: "Device verification failed"
                            }
                            allowDebugBypass("device verification is unavailable")
                            if (boardingPassData != null) {
                                Log.i("MainActivity", "Auth Flow: debug bypass allowed active session")
                                return@LaunchedEffect
                            }
                            authIsError = true
                            authIsWorking = false
                            PassengerSessionStore.clear(context)
                            boardingPassData = null
                            pendingAuthData = null
                            return@LaunchedEffect
                        } else {
                            Log.w("MainActivity", "Auth Flow: device reachability lookup failed but test number bypass active, continuing", exception)
                        }
                    } else {
                        Log.i("MainActivity", "Auth Flow: device reachability check success: ${reachabilityResult.getOrNull()?.reachabilityStatus}")
                    }

                    Log.d("MainActivity", "Auth Flow: retrieving device location for phoneNumber=$phoneNumber")
                    val locationResult = runCatching { Way2LandApiClient.retrieveLocation(phoneNumber) }
                    if (locationResult.isFailure) {
                        val exception = locationResult.exceptionOrNull()
                        Log.e("MainActivity", "Auth Flow: location retrieval failed for phoneNumber=$phoneNumber", exception)
                        if (!isBypassNumber) {
                            authStatusMessage = when (exception) {
                                is BackendApiException -> "Location check failed (${exception.statusCode}): ${exception.message}"
                                else -> exception?.message ?: "Device location check failed"
                            }
                            allowDebugBypass("location lookup is unavailable")
                            if (boardingPassData != null) {
                                Log.i("MainActivity", "Auth Flow: debug bypass allowed active session")
                                return@LaunchedEffect
                            }
                            authIsError = true
                            authIsWorking = false
                            PassengerSessionStore.clear(context)
                            boardingPassData = null
                            pendingAuthData = null
                            return@LaunchedEffect
                        } else {
                            Log.w("MainActivity", "Auth Flow: location retrieval failed but test number bypass active, continuing", exception)
                        }
                    } else {
                        val loc = locationResult.getOrNull()
                        Log.i("MainActivity", "Auth Flow: location retrieved successfully: latitude=${loc?.area?.center?.latitude}, longitude=${loc?.area?.center?.longitude}, radius=${loc?.area?.radius}")
                    }

                    val location = locationResult.getOrNull()
                    if (location != null || isBypassNumber) {
                        val lat = location?.area?.center?.latitude ?: 50.735851
                        val lon = location?.area?.center?.longitude ?: 7.10066
                        val rad = location?.area?.radius ?: 100.0

                        Log.d("MainActivity", "Auth Flow: verifying location for phoneNumber=$phoneNumber")
                        val verificationResult = runCatching {
                            Way2LandApiClient.verifyLocation(
                                phoneNumber = phoneNumber,
                                centerLatitude = lat,
                                centerLongitude = lon,
                                radiusMeters = rad
                            )
                        }

                        val verification = verificationResult.getOrNull()
                        if (verification == null || verification.verificationResult != "TRUE") {
                            val exception = verificationResult.exceptionOrNull()
                            Log.e("MainActivity", "Auth Flow: security verification failed for phoneNumber=$phoneNumber. Result=${verification?.verificationResult}", exception)
                            if (!isBypassNumber) {
                                authStatusMessage = "Security verification failed. Access denied."
                                allowDebugBypass("security verification did not pass")
                                if (boardingPassData != null) {
                                    Log.i("MainActivity", "Auth Flow: debug bypass allowed active session")
                                    return@LaunchedEffect
                                }
                                authIsError = true
                                authIsWorking = false
                                PassengerSessionStore.clear(context)
                                boardingPassData = null
                                pendingAuthData = null
                                return@LaunchedEffect
                            } else {
                                Log.w("MainActivity", "Auth Flow: security verification failed but test number bypass active, continuing", exception)
                            }
                        } else {
                            Log.i("MainActivity", "Auth Flow: security verification passed successfully")
                        }
                    }

                    val initialGroups = listOf(
                        FriendGroup(UUID.randomUUID().toString(), "Family Vacation", 4, "Nearby"),
                        FriendGroup(UUID.randomUUID().toString(), "Project Team", 3, "20m")
                    )

                    BackendClient.jwtToken = passengerToken
                    boardingPassData = data.copy(activeGroups = initialGroups)
                    authStatusMessage = if (isBypassNumber) "POC mode: debug bypass phone number detected" else "Passenger verified automatically."
                    Log.i("MainActivity", "Auth Flow Success: passenger verified. phone=$phoneNumber, bypass=$isBypassNumber")
                    navController.navigate("passenger_main") { popUpTo("auth") { inclusive = true } }

                    Log.d("MainActivity", "Auth Flow: syncing user tags to backend")
                    runCatching {
                        Way2LandApiClient.syncUserTags(
                            passengerToken = passengerToken,
                            tags = listOf(
                                "PASSENGER",
                                "FLIGHT_${data.carrier}_${data.flightNumber}",
                                "PNR_${data.pnr}"
                            ),
                            replace = false
                        )
                    }.onSuccess {
                        Log.i("MainActivity", "Auth Flow: user tags synced successfully to backend")
                    }.onFailure { exception ->
                        Log.e("MainActivity", "Auth Flow: syncUserTags failed", exception)
                    }

                    Log.d("MainActivity", "Auth Flow: fetching notifications from backend")
                    runCatching { Way2LandApiClient.getUserNotifications(passengerToken) }
                        .onSuccess { backendMessages ->
                            Log.i("MainActivity", "Auth Flow: successfully loaded ${backendMessages.size} notifications from backend")
                            boardingPassData = boardingPassData?.copy(notifications = backendMessages + (boardingPassData?.notifications.orEmpty()))
                        }
                        .onFailure { exception ->
                            Log.e("MainActivity", "Auth Flow: getUserNotifications failed", exception)
                        }

                    authIsWorking = false
                    pendingAuthData = null
                }

                NavHost(
                    navController = navController,
                    startDestination = "auth",
                    enterTransition = { fadeIn(tween(400)) + slideInHorizontally(tween(400)) { it } },
                    exitTransition = { fadeOut(tween(400)) + slideOutHorizontally(tween(400)) { -it } }
                ) {
                    composable("auth") {
                        AuthScreen(
                            statusMessage = authStatusMessage,
                            statusIsError = authIsError,
                            statusIsWorking = authIsWorking,
                            onPassengerAuthenticated = { data ->
                                pendingAuthData = data
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
                                onDeleteNotification = { id ->
                                    boardingPassData = data.copy(notifications = data.notifications.filter { it.id != id })
                                },
                                onCreateGroup = { name ->
                                    val newGroup = FriendGroup(UUID.randomUUID().toString(), name, 1, "0m")
                                    boardingPassData = data.copy(activeGroups = data.activeGroups + newGroup)
                                },
                                onDeleteGroup = { id ->
                                    val token = boardingPassData?.passengerToken
                                    if (token != null) {
                                        Log.i("MainActivity", "Leaving group: ID=$id")
                                        scope.launch {
                                            runCatching { Way2LandApiClient.leaveGroup(token, id) }
                                                .onSuccess { Log.i("MainActivity", "Successfully left group on backend: ID=$id") }
                                                .onFailure { exception -> Log.e("MainActivity", "Failed to leave group on backend: ID=$id", exception) }
                                        }
                                    }
                                    boardingPassData = data.copy(activeGroups = data.activeGroups.filter { it.id != id })
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
