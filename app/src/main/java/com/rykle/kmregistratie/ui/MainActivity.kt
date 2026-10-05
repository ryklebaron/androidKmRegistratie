package com.rykle.kmregistratie.ui

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rykle.kmregistratie.R
import com.rykle.kmregistratie.ui.screen.DriveDetailScreen
import com.rykle.kmregistratie.ui.screen.DriveListScreen
import com.rykle.kmregistratie.ui.screen.PendingKmDialog
import com.rykle.kmregistratie.ui.screen.SettingsScreen
import com.rykle.kmregistratie.ui.theme.KmRegistratieTheme
import com.rykle.kmregistratie.ui.viewmodel.DriveViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KmRegistratieTheme {
                val locationPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { /* resultaat wordt genegeerd; FusedLocationProvider valt terug op 0.0 bij weigering */ }

                val viewModel: DriveViewModel = hiltViewModel()

                LaunchedEffect(Unit) {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        )
                    )
                }

                val navController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }

                val pendingKm by viewModel.pendingKm.collectAsState()
                pendingKm?.let { state ->
                    PendingKmDialog(
                        state = state,
                        onConfirm = viewModel::confirmPendingKm,
                        onSync = viewModel::syncPendingKm,
                        onPostpone = viewModel::postponePendingKm,
                        onDelete = viewModel::deletePendingKmDrive,
                    )
                }

                val currentBackStack by navController.currentBackStackEntryAsState()
                val currentRoute = currentBackStack?.destination?.route

                val isDetailScreen = currentRoute?.startsWith("drive/") == true

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        if (!isDetailScreen) {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = currentRoute == "drives",
                                    onClick = { navController.navigate("drives") },
                                    icon = { Icon(Icons.Default.List, contentDescription = null) },
                                    label = { Text(stringResource(R.string.nav_drives)) }
                                )
                                NavigationBarItem(
                                    selected = currentRoute == "settings",
                                    onClick = { navController.navigate("settings") },
                                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                    label = { Text(stringResource(R.string.nav_settings)) }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "drives",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("drives") {
                            DriveListScreen(viewModel, onDriveClick = { driveId ->
                                navController.navigate("drive/$driveId")
                            })
                        }
                        composable("settings") { SettingsScreen(viewModel, snackbarHostState) }
                        composable("drive/{driveId}") { backStack ->
                            val driveId = backStack.arguments?.getString("driveId")?.toLongOrNull() ?: return@composable
                            DriveDetailScreen(
                                driveId = driveId,
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                                onDeleted = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
