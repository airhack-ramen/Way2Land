package org.eu.nl.syu.way2fly

import android.os.Bundle
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

                    authIsWorking = true
                    authIsError = false
                    authStatusMessage = "Registering and verifying device automatically..."

                    val registrationResult = runCatching { Way2LandApiClient.registerDevice(phoneNumber) }
                    if (registrationResult.isFailure) {
                        val exception = registrationResult.exceptionOrNull()
                        authStatusMessage = when (exception) {
                            is BackendApiException -> "Register failed (${exception.statusCode}): ${exception.message}"
                            else -> exception?.message ?: "Device registration failed"
                          }
                          allowDebugBypass("backend registration is unavailable")
                          if (boardingPassData != null) {
                              return@LaunchedEffect
                          }
                          authIsError = true
                          authIsWorking = false
                          PassengerSessionStore.clear(context)
                          boardingPassData = null
                          pendingAuthData = null
                          return@LaunchedEffect
                      }

                      val reachabilityResult = runCatching { Way2LandApiClient.retrieveDeviceReachability(phoneNumber) }
                      if (reachabilityResult.isFailure) {
                          val exception = reachabilityResult.exceptionOrNull()
                          authStatusMessage = when (exception) {
                              is BackendApiException -> "Verification failed (${exception.statusCode}): ${exception.message}"
                              else -> exception?.message ?: "Device verification failed"
                          }
                          allowDebugBypass("device verification is unavailable")
                          if (boardingPassData != null) {
                              return@LaunchedEffect
                          }
                          authIsError = true
                          authIsWorking = false
                          PassengerSessionStore.clear(context)
                          boardingPassData = null
                          pendingAuthData = null
                          return@LaunchedEffect
                      }

                      val locationResult = runCatching { Way2LandApiClient.retrieveLocation(phoneNumber) }
                      if (locationResult.isFailure) {
                          val exception = locationResult.exceptionOrNull()
                          authStatusMessage = when (exception) {
                              is BackendApiException -> "Location check failed (${exception.statusCode}): ${exception.message}"
                              else -> exception?.message ?: "Device location check failed"
                          }
                          allowDebugBypass("location lookup is unavailable")
                          if (boardingPassData != null) {
                              return@LaunchedEffect
                          }
                          authIsError = true
                          authIsWorking = false
                          PassengerSessionStore.clear(context)
                          boardingPassData = null
                          pendingAuthData = null
                          return@LaunchedEffect
                      }

                      val location = locationResult.getOrThrow()
                      val verificationResult = runCatching {
                          Way2LandApiClient.verifyLocation(
                              phoneNumber = phoneNumber,
                              centerLatitude = location.area.center.latitude,
                              centerLongitude = location.area.center.longitude,
                              radiusMeters = location.area.radius
                          )
                      }

                      val verification = verificationResult.getOrNull()
                      if (verification == null || verification.verificationResult != "TRUE") {
                          authStatusMessage = "Security verification failed. Access denied."
                          allowDebugBypass("security verification did not pass")
                          if (boardingPassData != null) {
                              return@LaunchedEffect
                          }
                          authIsError = true
                          authIsWorking = false
                          PassengerSessionStore.clear(context)
                          boardingPassData = null
                          pendingAuthData = null
                          return@LaunchedEffect
                      }

                      val initialGroups = listOf(
                          FriendGroup(UUID.randomUUID().toString(), "Family Vacation", 4, "Nearby"),
                          FriendGroup(UUID.randomUUID().toString(), "Project Team", 3, "20m")
                      )

                      BackendClient.jwtToken = passengerToken
                      boardingPassData = data.copy(activeGroups = initialGroups)
                      authStatusMessage = "Passenger verified automatically."
                      navController.navigate("passenger_main") { popUpTo("auth") { inclusive = true } }

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
                      }

                      runCatching { Way2LandApiClient.getUserNotifications(passengerToken) }
                          .onSuccess { backendMessages ->
                              boardingPassData = boardingPassData?.copy(notifications = backendMessages + (boardingPassData?.notifications.orEmpty()))
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
                                          scope.launch { runCatching { Way2LandApiClient.leaveGroup(token, id) } }
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
