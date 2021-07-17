package com.sztorm.timepickerdemo

import android.annotation.SuppressLint
import android.content.res.Resources
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
import com.sztorm.timepicker.TimePicker.Companion.toStringHourFormat
import com.sztorm.timepicker.TimePicker.Companion.toStringMinuteFormat
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
    lateinit var time24ValueText: TextView
    lateinit var time12ValueText: TextView

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

    @SuppressLint("SetTextI18n")
    private fun setTimeValueTexts(time: PickedTime) {
        val hourText: String = time.hour.toStringHourFormat()
        val minuteText: String = time.minute.toStringMinuteFormat()

        time24ValueText.text = "${hourText}:${minuteText}"
        time12ValueText.text = time.toString()
    }

    private fun createAmPmPickerLayout(view: View) {
        val amPmPicker: TimePicker = view.findViewById(R.id.amPmPicker)
        time24ValueText = view.findViewById(R.id.time24ValueText)
        time12ValueText = view.findViewById(R.id.time12ValueText)

        amPmPicker.clockColor = resources.getColorCompat(R.color.clockColor, theme = null)
        amPmPicker.pointerColor = resources.getColorCompat(R.color.pointerColor, theme = null)
        amPmPicker.trackSize = 20F
        amPmPicker.pointerRadius = 60F
        amPmPicker.isTrackTouchable = true
        amPmPicker.setTime(12, 45, TimePicker.AM)
        amPmPicker.timeChangedListener = TimeChangedListener {
            setTimeValueTexts(it)
        }
        setTimeValueTexts(amPmPicker.time)
    }

    private fun createNormalPickerLayout(view: View) {
        val picker: TimePicker = view.findViewById(R.id.picker)
        val checkBox: CheckBox = view.findViewById(R.id.checkbox)

        picker.isEnabled = checkBox.isChecked
        checkBox.setOnCheckedChangeListener { _, isChecked -> picker.isEnabled = isChecked }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(layoutID, container, false)

        if (layoutID == R.layout.ampm_picker) {
            createAmPmPickerLayout(view)
        }
        else {
            createNormalPickerLayout(view)
        }
        return view
    }
}
