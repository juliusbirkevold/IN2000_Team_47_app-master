package net.ttrstudios.in2000_team_47_app

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.gson.Gson
import com.mapbox.mapboxsdk.geometry.LatLng
import net.ttrstudios.in2000_team_47_app.backend.models.ForecastLimits
import net.ttrstudios.in2000_team_47_app.backend.models.Poi
import net.ttrstudios.in2000_team_47_app.backend.network.GeoCoding
import net.ttrstudios.in2000_team_47_app.backend.utils.EmergencyNbrService
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
class EnergencyNbrsTest {
    @Test
    fun getEmergencyNbrsTest() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val ens = EmergencyNbrService(appContext)

        val result = ens.getEmergencyNbrs()

        assertEquals(5, result.size)

        assertEquals("Kystradioen - 120", result[0])

        assertEquals("Politiet - 112", result[1])

        assertEquals("RS servicetelefon - 02016", result[2])

        assertEquals("Ambulanse - 113", result[3])

        assertEquals("Brannvesen - 110", result[4])
    }
}