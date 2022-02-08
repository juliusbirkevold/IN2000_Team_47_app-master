package net.ttrstudios.in2000_team_47_app

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.gson.Gson
import com.mapbox.mapboxsdk.geometry.LatLng
import net.ttrstudios.in2000_team_47_app.backend.models.ForecastLimits
import net.ttrstudios.in2000_team_47_app.backend.models.Poi
import net.ttrstudios.in2000_team_47_app.backend.utils.PointListService

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class LoadAndSaveDataIntegrationTest {

    @Test
    fun saveAndLoadPointList() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val poiListService = PointListService(appContext)
        val gson = Gson()

        val list: MutableList<Poi> = listOf<Poi>(Poi("Test Name", 10.0, 20.0, "green")).toMutableList()

        poiListService.saveData(list)

        val expectedResult: String = gson.toJson(list)

        val resultList = poiListService.loadData()

        val result: String = gson.toJson(resultList)

        assertEquals("The saved list was not the same when read from SharedPrefrences", expectedResult, result)
    }

    @Test
    fun saveAndLoadLimits() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val poiListService = PointListService(appContext)
        val gson = Gson()

        val objekt: ForecastLimits = ForecastLimits(0,1,2,3,4,5,6,7)

        poiListService.saveLimits(objekt)

        val result: ForecastLimits? = poiListService.loadLimits()

        assertNotNull("The result of the ForcastLimits Load was NULL", result)

        assertEquals("precipitationLow was not the same value", objekt.precipitationLow, result!!.precipitationLow)

        assertEquals("precipitationHigh was not the same value", objekt.precipitationHigh, result.precipitationHigh)

        assertEquals("waveHeightLow was not the same value", objekt.waveHeightLow, result.waveHeightLow)

        assertEquals("waveHeightHigh was not the same value", objekt.waveHeightHigh, result.waveHeightHigh)

        assertEquals("windSpeedLow was not the same value", objekt.windSpeedLow, result.windSpeedLow)

        assertEquals("windSpeedHigh was not the same value", objekt.windSpeedHigh, result.windSpeedHigh)

        assertEquals("temperatureLow was not the same value", objekt.temperatureLow, result.temperatureLow)

        assertEquals("temperatureHigh was not the same value", objekt.temperatureHigh, result.temperatureHigh)
    }

    @Suppress("DEPRECATION")
    @Test
    fun saveAndLoadCoordinates() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val poiListService = PointListService(appContext)
        val gson = Gson()

        val latLng: LatLng = LatLng(10.0, 20.0)

        poiListService.saveCoordinates(latLng)

        val result: LatLng? = poiListService.loadCoordinates()

        assertNotNull("The result of the ForcastLimits Load was NULL", result)

        assertEquals("precipitationLow was not the same value", latLng.latitude, result!!.latitude, 0.001)

        assertEquals("precipitationHigh was not the same value", latLng.longitude, result.longitude, 0.001)
    }
}