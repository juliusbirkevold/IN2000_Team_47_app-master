package net.ttrstudios.in2000_team_47_app

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.gson.Gson
import com.mapbox.mapboxsdk.geometry.LatLng
import net.ttrstudios.in2000_team_47_app.backend.models.ForecastLimits
import net.ttrstudios.in2000_team_47_app.backend.models.Poi
import net.ttrstudios.in2000_team_47_app.backend.network.GeoCoding
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
class GeoCodingIntegrationTest {
    @Test
    fun getAddressFromLocationNameTest() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val geoCoding = GeoCoding(appContext)
        val address = "Reistadjordet 21B"
        val result = geoCoding.getAddressFromLocationName(address)

        assertNotNull("The result of the getAddressFromLocationName was NULL", result)

        assertEquals("Viken", result!!.adminArea)

        assertEquals("NO", result!!.countryCode)

        assertEquals("1394", result!!.postalCode)

        assertEquals("Asker", result!!.subAdminArea)

    }

    @Test
    fun getAddressFromCoordinatesTest() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val geoCoding = GeoCoding(appContext)
        val lat = 59.845698
        val long = 10.474248

        val result = geoCoding.getAddressFromCoordinates(lat, long)

        assertNotNull("The result of the getAddressFromCoordinates Load was NULL", result)

        assertEquals("Reistadjordet", result!!.thoroughfare)

        assertEquals("21B", result!!.featureName)

        assertEquals("Viken", result!!.adminArea)

        assertEquals("NO", result!!.countryCode)

        assertEquals("1394", result!!.postalCode)

        assertEquals("Asker", result!!.subAdminArea)
    }
}