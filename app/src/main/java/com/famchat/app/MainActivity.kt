package com.famchat.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Navy = Color(0xFF0B1220)
private val Panel = Color(0xFF131F33)
private val Blue = Color(0xFF3987FF)
private val Muted = Color(0xFF91A0B7)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                FamChatHome()
            }
        }
    }
}

private data class ChatPreview(
    val name: String,
    val message: String,
    val time: String,
    val initials: String,
    val unread: Int = 0
)

private val sampleChats = listOf(
    ChatPreview("Keluarga", "Jangan lupa makan malam bersama.", "19:42", "KL", 2),
    ChatPreview("Andi", "Oke, nanti aku kabari lagi.", "18:15", "AN"),
    ChatPreview("Rina", "Foto tadi bagus banget 😄", "Kemarin", "RI", 1),
    ChatPreview("Budi", "Makasih sudah membantu!", "Senin", "BU")
)

@Composable
private fun FamChatHome() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Navy,
        contentColor = Color.White
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("FamChat", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text("Tetap dekat, di mana pun.", color = Muted, fontSize = 13.sp)
                }
                Surface(shape = CircleShape, color = Panel) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Cari",
                        tint = Color.White,
                        modifier = Modifier.padding(12.dp).size(22.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Pesan", fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text("Contoh tampilan", color = Muted, fontSize = 12.sp)
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sampleChats) { chat -> ChatRow(chat) }
            }

            Box(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                FloatingActionButton(
                    onClick = { },
                    containerColor = Blue,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Pesan baru")
                }
            }
        }
    }
}

@Composable
private fun ChatRow(chat: ChatPreview) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Panel, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(52.dp).background(Blue.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(chat.initials, color = Color(0xFF8DB8FF), fontWeight = FontWeight.Bold)
        }
        Column(modifier = Modifier.weight(1f).padding(start = 13.dp)) {
            Text(chat.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(chat.message, color = Muted, fontSize = 13.sp, maxLines = 1)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(chat.time, color = Muted, fontSize = 11.sp)
            if (chat.unread > 0) {
                Surface(color = Blue, shape = CircleShape) {
                    Text(
                        chat.unread.toString(),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = Muted, modifier = Modifier.size(15.dp))
            }
        }
    }
}
