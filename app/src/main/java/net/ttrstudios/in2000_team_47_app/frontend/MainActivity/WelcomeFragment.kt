package net.ttrstudios.in2000_team_47_app.frontend.MainActivity

import android.Manifest
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mapbox.android.core.permissions.PermissionsListener
import net.ttrstudios.in2000_team_47_app.R

class WelcomeFragment : Fragment(), PermissionsListener {
    private lateinit var viewModel: MainActivityViewModel
    private lateinit var mPermissionResult: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mPermissionResult =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
                onPermissionResult(result)
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        requireActivity()
            .findViewById<BottomNavigationView>(R.id.bottomNavigationView).visibility = View.GONE
        return inflater.inflate(R.layout.fragment_welcome, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val vm by activityViewModels<MainActivityViewModel>()
        viewModel = vm

        //Lager de første limitsene
        vm.addFirstLimits()

        // If user wants to grant permission, launch the permission request, otherwise
        // move to the weather overview fragment.
        view.findViewById<TextView>(R.id.grantPermissionText).setOnClickListener {
            mPermissionResult.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        view.findViewById<TextView>(R.id.denyPermissionText).setOnClickListener {
            val bottomNavigationBar = requireActivity()
                .findViewById<BottomNavigationView>(R.id.bottomNavigationView)
            requireActivity().supportFragmentManager.commit {
                replace<WeatherFragment>(R.id.fragment_container_view)
                setReorderingAllowed(true)
            }
            bottomNavigationBar.visibility = View.VISIBLE
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
        } else {
            Toast.makeText(
                requireActivity().baseContext,
                R.string.user_location_permission_not_granted,
                Toast.LENGTH_LONG
            ).show()
        }
        // Move to the weather overview fragment
        val bottomNavigationBar = requireActivity()
            .findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        requireActivity().supportFragmentManager.commit {
            replace<WeatherFragment>(R.id.fragment_container_view)
            setReorderingAllowed(true)
        }
        bottomNavigationBar.visibility = View.VISIBLE
    }
}