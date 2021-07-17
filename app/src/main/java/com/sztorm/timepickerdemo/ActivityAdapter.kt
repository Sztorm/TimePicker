package com.sztorm.timepickerdemo

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.sztorm.timepickerdemo.timepickerdemo.R

class ActivityAdapter(fm: FragmentManager) :
    FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    var layoutIDs = intArrayOf(R.layout.normal_picker, R.layout.ampm_picker)

    override fun getItem(position: Int): Fragment {
        return MainFragment.newInstance(layoutIDs[position])
    }

    override fun getCount(): Int {
        return layoutIDs.size
    }
}
