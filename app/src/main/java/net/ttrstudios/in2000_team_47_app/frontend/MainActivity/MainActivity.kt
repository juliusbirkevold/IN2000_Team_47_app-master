package net.ttrstudios.in2000_team_47_app.frontend.MainActivity

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mapbox.android.core.permissions.PermissionsManager
import net.ttrstudios.in2000_team_47_app.R
import net.ttrstudios.in2000_team_47_app.backend.models.Poi
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: MainActivityViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val vm by viewModels<MainActivityViewModel>()
        viewModel = vm

        if (viewModel.startup) {
            viewModel.setTimeStampsForSeekBarAndApi()

            // When the activity start, if the app has location permissions then start continuous
            // location updates and enter follow mode.
            if (PermissionsManager.areLocationPermissionsGranted(this)) {
                if (!viewModel.gpsUpdatesOn) viewModel.startContinuousLocationUpdates()
            }

            val places = viewModel.getData()
            viewModel.getForecastAndAlertsForSavedPlaces(places)

            // Get location weather data from API, only the first time a location updates is made.
            var locationUpdate = true
            viewModel.locationLiveData.observe(this) {
                if (!locationUpdate) return@observe
                if (viewModel.forecastLiveData.value == null) return@observe
                val place = Poi("Min posisjon", it.longitude, it.latitude, "Position")
                viewModel.getForecastAndAlertsForNewPlace(place, true)
                locationUpdate = false
            }
            viewModel.startup = false
        }

        // If the app is started for the first time since installation, then show welcome screen.
        if (viewModel.getFirstStart()) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                add<WelcomeFragment>(R.id.fragment_container_view)
            }
            viewModel.saveFirstStart(false)
        } else if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                add<WeatherFragment>(R.id.fragment_container_view)
            }
        }

        // Prevent reselection for bottom navigation.
        findViewById<BottomNavigationView>(R.id.bottomNavigationView).setOnNavigationItemReselectedListener { }

        // Change fragment using the bottom navigation.
        findViewById<BottomNavigationView>(R.id.bottomNavigationView).setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_emergency -> {
                    supportFragmentManager.commit {
                        replace<EmergencyFragment>(R.id.fragment_container_view)
                        setReorderingAllowed(true)
                        addToBackStack("")
                    }
                    true
                }
                R.id.navigation_weather_list -> {
                    supportFragmentManager.commit {
                        replace<WeatherFragment>(R.id.fragment_container_view)
                        setReorderingAllowed(true)
                        addToBackStack("")
                    }
                    true
                }
                R.id.navigation_weather_profile -> {
                    supportFragmentManager.commit {
                        replace<WeatherProfileFragment>(R.id.fragment_container_view)
                        setReorderingAllowed(true)
                        addToBackStack("")
                    }
                    true
                }
                R.id.navigation_map -> {
                    supportFragmentManager.commit {
                        replace<MapViewFragment>(R.id.fragment_container_view)
                        setReorderingAllowed(true)
                        addToBackStack("")
                    }
                    true
                }
                R.id.navigation_settings -> {
                    supportFragmentManager.commit {
                        replace<SettingsFragment>(R.id.fragment_container_view)
                        setReorderingAllowed(true)
                        addToBackStack("")
                    }
                    true
                }
                else -> false
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (viewModel.gpsUpdatesOn && !viewModel.gpsUpdatesPaused) {
            viewModel.stopContinuousLocationUpdates()
            viewModel.gpsUpdatesPaused = true
        }
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.gpsUpdatesOn && viewModel.gpsUpdatesPaused) {
            viewModel.resumeContinuousLocationUpdates()
            viewModel.gpsUpdatesPaused = false
        }
    }
}