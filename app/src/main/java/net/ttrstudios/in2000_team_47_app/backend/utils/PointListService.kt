package net.ttrstudios.in2000_team_47_app.backend.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.mapbox.mapboxsdk.geometry.LatLng
import net.ttrstudios.in2000_team_47_app.backend.models.ForecastLimits
import net.ttrstudios.in2000_team_47_app.backend.models.Poi


class PointListService(val context: Context) {
    private val PREFERENCE_NAME = "SharedPreferenceData"
    private val PREFERNECE_POI_LIST = "PoiList"
    private val PREFERENCE_COORDINATES = "Coordinates"
    private val PREFERENCE_GPS_STATUS = "GPS Status"
    private val PREFERENCE_FIRST_START = "First start"
    private val PREFERENCE_LIMITS = "Limits"
    private val PREFERENCE_GPS_MESSAGE_DISMISSED = "GPS message dismissed"
    private val PREFERENCE_TIMESTAMPS = "Timestamps for seekbar"
    private val PREFERENCE_TIMESTAMPS_API = "Timestamps for API"

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
    private val gson: Gson = Gson()

    fun saveData(poiList: MutableList<Poi>) {
        val jsonString: String = gson.toJson(poiList)
        val editor = sharedPreferences.edit()
        editor.putString(PREFERNECE_POI_LIST, jsonString)
        editor.apply()
    }

    fun loadData(): MutableList<Poi> {
        val jsonString = sharedPreferences.getString(PREFERNECE_POI_LIST, null)
            ?: return mutableListOf()
        return gson.fromJson(jsonString, Array<Poi>::class.java).toMutableList()
    }

    fun saveCoordinates(coordinates: LatLng) {
        val jsonString: String = gson.toJson(coordinates)
        val editor = sharedPreferences.edit()
        editor.putString(PREFERENCE_COORDINATES, jsonString)
        editor.apply()
    }

    fun loadCoordinates(): LatLng? {
        val jsonString = sharedPreferences.getString(PREFERENCE_COORDINATES, null)
            ?: return null
        return gson.fromJson(jsonString, LatLng::class.java)
    }

    fun saveGpsStatus(gpsStatus: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean(PREFERENCE_GPS_STATUS, gpsStatus)
        editor.apply()
    }

    fun loadGpsStatus(): Boolean {
        return sharedPreferences.getBoolean(PREFERENCE_GPS_STATUS, false)
    }

    fun saveFirstStart(firstStart: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean(PREFERENCE_FIRST_START, firstStart)
        editor.apply()
    }

    fun loadFirstStart(): Boolean {
        return sharedPreferences.getBoolean(PREFERENCE_FIRST_START, true)
    }

    fun saveLimits(limits: ForecastLimits) {
        val jsonString: String = gson.toJson(limits)
        val editor = sharedPreferences.edit()
        editor.putString(PREFERENCE_LIMITS, jsonString)
        editor.apply()
    }

    fun loadLimits(): ForecastLimits? {
        val jsonString = sharedPreferences.getString(PREFERENCE_LIMITS, null) ?: return null
        return gson.fromJson(jsonString, ForecastLimits::class.java)
    }

    fun saveGpsMessageDismissed(gpsMessageDismissed: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean(PREFERENCE_GPS_MESSAGE_DISMISSED, gpsMessageDismissed)
        editor.apply()
    }

    fun loadGpsMessageDismissed(): Boolean {
        return sharedPreferences.getBoolean(PREFERENCE_GPS_MESSAGE_DISMISSED, false)
    }

    fun saveTimestampsForSeekBar(timestamps: List<String>) {
        val jsonString: String = gson.toJson(timestamps)
        val editor = sharedPreferences.edit()
        editor.putString(PREFERENCE_TIMESTAMPS, jsonString)
        editor.apply()
    }

    fun loadTimeStampsForSeekBar(): List<String>? {
        val jsonString = sharedPreferences.getString(PREFERENCE_TIMESTAMPS, null) ?: return null
        return gson.fromJson(jsonString, Array<String>::class.java).toList()
    }

    fun saveTimestampsForAPI(timestamps: List<String>) {
        val jsonString: String = gson.toJson(timestamps)
        val editor = sharedPreferences.edit()
        editor.putString(PREFERENCE_TIMESTAMPS_API, jsonString)
        editor.apply()
    }

    fun loadTimeStampsAPI(): List<String>? {
        val jsonString = sharedPreferences.getString(PREFERENCE_TIMESTAMPS_API, null) ?: return null
        return gson.fromJson(jsonString, Array<String>::class.java).toList()
    }
}