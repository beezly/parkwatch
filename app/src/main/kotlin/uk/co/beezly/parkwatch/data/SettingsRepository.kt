package uk.co.beezly.parkwatch.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "parkwatch_settings")

class SettingsRepository(private val context: Context) {

    companion object {
        val CAR_DEVICE_MAC = stringPreferencesKey("car_device_mac")
        val CAR_DEVICE_NAME = stringPreferencesKey("car_device_name")
        val TIMER_DURATION_MINUTES = intPreferencesKey("timer_duration_minutes")
        val LOCATION_CHECK_ENABLED = booleanPreferencesKey("location_check_enabled")
        val ZONE_LAT = doublePreferencesKey("zone_lat")
        val ZONE_LNG = doublePreferencesKey("zone_lng")
        val ZONE_RADIUS = intPreferencesKey("zone_radius_metres")
        val WEEKDAY_START = stringPreferencesKey("weekday_start_time")
        val WEEKDAY_END = stringPreferencesKey("weekday_end_time")
        val SUNDAY_START = stringPreferencesKey("sunday_start_time")
        val SUNDAY_END = stringPreferencesKey("sunday_end_time")
        val PARK_EVENTS = stringPreferencesKey("park_events_json")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            carDeviceMac = prefs[CAR_DEVICE_MAC] ?: "",
            carDeviceName = prefs[CAR_DEVICE_NAME] ?: "",
            timerDurationMinutes = prefs[TIMER_DURATION_MINUTES] ?: 90,
            locationCheckEnabled = prefs[LOCATION_CHECK_ENABLED] ?: true,
            zoneLat = prefs[ZONE_LAT] ?: 53.6458,
            zoneLng = prefs[ZONE_LNG] ?: -1.7850,
            zoneRadiusMetres = prefs[ZONE_RADIUS] ?: 600,
            weekdayStartTime = prefs[WEEKDAY_START] ?: "08:00",
            weekdayEndTime = prefs[WEEKDAY_END] ?: "18:00",
            sundayStartTime = prefs[SUNDAY_START] ?: "12:00",
            sundayEndTime = prefs[SUNDAY_END] ?: "18:00"
        )
    }

    val parkEventsFlow: Flow<List<ParkEvent>> = context.dataStore.data.map { prefs ->
        val json = prefs[PARK_EVENTS] ?: "[]"
        try {
            val type = object : TypeToken<List<ParkEvent>>() {}.type
            Gson().fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateSettings(settings: AppSettings) {
        context.dataStore.edit { prefs ->
            prefs[CAR_DEVICE_MAC] = settings.carDeviceMac
            prefs[CAR_DEVICE_NAME] = settings.carDeviceName
            prefs[TIMER_DURATION_MINUTES] = settings.timerDurationMinutes
            prefs[LOCATION_CHECK_ENABLED] = settings.locationCheckEnabled
            prefs[ZONE_LAT] = settings.zoneLat
            prefs[ZONE_LNG] = settings.zoneLng
            prefs[ZONE_RADIUS] = settings.zoneRadiusMetres
            prefs[WEEKDAY_START] = settings.weekdayStartTime
            prefs[WEEKDAY_END] = settings.weekdayEndTime
            prefs[SUNDAY_START] = settings.sundayStartTime
            prefs[SUNDAY_END] = settings.sundayEndTime
        }
    }

    suspend fun addParkEvent(event: ParkEvent) {
        context.dataStore.edit { prefs ->
            val json = prefs[PARK_EVENTS] ?: "[]"
            val type = object : TypeToken<MutableList<ParkEvent>>() {}.type
            val events: MutableList<ParkEvent> = try {
                Gson().fromJson(json, type) ?: mutableListOf()
            } catch (e: Exception) {
                mutableListOf()
            }
            events.add(0, event)
            // Keep last 20 events
            if (events.size > 20) events.subList(20, events.size).clear()
            prefs[PARK_EVENTS] = Gson().toJson(events)
        }
    }

    suspend fun updateLatestEvent(outcome: ParkOutcome) {
        context.dataStore.edit { prefs ->
            val json = prefs[PARK_EVENTS] ?: "[]"
            val type = object : TypeToken<MutableList<ParkEvent>>() {}.type
            val events: MutableList<ParkEvent> = try {
                Gson().fromJson(json, type) ?: mutableListOf()
            } catch (e: Exception) {
                mutableListOf()
            }
            if (events.isNotEmpty()) {
                val latest = events[0]
                events[0] = latest.copy(endTime = System.currentTimeMillis(), outcome = outcome)
            }
            prefs[PARK_EVENTS] = Gson().toJson(events)
        }
    }
}
