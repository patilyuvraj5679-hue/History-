package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.WorkRecord
import com.example.ui.WorkViewModel
import com.example.ui.screens.AddEditWorkScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.PinLockScreen
import com.example.ui.screens.UpdatesScreen
import com.example.ui.theme.MyApplicationTheme

enum class Screen {
    DASHBOARD,
    ADD_EDIT,
    UPDATES
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: WorkViewModel = viewModel()
                var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }
                var recordToEdit by remember { mutableStateOf<WorkRecord?>(null) }

                if (viewModel.isAppLocked) {
                    PinLockScreen(
                        viewModel = viewModel,
                        onUnlock = { viewModel.isAppLocked = false }
                    )
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding)) {
                            AnimatedContent(
                                targetState = currentScreen,
                                transitionSpec = {
                                    (fadeIn() + slideInHorizontally { width -> width / 3 })
                                        .togetherWith(fadeOut() + slideOutHorizontally { width -> -width / 3 })
                                },
                                label = "screen_transition"
                            ) { screen ->
                                when (screen) {
                                    Screen.DASHBOARD -> {
                                        DashboardScreen(
                                            viewModel = viewModel,
                                            onAddRecord = {
                                                recordToEdit = null
                                                currentScreen = Screen.ADD_EDIT
                                            },
                                            onEditRecord = { record ->
                                                recordToEdit = record
                                                currentScreen = Screen.ADD_EDIT
                                            },
                                            onGoToUpdates = {
                                                currentScreen = Screen.UPDATES
                                            },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Screen.ADD_EDIT -> {
                                        AddEditWorkScreen(
                                            viewModel = viewModel,
                                            recordToEdit = recordToEdit,
                                            onBack = {
                                                currentScreen = Screen.DASHBOARD
                                            },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Screen.UPDATES -> {
                                        UpdatesScreen(
                                            viewModel = viewModel,
                                            onBack = {
                                                currentScreen = Screen.DASHBOARD
                                            },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    androidx.compose.material3.Text(text = "Hello $name!", modifier = modifier)
}

