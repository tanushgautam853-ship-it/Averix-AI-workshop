package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AmbientBackground
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(viewModel: MainViewModel, onNavigateNext: () -> Unit) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // 0: Log In, 1: Sign Up, 2: Forgot Password, 3: Guest Mode
    var authState by remember { mutableStateOf(0) }
    
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var guestName by remember { mutableStateOf("") }
    
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val googleLoading = remember { mutableStateOf(false) }

    // Read general theme values
    val isDark = true
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F172A), // Slate 900
            Color(0xFF020617)  // Slate 950
        )
    )

    AmbientBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBrush)
                .verticalScroll(scrollState)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp)
                    .padding(vertical = 32.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = Color(0xFF1E293B).copy(alpha = 0.9f) // Slate 800 with transparency
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Header Logo Icon
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF3B82F6), Color(0xFF8B5CF6))
                                ),
                                shape = RoundedCornerShape(20.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = "Averix AI",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Averix AI",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Command Center Workspace",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF94A3B8), // Slate 400
                            textAlign = TextAlign.Center
                        )
                    }

                    // Display error state elegantly
                    errorMessage?.let { msg ->
                        Surface(
                            color = Color(0xFFEF4444).copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Error, contentDescription = "Error", tint = Color(0xFFF87171))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFF87171),
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = Color(0xFFF87171),
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { errorMessage = null }
                                )
                            }
                        }
                    }

                    AnimatedContent(
                        targetState = authState,
                        transitionSpec = {
                            fadeIn() with fadeOut()
                        }
                    ) { state ->
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            when (state) {
                                0 -> { // EMAIL LOGIN
                                    Text(
                                        text = "Please authenticate to start session",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF94A3B8),
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center
                                    )

                                    OutlinedTextField(
                                        value = email,
                                        onValueChange = { email = it },
                                        label = { Text("Email Address", color = Color(0xFF94A3B8)) },
                                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF3B82F6)) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = Color(0xFF3B82F6),
                                            unfocusedBorderColor = Color(0xFF475569)
                                        ),
                                        modifier = Modifier.fillMaxWidth().testTag("email_input"),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                                    )

                                    OutlinedTextField(
                                        value = password,
                                        onValueChange = { password = it },
                                        label = { Text("Password", color = Color(0xFF94A3B8)) },
                                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF3B82F6)) },
                                        trailingIcon = {
                                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                                Icon(
                                                    imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                    contentDescription = "Toggle password visibility",
                                                    tint = Color(0xFF94A3B8)
                                                )
                                            }
                                        },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = Color(0xFF3B82F6),
                                            unfocusedBorderColor = Color(0xFF475569)
                                        ),
                                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        modifier = Modifier.fillMaxWidth().testTag("password_input"),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                                    )

                                    Text(
                                        text = "Forgot password?",
                                        color = Color(0xFF60A5FA),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier
                                            .align(Alignment.End)
                                            .clickable { authState = 2 }
                                    )

                                    Button(
                                        onClick = {
                                            if (email.isBlank() || password.isBlank()) {
                                                errorMessage = "All fields are required."
                                                return@Button
                                            }
                                            viewModel.login(
                                                name = email.substringBefore("@"),
                                                email = email.trim(),
                                                password = password,
                                                method = "email"
                                            ) { success, error ->
                                                if (success) {
                                                    onNavigateNext()
                                                } else {
                                                    errorMessage = error ?: "Log in failed. Check credentials."
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("email_login_button"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF3B82F6)
                                        )
                                    ) {
                                        Text("Log In Securely", fontWeight = FontWeight.Bold, color = Color.White)
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Don't have an account? ", color = Color(0xFF94A3B8), fontSize = 14.sp)
                                        Text(
                                            text = "Sign Up",
                                            color = Color(0xFF60A5FA),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            modifier = Modifier.clickable { authState = 1 }
                                        )
                                    }
                                }

                                1 -> { // EMAIL SIGN UP
                                    OutlinedTextField(
                                        value = displayName,
                                        onValueChange = { displayName = it },
                                        label = { Text("Display Name", color = Color(0xFF94A3B8)) },
                                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF3B82F6)) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = Color(0xFF3B82F6),
                                            unfocusedBorderColor = Color(0xFF475569)
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )

                                    OutlinedTextField(
                                        value = email,
                                        onValueChange = { email = it },
                                        label = { Text("Email Address", color = Color(0xFF94A3B8)) },
                                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF3B82F6)) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = Color(0xFF3B82F6),
                                            unfocusedBorderColor = Color(0xFF475569)
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                                    )

                                    OutlinedTextField(
                                        value = password,
                                        onValueChange = { password = it },
                                        label = { Text("Create Password (6+ char)", color = Color(0xFF94A3B8)) },
                                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF3B82F6)) },
                                        trailingIcon = {
                                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                                Icon(
                                                    imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                    contentDescription = null,
                                                    tint = Color(0xFF94A3B8)
                                                )
                                            }
                                        },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = Color(0xFF3B82F6),
                                            unfocusedBorderColor = Color(0xFF475569)
                                        ),
                                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                                    )

                                    OutlinedTextField(
                                        value = confirmPassword,
                                        onValueChange = { confirmPassword = it },
                                        label = { Text("Confirm Password", color = Color(0xFF94A3B8)) },
                                        leadingIcon = { Icon(Icons.Default.LockReset, contentDescription = null, tint = Color(0xFF3B82F6)) },
                                        trailingIcon = {
                                            IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                                                Icon(
                                                    imageVector = if (isConfirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                    contentDescription = null,
                                                    tint = Color(0xFF94A3B8)
                                                )
                                            }
                                        },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = Color(0xFF3B82F6),
                                            unfocusedBorderColor = Color(0xFF475569)
                                        ),
                                        visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                                    )

                                    Button(
                                        onClick = {
                                            if (email.isBlank() || password.isBlank() || displayName.isBlank()) {
                                                errorMessage = "All fields are required."
                                                return@Button
                                            }
                                            if (password != confirmPassword) {
                                                errorMessage = "Passwords do not match."
                                                return@Button
                                            }
                                            if (password.length < 6) {
                                                errorMessage = "Password must be at least 6 characters."
                                                return@Button
                                            }
                                            viewModel.login(
                                                name = displayName.trim(),
                                                email = email.trim(),
                                                password = password,
                                                method = "email"
                                            ) { success, error ->
                                                if (success) {
                                                    Toast.makeText(context, "Registration successful!", Toast.LENGTH_SHORT).show()
                                                    onNavigateNext()
                                                } else {
                                                    errorMessage = error ?: "Sign Up failed. Email might exist."
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(50.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF10B981) // Emerald 500
                                        )
                                    ) {
                                        Text("Register Command Account", fontWeight = FontWeight.Bold, color = Color.White)
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Already registered? ", color = Color(0xFF94A3B8), fontSize = 14.sp)
                                        Text(
                                            text = "Log In",
                                            color = Color(0xFF60A5FA),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            modifier = Modifier.clickable { authState = 0 }
                                        )
                                    }
                                }

                                2 -> { // PASSWORD RESET
                                    Text(
                                        text = "Reset Passkey link will be dispatched via email immediately.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF94A3B8),
                                        textAlign = TextAlign.Center
                                    )

                                    OutlinedTextField(
                                        value = email,
                                        onValueChange = { email = it },
                                        label = { Text("Registered Email Address", color = Color(0xFF94A3B8)) },
                                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF3B82F6)) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = Color(0xFF3B82F6),
                                            unfocusedBorderColor = Color(0xFF475569)
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                                    )

                                    Button(
                                        onClick = {
                                            if (email.isBlank()) {
                                                errorMessage = "Please specify a valid email."
                                                return@Button
                                            }
                                            viewModel.resetPassword(email.trim()) { success, err ->
                                                if (success) {
                                                    Toast.makeText(context, "Password reset dispatch complete!", Toast.LENGTH_LONG).show()
                                                    errorMessage = null
                                                    authState = 0
                                                } else {
                                                    errorMessage = err ?: "Unable to reset password."
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(50.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)) // Purple 500
                                    ) {
                                        Text("Send Reset Link", fontWeight = FontWeight.Bold, color = Color.White)
                                    }

                                    TextButton(
                                        onClick = { authState = 0 },
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    ) {
                                        Text("Return to Log In", color = Color(0xFF60A5FA))
                                    }
                                }

                                3 -> { // GUEST PASS/ANONYMOUS
                                    Text(
                                        text = "Guest session does not persist across endpoints but uses real Firebase Anonymous authentication.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF94A3B8),
                                        textAlign = TextAlign.Center
                                    )

                                    OutlinedTextField(
                                        value = guestName,
                                        onValueChange = { guestName = it },
                                        label = { Text("Guest Profile Handle/Alias", color = Color(0xFF94A3B8)) },
                                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF3B82F6)) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = Color(0xFF3B82F6),
                                            unfocusedBorderColor = Color(0xFF475569)
                                        ),
                                        modifier = Modifier.fillMaxWidth().testTag("guest_name_input"),
                                        singleLine = true
                                    )

                                    Button(
                                        onClick = {
                                            val finalGuestName = guestName.ifBlank { "Anonymous Commander" }
                                            viewModel.login(
                                                name = finalGuestName.trim(),
                                                email = "Guest",
                                                password = "",
                                                method = "guest"
                                            ) { success, error ->
                                                if (success) {
                                                    onNavigateNext()
                                                } else {
                                                    errorMessage = error ?: "Guest Login initialization failed."
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("guest_login_button"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)) // Slate 600
                                    ) {
                                        Text("Begin Guest Session", fontWeight = FontWeight.Bold, color = Color.White)
                                    }

                                    TextButton(
                                        onClick = { authState = 0 },
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    ) {
                                        Text("Back to secure login options", color = Color(0xFF60A5FA))
                                    }
                                }
                            }
                        }
                    }

                    // Separation Divider
                    if (authState == 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF334155))
                            Text(
                                "OR",
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = Color(0xFF64748B),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF334155))
                        }

                        // Google Sign-In and Guest Mode Triggers
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = {
                                    googleLoading.value = true
                                    // Robust Credential Manager or fallback notice
                                    Toast.makeText(context, "Google Sign In requires developer Google Console mapping. Proceeding via anonymous integration fallback.", Toast.LENGTH_LONG).show()
                                    viewModel.login(
                                        name = "Google User",
                                        email = "google_user@gmail.com",
                                        password = "",
                                        method = "guest"
                                    ) { success, err ->
                                        googleLoading.value = false
                                        if (success) {
                                            onNavigateNext()
                                        } else {
                                            errorMessage = err ?: "Google auto alignment failed."
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.White
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow, // Icon replacement representing Play Services auth
                                        contentDescription = "Google Play Icon",
                                        tint = Color(0xFFEF4444)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Sign In with Google", fontWeight = FontWeight.SemiBold)
                                }
                            }

                            OutlinedButton(
                                onClick = { authState = 3 },
                                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("guest_mode_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.White
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.PersonOutline, contentDescription = "Guest", tint = Color(0xFF60A5FA))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Enter Isolated Guest Mode", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
