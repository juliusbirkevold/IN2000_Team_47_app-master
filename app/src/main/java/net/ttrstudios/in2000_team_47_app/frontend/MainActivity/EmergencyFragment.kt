package net.ttrstudios.in2000_team_47_app.frontend.MainActivity

import android.Manifest
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.location.Location
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
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
import net.ttrstudios.in2000_team_47_app.backend.utils.convertDDcoordsToDMS
import java.util.*

class EmergencyFragment : Fragment(), OnMapReadyCallback, PermissionsListener {
    private lateinit var mapView: MapView
    private lateinit var mapboxMap: MapboxMap
    private val DROPPED_MARKER_LAYER_ID = "DROPPED_MARKER_LAYER_ID"
    private lateinit var droppedMarkerLayer: Layer
    private lateinit var location: Location
    private lateinit var mPermissionResult: ActivityResultLauncher<String>
    private lateinit var viewModel: MainActivityViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(requireActivity().baseContext, getString(R.string.mapbox_access_token))
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
        return inflater.inflate(R.layout.fragment_emergency, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!PermissionsManager.areLocationPermissionsGranted(requireActivity().baseContext)) {
            requireActivity().findViewById<CardView>(R.id.cantUseCard).visibility = VISIBLE
            mPermissionResult.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        val vm by activityViewModels<MainActivityViewModel>()
        viewModel = vm
        viewModel.singleLocationUpdate()
        viewModel.locationLiveData.observe(viewLifecycleOwner) {
            requireView().findViewById<ProgressBar>(R.id.assistanceProgressBar).visibility = GONE
            location = it
            val coordinateStrings = convertDDcoordsToDMS(location.latitude, location.longitude)
            requireView().findViewById<TextView>(R.id.assistanceLatValue).text =
                coordinateStrings[0]
            requireView().findViewById<TextView>(R.id.assistanceLonValue).text =
                coordinateStrings[1]
            val time = requireView().findViewById<TextView>(R.id.assistanceTimeValue)
            time.text = Calendar.getInstance().time.toString().substring(11, 16)
            time.visibility = VISIBLE
            mapView = requireView().findViewById(R.id.assistanceMapView)
            mapView.onCreate(savedInstanceState)
            mapView.getMapAsync(this)
        }

        // Getting emergency numbers from assets
        val emtNbrs = viewModel.getEmt()

        // Splitting emergencies
        val emt1 = emtNbrs[0].split(" - ")
        val emt2 = emtNbrs[1].split(" - ")
        val emt3 = emtNbrs[2].split(" - ")
        val emt4 = emtNbrs[3].split(" - ")
        val emt5 = emtNbrs[4].split(" - ")

        // Setting the text into the fragment
        view.findViewById<TextView>(R.id.emt1_1).text = emt1[0]
        view.findViewById<TextView>(R.id.emt1_2).text = emt1[1]

        view.findViewById<TextView>(R.id.emt2_1).text = emt2[0]
        view.findViewById<TextView>(R.id.emt2_2).text = emt2[1]

        view.findViewById<TextView>(R.id.emt3_1).text = emt3[0]
        view.findViewById<TextView>(R.id.emt3_2).text = emt3[1]

        view.findViewById<TextView>(R.id.emt4_1).text = emt4[0]
        view.findViewById<TextView>(R.id.emt4_2).text = emt4[1]

        view.findViewById<TextView>(R.id.emt5_1).text = emt5[0]
        view.findViewById<TextView>(R.id.emt5_2).text = emt5[1]
    }

    override fun onStart() {
        super.onStart()
        requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView).menu
            .findItem(R.id.navigation_emergency).isChecked = true
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.uiSettings.setAllGesturesEnabled(false) // Prevent moving/zooming map
        mapboxMap.setStyle(
            Style.Builder().fromUri("mapbox://styles/mapbox/outdoors-v11")
        ) { style ->

            // Map is set up and the style has loaded. Now you can add data or make other map adjustments

            // Show the users location on the map
            initDroppedMarker(style)
            updateCamera(location.latitude, location.longitude)
            if (style.getLayer(DROPPED_MARKER_LAYER_ID) != null) {
                val source: GeoJsonSource = style.getSourceAs("dropped-marker-source-id")
                    ?: return@setStyle
                source.setGeoJson(Point.fromLngLat(location.longitude, location.latitude))

                droppedMarkerLayer = style.getLayer(DROPPED_MARKER_LAYER_ID) ?: return@setStyle
                droppedMarkerLayer.setProperties(
                    PropertyFactory.visibility(Property.VISIBLE),
                    PropertyFactory.iconAnchor(Property.ICON_ANCHOR_BOTTOM)
                )
            }
        }
    }

    private fun updateCamera(latitude: Double, longitude: Double) {
        val position = CameraPosition.Builder()
            .target(LatLng(latitude, longitude)) // Sets the new camera position
            .zoom(12.0) // Sets the zoom
            .bearing(0.0) // Rotate the camera
            .tilt(0.0) // Set the camera tilt
            .build() // Creates a CameraPosition from the builder

        mapboxMap.moveCamera(
            CameraUpdateFactory
                .newCameraPosition(position)
        )
    }

    // Initializes the marker that shows user-location.
    private fun initDroppedMarker(loadedMapStyle: Style) {
        // Add the marker image to map
        val drawable: Drawable? = ResourcesCompat.getDrawable(
            resources,
            R.drawable.ic_baseline_place_24,
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

    override fun onExplanationNeeded(permissionsToExplain: List<String>) {
        Toast.makeText(
            requireActivity().baseContext,
            R.string.user_location_permission_explanation,
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            Toast.makeText(
                requireActivity().baseContext,
                R.string.user_location_permission_granted,
                Toast.LENGTH_LONG
            ).show()
            viewModel.singleLocationUpdate()
            requireActivity().findViewById<CardView>(R.id.cantUseCard).visibility = GONE
        } else {
            requireActivity().findViewById<CardView>(R.id.cantUseCard).visibility = VISIBLE
            Toast.makeText(
                requireActivity().baseContext,
                R.string.user_location_permission_not_granted_mob,
                Toast.LENGTH_LONG
            ).show()
        }
    }
}