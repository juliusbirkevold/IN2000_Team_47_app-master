package net.ttrstudios.in2000_team_47_app.frontend.MainActivity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
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
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.Layer
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.mapboxsdk.utils.BitmapUtils
import net.ttrstudios.in2000_team_47_app.R
import net.ttrstudios.in2000_team_47_app.backend.models.Poi
import net.ttrstudios.in2000_team_47_app.backend.models.WeatherForecast
import java.util.*

class MapViewFragment : Fragment(R.layout.fragment_map), OnMapReadyCallback, PermissionsListener {
    private lateinit var places: MutableList<Poi>
    private var coordinates: LatLng? = null
    private var gpsStatus = false
    private var markerPosition = LatLng()
    private var markerDropped = false

    private lateinit var mapView: MapView
    private lateinit var mPermissionResult: ActivityResultLauncher<String>
    private lateinit var mapboxMap: MapboxMap
    private lateinit var droppedMarkerLayer: Layer
    private val DROPPED_MARKER_LAYER_ID = "DROPPED_MARKER_LAYER_ID"
    private val SAVED_MARKERS_LAYER_ID = "SAVED_MARKERS_LAYER_ID"
    private var nextLayerIndex = 0
    private val activeLayerIndices = mutableListOf<Int>()
    private lateinit var locationBar: EditText
    private lateinit var gpsButton: ImageView

    private lateinit var viewModel: MainActivityViewModel

    private var fromSearch = false
    private val positionReservedName = "Min posisjon"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(requireActivity().baseContext, getString(R.string.mapbox_access_token))
        mPermissionResult =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
                onPermissionResult(result)
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val vm by activityViewModels<MainActivityViewModel>()
        viewModel = vm

        places = viewModel.getData()
        coordinates = viewModel.getCoordinates()
        gpsStatus = viewModel.getGpsStatus()

        // Request map from mapbox
        mapView = requireView().findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        locationBar = requireView().findViewById(R.id.locationSearchEditText)
        gpsButton = requireView().findViewById(R.id.gpsStatusImageView)

        if (gpsStatus) gpsButton.setImageResource(R.drawable.ic_baseline_my_location_24)
        else gpsButton.setImageResource(R.drawable.ic_baseline_location_disabled_24)

        // Start geocoding of coordinates
        coordinates?.let {
            viewModel.getAddressFromCoordinates(
                coordinates?.latitude!!,
                coordinates?.longitude!!
            )
        }

        // When the GPS service updates the live data for the current location, if follow mode
        // is on, we move the camera and update the views
        viewModel.locationLiveData.observe(viewLifecycleOwner) { location ->
            if (gpsStatus && this::mapboxMap.isInitialized) {
                updateCamera(location.latitude, location.longitude, true, 18.0)
                coordinates = LatLng(location.latitude, location.longitude)
                viewModel.getAddressFromCoordinates(location.latitude, location.longitude)
            }
        }

        // Update the search bar hint with the address of marked/searched/user location. If it
        // was a successful name search, move the camera to the resulting coordinates. If GeoCoding
        // couldn't find an address for coordinates, default the hint to "Søk på sted". If GeoCoding
        // couldn't find an address for name search, then inform the user with a toast and don't
        // update anything.
        viewModel.addressLiveData.observe(viewLifecycleOwner) { address ->
            when (val addressLine = address.getAddressLine(0)) {
                "Ingen adresse" -> locationBar.hint = "Søk på sted"
                "Fant ingen adresse" -> {
                    if (fromSearch) Toast.makeText(
                        requireContext(), "Fant ikke sted",
                        Toast.LENGTH_SHORT
                    ).show()
                    fromSearch = false
                    return@observe
                }
                else -> locationBar.hint = addressLine
            }
            if (fromSearch) {
                coordinates = LatLng(address.latitude, address.longitude)
                if (this::mapboxMap.isInitialized) updateCamera(
                    coordinates?.latitude!!,
                    coordinates?.longitude!!, fromSearch, 18.0
                )
            }
            fromSearch = false
        }

