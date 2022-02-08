package net.ttrstudios.in2000_team_47_app.frontend.MainActivity

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mapbox.geojson.Point
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
import net.ttrstudios.in2000_team_47_app.backend.models.Poi
import net.ttrstudios.in2000_team_47_app.backend.models.WeatherForecast
import net.ttrstudios.in2000_team_47_app.backend.utils.ItemMoveCallback
import net.ttrstudios.in2000_team_47_app.backend.utils.selectWeatherIcon
import java.util.*

private var expandedPosition = -1
private var previousExpandedPosition = -1
private var pos = -1

class ForecastAdapter(
    private val forecasts: MutableList<WeatherForecast>,
    private val savedInstanceState: Bundle?,
    private val context: Context,
    private val activity: FragmentActivity,
    private val fragmentView: View,
    private val viewModel: MainActivityViewModel
) :
    RecyclerView.Adapter<ForecastAdapter.ViewHolder>(), OnMapReadyCallback,
    ItemMoveCallback.ItemTouchHelperContract {

    private lateinit var mapboxMap: MapboxMap
    private val DROPPED_MARKER_LAYER_ID = "DROPPED_MARKER_LAYER_ID"
    private lateinit var droppedMarkerLayer: Layer
    private val positionReservedName = "Min posisjon"

    inner class ViewHolder(cardView: View) : RecyclerView.ViewHolder(cardView) {
        var forecastView: CardView = cardView.findViewById(R.id.forecastCardView)
        var forecastViewConstraintLayout: ConstraintLayout =
            cardView.findViewById(R.id.forecastConstraintLayout)
        var locationName: TextView = cardView.findViewById(R.id.locationName)
        var locationLatitude: TextView = cardView.findViewById(R.id.locationLatitude)
        var locationLongitude: TextView = cardView.findViewById(R.id.locationLongitude)
        var locationLatText: TextView = cardView.findViewById(R.id.locationLatText)
        var locationLonText: TextView = cardView.findViewById(R.id.locationLonText)
        var precipitation: TextView = cardView.findViewById(R.id.precipitationText)
        var waveHeight: TextView = cardView.findViewById(R.id.waveHeightText)
        var wind: TextView = cardView.findViewById(R.id.windText)
        var temperature: TextView = cardView.findViewById(R.id.temperatureText)
        var alerts: ConstraintLayout = cardView.findViewById(R.id.constraintLayoutAlerts)
        var alertContent: TextView = cardView.findViewById(R.id.alertContent)
        var alertInstruction: TextView = cardView.findViewById(R.id.instructionContent)
        var alertConsequence: TextView = cardView.findViewById(R.id.consequenceContent)
        var alertTimePeriod: TextView = cardView.findViewById(R.id.timePeriodContent)
        var alertNext: ImageView = cardView.findViewById(R.id.nextAlertIcon)
        var alertPrev: ImageView = cardView.findViewById(R.id.previousAlertIcon)
        var colorMark: ImageView = cardView.findViewById(R.id.colorMarkIcon)
        var mapView: MapView = cardView.findViewById(R.id.forecastMapView)
        var expandIcon: ImageView = cardView.findViewById(R.id.expandIcon)
        var precipitationUnit: TextView = cardView.findViewById(R.id.precipitationUnit)
        var waveHeightUnit: TextView = cardView.findViewById(R.id.waveHeightUnit)
        var windUnit: TextView = cardView.findViewById(R.id.windUnit)
        var temperatureUnit: TextView = cardView.findViewById(R.id.temperatureUnit)
        var precipitationIcon: ImageView = cardView.findViewById(R.id.precipitationIcon)
        var waveHeightIcon: ImageView = cardView.findViewById(R.id.waveHeightIcon)
        var windIcon: ImageView = cardView.findViewById(R.id.windIcon)
        var temperatureIcon: ImageView = cardView.findViewById(R.id.temperatureIcon)
        var precipitationUnderline: View = cardView.findViewById(R.id.precipitationUnderline)
        var waveHeightUnderline: View = cardView.findViewById(R.id.waveHeightUnderline)
        var windUnderline: View = cardView.findViewById(R.id.windUnderline)
        var temperatureUnderline: View = cardView.findViewById(R.id.temperatureUnderline)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForecastAdapter.ViewHolder {
        val cardView = LayoutInflater.from(parent.context)
            .inflate(R.layout.places_element, parent, false)
        return ViewHolder(cardView)
    }

    // Set up the forecast card views
    override fun onBindViewHolder(holder: ForecastAdapter.ViewHolder, position: Int) {
        val forecast: WeatherForecast = forecasts[position]
        holder.locationName.text = forecast.locationName
        holder.locationLatitude.text = String.format("%.5f", forecast.coordinates.latitude)
        holder.locationLongitude.text = String.format("%.5f", forecast.coordinates.longitude)

        val waveHeightText = "${forecast.waveHeight ?: "—"}"
        holder.waveHeight.text = waveHeightText
        holder.waveHeightUnit.visibility =
            if (forecast.waveHeight != null) View.VISIBLE else View.INVISIBLE

        val windText = "${forecast.wind ?: "—"}"
        holder.wind.text = windText
        holder.windUnit.visibility = if (forecast.wind != null) View.VISIBLE else View.INVISIBLE

        val tempText = "${forecast.temperature ?: "—"}"
        holder.temperatureUnit.visibility =
            if (forecast.temperature != null) View.VISIBLE else View.INVISIBLE
        holder.temperature.text = tempText

        val precipitationText = "${forecast.precipitation ?: "—"}"
        holder.precipitation.text = precipitationText
        holder.precipitationUnit.visibility =
            if (forecast.precipitation != null) View.VISIBLE else View.INVISIBLE

        when (forecast.markerColor) {
            "Position" -> holder.colorMark.setImageResource(R.drawable.ic_baseline_place_24)
            "Red" -> holder.colorMark.setImageResource(R.drawable.ic_baseline_circle_red_24)
            "Orange" -> holder.colorMark.setImageResource(R.drawable.ic_baseline_circle_orange_24)
            "Yellow" -> holder.colorMark.setImageResource(R.drawable.ic_baseline_circle_yellow_24)
            "Green" -> holder.colorMark.setImageResource(R.drawable.ic_baseline_circle_green_24)
            "Pink" -> holder.colorMark.setImageResource(R.drawable.ic_baseline_circle_pink_24)
            "Purple" -> holder.colorMark.setImageResource(R.drawable.ic_baseline_circle_purple_24)
            "Blue" -> holder.colorMark.setImageResource(R.drawable.ic_baseline_circle_blue_24)
            "Navy Blue" -> holder.colorMark.setImageResource(R.drawable.ic_baseline_circle_navy_blue_24)
        }

        holder.precipitation.setTextColor(ContextCompat.getColor(context, R.color.titleBlue))
        holder.precipitationUnit.setTextColor(ContextCompat.getColor(context, R.color.titleBlue))
        holder.precipitationIcon.setImageResource(R.drawable.rain_icon_figma)
        holder.precipitationUnderline.visibility = View.INVISIBLE

        holder.waveHeight.setTextColor(ContextCompat.getColor(context, R.color.titleBlue))
        holder.waveHeightUnit.setTextColor(ContextCompat.getColor(context, R.color.titleBlue))
        holder.waveHeightIcon.setImageResource(R.drawable.wave_height_icon_figma)
        holder.waveHeightUnderline.visibility = View.INVISIBLE

        holder.wind.setTextColor(ContextCompat.getColor(context, R.color.titleBlue))
        holder.windUnit.setTextColor(ContextCompat.getColor(context, R.color.titleBlue))
        holder.windIcon.setImageResource(R.drawable.ic_round_air_24)
        holder.windUnderline.visibility = View.INVISIBLE

        holder.temperature.setTextColor(ContextCompat.getColor(context, R.color.titleBlue))
        holder.temperatureUnit.setTextColor(ContextCompat.getColor(context, R.color.titleBlue))
        holder.temperatureIcon.setImageResource(selectWeatherIcon(forecast.symbolCode))
        holder.temperatureUnderline.visibility = View.INVISIBLE

        // If weather values are outside preferences they will have a warning, and if
        // that is the case we change the color of text and icons.
        for (warning in forecast.warnings) {
            when (warning) {
                "Precipitation" -> {
                    holder.precipitation
                        .setTextColor(ContextCompat.getColor(context, R.color.warningOrange))
                    holder.precipitationUnit
                        .setTextColor(ContextCompat.getColor(context, R.color.warningOrange))
                    holder.precipitationIcon.setImageResource(R.drawable.rain_icon_orange_figma)
                    holder.precipitationUnderline.visibility = View.VISIBLE
                }
                "Waves" -> {
                    holder.waveHeight
                        .setTextColor(ContextCompat.getColor(context, R.color.warningOrange))
                    holder.waveHeightUnit
                        .setTextColor(ContextCompat.getColor(context, R.color.warningOrange))
                    holder.waveHeightIcon.setImageResource(R.drawable.wave_height_icon_orange_figma)
                    holder.waveHeightUnderline.visibility = View.VISIBLE
                }
                "Wind" -> {
                    holder.wind.setTextColor(ContextCompat.getColor(context, R.color.warningOrange))
                    holder.windUnit.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.warningOrange
                        )
                    )
                    holder.windIcon.setImageResource(R.drawable.ic_round_air_red_24)
                    holder.windUnderline.visibility = View.VISIBLE
                }
                "Temperature" -> {
                    holder.temperature
                        .setTextColor(ContextCompat.getColor(context, R.color.warningOrange))
                    holder.temperatureUnit
                        .setTextColor(ContextCompat.getColor(context, R.color.warningOrange))
                    holder.temperatureUnderline.visibility = View.VISIBLE
                }
            }
        }

        val isExpanded = position == expandedPosition

        // emptyListMessage is only in front of recycler view (and therefore clickable) when
        // no card view is expanded (only position card view possible since list is empty).
        val emptyListMessage =
            fragmentView.findViewById<ConstraintLayout>(R.id.emptyListMessageConstraintLayout)
        val recyclerView = fragmentView.findViewById<RecyclerView>(R.id.forecastRecyclerView)
        if (isExpanded) recyclerView.bringToFront()
        else emptyListMessage.bringToFront()

        // Show/hide expanded card view
        holder.mapView.visibility = if (isExpanded) View.VISIBLE else View.GONE
        holder.forecastView.isActivated = isExpanded

        if (isExpanded) {
            holder.colorMark.visibility = View.VISIBLE
            holder.locationLatitude.visibility = View.VISIBLE
            holder.locationLongitude.visibility = View.VISIBLE
            holder.locationLatText.visibility = View.VISIBLE
            holder.locationLonText.visibility = View.VISIBLE
        } else {
            holder.colorMark.visibility = View.INVISIBLE
            holder.locationLatitude.visibility = View.GONE
            holder.locationLongitude.visibility = View.GONE
            holder.locationLatText.visibility = View.GONE
            holder.locationLonText.visibility = View.GONE
        }

        // Set up alerts in the card views if there are any
        val alertNumber = forecast.alertShownNumber
        when (forecast.type) {
            "Regular" -> {
                holder.forecastViewConstraintLayout
                    .setBackgroundResource(R.drawable.places_element_bg)
                holder.alerts.visibility = View.GONE

            }
            "Alerts" -> {
                holder.alertContent.text = forecast.alerts[alertNumber].headline ?: "n/a"
                holder.alertTimePeriod.text = forecast.alerts[alertNumber].timePeriod ?: "n/a"
                holder.alertInstruction.text = forecast.alerts[alertNumber].instruction ?: "n/a"
                holder.alertConsequence.text = forecast.alerts[alertNumber].description ?: "n/a"

                if (isExpanded) holder.alerts.visibility = View.VISIBLE
                else {
                    holder.colorMark.setImageResource(R.drawable.warning_icon)
                    holder.alerts.visibility = View.GONE
                    holder.colorMark.visibility = View.VISIBLE
                }
                holder.forecastViewConstraintLayout
                    .setBackgroundResource(R.drawable.places_element_bg_alerts)

                holder.alertNext.visibility = if (alertNumber < forecast.alerts.size - 1)
                    View.VISIBLE else View.GONE
                holder.alertPrev.visibility = if (alertNumber > 0) View.VISIBLE else View.GONE
            }
            "Warnings" -> {
                holder.forecastViewConstraintLayout
                    .setBackgroundResource(R.drawable.places_element_bg_warnings)
                holder.alerts.visibility = View.GONE
            }
        }

        // Set up map for expanded card view
        if (isExpanded) {
            pos = position
            previousExpandedPosition = position
            holder.mapView.onCreate(savedInstanceState)
            holder.mapView.getMapAsync(this)
            if (this::mapboxMap.isInitialized) updateCamera(
                forecasts[position].coordinates.latitude,
                forecasts[position].coordinates.longitude
            )
        }

        // Expand/collapse card view on click
        holder.forecastView.setOnClickListener {
            expandedPosition = if (isExpanded) -1 else position
            notifyItemChanged(previousExpandedPosition)
            notifyItemChanged(position)
        }

        // Remove edit button for user location card view
        if (forecast.locationName == positionReservedName) holder.expandIcon.visibility =
            View.INVISIBLE
        else holder.expandIcon.visibility = View.VISIBLE

        // Open forecast settings dialog when edit button is clicked
        holder.expandIcon.setOnClickListener {
            val placeSettingsFragment: PlaceSettingsDialogFragment = PlaceSettingsDialogFragment
                .newInstance(
                    forecast.locationName, forecast.coordinates.latitude,
                    forecast.coordinates.longitude, forecast.markerColor, position
                )
            placeSettingsFragment.setForecastAdapter(this)
            placeSettingsFragment.show(
                activity.supportFragmentManager,
                PlaceSettingsDialogFragment.TAG
            )
        }

        holder.alertNext.setOnClickListener {
            forecast.alertShownNumber++
            notifyItemChanged(position)
        }
        holder.alertPrev.setOnClickListener {
            forecast.alertShownNumber--
            notifyItemChanged(position)
        }
    }

    // When the save button is pressed in the forecast settings dialog, change the forecasts/saved point
    fun doSaveClick(placeName: String, placeColor: String, position: Int) {
        val nameIsNotBlank = (placeName.isNotBlank())
        if (nameIsNotBlank && (placeName != positionReservedName)) forecasts[position]
            .locationName = placeName
        forecasts[position].markerColor = placeColor
        notifyDataSetChanged()

        for (forecastForTimeLD in viewModel.forecastLiveData.value!!) {
            if (nameIsNotBlank && (placeName != positionReservedName)) forecastForTimeLD[position]
                .locationName = placeName
            forecastForTimeLD[position].markerColor = placeColor
        }

        val placesToSave = mutableListOf<Poi>()
        for (forecast in forecasts) {
            if (forecast.locationName == positionReservedName) continue
            placesToSave.add(
                Poi(
                    forecast.locationName, forecast.coordinates.longitude,
                    forecast.coordinates.latitude, forecast.markerColor
                )
            )
        }
        viewModel.saveData(placesToSave)
    }

    // When the delete button is pressed in the forecast settings dialog, delete the forecasts/saved point
    fun doDeleteClick(position: Int) {
        forecasts.removeAt(position)
        resetExpandedPos()
        notifyDataSetChanged()

        for (forecastForTimeLD in viewModel.forecastLiveData.value!!) {
            forecastForTimeLD.removeAt(position)
        }

        val placesToSave = mutableListOf<Poi>()
        for (forecast in forecasts) {
            if (forecast.locationName == positionReservedName) continue
            placesToSave.add(
                Poi(
                    forecast.locationName, forecast.coordinates.longitude,
                    forecast.coordinates.latitude, forecast.markerColor
                )
            )
        }
        viewModel.saveData(placesToSave)
        if (placesToSave.isEmpty()) {
            val emptyListMessage =
                fragmentView.findViewById<ConstraintLayout>(R.id.emptyListMessageConstraintLayout)
            val addPlaceBox =
                fragmentView.findViewById<ConstraintLayout>(R.id.addPlaceConstraintLayout)
            emptyListMessage.visibility = View.VISIBLE
            addPlaceBox.visibility = View.GONE
        }
    }

    override fun onRowMoved(fromPosition: Int, toPosition: Int) {
        if ((forecasts[fromPosition].locationName == positionReservedName) ||
            (forecasts[toPosition].locationName == positionReservedName)
        ) return
        if (toPosition == expandedPosition) expandedPosition = fromPosition
        else if (fromPosition == expandedPosition) expandedPosition = toPosition
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(forecasts, i, i + 1)
                for (forecastForTimeLD in viewModel.forecastLiveData.value!!) {
                    Collections.swap(forecastForTimeLD, i, i + 1)
                }
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(forecasts, i, i - 1)
                for (forecastForTimeLD in viewModel.forecastLiveData.value!!) {
                    Collections.swap(forecastForTimeLD, i, i - 1)
                }
            }
        }
        pos = fromPosition
        notifyItemMoved(fromPosition, toPosition)
    }

    override fun onRowSelected(viewHolder: ViewHolder) {
        if (viewHolder.locationName.text == positionReservedName) return
        viewHolder.forecastViewConstraintLayout.setBackgroundResource(R.drawable.places_element_bg_moving)
    }

    override fun onRowClear(viewHolder: ViewHolder) {
        notifyDataSetChanged()
        val placesToSave = mutableListOf<Poi>()
        for (forecast in forecasts) {
            if (forecast.locationName == positionReservedName) continue
            placesToSave.add(
                Poi(
                    forecast.locationName, forecast.coordinates.longitude,
                    forecast.coordinates.latitude, forecast.markerColor
                )
            )
        }
        viewModel.saveData(placesToSave)
    }

    override fun getItemCount(): Int {
        return forecasts.size
    }


    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.uiSettings.setAllGesturesEnabled(false) // Prevent moving/zooming map
        mapboxMap.setStyle(
            Style.Builder().fromUri("mapbox://styles/mapbox/outdoors-v11")
        ) { style ->

            // Map is set up and the style has loaded. Now you can add data or make other map adjustments

            // Move the camera to the point coordinates and place a marker there.
            initDroppedMarker(style, forecasts[pos].markerColor)
            updateCamera(forecasts[pos].coordinates.latitude, forecasts[pos].coordinates.longitude)
            if (style.getLayer(DROPPED_MARKER_LAYER_ID) != null) {
                val source: GeoJsonSource =
                    style.getSourceAs("dropped-marker-source-id") ?: return@setStyle
                source.setGeoJson(
                    Point.fromLngLat(
                        forecasts[pos].coordinates.longitude,
                        forecasts[pos].coordinates.latitude
                    )
                )

                droppedMarkerLayer = style.getLayer(DROPPED_MARKER_LAYER_ID) ?: return@setStyle
                droppedMarkerLayer.setProperties(
                    PropertyFactory.visibility(Property.VISIBLE),
                    PropertyFactory.iconAnchor(Property.ICON_ANCHOR_BOTTOM)
                )
            }
        }
        // When the user clicks a point map, move to the map fragment using the coordinates
        // of the map that was clicked.
        mapboxMap.addOnMapClickListener {
            viewModel.saveGpsStatus(false)
            //pointListService.saveFromClick(true)
            viewModel.saveCoordinates(forecasts[pos].coordinates)
            activity.findViewById<BottomNavigationView>(R.id.bottomNavigationView)
                .selectedItemId = R.id.navigation_map
            true
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

    // Initializes the marker showing the saved point
    private fun initDroppedMarker(loadedMapStyle: Style, markerColor: String) {
        // Add the marker image to map
        val drawable: Drawable? = when (markerColor) {
            "Position" -> ResourcesCompat.getDrawable(
                context.resources,
                R.drawable.ic_baseline_place_24,
                null
            )
            "Red" -> ResourcesCompat.getDrawable(
                context.resources,
                R.drawable.ic_baseline_place_red_24,
                null
            )
            "Orange" -> ResourcesCompat.getDrawable(
                context.resources,
                R.drawable.ic_baseline_place_orange_24,
                null
            )
            "Yellow" -> ResourcesCompat.getDrawable(
                context.resources,
                R.drawable.ic_baseline_place_yellow_24,
                null
            )
            "Green" -> ResourcesCompat.getDrawable(
                context.resources,
                R.drawable.ic_baseline_place_green_24,
                null
            )
            "Pink" -> ResourcesCompat.getDrawable(
                context.resources,
                R.drawable.ic_baseline_place_pink_24,
                null
            )
            "Purple" -> ResourcesCompat.getDrawable(
                context.resources,
                R.drawable.ic_baseline_place_purple_24,
                null
            )
            "Blue" -> ResourcesCompat.getDrawable(
                context.resources,
                R.drawable.ic_baseline_place_blue_24,
                null
            )
            "Navy Blue" -> ResourcesCompat.getDrawable(
                context.resources,
                R.drawable.ic_baseline_place_navy_blue_24,
                null
            )
            else -> ResourcesCompat.getDrawable(
                context.resources,
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
        loadedMapStyle.addSource(GeoJsonSource("dropped-marker-source-id"))
        loadedMapStyle.addLayer(
            SymbolLayer(
                DROPPED_MARKER_LAYER_ID,
                "dropped-marker-source-id"
            ).withProperties(
                PropertyFactory.iconImage(imageName),
                PropertyFactory.visibility(Property.NONE),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconIgnorePlacement(true)
            )
        )
    }

    fun resetExpandedPos() {
        expandedPosition = -1
    }
}