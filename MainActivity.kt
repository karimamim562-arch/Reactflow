package com.reactflow.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val Blue = Color(0xFF2478F3)
private val Navy = Color(0xFF101827)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ReactFlowTheme { ReactFlowApp(intent?.data) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        setContent { ReactFlowTheme { ReactFlowApp(intent.data) } }
    }
}

@Composable
fun ReactFlowTheme(content: @Composable () -> Unit) {
    var dark by remember { mutableStateOf(false) }
    val colors = if (dark) darkColorScheme(
        primary = Blue, background = Navy, surface = Color(0xFF1D2B40)
    ) else lightColorScheme(primary = Blue, background = Color(0xFFF4F6FA))
    MaterialTheme(colorScheme = colors, content = content)
}

@Composable
fun ReactFlowApp(authUri: Uri?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf("Home") }
    var sessionId by remember { mutableStateOf<String?>(authUri?.getQueryParameter("session")) }
    var status by remember { mutableStateOf(if (sessionId == null) "Not connected" else "Connected session received") }
    var dark by remember { mutableStateOf(false) }
    var pages by remember { mutableStateOf<List<String>>(emptyList()) }
    var posts by remember { mutableStateOf<List<PostItem>>(emptyList()) }
    var message by remember { mutableStateOf("Connect a Facebook account to load authorized data.") }

    val api = remember { ReactFlowApi(BuildConfig.API_BASE_URL) }

    LaunchedEffect(sessionId) {
        val id = sessionId ?: return@LaunchedEffect
        try {
            pages = api.getPages(id)
            status = "Connected"
            message = "Loaded ${pages.size} managed Page(s)."
        } catch (e: Exception) {
            status = "Connection needs attention"
            message = e.message ?: "Could not load Pages."
        }
    }

    MaterialTheme(colorScheme = if (dark) darkColorScheme(primary = Blue) else lightColorScheme(primary = Blue)) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    listOf("Home", "Posts", "Analytics", "Settings").forEach { item ->
                        NavigationBarItem(
                            selected = tab == item,
                            onClick = { tab = item },
                            icon = {
                                Icon(
                                    when (item) {
                                        "Posts" -> Icons.Default.Article
                                        "Analytics" -> Icons.Default.Analytics
                                        "Settings" -> Icons.Default.Settings
                                        else -> Icons.Default.Home
                                    }, contentDescription = item
                                )
                            },
                            label = { Text(item) }
                        )
                    }
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text("ReactFlow", fontSize = 29.sp, fontWeight = FontWeight.Bold)
                    Text("Facebook Reaction Manager", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                when (tab) {
                    "Home" -> {
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = Navy)) {
                                Column(Modifier.fillMaxWidth().padding(22.dp)) {
                                    Text("ENGAGEMENT OVERVIEW", color = Color.LightGray, fontSize = 12.sp)
                                    Text(if (posts.isEmpty()) "—" else posts.size.toString(),
                                        color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Bold)
                                    Text("Accessible posts loaded", color = Color.LightGray)
                                }
                            }
                        }
                        item { Text("Accounts", fontSize = 20.sp, fontWeight = FontWeight.SemiBold) }
                        item { AccountCard("Personal Facebook", status) }
                        item { AccountCard("Managed Facebook Pages", if (pages.isEmpty()) "No Pages loaded" else "${pages.size} Page(s) found") }
                        item {
                            Button(
                                onClick = {
                                    val url = BuildConfig.API_BASE_URL + "auth/facebook?platform=android"
                                    CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(url))
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Connect Facebook") }
                        }
                        item { Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    "Posts" -> {
                        item { Text("Your accessible posts", fontSize = 21.sp, fontWeight = FontWeight.Bold) }
                        item {
                            Text("Choose a managed Page in the backend-connected version. Personal profile posts may be unavailable unless Meta grants the required permission.")
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    val id = sessionId
                                    if (id == null) message = "Connect Facebook first."
                                    else scope.launch {
                                        try {
                                            posts = api.getPosts(id, "pages")
                                            message = "Loaded ${posts.size} Page post(s)."
                                        } catch (e: Exception) { message = e.message ?: "Page post request failed." }
                                    }
                                }) { Text("Page posts") }
                                OutlinedButton(onClick = {
                                    val id = sessionId
                                    if (id == null) message = "Connect Facebook first."
                                    else scope.launch {
                                        try {
                                            posts = api.getPosts(id, "personal")
                                            message = "Loaded ${posts.size} accessible personal post(s)."
                                        } catch (e: Exception) { message = e.message ?: "Personal posts unavailable." }
                                    }
                                }) { Text("Personal posts") }
                            }
                        }
                        items(posts.size) { index ->
                            val post = posts[index]
                            PostCard(
                                post = post,
                                onOpen = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(post.permalink))
                                    context.startActivity(intent)
                                },
                                onStats = {
                                    val id = sessionId
                                    if (id != null) scope.launch {
                                        try {
                                            val count = api.getReactionCount(id, post.id, post.target)
                                            posts = posts.toMutableList().also {
                                                it[index] = post.copy(reactionCount = count)
                                            }
                                        } catch (e: Exception) {
                                            message = e.message ?: "Reaction data unavailable."
                                        }
                                    }
                                }
                            )
                        }
                        item { Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    "Analytics" -> {
                        item { Text("Reaction analytics", fontSize = 21.sp, fontWeight = FontWeight.Bold) }
                        item { Text("Reaction totals are populated from authorized Graph API responses when available. This starter does not fabricate counts.") }
                        item { Text("Like · Love · Haha · Wow · Sad · Angry", color = Blue) }
                        item { Text(message) }
                    }
                    else -> {
                        item { Text("Settings", fontSize = 21.sp, fontWeight = FontWeight.Bold) }
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Dark theme", modifier = Modifier.weight(1f))
                                Switch(checked = dark, onCheckedChange = { dark = it })
                            }
                        }
                        item {
                            OutlinedButton(onClick = {
                                sessionId = null
                                pages = emptyList()
                                posts = emptyList()
                                status = "Not connected"
                                message = "Disconnected locally. Revoke app access in Facebook settings if needed."
                            }) { Text("Disconnect this session") }
                        }
                        item { Text("Privacy: access tokens are held by the backend, not stored in this APK.") }
                    }
                }
            }
        }
    }
}

@Composable
fun AccountCard(name: String, status: String) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.SemiBold)
                Text(status, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
            Box(Modifier.size(9.dp).background(if (status == "Connected") Color(0xFF20A36A) else Color.Gray, RoundedCornerShape(50)))
        }
    }
}

@Composable
fun PostCard(post: PostItem, onOpen: () -> Unit, onStats: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(15.dp)) {
            Text(post.message.ifBlank { "Facebook post" }, maxLines = 4)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Open in Facebook  ↗",
                    color = Blue,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f).clickable(onClick = onOpen)
                )
                TextButton(onClick = onStats) { Text("Reaction count") }
            }
            post.reactionCount?.let {
                Text("Available reactions: $it", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
