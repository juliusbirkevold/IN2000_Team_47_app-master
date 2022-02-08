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
import androidx.fragment.app.activityViewModels
import net.ttrstudios.in2000_team_47_app.R
import net.ttrstudios.in2000_team_47_app.backend.models.Poi

class EditPointDialogFragment : DialogFragment() {
    private lateinit var viewmodel: MainActivityViewModel
    private var poiIndex: Int = 0
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private lateinit var pointName: String
    private lateinit var poiColor: String
    private var poiIsNew: Boolean = true
    private val positionReservedName = "Min posisjon"

    companion object {

        const val TAG = "EditPoiDialog"

        private const val KEY_ISNEWPOINT = "KEY_ISNEWPOINT"
        private const val KEY_INDEX = "KEY_INDEX"
        private const val KEY_NAME = "KEY_NAME"
        private const val KEY_LAT = "KEY_LAT"
        private const val KEY_LONG = "KEY_LONG"
        private const val KEY_COLOR = "KEY_COLOR"

        fun newInstance(
            isNewPoint: Boolean,
            index: Int,
            name: String,
            lat: Double,
            long: Double,
            color: String
        ): EditPointDialogFragment {
            val args = Bundle()
            args.putString(KEY_NAME, name)
            args.putDouble(KEY_LAT, lat)
            args.putDouble(KEY_LONG, long)
            args.putString(KEY_COLOR, color)
            args.putInt(KEY_INDEX, index)
            args.putBoolean(KEY_ISNEWPOINT, isNewPoint)
            val fragment = EditPointDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        arguments?.let {
            poiIsNew = requireArguments().getBoolean(KEY_ISNEWPOINT)
            poiIndex = requireArguments().getInt(KEY_INDEX)
            longitude = requireArguments().getDouble(KEY_LONG)
            latitude = requireArguments().getDouble(KEY_LAT)
            pointName = requireArguments().getString(KEY_NAME).toString()
            poiColor = requireArguments().getString(KEY_COLOR).toString()
        }

        val vm by activityViewModels<MainActivityViewModel>() //Connecting to ViewModel
        viewmodel = vm

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
        dialog?.findViewById<TextView>(R.id.deleteText)?.text =
            resources.getString(R.string.abort_clickable_text)
        val latitudeText = String.format("%.5f", latitude)
        val longitudeText = String.format("%.5f", longitude)
        dialog?.findViewById<TextView>(R.id.placeSettingsLatitude)?.text = latitudeText
        dialog?.findViewById<TextView>(R.id.placeSettingsLongitude)?.text = longitudeText
        val placeNameBar = dialog?.findViewById<EditText>(R.id.placeNameEditText)
        placeNameBar?.hint = pointName
        placeNameBar?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                placeNameBar.clearFocus()
                val imm =
                    activity?.getSystemService(Activity.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.hideSoftInputFromWindow(dialog?.currentFocus?.windowToken, 0)
            }
            false
        }
    }

    private fun setupClickListeners() {
        val placeNameBar = dialog?.findViewById<EditText>(R.id.placeNameEditText)

        dialog?.findViewById<TextView>(R.id.deleteText)?.setOnClickListener {
            (parentFragment as MapViewFragment).removeDroppedMarker()
            dismiss()
        }

        dialog?.findViewById<TextView>(R.id.saveText)?.setOnClickListener {
            val pointList: MutableList<Poi> = viewmodel.getData()
            var name = "Ikke navngitt"
            val inputName = placeNameBar?.text.toString()
            if (inputName.isNotBlank() && (inputName != positionReservedName)) name = inputName
            val point = Poi(name, longitude, latitude, poiColor)
            pointList.add(point)
            (parentFragment as MapViewFragment).addPlace(point)
            viewmodel.saveData(pointList)
            dismiss()
        }

        dialog?.findViewById<ImageView>(R.id.placeSettingsDismissIcon)?.setOnClickListener {
            (parentFragment as MapViewFragment).removeDroppedMarker()
            dismiss()
        }

        dialog?.findViewById<ConstraintLayout>(R.id.placeSettingsLayout)?.setOnClickListener {
            placeNameBar?.onEditorAction(EditorInfo.IME_ACTION_DONE)
        }

        dialog?.findViewById<ImageView>(R.id.changeColorRed)?.setOnClickListener {
            placeNameBar?.onEditorAction(EditorInfo.IME_ACTION_DONE)
            changeMarkedColor(it as ImageView?)
            poiColor = "Red"
        }

        dialog?.findViewById<ImageView>(R.id.changeColorOrange)?.setOnClickListener {
            placeNameBar?.onEditorAction(EditorInfo.IME_ACTION_DONE)
            changeMarkedColor(it as ImageView?)
            poiColor = "Orange"
        }

        dialog?.findViewById<ImageView>(R.id.changeColorYellow)?.setOnClickListener {
            placeNameBar?.onEditorAction(EditorInfo.IME_ACTION_DONE)
            changeMarkedColor(it as ImageView?)
            poiColor = "Yellow"
        }

        dialog?.findViewById<ImageView>(R.id.changeColorPink)?.setOnClickListener {
            placeNameBar?.onEditorAction(EditorInfo.IME_ACTION_DONE)
            changeMarkedColor(it as ImageView?)
            poiColor = "Pink"
        }

        dialog?.findViewById<ImageView>(R.id.changeColorGreen)?.setOnClickListener {
            placeNameBar?.onEditorAction(EditorInfo.IME_ACTION_DONE)
            changeMarkedColor(it as ImageView?)
            poiColor = "Green"
        }

        dialog?.findViewById<ImageView>(R.id.changeColorPurple)?.setOnClickListener {
            placeNameBar?.onEditorAction(EditorInfo.IME_ACTION_DONE)
            changeMarkedColor(it as ImageView?)
            poiColor = "Purple"
        }

        dialog?.findViewById<ImageView>(R.id.changeColorBlue)?.setOnClickListener {
            placeNameBar?.onEditorAction(EditorInfo.IME_ACTION_DONE)
            changeMarkedColor(it as ImageView?)
            poiColor = "Blue"
        }

        dialog?.findViewById<ImageView>(R.id.changeColorNavyBlue)?.setOnClickListener {
            placeNameBar?.onEditorAction(EditorInfo.IME_ACTION_DONE)
            changeMarkedColor(it as ImageView?)
            poiColor = "Navy Blue"
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
}