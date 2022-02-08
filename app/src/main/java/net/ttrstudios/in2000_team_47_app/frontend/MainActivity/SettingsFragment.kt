package net.ttrstudios.in2000_team_47_app.frontend.MainActivity

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import net.ttrstudios.in2000_team_47_app.R

private lateinit var mPermissionResult: ActivityResultLauncher<String>
private lateinit var viewModel: MainActivityViewModel

class SettingsFragment : Fragment(R.layout.fragment_settings), PermissionsListener {
    private lateinit var stedtjenesterStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mPermissionResult =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { result -> // Initializing permission listener
                onPermissionResult(result)
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val vm by activityViewModels<MainActivityViewModel>()
        viewModel = vm

        val turnOnGpsMessage: ConstraintLayout =
            view.findViewById(R.id.gpsMessageConstraintLayout) // Finding the gps constraint

        stedtjenesterStatus = view.findViewById(R.id.stedstjenester)
        stedtjenesterStatus.text =
            getString(R.string.location_services_off_settings) // Setting the on or off text to off

        if (PermissionsManager.areLocationPermissionsGranted(requireActivity().baseContext)) { // Checking if the app has location permissions
            stedtjenesterStatus.text =
                getString(R.string.location_services_on_settings) // If it has set the on off text to on
        }

        view.findViewById<Button>(R.id.gpsButton)
            .setOnClickListener { // When clicking the change button
                if (PermissionsManager.areLocationPermissionsGranted(requireActivity().baseContext)) { // If the app has location permissions
                    val intent =
                        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS) // Sends the user to the settings where they can turn off the location permission
                    startActivity(intent)
                } else { // If location permissions are off
                    mPermissionResult.launch(Manifest.permission.ACCESS_FINE_LOCATION) // Displaying the Accept or deny popup
                    turnOnGpsMessage.visibility = View.GONE
                }
            }
    }

    override fun onExplanationNeeded(permissionsToExplain: List<String>) {
        Toast.makeText(
            requireActivity().baseContext,
            R.string.user_location_permission_explanation,
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            viewModel.startContinuousLocationUpdates()
            stedtjenesterStatus.text =
                requireContext().applicationContext.getString(R.string.location_services_on_settings)
        } else {
            stedtjenesterStatus.text =
                requireContext().applicationContext.getString(R.string.location_services_off_settings)
        }
    }
}