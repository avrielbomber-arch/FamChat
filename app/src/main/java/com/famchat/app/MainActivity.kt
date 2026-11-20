package com.famchat.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.Locale
import kotlin.random.Random

private val Navy = Color(0xFF0B1220)
private val Panel = Color(0xFF131F33)
private val Blue = Color(0xFF3987FF)
private val Muted = Color(0xFF91A0B7)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { FamChatApp() } }
    }
}

@Composable
private fun FamChatApp() {
    val auth = remember { FirebaseAuth.getInstance() }
    var currentUser by remember { mutableStateOf(auth.currentUser) }
    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { currentUser = it.currentUser }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }
    if (currentUser == null) AuthScreen(auth) else HomeScreen(auth, currentUser!!.uid)
}

@Composable
private fun AuthScreen(auth: FirebaseAuth) {
    var isRegister by remember { mutableStateOf(true) }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    Surface(Modifier.fillMaxSize(), color = Navy, contentColor = Color.White) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("FamChat", fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text("Tetap dekat, di mana pun.", color = Muted)
            Text(if (isRegister) "Buat akun baru" else "Selamat datang kembali", Modifier.padding(top = 28.dp, bottom = 16.dp), fontSize = 20.sp)
            if (isRegister) {
                OutlinedTextField(username, { username = it }, Modifier.fillMaxWidth(), label = { Text("Username") }, leadingIcon = { Icon(Icons.Default.Person, null) }, singleLine = true, colors = fieldColors())
                Spacer(Modifier.height(10.dp))
            }
            OutlinedTextField(email, { email = it.trim() }, Modifier.fillMaxWidth(), label = { Text("Email") }, leadingIcon = { Icon(Icons.Default.Mail, null) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), singleLine = true, colors = fieldColors())
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("Password (minimal 6 karakter)") }, leadingIcon = { Icon(Icons.Default.Lock, null) }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), singleLine = true, colors = fieldColors())
            if (message.isNotBlank()) Text(message, Modifier.fillMaxWidth().padding(top = 12.dp), color = Color(0xFFFFB4AB), fontSize = 13.sp)
            Button(onClick = {
                val cleanName = username.trim().lowercase(Locale.ROOT)
                if (email.isBlank() || password.length < 6 || (isRegister && cleanName.length < 3)) {
                    message = "Isi email, username minimal 3 karakter, dan password minimal 6 karakter."
                } else {
                    loading = true; message = ""
                    if (isRegister) {
                        auth.createUserWithEmailAndPassword(email.trim(), password).addOnCompleteListener { task ->
                            if (!task.isSuccessful) { loading = false; message = task.exception?.localizedMessage ?: "Registrasi gagal." }
                            else {
                                val user = auth.currentUser
                                if (user == null) { loading = false; message = "Akun dibuat. Silakan login kembali." }
                                else {
                                    val db = FirebaseFirestore.getInstance()
                                    val publicId = generatePublicId()
                                    val profile = mapOf("uid" to user.uid, "username" to cleanName, "email" to email.trim(), "publicId" to publicId, "createdAt" to System.currentTimeMillis())
                                    val userRef = db.collection("users").document(user.uid)
                                    val usernameRef = db.collection("usernames").document(cleanName)
                                    val idRef = db.collection("publicIds").document(publicId)
                                    db.runTransaction { tr ->
                                        if (tr.get(usernameRef).exists()) throw IllegalStateException("Username sudah digunakan. Pilih username lain.")
                                        if (tr.get(idRef).exists()) throw IllegalStateException("Terjadi benturan ID. Silakan daftar kembali.")
                                        tr.set(usernameRef, mapOf("uid" to user.uid))
                                        tr.set(idRef, mapOf("uid" to user.uid))
                                        tr.set(userRef, profile)
                                        tr.set(db.collection("publicProfiles").document(user.uid), mapOf("uid" to user.uid, "username" to cleanName, "publicId" to publicId))
                                        null
                                    }.addOnCompleteListener { save ->
                                        loading = false
                                        if (!save.isSuccessful) message = save.exception?.localizedMessage ?: "Profil gagal disimpan."
                                    }
                                }
                            }
                        }
                    } else {
                        auth.signInWithEmailAndPassword(email.trim(), password).addOnCompleteListener { task ->
                            loading = false
                            if (!task.isSuccessful) message = task.exception?.localizedMessage ?: "Login gagal."
                        }
                    }
                }
            }, Modifier.fillMaxWidth().padding(top = 18.dp), enabled = !loading, colors = ButtonDefaults.buttonColors(containerColor = Blue), shape = RoundedCornerShape(14.dp)) {
                if (loading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp) else Text(if (isRegister) "Daftar" else "Masuk")
            }
            TextButton(onClick = { isRegister = !isRegister; message = "" }) {
                Text(if (isRegister) "Sudah punya akun? Masuk" else "Belum punya akun? Daftar", color = Color(0xFF8DB8FF))
            }
        }
    }
}

