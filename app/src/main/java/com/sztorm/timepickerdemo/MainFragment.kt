package com.sztorm.timepickerdemo

import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.sztorm.timepicker.PickedTime
import com.sztorm.timepicker.TimePicker
import com.sztorm.timepicker.TimeChangedListener
import com.sztorm.timepickerdemo.timepickerdemo.R

class MainFragment : Fragment() {
    companion object {
        const val ID_KEY = "layoutID"

        fun newInstance(layoutID: Int): MainFragment {
            val fragment = MainFragment()
            fragment.layoutID = layoutID
            return fragment
        }

        fun Resources.getColorCompat(@ColorRes id: Int, theme: Resources.Theme?)
            = ResourcesCompat.getColor(this, id, theme)
    }

    private var layoutID = 0
    private lateinit var time24ValueText: TextView
    private lateinit var time12ValueText: TextView

    override fun onSaveInstanceState(bundle: Bundle) {
        super.onSaveInstanceState(bundle)
        bundle.putInt(ID_KEY, layoutID)
    }

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)

        if (bundle?.getInt(ID_KEY) != null) {
            layoutID = bundle.getInt(ID_KEY)
        }
    }

    private fun setTimeValueTexts(time: PickedTime) {
        time24ValueText.text = time.toString24HourFormat()
        time12ValueText.text = time.toString12HourFormat()
    }

    private fun set12HPickerLayout(view: View) {
        val picker: TimePicker = view.findViewById(R.id.picker)
        time24ValueText = view.findViewById(R.id.time24ValueText)
        time12ValueText = view.findViewById(R.id.time12ValueText)

        picker.trackColor = resources.getColorCompat(R.color.clockColor, theme = null)
        picker.clockFaceColor = Color.WHITE
        picker.pointerColor = resources.getColorCompat(R.color.pointerColor, theme = null)
        picker.trackSize = 20F
        picker.pointerRadius = 60F
        picker.isTrackTouchable = true
        picker.setTime(12, 45, TimePicker.AM)

        picker.timeChangedListener = TimeChangedListener {
            setTimeValueTexts(it)
        }
        setTimeValueTexts(picker.time)
    }

    private fun set24HPickerLayout(view: View) {
        val picker: TimePicker = view.findViewById(R.id.picker)
        val checkBox: CheckBox = view.findViewById(R.id.checkbox)

        picker.isEnabled = checkBox.isChecked
        checkBox.setOnCheckedChangeListener { _, isChecked -> picker.isEnabled = isChecked }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(layoutID, container, false)

        when (layoutID) {
            R.layout.layout_12h_picker -> set12HPickerLayout(view)
            R.layout.layout_24h_picker -> set24HPickerLayout(view)
        }
        return view
    }
}
