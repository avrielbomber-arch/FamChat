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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale
import kotlin.random.Random

private val Navy = Color(0xFF0B1220)
private val Panel = Color(0xFF131F33)
private val Blue = Color(0xFF3987FF)
private val Muted = Color(0xFF91A0B7)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                FamChatApp()
            }
        }
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

    if (currentUser == null) {
        AuthScreen(auth)
    } else {
        ProfileScreen(auth, currentUser!!.uid)
    }
}

@Composable
private fun AuthScreen(auth: FirebaseAuth) {
    var isRegister by remember { mutableStateOf(true) }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = Navy, contentColor = Color.White) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 30.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(76.dp).background(Blue.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("F", color = Blue, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold)
            }
            Text("FamChat", modifier = Modifier.padding(top = 16.dp), fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text("Tetap dekat, di mana pun.", color = Muted, fontSize = 13.sp)
            Text(
                if (isRegister) "Buat akun baru" else "Selamat datang kembali",
                modifier = Modifier.padding(top = 30.dp, bottom = 16.dp),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )

            if (isRegister) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Username") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    colors = fieldColors()
                )
                Spacer(Modifier.size(10.dp))
            }
            OutlinedTextField(
                value = email,
                onValueChange = { email = it.trim() },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Default.Mail, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                colors = fieldColors()
            )
            Spacer(Modifier.size(10.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Password (minimal 6 karakter)") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                colors = fieldColors()
            )

            if (message.isNotBlank()) {
                Text(message, modifier = Modifier.fillMaxWidth().padding(top = 12.dp), color = Color(0xFFFFB4AB), fontSize = 13.sp)
            }

            Button(
                onClick = {
                    val cleanName = username.trim().lowercase(Locale.ROOT)
                    if (email.isBlank() || password.length < 6 || (isRegister && cleanName.length < 3)) {
                        message = if (isRegister) "Isi email, username minimal 3 karakter, dan password minimal 6 karakter." else "Isi email dan password minimal 6 karakter."
                    } else {
                        loading = true
                        message = ""
                        if (isRegister) {
                            auth.createUserWithEmailAndPassword(email.trim(), password)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val user = auth.currentUser
                                        if (user != null) {
                                            val publicId = generatePublicId()
                                            val profile = hashMapOf(
                                                "uid" to user.uid,
                                                "username" to cleanName,
                                                "email" to email.trim(),
                                                "publicId" to publicId,
                                                "createdAt" to System.currentTimeMillis()
                                            )
                                            FirebaseFirestore.getInstance().collection("users")
                                                .document(user.uid).set(profile)
                                                .addOnCompleteListener { saveTask ->
                                                    loading = false
                                                    if (!saveTask.isSuccessful) {
                                                        message = "Akun dibuat, tetapi profil gagal disimpan. Periksa Firestore."
                                                    }
                                                }
                                        } else {
                                            loading = false
                                            message = "Akun dibuat. Silakan login kembali."
                                        }
                                    } else {
                                        loading = false
                                        message = task.exception?.localizedMessage ?: "Registrasi gagal."
                                    }
                                }
                        } else {
                            auth.signInWithEmailAndPassword(email.trim(), password)
                                .addOnCompleteListener { task ->
                                    loading = false
                                    if (!task.isSuccessful) {
                                        message = task.exception?.localizedMessage ?: "Login gagal."
                                    }
                                }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                enabled = !loading,
                colors = ButtonDefaults.buttonColors(containerColor = Blue),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                else Text(if (isRegister) "Daftar" else "Masuk", modifier = Modifier.padding(vertical = 5.dp))
            }

            TextButton(onClick = {
                isRegister = !isRegister
                message = ""
            }) {
                Text(if (isRegister) "Sudah punya akun? Masuk" else "Belum punya akun? Daftar", color = Color(0xFF8DB8FF))
            }
        }
    }
}

@Composable
private fun ProfileScreen(auth: FirebaseAuth, uid: String) {
    var username by remember(uid) { mutableStateOf("Memuat profil...") }
    var publicId by remember(uid) { mutableStateOf("...") }

    LaunchedEffect(uid) {
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                username = doc.getString("username") ?: auth.currentUser?.email ?: "Pengguna FamChat"
                publicId = doc.getString("publicId") ?: "Belum tersedia"
            }
            .addOnFailureListener {
                username = auth.currentUser?.email ?: "Pengguna FamChat"
                publicId = "Profil belum tersinkron"
            }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Navy, contentColor = Color.White) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("FamChat", fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text("Akun berhasil masuk", color = Color(0xFF8DB8FF), modifier = Modifier.padding(top = 8.dp))
            Text(username, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 28.dp))
            Text("ID FamChat: $publicId", color = Muted, modifier = Modifier.padding(top = 8.dp))
            Text(auth.currentUser?.email ?: "", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 5.dp))
            Button(
                onClick = { auth.signOut() },
                modifier = Modifier.fillMaxWidth().padding(top = 30.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Blue),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Keluar")
            }
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = Blue,
    unfocusedBorderColor = Muted,
    focusedLabelColor = Color(0xFF8DB8FF),
    unfocusedLabelColor = Muted,
    cursorColor = Blue,
    focusedLeadingIconColor = Blue,
    unfocusedLeadingIconColor = Muted
)

private fun generatePublicId(): String = (10000000 + Random.nextInt(90000000)).toString()
