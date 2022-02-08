package net.ttrstudios.in2000_team_47_app.frontend.MainActivity

import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import net.ttrstudios.in2000_team_47_app.R
import net.ttrstudios.in2000_team_47_app.backend.models.Poi
import net.ttrstudios.in2000_team_47_app.backend.utils.selectWeatherIcon

private const val POINT_NAME = "pointName"
private const val LATITUDE = "placeLatitude"
private const val LONGITUDE = "placeLongitude"
private const val PRECIPITATION = "precipitation"
private const val WAVE_HEIGHT = "waveHeight"
private const val WIND_SPEED = "windSpeed"
private const val TEMPERATURE = "temperature"
private const val SYMBOL_CODE = "symbolCode"
private const val WARNINGS = "warnings"
private const val POINT_INDEX = "pointIndex"

class ClickPointDialogFragment : DialogFragment() {
    private lateinit var viewmodel: MainActivityViewModel
    private var pointName: String? = null
    private var latitude: Double? = null
    private var longitude: Double? = null
    private var precipitation: Double? = null
    private var waveHeight: Double? = null
    private var windSpeed: Int? = null
    private var temperature: Int? = null
    private var symbolCode: String? = null
    private var warnings: Array<String>? = null
    private var pointIndex: Int? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        arguments?.let {
            pointName = it.getString(POINT_NAME)
            latitude = it.getDouble(LATITUDE)
            longitude = it.getDouble(LONGITUDE)
            precipitation = it.getDouble(PRECIPITATION)
            waveHeight = it.getDouble(WAVE_HEIGHT)
            windSpeed = it.getInt(WIND_SPEED)
            temperature = it.getInt(TEMPERATURE)
            symbolCode = it.getString(SYMBOL_CODE)
            warnings = it.getStringArray(WARNINGS)
            pointIndex = it.getInt(POINT_INDEX)
        }

        val vm by activityViewModels<MainActivityViewModel>() //Connecting to ViewModel
        viewmodel = vm

        return AlertDialog.Builder(requireContext())
            .setView(R.layout.fragment_click_point_dialog)
            .create()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setupView()
        setupClickListeners()
    }

    private fun setupView() {
        dialog?.findViewById<TextView>(R.id.clickPointLocationName)?.text = pointName
        val latitudeText = String.format("%.5f", latitude)
        val longitudeText = String.format("%.5f", longitude)
        dialog?.findViewById<TextView>(R.id.clickPointLatitude)?.text = latitudeText
        dialog?.findViewById<TextView>(R.id.clickPointLongitude)?.text = longitudeText

        val precipitationTextView = dialog?.findViewById<TextView>(R.id.clickPointPrecipitationText)
        precipitationTextView?.text = if (precipitation != -1.0) precipitation.toString() else "—"

        val waveHeightTextView = dialog?.findViewById<TextView>(R.id.clickPointWaveHeightText)
        waveHeightTextView?.text = if (waveHeight != -1.0) waveHeight.toString() else "—"

        val windSpeedTextView = dialog?.findViewById<TextView>(R.id.clickPointWindText)
        windSpeedTextView?.text = if (windSpeed != -1) windSpeed.toString() else "—"

        val temperatureTextView = dialog?.findViewById<TextView>(R.id.clickPointTemperatureText)
        temperatureTextView?.text = if (temperature != -99) temperature.toString() else "—"
        dialog?.findViewById<ImageView>(R.id.clickPointTemperatureIcon)
            ?.setImageResource(selectWeatherIcon(symbolCode))

        // If weather values are outside preferences they will have a warning, and if
        // that is the case we change the color of text and icons.
        for (warning in warnings!!) {
            when (warning) {
                "Precipitation" -> {
                    precipitationTextView
                        ?.setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.warningOrange
                            )
                        )
                    dialog?.findViewById<TextView>(R.id.clickPointPrecipitationUnit)
                        ?.setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.warningOrange
                            )
                        )
                    dialog?.findViewById<ImageView>(R.id.clickPointPrecipitationIcon)
                        ?.setImageResource(R.drawable.rain_icon_orange_figma)
                    dialog?.findViewById<View>(R.id.clickPointPrecipitationUnderline)
                        ?.visibility = View.VISIBLE
                }
                "Waves" -> {
                    waveHeightTextView
                        ?.setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.warningOrange
                            )
                        )
                    dialog?.findViewById<TextView>(R.id.clickPointWaveHeightUnit)
                        ?.setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.warningOrange
                            )
                        )
                    dialog?.findViewById<ImageView>(R.id.clickPointWaveHeightIcon)
                        ?.setImageResource(R.drawable.wave_height_icon_orange_figma)
                    dialog?.findViewById<View>(R.id.clickPointWaveHeightUnderline)
                        ?.visibility = View.VISIBLE
                }
                "Wind" -> {
                    windSpeedTextView
                        ?.setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.warningOrange
                            )
                        )
                    dialog?.findViewById<TextView>(R.id.clickPointWindUnit)
                        ?.setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.warningOrange
                            )
                        )
                    dialog?.findViewById<ImageView>(R.id.clickPointWindIcon)
                        ?.setImageResource(R.drawable.ic_round_air_red_24)
                    dialog?.findViewById<View>(R.id.clickPointWindUnderline)?.visibility =
                        View.VISIBLE
                }
                "Temperature" -> {
                    temperatureTextView?.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.warningOrange
                        )
                    )
                    dialog?.findViewById<TextView>(R.id.clickPointTemperatureUnit)
                        ?.setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.warningOrange
                            )
                        )
                    dialog?.findViewById<View>(R.id.clickPointTemperatureUnderline)
                        ?.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setupClickListeners() {
        dialog?.findViewById<ImageView>(R.id.clickPointDeleteIcon)?.setOnClickListener {
            val pointList: MutableList<Poi> = viewmodel.getData()
            pointList.removeAt(pointIndex!!)
            viewmodel.saveData(pointList)
            (parentFragment as MapViewFragment).deletePlace(pointIndex!!)
            dismiss()
        }
        dialog?.findViewById<ImageView>(R.id.clickPointDismissIcon)?.setOnClickListener {
            dismiss()
        }
    }

    companion object {
        const val TAG = "ClickPointDialog"

        @JvmStatic
        fun newInstance(
            pointName: String, latitude: Double, longitude: Double, precipitation: Double,
            waveHeight: Double, windSpeed: Int, temperature: Int, symbolCode: String,
            warnings: Array<String>, pointIndex: Int
        ) =
            ClickPointDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(POINT_NAME, pointName)
                    putDouble(LATITUDE, latitude)
                    putDouble(LONGITUDE, longitude)
                    putDouble(PRECIPITATION, precipitation)
                    putDouble(WAVE_HEIGHT, waveHeight)
                    putInt(WIND_SPEED, windSpeed)
                    putInt(TEMPERATURE, temperature)
                    putString(SYMBOL_CODE, symbolCode)
                    putStringArray(WARNINGS, warnings)
                    putInt(POINT_INDEX, pointIndex)
                }
            }
    }
}