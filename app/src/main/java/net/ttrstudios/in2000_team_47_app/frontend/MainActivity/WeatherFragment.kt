package net.ttrstudios.in2000_team_47_app.frontend.MainActivity

import android.Manifest
import android.app.Activity
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.geometry.LatLng
import net.ttrstudios.in2000_team_47_app.R
import net.ttrstudios.in2000_team_47_app.backend.models.WeatherForecast
import net.ttrstudios.in2000_team_47_app.backend.utils.ItemMoveCallback
import java.text.SimpleDateFormat
import java.util.*

class WeatherFragment : Fragment(), PermissionsListener {

    private lateinit var viewModel: MainActivityViewModel
    private val forecasts = mutableListOf<WeatherForecast>()
    private lateinit var forecastAdapter: ForecastAdapter
    private lateinit var mPermissionResult: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(requireContext(), getString(R.string.mapbox_access_token))
        mPermissionResult =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
                onPermissionResult(result)
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val vm by activityViewModels<MainActivityViewModel>()
        viewModel = vm

        // Setup recycler view + adapter
        forecastAdapter = ForecastAdapter(
            forecasts, savedInstanceState, requireContext(),
            requireActivity(), requireView(), viewModel
        )
        val recyclerView = requireView().findViewById<RecyclerView>(R.id.forecastRecyclerView)
        val callback: ItemTouchHelper.Callback = ItemMoveCallback(forecastAdapter)
        val touchHelper = ItemTouchHelper(callback)
        touchHelper.attachToRecyclerView(recyclerView)

        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            adapter = forecastAdapter
        }

        // Hide/show empty list message
        val emptyListMessage =
            requireView().findViewById<ConstraintLayout>(R.id.emptyListMessageConstraintLayout)
        val addPlaceBox =
            requireView().findViewById<ConstraintLayout>(R.id.addPlaceConstraintLayout)
        if (viewModel.getData().isEmpty()) {
            emptyListMessage.visibility = View.VISIBLE
            emptyListMessage.bringToFront()
            addPlaceBox.visibility = View.GONE
        } else {
            emptyListMessage.visibility = View.GONE
            addPlaceBox.visibility = View.VISIBLE
        }
        emptyListMessage.setOnClickListener {
            requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
                .selectedItemId = R.id.navigation_map
        }

        val turnOnGpsMessage =
            requireView().findViewById<ConstraintLayout>(R.id.gpsMessageConstraintLayout)
        if (!PermissionsManager.areLocationPermissionsGranted(requireActivity().baseContext)
            && !viewModel.getGpsMessageDismissed()
        ) {
            turnOnGpsMessage.visibility = View.VISIBLE
            turnOnGpsMessage.bringToFront()
        }
        turnOnGpsMessage.setOnClickListener {
            mPermissionResult.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            turnOnGpsMessage.visibility = View.GONE
            viewModel.saveGpsMessageDismissed(true)
        }

        requireView().findViewById<ImageView>(R.id.gpsMessageDismissIcon).setOnClickListener {
            turnOnGpsMessage.visibility = View.GONE
            viewModel.saveGpsMessageDismissed(true)
        }

        val addPlaceBar = requireView().findViewById<EditText>(R.id.addPlaceEditText)
        var addPlaceIconPressed = false

        fun startGeoCodingOfPlaceName() {
            addPlaceIconPressed = true
            val placeName = addPlaceBar.text.toString()
            viewModel.getAddressFromLocationName(placeName)
            addPlaceBar.clearFocus()
            val imm =
                requireActivity().getSystemService(Activity.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(requireActivity().currentFocus?.windowToken, 0)
        }

        requireView().findViewById<ImageView>(R.id.addPlaceIcon).setOnClickListener {
            startGeoCodingOfPlaceName()
        }

        addPlaceBar.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                startGeoCodingOfPlaceName()
            }
            false
        }

        // If an address was found from GeoCoding, move to that location on the map fragment.
        viewModel.addressLiveData.observe(viewLifecycleOwner) { address ->
            if (addPlaceIconPressed) {
                addPlaceIconPressed = false
                val addressLine = address.getAddressLine(0)
                if (addressLine == "Fant ingen adresse") {
                    Toast.makeText(requireContext(), "Fant ikke sted", Toast.LENGTH_SHORT).show()
                    return@observe
                }
                viewModel.saveGpsStatus(false)
                viewModel.saveCoordinates(LatLng(address.latitude, address.longitude))
                requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
                    .selectedItemId = R.id.navigation_map
            }
        }

        val dateTimeSeekbar: SeekBar = view.findViewById(R.id.dayAndTimeSeekBar)

        // Add forecast data to recycler view for the selected time.
        viewModel.forecastLiveData.observe(viewLifecycleOwner) { forecastsLD ->
            requireView().findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
            if (forecastsLD.isEmpty()) return@observe
            forecasts.clear()
            forecasts.addAll(forecastsLD[dateTimeSeekbar.progress])
            forecastAdapter.notifyDataSetChanged()
        }

        setupTimePicker(view)
    }

    private fun setupTimePicker(view: View) {
        val dateTimeSeekbar: SeekBar = view.findViewById(R.id.dayAndTimeSeekBar)
        val dayTextView: TextView = view.findViewById(R.id.dayTxt)
        val dateTextView: TextView = view.findViewById(R.id.dateTxt)
        val timeTextView: TextView = view.findViewById(R.id.timeTxt)

        val timestamps = viewModel.getTimeStampsForSeekBar() ?: return
        dateTimeSeekbar.max = timestamps.size - 1

        // Setup view for initial time.
        dayTextView.text = "i dag"
        val firstTimestampParts = timestamps[0].split(" ")
        dateTextView.text = "${firstTimestampParts[1].toInt()}. ${firstTimestampParts[2]}"
        timeTextView.text = "kl. ${firstTimestampParts[3]}"

        // Update view for new time and switch out the forecasts in the recycler view.
        dateTimeSeekbar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Update view for selected time.
                val timestampParts = timestamps[progress].split(" ")
                val sdf = SimpleDateFormat("dd", Locale("no", "NO"))
                val calendar = Calendar.getInstance()
                val today = sdf.format(calendar.time)
                calendar.add(Calendar.DATE, 1)
                val tomorrow = sdf.format(calendar.time)
                when (timestampParts[1]) {
                    today -> dayTextView.text = "i dag"
                    tomorrow -> dayTextView.text = "i morgen"
                    else -> dayTextView.text = timestampParts[0]
                }
                dateTextView.text = "${timestampParts[1].toInt()}. ${timestampParts[2]}"
                if (progress == 0) timeTextView.text = "kl. ${timestampParts[3]}"
                else timeTextView.text = "kl. ${timestampParts[3]} - ${timestampParts[5]}"

                // Add forecast data to recycler view for the selected time.
                val forecastLD = viewModel.forecastLiveData.value ?: return
                if (forecastLD.isEmpty()) return
                forecasts.clear()
                forecasts.addAll(forecastLD[progress])
                forecastAdapter.notifyDataSetChanged()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // Collapse expanded card when changing time.
                forecastAdapter.resetExpandedPos()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })
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
    }

    override fun onResume() {
        super.onResume()
        requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView).menu
            .findItem(R.id.navigation_weather_list).isChecked = true
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_weather, container, false)
    }

    override fun onPause() {
        super.onPause()
        forecastAdapter.resetExpandedPos()
    }
}