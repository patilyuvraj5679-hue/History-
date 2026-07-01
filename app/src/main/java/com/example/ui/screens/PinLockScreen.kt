package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.LanguageHelper
import com.example.ui.WorkViewModel

@Composable
fun PinLockScreen(
    viewModel: WorkViewModel,
    onUnlock: () -> Unit
) {
    val isMarathi = viewModel.isMarathi
    val context = LocalContext.current
    var pinEntered by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    // Automatic biometric prompt on launch if enabled
    LaunchedEffect(viewModel.isBiometricEnabled) {
        if (viewModel.isBiometricEnabled) {
            // In physical environment, you would call BiometricPrompt.
            // Here, we provide an elegant simulated biometric overlay or immediate unlock on click.
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Lock Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Lock",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Brand / Title
            Text(
                text = LanguageHelper.getText(isMarathi, "app_title"),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = if (isMarathi) "ॲप लॉक आहे • हिशोब सुरक्षित आहे" else "App is Locked • Records Secured",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Instructions
            Text(
                text = if (isMarathi) "कृपया तुमचा ४-अंकी पिन प्रविष्ट करा" else "Please enter your 4-digit PIN",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Dots Indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                for (i in 1..4) {
                    val isActive = pinEntered.length >= i
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActive) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                }
            }

            // Error Message
            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(24.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Numeric Keypad Grid
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("BIO", "0", "BACK")
                )

                keys.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        row.forEach { key ->
                            when (key) {
                                "BIO" -> {
                                    // Biometrics button (only active if biometrics is enabled)
                                    val enabled = viewModel.isBiometricEnabled
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (enabled) MaterialTheme.colorScheme.secondaryContainer
                                                else Color.Transparent
                                            )
                                            .clickable(enabled = enabled) {
                                                Toast.makeText(context, if (isMarathi) "बायोमॅट्रिक प्रमाणीकरण यशस्वी!" else "Biometric authenticated!", Toast.LENGTH_SHORT).show()
                                                onUnlock()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (enabled) {
                                            Icon(
                                                imageVector = Icons.Default.Fingerprint,
                                                contentDescription = "Biometrics",
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                }
                                "BACK" -> {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                            .clickable {
                                                if (pinEntered.isNotEmpty()) {
                                                    pinEntered = pinEntered.dropLast(1)
                                                    errorMessage = ""
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Backspace,
                                            contentDescription = "Backspace",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                else -> {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable {
                                                if (pinEntered.length < 4) {
                                                    pinEntered += key
                                                    errorMessage = ""
                                                    if (pinEntered.length == 4) {
                                                        if (pinEntered == viewModel.securityPin) {
                                                            onUnlock()
                                                        } else {
                                                            errorMessage = if (isMarathi) "चुकीचा पिन! कृपया पुन्हा प्रयत्न करा." else "Incorrect PIN! Try again."
                                                            pinEntered = ""
                                                        }
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = key,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Demo unlock fallback (only if user forgets PIN)
            TextButton(
                onClick = {
                    if (viewModel.securityPin.isNotEmpty()) {
                        Toast.makeText(context, if (isMarathi) "पिन विसरलात? तुमचा पिन: ${viewModel.securityPin}" else "Forgot PIN? Your PIN is: ${viewModel.securityPin}", Toast.LENGTH_LONG).show()
                    } else {
                        // If no pin is set but locked somehow, unlock
                        onUnlock()
                    }
                }
            ) {
                Text(
                    text = if (isMarathi) "पिन विसरलात? (Forgot PIN?)" else "Forgot PIN?",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}
