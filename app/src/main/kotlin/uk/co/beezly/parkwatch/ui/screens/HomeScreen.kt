package uk.co.beezly.parkwatch.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uk.co.beezly.parkwatch.data.ParkEvent
import uk.co.beezly.parkwatch.data.ParkOutcome
import uk.co.beezly.parkwatch.ui.components.CountdownArc
import uk.co.beezly.parkwatch.viewmodel.MainViewModel
import uk.co.beezly.parkwatch.viewmodel.TimerState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit
) {
    val timerState by viewModel.timerState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val parkEvents by viewModel.parkEvents.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ParkWatch") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                StatusCard(
                    timerState = timerState,
                    carDeviceName = settings.carDeviceName,
                    onCancel = { viewModel.cancelTimer() }
                )
            }

            if (parkEvents.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(parkEvents.take(10)) { event ->
                    ParkEventItem(event = event)
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun StatusCard(
    timerState: TimerState,
    carDeviceName: String,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (timerState) {
                is TimerState.Idle -> {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "ParkWatch is ready",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (carDeviceName.isNotBlank()) {
                        Text(
                            text = "Watching: $carDeviceName",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "Configure your car in Settings",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                is TimerState.Running -> {
                    Text(
                        text = "🅿️ Parked",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    CountdownArc(
                        remainingMs = timerState.remainingMs,
                        totalMs = timerState.totalMs,
                        size = 220.dp
                    )
                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Icon(Icons.Default.Cancel, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Cancel Timer")
                    }
                }

                is TimerState.OutsideHours -> {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "Outside restriction hours",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Timer was not started",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                is TimerState.OutsideZone -> {
                    Icon(
                        imageVector = Icons.Default.LocationOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "Not in town centre",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Timer was not started",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ParkEventItem(event: ParkEvent) {
    val dateFormat = remember { SimpleDateFormat("EEE d MMM, HH:mm", Locale.UK) }
    val startDate = dateFormat.format(Date(event.startTime))

    val durationText = event.endTime?.let {
        val mins = ((it - event.startTime) / 1000 / 60).toInt()
        if (mins >= 60) "${mins / 60}h ${mins % 60}m" else "${mins}m"
    } ?: "Active"

    val (outcomeIcon, outcomeColor) = when (event.outcome) {
        ParkOutcome.ACTIVE -> Icons.Default.Timer to MaterialTheme.colorScheme.primary
        ParkOutcome.CANCELLED -> Icons.Default.CheckCircle to MaterialTheme.colorScheme.tertiary
        ParkOutcome.EXPIRED -> Icons.Default.Warning to MaterialTheme.colorScheme.error
    }

    ListItem(
        headlineContent = { Text(startDate) },
        supportingContent = { Text(durationText) },
        trailingContent = {
            Icon(
                imageVector = outcomeIcon,
                contentDescription = event.outcome.name,
                tint = outcomeColor,
                modifier = Modifier.size(20.dp)
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
    )
}