        // When the user has entered a search query; get address from location name, clear the
        // search bar, and hide the soft keyboard.
        locationBar.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val searchQuery = locationBar.text.toString()
                fromSearch = true
                gpsStatus = false
                gpsButton.setImageResource(R.drawable.ic_baseline_location_disabled_24)
                viewModel.getAddressFromLocationName(searchQuery)
                locationBar.text.clear()
                locationBar.clearFocus()
                val imm =
                    activity?.getSystemService(Activity.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.hideSoftInputFromWindow(activity?.currentFocus?.windowToken, 0)
            }
            false
        }

        // When GPS button is pressed
        gpsButton.setOnClickListener {
            if (gpsStatus) {
                // If the button was on when pressed, turn it off and stop follow mode
                gpsStatus = false
                gpsButton.setImageResource(R.drawable.ic_baseline_location_disabled_24)
            } else {
                // If it was off
                if (PermissionsManager.areLocationPermissionsGranted(requireActivity().baseContext)) {
                    // If app has location permissions, turn the button on and start follow mode
                    gpsStatus = true
                    gpsButton.setImageResource(R.drawable.ic_baseline_my_location_24)
                    viewModel.singleLocationUpdate()
                } else {
                    // Otherwise request location permissions
                    mPermissionResult.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
        }
    }

    fun addPlace(place: Poi) {
        // Tell viewmodel to do weather/alert data API calls for new place
        viewModel.getForecastAndAlertsForNewPlace(place, false)

        // Make persistent marker for new place
        val style = mapboxMap.style ?: return
        initSavedMarker(style, place.markerColor, nextLayerIndex)
        if (style.getLayer("SAVED_MARKERS_LAYER_ID_$nextLayerIndex") != null) {
            val source: GeoJsonSource? =
                style.getSourceAs("saved-markers-source-id-$nextLayerIndex")
            source?.setGeoJson(Point.fromLngLat(place.longitude, place.latitude))
        }
        // Remove temporary marker
        droppedMarkerLayer = style.getLayer(DROPPED_MARKER_LAYER_ID) ?: return
        droppedMarkerLayer.setProperties(PropertyFactory.visibility(Property.NONE))
        markerDropped = false

        places.add(place) // Add place to temporary (not saved) list of places.
        // Add the index for the markers layer to the list of active marker layer indices.
        activeLayerIndices.add(nextLayerIndex)
        nextLayerIndex++ // Prepare the index for the marker of the next new place.
    }

    fun deletePlace(index: Int) {
        places.removeAt(index) // Remove place from temporary (not saved) list of places.

        // Remove forecasts for place from forecastLiveData.
        val forecasts = viewModel.forecastLiveData.value!!
        var i = index
        // If there are forecasts for my position, add 1 to index to match saved places (since
        // saved places does not include my position)
        if (forecasts[0][0].locationName == positionReservedName) i++
        for (forecastsForTime in forecasts) {
            forecastsForTime.removeAt(i)
        }
        // Remove the marker for the place
        val layer =
            mapboxMap.style?.getLayer("${SAVED_MARKERS_LAYER_ID}_${activeLayerIndices[index]}")
                ?: return
        layer.setProperties(PropertyFactory.visibility(Property.NONE))
        activeLayerIndices.removeAt(index)
    }

    fun removeDroppedMarker() {
        droppedMarkerLayer = mapboxMap.style?.getLayer(DROPPED_MARKER_LAYER_ID) ?: return
        droppedMarkerLayer.setProperties(PropertyFactory.visibility(Property.NONE))
        markerDropped = false
    }

    private fun updateCamera(
        latitude: Double,
        longitude: Double,
        animation: Boolean,
        zoom: Double
    ) {
        val position = CameraPosition.Builder()
            .target(LatLng(latitude, longitude)) // Sets the new camera position
            .zoom(zoom) // Sets the zoom
            .bearing(0.0) // Rotate the camera
            .tilt(0.0) // Set the camera tilt
            .build() // Creates a CameraPosition from the builder

        if (animation) {
            mapboxMap.animateCamera(
                CameraUpdateFactory
                    .newCameraPosition(position), 7000
            )
        } else {
            mapboxMap.moveCamera(
                CameraUpdateFactory
                    .newCameraPosition(position)
            )
        }
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.setStyle(
            Style.Builder().fromUri(
                "mapbox://styles/mapbox/outdoors-v11"
            )
        ) { style ->

            // Map is set up and the style has loaded. Now you can add data or make other map adjustments
            enableLocationComponent(style)
            // Move the compass so it's not blocked by the searchbar.
            val uiSettings = mapboxMap.uiSettings
            uiSettings.setCompassMargins(
                uiSettings.compassMarginLeft, 200,
                uiSettings.compassMarginRight, uiSettings.compassMarginBottom
            )

            initDroppedMarker(style)

            // Show the markers for saved places
            for (i in 0 until places.size) {
                initSavedMarker(style, places[i].markerColor, nextLayerIndex)
                if (style.getLayer("SAVED_MARKERS_LAYER_ID_$nextLayerIndex") != null) {
                    val source: GeoJsonSource? =
                        style.getSourceAs("saved-markers-source-id-$nextLayerIndex")
                    source?.setGeoJson(Point.fromLngLat(places[i].longitude, places[i].latitude))
                }
                activeLayerIndices.add(nextLayerIndex)
                nextLayerIndex++
            }

            // When the user moves the map, turn off follow mode and close soft keyboard if open.
            mapboxMap.addOnMoveListener(object : MapboxMap.OnMoveListener {
                override fun onMoveBegin(detector: MoveGestureDetector) {
                    gpsStatus = false
                    gpsButton.setImageResource(R.drawable.ic_baseline_location_disabled_24)
                    mapView.requestFocus()
                    val imm =
                        activity?.getSystemService(Activity.INPUT_METHOD_SERVICE) as? InputMethodManager
                    imm?.hideSoftInputFromWindow(activity?.currentFocus?.windowToken, 0)
                }

                override fun onMove(detector: MoveGestureDetector) {}
                override fun onMoveEnd(detector: MoveGestureDetector) {}
            })
            // When the user clicks on the map
            mapboxMap.addOnMapClickListener { clickedCoordinates ->
                // Close soft keyboard if open.
                mapView.requestFocus()
                val imm =
                    activity?.getSystemService(Activity.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.hideSoftInputFromWindow(activity?.currentFocus?.windowToken, 0)

                // Check if user clicked on a marker for a saved place.
                val screenPoint = mapboxMap.projection.toScreenLocation(clickedCoordinates)
                for (i in 0 until places.size) {
                    val features = mapboxMap.queryRenderedFeatures(
                        screenPoint,
                        "SAVED_MARKERS_LAYER_ID_${activeLayerIndices[i]}"
                    )
                    if (features.isNotEmpty()) {
                        // If forecastLiveData is currently being updated from API, ask user to wait.
                        if (viewModel.forecastLiveData.value == null) {
                            Toast.makeText(
                                requireContext(),
                                "Vennligst vent til oversikten har lastet inn " +
                                        "før du trykker på lagrede steder", Toast.LENGTH_LONG
                            ).show()
                            return@addOnMapClickListener false
                        }
                        val place = places[i]
                        val forecasts = viewModel.forecastLiveData.value!!
                        // Check if there are forecasts for "my position", if so we need to add
                        // one to the index since "my position" is not present in saved places.
                        var gpsForecastPresent = false
                        try {
                            gpsForecastPresent =
                                (forecasts[0][0].locationName == positionReservedName)
                        } catch (exception: Exception) {
                        }
                        lateinit var forecast: WeatherForecast
                        // If clicking on a new marker before API fetching for the place has
                        // finished, then ask the user to try again later.
                        try {
                            forecast =
                                if (gpsForecastPresent) forecasts[0][i + 1] else forecasts[0][i]
                        } catch (exception: Exception) {
                            Toast.makeText(
                                requireContext(),
                                "Værvarsel for punkt har ikke lastet inn " +
                                        "enda, prøv igjen om litt.", Toast.LENGTH_LONG
                            ).show()
                            return@addOnMapClickListener false
                        }
                        // If data is not available use an impossible value in place of null. When
                        // the forecast is shown, a dash will then be shown in place of the value.
                        val precipitation = forecast.precipitation ?: -1.0
                        val waveHeight = forecast.waveHeight ?: -1.0
                        val windSpeed = forecast.wind ?: -1
                        val temperature = forecast.temperature ?: -99
                        val symbolCode = forecast.symbolCode ?: ""

                        // Open the forecast dialog.
                        ClickPointDialogFragment.newInstance(
                            place.name, place.latitude,
                            place.longitude, precipitation, waveHeight, windSpeed,
                            temperature, symbolCode, forecast.warnings.toTypedArray(), i
                        )
                            .show(childFragmentManager, ClickPointDialogFragment.TAG)
                        return@addOnMapClickListener true
                    }
                }
                // Check if user clicked on the dropped marker to save a new place.
                val features = mapboxMap.queryRenderedFeatures(screenPoint, DROPPED_MARKER_LAYER_ID)
                if (features.isNotEmpty()) {
                    // If forecastLiveData is currently being updated from API, ask user to wait.
                    if (viewModel.forecastLiveData.value == null) {
                        Toast.makeText(
                            requireContext(),
                            "Vennligst vent til oversikten har lastet inn " +
                                    "før du legger til nye steder.", Toast.LENGTH_LONG
                        ).show()
                        return@addOnMapClickListener false
                    }
                    if (places.size >= 8) {
                        Toast.makeText(
                            requireContext(),
                            "Max antall steder lagret (8). Du må slette minst ett før du " +
                                    "kan legge til ett nytt.", Toast.LENGTH_LONG
                        ).show()
                        return@addOnMapClickListener false
                    }
                    // Open the save new place dialog.
                    EditPointDialogFragment.newInstance(
                        true, 0,
                        "Navn på punkt", markerPosition.latitude, markerPosition.longitude, "Red"
                    )
                        .show(childFragmentManager, EditPointDialogFragment.TAG)
                    return@addOnMapClickListener true
                }

                // When the user just clicks somewhere on the map (not on a marker):
                // If there is no dropped marker, add a dropped "save" marker where they clicked and
                // set the coordinates as the saved coordinates. Also tell viewmodel to use GeoCoding
                // to find an address for the coordinates. Otherwise remove the dropped "save" marker.
                if (!markerDropped) {
                    gpsStatus = false
                    gpsButton.setImageResource(R.drawable.ic_baseline_location_disabled_24)
                    coordinates = clickedCoordinates
                    viewModel.getAddressFromCoordinates(
                        clickedCoordinates.latitude,
                        clickedCoordinates.longitude
                    )

                    // Add the dropped "save" marker to the map
                    if (style.getLayer(DROPPED_MARKER_LAYER_ID) != null) {
                        val source: GeoJsonSource = style.getSourceAs("dropped-marker-source-id")
                            ?: return@addOnMapClickListener false
                        source.setGeoJson(
                            Point.fromLngLat(
                                clickedCoordinates.longitude,
                                clickedCoordinates.latitude
                            )
                        )
                        droppedMarkerLayer = style.getLayer(DROPPED_MARKER_LAYER_ID)
                            ?: return@addOnMapClickListener false
                        droppedMarkerLayer.setProperties(
                            PropertyFactory.visibility(Property.VISIBLE),
                            PropertyFactory.iconAnchor(Property.ICON_ANCHOR_BOTTOM)
                        )
                        markerPosition = clickedCoordinates
                        markerDropped = true
                    }
                } else {
                    droppedMarkerLayer = style.getLayer(DROPPED_MARKER_LAYER_ID)
                        ?: return@addOnMapClickListener false
                    droppedMarkerLayer.setProperties(PropertyFactory.visibility(Property.NONE))
                    markerDropped = false
                }
                true
            }
            // When the user long clicks on the map, remove the dropped "save" marker.
            mapboxMap.addOnMapLongClickListener {
                droppedMarkerLayer = style.getLayer(DROPPED_MARKER_LAYER_ID)
                    ?: return@addOnMapLongClickListener false
                droppedMarkerLayer.setProperties(PropertyFactory.visibility(Property.NONE))
                markerDropped = false
                true
            }
        }

        // When the map is ready set the camera to stored coordinates if they exist, otherwise
        // set the camera to Norway. If gpsStatus is true, get an update and move camera.
        if (coordinates != null) {
            updateCamera(coordinates!!.latitude, coordinates!!.longitude, false, 18.0)
        } else updateCamera(65.95332, 11.15346, false, 3.5)
        if (gpsStatus) {
            if (PermissionsManager.areLocationPermissionsGranted(requireActivity().baseContext)) {
                viewModel.singleLocationUpdate()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableLocationComponent(loadedMapStyle: Style) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(requireActivity().baseContext)) {

            // Create and customize the LocationComponent's options
            val customLocationComponentOptions =
                LocationComponentOptions.builder(requireActivity().baseContext)
                    .trackingGesturesManagement(true)
                    .accuracyColor(
                        ContextCompat.getColor(
                            requireActivity().baseContext,
                            R.color.mapboxGreen
                        )
                    )
                    .build()

            val locationComponentActivationOptions = LocationComponentActivationOptions.builder(
                requireActivity().baseContext,
                loadedMapStyle
            )
                .locationComponentOptions(customLocationComponentOptions)
                .build()

            // Get an instance of the LocationComponent and then adjust its settings
            mapboxMap.locationComponent.apply {
                // Activate the LocationComponent with options
                activateLocationComponent(locationComponentActivationOptions)

                // Enable to make the LocationComponent visible
                isLocationComponentEnabled = true

                // Set the LocationComponent's camera mode
                cameraMode = CameraMode.NONE
                if (gpsStatus) cameraMode = CameraMode.TRACKING_COMPASS

                // Set the LocationComponent's render mode
                renderMode = RenderMode.COMPASS
            }

        } else {
            mPermissionResult.launch(Manifest.permission.ACCESS_FINE_LOCATION)
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
            enableLocationComponent(mapboxMap.style!!)
            viewModel.startContinuousLocationUpdates()
            gpsButton.performClick()
        } else {
            Toast.makeText(
                requireActivity().baseContext,
                R.string.user_location_permission_not_granted,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onStart() {
        super.onStart()
        requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView).menu
            .findItem(R.id.navigation_map).isChecked = true
        if (this::mapView.isInitialized) mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        if (this::mapView.isInitialized) mapView.onResume()
        if (!PermissionsManager.areLocationPermissionsGranted(requireActivity().baseContext)) {
            gpsStatus = false
            gpsButton.setImageResource(R.drawable.ic_baseline_location_disabled_24)
        }
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        viewModel.saveGpsStatus(gpsStatus)
        coordinates?.let { viewModel.saveCoordinates(coordinates!!) }
    }

    override fun onStop() {
        super.onStop()
        if (this::mapView.isInitialized) mapView.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (this::mapView.isInitialized) mapView.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (this::mapView.isInitialized) mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        if (this::mapView.isInitialized) mapView.onLowMemory()
    }

    // Initializes the "save" marker that is dropped when clicking on the map.
    private fun initDroppedMarker(loadedMapStyle: Style) {
        val drawable: Drawable? = ResourcesCompat.getDrawable(
            resources,
            R.drawable.save_marker,
            null
        )
        val bitmap: Bitmap = BitmapUtils.getBitmapFromDrawable(drawable) ?: return
        loadedMapStyle.addImage("dropped-icon-image", bitmap)
        loadedMapStyle.addSource(GeoJsonSource("dropped-marker-source-id"))
        loadedMapStyle.addLayer(
            SymbolLayer(
                DROPPED_MARKER_LAYER_ID,
                "dropped-marker-source-id"
            ).withProperties(
                PropertyFactory.iconImage("dropped-icon-image"),
                PropertyFactory.visibility(Property.NONE),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconIgnorePlacement(true)
            )
        )
    }

    // Initializes a marker for a saved place using the markerColor for the place.
    private fun initSavedMarker(loadedMapStyle: Style, markerColor: String, position: Int) {
        val drawable: Drawable? = when (markerColor) {
            "Red" -> ResourcesCompat.getDrawable(
                resources,
                R.drawable.ic_baseline_place_red_24,
                null
            )
            "Orange" -> ResourcesCompat.getDrawable(
                resources,
                R.drawable.ic_baseline_place_orange_24,
                null
            )
            "Yellow" -> ResourcesCompat.getDrawable(
                resources,
                R.drawable.ic_baseline_place_yellow_24,
                null
            )
            "Green" -> ResourcesCompat.getDrawable(
                resources,
                R.drawable.ic_baseline_place_green_24,
                null
            )
            "Pink" -> ResourcesCompat.getDrawable(
                resources,
                R.drawable.ic_baseline_place_pink_24,
                null
            )
            "Purple" -> ResourcesCompat.getDrawable(
                resources,
                R.drawable.ic_baseline_place_purple_24,
                null
            )
            "Blue" -> ResourcesCompat.getDrawable(
                resources,
                R.drawable.ic_baseline_place_blue_24,
                null
            )
            "Navy Blue" -> ResourcesCompat.getDrawable(
                resources,
                R.drawable.ic_baseline_place_navy_blue_24,
                null
            )
            else -> ResourcesCompat.getDrawable(
                resources,
                R.drawable.ic_baseline_place_red_24,
                null
            )
        }
        val bitmap: Bitmap = BitmapUtils.getBitmapFromDrawable(drawable) ?: return
        val imageName = when (markerColor) {
            "Red" -> "dropped-icon-image-red"
            "Orange" -> "dropped-icon-image-orange"
            "Yellow" -> "dropped-icon-image-yellow"
            "Green" -> "dropped-icon-image-green"
            "Pink" -> "dropped-icon-image-pink"
            "Purple" -> "dropped-icon-image-purple"
            "Blue" -> "dropped-icon-image-blue"
            "Navy Blue" -> "dropped-icon-image-navy-blue"
            else -> "dropped-icon-image-red"
        }
        loadedMapStyle.addImage(imageName, bitmap)
        loadedMapStyle.addSource(GeoJsonSource("saved-markers-source-id-$position"))
        loadedMapStyle.addLayer(
            SymbolLayer(
                "${SAVED_MARKERS_LAYER_ID}_$position",
                "saved-markers-source-id-$position"
            ).withProperties(
                PropertyFactory.iconImage(imageName),
                PropertyFactory.visibility(Property.VISIBLE),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconIgnorePlacement(true),
                PropertyFactory.iconAnchor(Property.ICON_ANCHOR_BOTTOM)
            )
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_map, container, false)
    }
}