package net.ttrstudios.in2000_team_47_app.frontend.MainActivity

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.DialogFragment
import net.ttrstudios.in2000_team_47_app.R

private const val PLACE_NAME = "placeName"
private const val PLACE_LATITUDE = "placeLatitude"
private const val PLACE_LONGITUDE = "placeLongitude"
private const val PLACE_COLOR = "placeColor"
private const val POSITION = "position"

class PlaceSettingsDialogFragment : DialogFragment() {

    private var placeName: String? = null
    private var placeLatitude: Double? = null
    private var placeLongitude: Double? = null
    private var placeColor: String? = null
    private var position: Int? = null
    private var forecastAdapter: ForecastAdapter? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        arguments?.let {
            placeName = it.getString(PLACE_NAME)
            placeLatitude = it.getDouble(PLACE_LATITUDE)
            placeLongitude = it.getDouble(PLACE_LONGITUDE)
            placeColor = it.getString(PLACE_COLOR)
            position = it.getInt(POSITION)
        }
        return AlertDialog.Builder(requireContext())
            .setView(R.layout.fragment_place_settings_dialog)
            .create()
    }

    override fun onStart() {
        super.onStart()
        setupView()
        setupClickListeners()
    }

    private fun setupView() {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val latitudeText = String.format("%.5f", placeLatitude)
        val longitudeText = String.format("%.5f", placeLongitude)
        dialog?.findViewById<TextView>(R.id.placeSettingsLatitude)?.text = latitudeText
        dialog?.findViewById<TextView>(R.id.placeSettingsLongitude)?.text = longitudeText
        val placeNameBar = dialog?.findViewById<EditText>(R.id.placeNameEditText)
        placeNameBar?.hint = placeName
    }

    private fun setupClickListeners() {
        val placeNameBar = dialog?.findViewById<EditText>(R.id.placeNameEditText)
        placeNameBar?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                placeNameBar.clearFocus()
                val imm =
                    activity?.getSystemService(Activity.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.hideSoftInputFromWindow(dialog?.currentFocus?.windowToken, 0)
            }
            false
        }

        // Hide soft keyboard
        dialog?.findViewById<ConstraintLayout>(R.id.placeSettingsLayout)?.setOnClickListener {
            placeNameBar?.onEditorAction(EditorInfo.IME_ACTION_DONE)
        }

        dialog?.findViewById<TextView>(R.id.saveText)?.setOnClickListener {
            forecastAdapter?.doSaveClick(placeNameBar?.text.toString(), placeColor!!, position!!)
            dialog?.dismiss()
        }

        dialog?.findViewById<TextView>(R.id.deleteText)?.setOnClickListener {
            forecastAdapter?.doDeleteClick(position!!)
            dialog?.dismiss()
        }

        dialog?.findViewById<ImageView>(R.id.placeSettingsDismissIcon)?.setOnClickListener {
            dialog?.dismiss()
        }

        // Color selection buttons
        dialog?.findViewById<ImageView>(R.id.changeColorRed)?.setOnClickListener {
            placeNameBar?.onEditorAction(EditorInfo.IME_ACTION_DONE)
            changeMarkedColor(it as ImageView?)
            placeColor = "Red"
        }

        dialog?.findViewById<ImageView>(R.id.changeColorOrange)?.setOnClickListener {
            placeNameBar?.onEditorAction(EditorInfo.IME_ACTION_DONE)
            changeMarkedColor(it as ImageView?)
            placeColor = "Orange"
        }

        dialog?.findViewById<ImageView>(R.id.changeColorYellow)?.setOnClickListener {
            placeNameBar?.onEditorAction(EditorInfo.IME_ACTION_DONE)
            changeMarkedColor(it as ImageView?)
            placeColor = "Yellow"
        }

        dialog?.findViewById<ImageView>(R.id.changeColorPink)?.setOnClickListener {
            placeNameBar?.onEditorAction(EditorInfo.IME_ACTION_DONE)
            changeMarkedColor(it as ImageView?)
            placeColor = "Pink"
        }

        dialog?.findViewById<ImageView>(R.id.changeColorGreen)?.setOnClickListener {
            placeNameBar?.onEditorAction(EditorInfo.IME_ACTION_DONE)
            changeMarkedColor(it as ImageView?)
            placeColor = "Green"
        }

        dialog?.findViewById<ImageView>(R.id.changeColorPurple)?.setOnClickListener {
            placeNameBar?.onEditorAction(EditorInfo.IME_ACTION_DONE)
            changeMarkedColor(it as ImageView?)
            placeColor = "Purple"
        }

        dialog?.findViewById<ImageView>(R.id.changeColorBlue)?.setOnClickListener {
            placeNameBar?.onEditorAction(EditorInfo.IME_ACTION_DONE)
            changeMarkedColor(it as ImageView?)
            placeColor = "Blue"
        }

        dialog?.findViewById<ImageView>(R.id.changeColorNavyBlue)?.setOnClickListener {
            placeNameBar?.onEditorAction(EditorInfo.IME_ACTION_DONE)
            changeMarkedColor(it as ImageView?)
            placeColor = "Navy Blue"
        }
    }

    private fun changeMarkedColor(colorButton: ImageView?) {
        val redButton = dialog?.findViewById<ImageView>(R.id.changeColorRed)
        val orangeButton = dialog?.findViewById<ImageView>(R.id.changeColorOrange)
        val yellowButton = dialog?.findViewById<ImageView>(R.id.changeColorYellow)
        val pinkButton = dialog?.findViewById<ImageView>(R.id.changeColorPink)
        val greenButton = dialog?.findViewById<ImageView>(R.id.changeColorGreen)
        val purpleButton = dialog?.findViewById<ImageView>(R.id.changeColorPurple)
        val blueButton = dialog?.findViewById<ImageView>(R.id.changeColorBlue)
        val navyBlueButton = dialog?.findViewById<ImageView>(R.id.changeColorNavyBlue)

        val buttons = listOf(
            redButton, orangeButton, yellowButton, pinkButton, greenButton,
            purpleButton, blueButton, navyBlueButton
        )
        for (button in buttons) button?.setBackgroundResource(R.drawable.change_color_not_selected_bg)
        colorButton?.setBackgroundResource(R.drawable.change_color_selected_bg)
    }

    fun setForecastAdapter(adapter: ForecastAdapter) {
        forecastAdapter = adapter
    }

    companion object {
        const val TAG = "placeSettingsDialog"

        @JvmStatic
        fun newInstance(
            placeName: String, placeLatitude: Double, placeLongitude: Double,
            placeColor: String, position: Int
        ) =
            PlaceSettingsDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(PLACE_NAME, placeName)
                    putDouble(PLACE_LATITUDE, placeLatitude)
                    putDouble(PLACE_LONGITUDE, placeLongitude)
                    putString(PLACE_COLOR, placeColor)
                    putInt(POSITION, position)
                }
            }
    }
}