private data class PersonRow(val uid: String, val username: String, val publicId: String)
private data class RequestRow(val id: String, val fromUid: String, val username: String, val publicId: String)

@Composable
private fun HomeScreen(auth: FirebaseAuth, uid: String) {
    val db = remember { FirebaseFirestore.getInstance() }
    var username by remember(uid) { mutableStateOf("Memuat profil...") }
    var publicId by remember(uid) { mutableStateOf("...") }
    var tab by remember { mutableStateOf(0) }
    var searchId by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<PersonRow?>(null) }
    var friends by remember { mutableStateOf<List<PersonRow>>(emptyList()) }
    var requests by remember { mutableStateOf<List<RequestRow>>(emptyList()) }
    var message by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var selectedChat by remember { mutableStateOf<PersonRow?>(null) }

    fun refreshLists() {
        db.collection("friendRequests").whereEqualTo("toUid", uid).whereEqualTo("status", "pending").get()
            .addOnSuccessListener { snapshot ->
                val rows = snapshot.documents.map { doc ->
                    RequestRow(doc.id, doc.getString("fromUid") ?: "", doc.getString("fromUsername") ?: "Pengguna FamChat", doc.getString("fromPublicId") ?: "")
                }
                requests = rows
            }
        db.collection("friendships").whereArrayContains("members", uid).get().addOnSuccessListener { snapshot ->
            val otherUids = snapshot.documents.mapNotNull { doc -> (doc.get("members") as? List<*>)?.filterIsInstance<String>()?.firstOrNull { it != uid } }.distinct()
            if (otherUids.isEmpty()) friends = emptyList()
            else {
                val loaded = mutableListOf<PersonRow>()
                var remaining = otherUids.size
                otherUids.forEach { other ->
                    db.collection("publicProfiles").document(other).get().addOnSuccessListener { p ->
                        loaded.add(PersonRow(other, p.getString("username") ?: "Pengguna", p.getString("publicId") ?: ""))
                    }.addOnCompleteListener {
                        remaining--
                        if (remaining == 0) friends = loaded.toList()
                    }
                }
            }
        }
    }

    LaunchedEffect(uid) {
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            username = doc.getString("username") ?: auth.currentUser?.email ?: "Pengguna FamChat"
            publicId = doc.getString("publicId") ?: "Belum tersedia"
            if (doc.exists() && publicId != "Belum tersedia") {
                db.collection("publicProfiles").document(uid).set(mapOf("uid" to uid, "username" to username, "publicId" to publicId))
            }
        }
        refreshLists()
    }

    if (selectedChat != null) {
        val person = selectedChat!!
        ChatScreen(uid = uid, friend = person, onBack = { selectedChat = null })
    } else Surface(Modifier.fillMaxSize(), color = Navy, contentColor = Color.White) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Text("FamChat", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Halo, $username", color = Color(0xFF8DB8FF), modifier = Modifier.padding(top = 4.dp))
            Text("ID FamChat: $publicId", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(20.dp))
            TabRow(selectedTabIndex = tab, containerColor = Panel, contentColor = Blue) {
                listOf("Cari", "Permintaan", "Teman").forEachIndexed { index, label -> Tab(selected = tab == index, onClick = { tab = index; message = ""; refreshLists() }, text = { Text(label) }) }
            }
            Spacer(Modifier.height(16.dp))
            when (tab) {
                0 -> {
                    Text("Cari teman dengan Public ID", fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(searchId, { searchId = it.filter(Char::isDigit).take(8); result = null }, Modifier.fillMaxWidth().padding(top = 10.dp), label = { Text("Masukkan 8 digit Public ID") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, colors = fieldColors())
                    Button(onClick = {
                        if (searchId.length != 8) message = "Public ID harus terdiri dari 8 angka."
                        else {
                            busy = true; message = ""; result = null
                            db.collection("publicIds").document(searchId).get().addOnSuccessListener { idDoc ->
                                val foundUid = idDoc.getString("uid")
                                if (foundUid.isNullOrBlank()) { message = "Pengguna tidak ditemukan."; busy = false }
                                else if (foundUid == uid) { message = "Itu adalah ID akun kamu sendiri."; busy = false }
                                else db.collection("publicProfiles").document(foundUid).get().addOnSuccessListener { p ->
                                    if (p.exists()) result = PersonRow(foundUid, p.getString("username") ?: "Pengguna", p.getString("publicId") ?: searchId)
                                    else message = "Profil pengguna belum tersedia."
                                }.addOnCompleteListener { busy = false }
                            }.addOnFailureListener { e -> message = e.localizedMessage ?: "Pencarian gagal."; busy = false }
                        }
                    }, Modifier.fillMaxWidth().padding(top = 10.dp), enabled = !busy, colors = ButtonDefaults.buttonColors(containerColor = Blue)) { Text(if (busy) "Mencari..." else "Cari pengguna") }
                    result?.let { person ->
                        Card(Modifier.fillMaxWidth().padding(top = 14.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
                            Column(Modifier.padding(16.dp)) {
                                Text(person.username, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text("ID: ${person.publicId}", color = Muted)
                                Button(onClick = {
                                    busy = true; message = ""
                                    val requestId = "${uid}_${person.uid}"
                                    val ref = db.collection("friendRequests").document(requestId)
                                    ref.set(mapOf("fromUid" to uid, "toUid" to person.uid, "fromUsername" to username, "fromPublicId" to publicId, "status" to "pending", "createdAt" to FieldValue.serverTimestamp()))
                                        .addOnSuccessListener { message = "Permintaan pertemanan terkirim." }
                                        .addOnFailureListener { e ->
                                            message = if (e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true) {
                                                "Permintaan sudah pernah dibuat atau tidak diizinkan."
                                            } else e.localizedMessage ?: "Gagal mengirim permintaan."
                                        }
                                        .addOnCompleteListener { busy = false }
                                }, enabled = !busy, colors = ButtonDefaults.buttonColors(containerColor = Blue)) { Text("Tambah teman") }
                            }
                        }
                    }
                }
                1 -> {
                    Text("Permintaan pertemanan masuk", fontWeight = FontWeight.SemiBold)
                    if (requests.isEmpty()) Text("Belum ada permintaan pertemanan.", color = Muted, modifier = Modifier.padding(top = 12.dp))
                    LazyColumn {
                        items(requests, key = { it.id }) { request ->
                            Card(Modifier.fillMaxWidth().padding(vertical = 6.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
                                Column(Modifier.padding(14.dp)) {
                                    Text(request.username, fontWeight = FontWeight.Bold)
                                    Text("ID: ${request.publicId}", color = Muted, fontSize = 13.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = {
                                            val requestRef = db.collection("friendRequests").document(request.id)
                                            val members = listOf(uid, request.fromUid).sorted()
                                            val friendshipId = members.joinToString("_")
                                            db.runTransaction { tr ->
                                                val snap = tr.get(requestRef)
                                                if (!snap.exists() || snap.getString("status") != "pending") throw IllegalStateException("Permintaan sudah tidak tersedia.")
                                                tr.update(requestRef, "status", "accepted")
                                                tr.set(db.collection("friendships").document(friendshipId), mapOf("members" to members, "requestId" to request.id, "createdAt" to FieldValue.serverTimestamp()))
                                                null
                                            }.addOnSuccessListener { message = "Permintaan diterima."; refreshLists() }
                                                .addOnFailureListener { e -> message = e.localizedMessage ?: "Gagal menerima permintaan." }
                                        }, colors = ButtonDefaults.buttonColors(containerColor = Blue)) { Text("Terima") }
                                        OutlinedButton(onClick = {
                                            db.collection("friendRequests").document(request.id).update("status", "rejected")
                                                .addOnSuccessListener { message = "Permintaan ditolak."; refreshLists() }
                                                .addOnFailureListener { e -> message = e.localizedMessage ?: "Gagal menolak permintaan." }
                                        }) { Text("Tolak") }
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {
                    Text("Daftar teman", fontWeight = FontWeight.SemiBold)
                    if (friends.isEmpty()) Text("Kamu belum memiliki teman.", color = Muted, modifier = Modifier.padding(top = 12.dp))
                    LazyColumn {
                        items(friends, key = { it.uid }) { person ->
                            Card(Modifier.fillMaxWidth().padding(vertical = 5.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
                                Column(Modifier.padding(14.dp)) {
                                    Text(person.username, fontWeight = FontWeight.Bold)
                                    Text("ID: ${person.publicId}", color = Muted, fontSize = 13.sp)
                                    Button(onClick = { selectedChat = person }, modifier = Modifier.padding(top = 8.dp), colors = ButtonDefaults.buttonColors(containerColor = Blue)) {
                                        Text("Buka chat")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (message.isNotBlank()) Text(message, color = Color(0xFF9CC2FF), modifier = Modifier.padding(top = 12.dp), fontSize = 13.sp)
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = { auth.signOut() }, Modifier.fillMaxWidth()) { Text("Keluar") }
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
    focusedBorderColor = Blue, unfocusedBorderColor = Muted,
    focusedLabelColor = Color(0xFF8DB8FF), unfocusedLabelColor = Muted,
    cursorColor = Blue, focusedLeadingIconColor = Blue, unfocusedLeadingIconColor = Muted
)

private fun generatePublicId(): String = (10000000 + Random.nextInt(90000000)).toString()
