package net.ttrstudios.in2000_team_47_app.frontend.MainActivity

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.crystal.crystalrangeseekbar.widgets.CrystalRangeSeekbar
import net.ttrstudios.in2000_team_47_app.R
import net.ttrstudios.in2000_team_47_app.backend.models.ForecastLimits

class WeatherProfileFragment : Fragment(R.layout.fragment_weather_profile) {
    private lateinit var viewModel: MainActivityViewModel
    private lateinit var windSpeedSeek: CrystalRangeSeekbar
    private lateinit var waveHeightSeek: CrystalRangeSeekbar
    private lateinit var tempSeek: CrystalRangeSeekbar
    private lateinit var rainSeek: CrystalRangeSeekbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val vm by activityViewModels<MainActivityViewModel>() //Connecting to ViewModel
        viewModel = vm
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        windSpeedSeek = view.findViewById(R.id.windSeek)
        waveHeightSeek = view.findViewById(R.id.waveHeightSeek)
        tempSeek = view.findViewById(R.id.tempSeek)
        rainSeek = view.findViewById(R.id.rainSeek)

        val windRange = view.findViewById<TextView>(R.id.rangeWindText)
        val waveHeightRange = view.findViewById<TextView>(R.id.rangeWaveHeightText)
        val tempRange = view.findViewById<TextView>(R.id.rangeTempText)
        val rainRange = view.findViewById<TextView>(R.id.rangeRainText)

        var precipitationLow: Int
        var precipitationHigh: Int
        var waveHeightLow: Int
        var waveHeightHigh: Int
        var windSpeedLow: Int
        var windSpeedHigh: Int
        var temperatureLow: Int
        var temperatureHigh: Int

        viewModel.getLimits() // Updates the limits

        viewModel.limitsLiveData.observe(viewLifecycleOwner) { fl -> // Updates the preferences every time a new change has happened to them
            precipitationLow = fl.precipitationLow
            precipitationHigh = fl.precipitationHigh
            waveHeightLow = fl.waveHeightLow
            waveHeightHigh = fl.waveHeightHigh
            windSpeedLow = fl.windSpeedLow
            windSpeedHigh = fl.windSpeedHigh
            temperatureLow = fl.temperatureLow
            temperatureHigh = fl.temperatureHigh

            // Setting the range of each seekbar and then finding their limits and set it to that
            windSpeedSeek.setMaxValue(30F)
            windSpeedSeek.setMinValue(0F)

            windSpeedSeek.setMaxStartValue(windSpeedHigh.toFloat())
            windSpeedSeek.setMinStartValue(windSpeedLow.toFloat())
            windSpeedSeek.apply()

            waveHeightSeek.setMaxValue(10F)
            waveHeightSeek.setMinValue(0F)

            waveHeightSeek.setMaxStartValue(waveHeightHigh.toFloat())
            waveHeightSeek.setMinStartValue(waveHeightLow.toFloat())
            waveHeightSeek.apply()

            tempSeek.setMaxValue(40F)
            tempSeek.setMinValue(-20F)

            tempSeek.setMaxStartValue(temperatureHigh.toFloat())
            tempSeek.setMinStartValue(temperatureLow.toFloat())
            tempSeek.apply()

            rainSeek.setMaxValue(30F)
            rainSeek.setMinValue(0F)

            rainSeek.setMaxStartValue(precipitationHigh.toFloat())
            rainSeek.setMinStartValue(precipitationLow.toFloat())
            rainSeek.apply()
        }

        //On change for all seekbars
        windSpeedSeek.setOnRangeSeekbarChangeListener { minValue, maxValue ->
            val windText = "$minValue-$maxValue m/s"
            windRange.text = windText
        }

        waveHeightSeek.setOnRangeSeekbarChangeListener { minValue, maxValue ->
            val waveHeightText = "$minValue-$maxValue m"
            waveHeightRange.text = waveHeightText
        }

        tempSeek.setOnRangeSeekbarChangeListener { minValue, maxValue ->
            val tempText = "$minValue-$maxValue\u2103"
            tempRange.text = tempText
        }

        rainSeek.setOnRangeSeekbarChangeListener { minValue, maxValue ->
            val rainText = "$minValue-$maxValue mm/h"
            rainRange.text = rainText
        }
    }

    private fun updateLimits() {
        val newLimits = ForecastLimits(
            rainSeek.selectedMinValue.toInt(),
            rainSeek.selectedMaxValue.toInt(), waveHeightSeek.selectedMinValue.toInt(),
            waveHeightSeek.selectedMaxValue.toInt(), windSpeedSeek.selectedMinValue.toInt(),
            windSpeedSeek.selectedMaxValue.toInt(), tempSeek.selectedMinValue.toInt(),
            tempSeek.selectedMaxValue.toInt()
        )
        viewModel.saveLimits(newLimits) //Saving the new limits
        viewModel.updateForecastWarnings() //Telling the view model to update the forcast warnings
    }

    override fun onDestroyView() {
        super.onDestroyView()
        updateLimits()
    }
}
