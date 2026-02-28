package uk.co.beezly.parkwatch.ui.screens

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import uk.co.beezly.parkwatch.data.AppSettings
import uk.co.beezly.parkwatch.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    var editedSettings by remember(settings) { mutableStateOf(settings) }
    var showDevicePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.saveSettings(editedSettings)
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        viewModel.saveSettings(editedSettings)
                        onBack()
                    }) {
                        Text("Save")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            // --- Car Device ---
            item {
                SettingsSectionHeader(title = "Car Bluetooth Device")
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = {
                            Text(
                                if (editedSettings.carDeviceName.isNotBlank())
                                    editedSettings.carDeviceName
                                else
                                    "No device selected"
                            )
                        },
                        supportingContent = {
                            Text(
                                if (editedSettings.carDeviceMac.isNotBlank())
                                    editedSettings.carDeviceMac
                                else
                                    "Tap to choose your car's Bluetooth device"
                            )
                        },
                        leadingContent = {
                            Icon(Icons.Default.BluetoothConnected, contentDescription = null)
                        },
                        trailingContent = {
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        },
                        modifier = Modifier.clickable { showDevicePicker = true }
                    )
                }
            }

            // --- Timer Duration ---
            item { SettingsSectionHeader(title = "Timer") }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Duration: ${editedSettings.timerDurationMinutes} minutes")
                        Spacer(Modifier.height(8.dp))
                        Slider(
                            value = editedSettings.timerDurationMinutes.toFloat(),
                            onValueChange = { editedSettings = editedSettings.copy(timerDurationMinutes = it.toInt()) },
                            valueRange = 15f..180f,
                            steps = 10
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("15m", style = MaterialTheme.typography.labelSmall)
                            Text("3h", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // --- Restriction Hours ---
            item { SettingsSectionHeader(title = "Restriction Hours") }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Mon–Sat", fontWeight = FontWeight.SemiBold)
                        TimeRangeRow(
                            startTime = editedSettings.weekdayStartTime,
                            endTime = editedSettings.weekdayEndTime,
                            onStartChange = { editedSettings = editedSettings.copy(weekdayStartTime = it) },
                            onEndChange = { editedSettings = editedSettings.copy(weekdayEndTime = it) }
                        )
                        HorizontalDivider()
                        Text("Sunday", fontWeight = FontWeight.SemiBold)
                        TimeRangeRow(
                            startTime = editedSettings.sundayStartTime,
                            endTime = editedSettings.sundayEndTime,
                            onStartChange = { editedSettings = editedSettings.copy(sundayStartTime = it) },
                            onEndChange = { editedSettings = editedSettings.copy(sundayEndTime = it) }
                        )
                    }
                }
            }

            // --- Location Zone ---
            item { SettingsSectionHeader(title = "Location Zone") }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text("Location check", fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Only start timer when in zone",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = editedSettings.locationCheckEnabled,
                                onCheckedChange = { editedSettings = editedSettings.copy(locationCheckEnabled = it) }
                            )
                        }

                        if (editedSettings.locationCheckEnabled) {
                            HorizontalDivider()
                            OutlinedTextField(
                                value = editedSettings.zoneLat.toString(),
                                onValueChange = { v ->
                                    v.toDoubleOrNull()?.let { editedSettings = editedSettings.copy(zoneLat = it) }
                                },
                                label = { Text("Latitude") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = editedSettings.zoneLng.toString(),
                                onValueChange = { v ->
                                    v.toDoubleOrNull()?.let { editedSettings = editedSettings.copy(zoneLng = it) }
                                },
                                label = { Text("Longitude") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = editedSettings.zoneRadiusMetres.toString(),
                                onValueChange = { v ->
                                    v.toIntOrNull()?.let { editedSettings = editedSettings.copy(zoneRadiusMetres = it) }
                                },
                                label = { Text("Radius (metres)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    if (showDevicePicker) {
        BluetoothDevicePickerDialog(
            context = context,
            onDeviceSelected = { mac, name ->
                editedSettings = editedSettings.copy(carDeviceMac = mac, carDeviceName = name)
                showDevicePicker = false
            },
            onDismiss = { showDevicePicker = false }
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 4.dp)
    )
}

@Composable
fun TimeRangeRow(
    startTime: String,
    endTime: String,
    onStartChange: (String) -> Unit,
    onEndChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = startTime,
            onValueChange = { v ->
                if (v.matches(Regex("\\d{0,2}:?\\d{0,2}"))) onStartChange(v)
            },
            label = { Text("From") },
            placeholder = { Text("08:00") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Text("–")
        OutlinedTextField(
            value = endTime,
            onValueChange = { v ->
                if (v.matches(Regex("\\d{0,2}:?\\d{0,2}"))) onEndChange(v)
            },
            label = { Text("To") },
            placeholder = { Text("18:00") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
    }
}

@Composable
fun BluetoothDevicePickerDialog(
    context: Context,
    onDeviceSelected: (mac: String, name: String) -> Unit,
    onDismiss: () -> Unit
) {
    val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    val btAdapter: BluetoothAdapter? = btManager?.adapter

    val pairedDevices = remember {
        try {
            btAdapter?.bondedDevices?.toList() ?: emptyList()
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Car Device") },
        text = {
            if (pairedDevices.isEmpty()) {
                Text("No paired Bluetooth devices found. Pair your car in Android Settings first.")
            } else {
                Column {
                    pairedDevices.forEach { device ->
                        val name = try { device.name ?: device.address } catch (e: SecurityException) { device.address }
                        val mac = device.address
                        ListItem(
                            headlineContent = { Text(name) },
                            supportingContent = { Text(mac) },
                            leadingContent = { Icon(Icons.Default.Bluetooth, contentDescription = null) },
                            modifier = Modifier.clickable { onDeviceSelected(mac, name) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
