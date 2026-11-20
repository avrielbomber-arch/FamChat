package com.famchat.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class ChatMessage(
    val id: String,
    val senderUid: String,
    val text: String,
    val createdAt: Date?,
    val readBy: List<String>
)

@Composable
internal fun ChatScreen(uid: String, friend: PersonRow, onBack: () -> Unit, isDark: Boolean = true) {
    BackHandler(enabled = true) { onBack() }
    val bg = if (isDark) Color(0xFF0B1220) else Color(0xFFF3F6FC)
    val fg = if (isDark) Color.White else Color(0xFF17243A)
    val incomingBubble = if (isDark) Color(0xFF17263C) else Color.White
    val mutedText = if (isDark) Color(0xFFCAD7EA) else Color(0xFF68758A)
    val db = remember { FirebaseFirestore.getInstance() }
    val members = remember(uid, friend.uid) { listOf(uid, friend.uid).sorted() }
    val chatId = remember(members) { members.joinToString("_") }
    val chatRef = remember(chatId) { db.collection("chats").document(chatId) }
    var messages by remember(chatId) { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var draft by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Menghubungkan...") }
    var sending by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    DisposableEffect(chatId) {
        var registration: ListenerRegistration? = null

        fun listenForMessages() {
            registration = chatRef.collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        status = error.localizedMessage ?: "Gagal memuat pesan."
                    } else if (snapshot != null) {
                        val rows = snapshot.documents.map { d ->
                            ChatMessage(
                                id = d.id,
                                senderUid = d.getString("senderUid") ?: "",
                                text = d.getString("text") ?: "",
                                createdAt = d.getTimestamp("createdAt")?.toDate(),
                                readBy = (d.get("readBy") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                            )
                        }
                        messages = rows
                        status = ""
                        val unread = snapshot.documents.filter { d ->
                            d.getString("senderUid") != uid &&
                                !((d.get("readBy") as? List<*>)?.contains(uid) ?: false)
                        }
                        if (unread.isNotEmpty()) {
                            val batch = db.batch()
                            unread.forEach { d -> batch.update(d.reference, "readBy", FieldValue.arrayUnion(uid)) }
                            batch.commit()
                        }
                    }
                }
        }

        chatRef.get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                listenForMessages()
            } else {
                chatRef.set(
                    mapOf(
                        "members" to members,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "lastMessage" to "",
                        "lastMessageAt" to FieldValue.serverTimestamp()
                    )
                ).addOnSuccessListener {
                    // Start listening only after the parent chat document exists.
                    listenForMessages()
                }.addOnFailureListener { e ->
                    status = e.localizedMessage ?: "Gagal membuat percakapan."
                }
            }
        }.addOnFailureListener { e ->
            status = e.localizedMessage ?: "Tidak dapat membuka percakapan."
        }

        onDispose { registration?.remove() }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Surface(Modifier.fillMaxSize(), color = bg, contentColor = fg) {
        Column(Modifier.fillMaxSize().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onBack) { Text("Kembali", color = Color(0xFF3987FF)) }
                Column(Modifier.weight(1f)) {
                    Text(friend.username, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                    Text("Percakapan pribadi", color = mutedText, fontSize = 12.sp)
                }
            }
            HorizontalDivider(color = if (isDark) Color(0xFF26364F) else Color(0xFFDCE4F0))
            if (status.isNotBlank()) {
                Text(status, color = Color(0xFF91A0B7), fontSize = 12.sp, modifier = Modifier.padding(8.dp))
            }
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    val mine = msg.senderUid == uid
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
                        Column(
                            Modifier.widthIn(max = 290.dp)
                                .background(if (mine) Color(0xFF245EB8) else incomingBubble, RoundedCornerShape(16.dp))
                                .padding(horizontal = 13.dp, vertical = 9.dp)
                        ) {
                            Text(msg.text, fontSize = 15.sp)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(msg.createdAt?.let { SimpleDateFormat("HH:mm", Locale("id", "ID")).format(it) } ?: "…", color = mutedText, fontSize = 10.sp)
                                if (mine) {
                                    val read = msg.readBy.contains(friend.uid)
                                    Text(if (read) "Dibaca" else "Terkirim", color = Color(0xFFCAD7EA), fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Tulis pesan...") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = fg,
                        unfocusedTextColor = fg,
                        focusedBorderColor = Color(0xFF3987FF),
                        unfocusedBorderColor = mutedText,
                        cursorColor = Color(0xFF3987FF)
                    )
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    enabled = draft.isNotBlank() && !sending,
                    onClick = {
                        val text = draft.trim()
                        if (text.isNotEmpty()) {
                            sending = true
                            val messageData = mapOf(
                                "senderUid" to uid,
                                "text" to text,
                                "createdAt" to FieldValue.serverTimestamp(),
                                "readBy" to listOf(uid)
                            )
                            chatRef.collection("messages").add(messageData)
                                .addOnSuccessListener {
                                    chatRef.update(
                                        mapOf(
                                            "lastMessage" to text,
                                            "lastMessageAt" to FieldValue.serverTimestamp()
                                        )
                                    )
                                    draft = ""
                                }
                                .addOnFailureListener { e -> status = e.localizedMessage ?: "Pesan gagal dikirim." }
                                .addOnCompleteListener { sending = false }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3987FF)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp)
                ) {
                    Text(if (sending) "..." else "Kirim")
                }
            }
        }
    }
}
