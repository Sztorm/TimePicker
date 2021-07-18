package com.sztorm.timepickerdemo

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ActivityAdapter(fragmentActivity: FragmentActivity, private val layoutIDs: IntArray)
    : FragmentStateAdapter(fragmentActivity) {

    override fun createFragment(position: Int): Fragment
        = MainFragment.newInstance(layoutIDs[position])

    override fun getItemCount(): Int = layoutIDs.size
}
