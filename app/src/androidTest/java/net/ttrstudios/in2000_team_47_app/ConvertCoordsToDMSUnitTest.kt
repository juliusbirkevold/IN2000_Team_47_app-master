package net.ttrstudios.in2000_team_47_app

import androidx.test.ext.junit.runners.AndroidJUnit4
import junit.framework.Assert.assertEquals
import net.ttrstudios.in2000_team_47_app.backend.utils.convertDDcoordsToDMS
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ConvertCoordsToDMSUnitTest {
    @Test
    fun getAddressFromLocationNameTest() {
        val lat = 59.8456
        val long = 10.4742

        val result = convertDDcoordsToDMS(lat, long)

        assertEquals("59°50'44.2\"N", result[0])

        assertEquals("10°28'27.1\"E", result[1])
    }
}