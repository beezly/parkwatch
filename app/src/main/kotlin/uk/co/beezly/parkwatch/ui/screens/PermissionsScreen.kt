package uk.co.beezly.parkwatch.ui.screens

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.*

data class PermissionInfo(
    val permission: String,
    val title: String,
    val rationale: String
)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionsScreen(onAllGranted: () -> Unit) {
    val permissionsList = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(PermissionInfo(
                Manifest.permission.BLUETOOTH_CONNECT,
                "Bluetooth Connect",
                "Required to detect when you connect or disconnect from your car."
            ))
            add(PermissionInfo(
                Manifest.permission.BLUETOOTH_SCAN,
                "Bluetooth Scan",
                "Required to list paired Bluetooth devices in Settings."
            ))
        }
        add(PermissionInfo(
            Manifest.permission.ACCESS_FINE_LOCATION,
            "Location",
            "Used to check you're in the town centre before starting the timer."
        ))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(PermissionInfo(
                Manifest.permission.POST_NOTIFICATIONS,
                "Notifications",
                "Required to show the live countdown and alert when time expires."
            ))
        }
    }

    val multiplePermissionsState = rememberMultiplePermissionsState(
        permissions = permissionsList.map { it.permission }
    )

    LaunchedEffect(multiplePermissionsState.allPermissionsGranted) {
        if (multiplePermissionsState.allPermissionsGranted) {
            onAllGranted()
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(Modifier.height(24.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "🅿️",
                    style = MaterialTheme.typography.displayMedium
                )
                Text(
                    text = "Welcome to ParkWatch",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ParkWatch needs a few permissions to automatically track your parking time.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                permissionsList.forEach { info ->
                    val isGranted = multiplePermissionsState.permissions
                        .firstOrNull { it.permission == info.permission }
                        ?.status?.isGranted ?: false

                    PermissionItem(
                        title = info.title,
                        rationale = info.rationale,
                        isGranted = isGranted
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = { multiplePermissionsState.launchMultiplePermissionRequest() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !multiplePermissionsState.allPermissionsGranted
            ) {
                Text(if (multiplePermissionsState.allPermissionsGranted) "All Set!" else "Grant Permissions")
            }

            TextButton(
                onClick = onAllGranted,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Skip (some features may not work)")
            }
        }
    }
}

@Composable
fun PermissionItem(title: String, rationale: String, isGranted: Boolean) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
            Column {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(
                    rationale,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
