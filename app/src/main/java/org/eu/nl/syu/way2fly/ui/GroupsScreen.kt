package org.eu.nl.syu.way2fly.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.eu.nl.syu.way2fly.model.BoardingPassData
import org.eu.nl.syu.way2fly.model.FriendGroup
import org.eu.nl.syu.way2fly.network.BackendApiException
import org.eu.nl.syu.way2fly.network.Way2LandApiClient

data class MockFriend(val name: String, val distance: String, val location: String, val color: Color)

@Composable
fun GroupsScreen(
    data: BoardingPassData,
    groups: List<FriendGroup>,
    onCreateGroup: (String) -> Unit,
    onDeleteGroup: (String) -> Unit,
    onRenameGroup: (String, String) -> Unit,
    onJoinGroup: (String) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var groupToRename by remember { mutableStateOf<FriendGroup?>(null) }
    var backendStatus by remember { mutableStateOf<String?>(null) }
    var backendError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val mockFriends = listOf(
        MockFriend("Mihai P.", "12m", "Sky Café", Color(0xFF2196F3)),
        MockFriend("Elena I.", "45m", "Duty Free", Color(0xFFE91E63)),
        MockFriend("Andrei D.", "120m", "Gate B2", Color(0xFF4CAF50)),
        MockFriend("Maria S.", "8m", "Business Lounge", Color(0xFFFF9800))
    )

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FloatingActionButton(
                    onClick = { showJoinDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Login, "Join Group")
                }
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, "Create Group")
                }
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background)) {
            Column(modifier = Modifier.fillMaxSize()) {
                GlobalHeroHeader(
                    title = "Groups",
                    subtitle = "Stay connected with friends",
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

                if (backendStatus != null) {
                    Text(
                        backendStatus!!,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (backendError != null) {
                    Text(
                        backendError!!,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            "Active Groups",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (groups.isEmpty()) {
                        item {
                            Text(
                                "No active groups. Create one!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }

                    items(groups) { group ->
                        GroupCard(
                            group = group,
                            onDelete = { onDeleteGroup(group.id) },
                            onRename = { groupToRename = group }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Friends Nearby",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    items(mockFriends) { friend ->
                        FriendCard(friend)
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        GroupActionDialog(
            title = "Create New Group",
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                val token = data.passengerToken
                if (token == null) {
                    backendError = "Authenticate first to create a backend group"
                    return@GroupActionDialog
                }

                scope.launch {
                    backendError = null
                    try {
                        val response = Way2LandApiClient.createGroup(token)
                        onCreateGroup(name)
                        backendStatus = "Created group ${response.groupId} with join code ${response.joinCode}"
                        showCreateDialog = false
                    } catch (exception: BackendApiException) {
                        backendError = "Create group failed (${exception.statusCode}): ${exception.message}"
                    } catch (exception: Exception) {
                        backendError = exception.message ?: "Create group failed"
                    }
                }
            }
        )
    }

    if (showJoinDialog) {
        GroupActionDialog(
            title = "Join Group",
            initialName = "",
            placeholder = "Join Code",
            onDismiss = { showJoinDialog = false },
            onConfirm = { joinCode ->
                val token = data.passengerToken
                if (token == null) {
                    backendError = "Authenticate first to join a backend group"
                    return@GroupActionDialog
                }

                scope.launch {
                    backendError = null
                    try {
                        val response = Way2LandApiClient.joinGroup(token, joinCode.trim())
                        onJoinGroup(response.groupId)
                        backendStatus = "Joined group ${response.groupId}"
                        showJoinDialog = false
                    } catch (exception: BackendApiException) {
                        backendError = "Join group failed (${exception.statusCode}): ${exception.message}"
                    } catch (exception: Exception) {
                        backendError = exception.message ?: "Join group failed"
                    }
                }
            }
        )
    }

    groupToRename?.let { group ->
        GroupActionDialog(
            title = "Rename Group",
            initialName = group.name,
            onDismiss = { groupToRename = null },
            onConfirm = { newName ->
                onRenameGroup(group.id, newName)
                groupToRename = null
            }
        )
    }
}

@Composable
fun GroupActionDialog(
    title: String,
    initialName: String = "",
    placeholder: String = "Group Name",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            TextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text(placeholder) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
                enabled = name.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun GroupCard(
    group: FriendGroup,
    onDelete: () -> Unit,
    onRename: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Groups, null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(group.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text("${group.memberCount} members active", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = {
                            showMenu = false
                            onRename()
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                    )
                }
            }
        }
    }
}

@Composable
fun FriendCard(friend: MockFriend) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(friend.color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    friend.name.take(1),
                    color = friend.color,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(friend.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Place, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(friend.location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(friend.distance, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary)
                Text("away", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
