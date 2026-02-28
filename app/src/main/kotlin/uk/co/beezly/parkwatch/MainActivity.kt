package uk.co.beezly.parkwatch

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import uk.co.beezly.parkwatch.ui.screens.HomeScreen
import uk.co.beezly.parkwatch.ui.screens.PermissionsScreen
import uk.co.beezly.parkwatch.ui.screens.SettingsScreen
import uk.co.beezly.parkwatch.ui.theme.ParkWatchTheme
import uk.co.beezly.parkwatch.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ParkWatchTheme {
                ParkWatchApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun ParkWatchApp(viewModel: MainViewModel) {
    val context = LocalContext.current
    val navController = rememberNavController()

    val needsPermissions = remember {
        val required = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        required.any { perm ->
            ContextCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED
        }
    }

    val startDestination = if (needsPermissions) "permissions" else "home"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("permissions") {
            PermissionsScreen(
                onAllGranted = {
                    navController.navigate("home") {
                        popUpTo("permissions") { inclusive = true }
                    }
                }
            )
        }
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }
        composable("settings") {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
