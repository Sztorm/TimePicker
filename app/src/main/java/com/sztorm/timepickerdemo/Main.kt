package com.sztorm.timepickerdemo

import android.os.Bundle
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.sztorm.timepickerdemo.timepickerdemo.R

class Main : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)

        title = resources.getString(R.string.appTitle)

        val viewPager: ViewPager2 = findViewById(R.id.viewPager)
        val layoutIDs = intArrayOf(R.layout.normal_picker, R.layout.ampm_picker)

        viewPager.adapter = ActivityAdapter(this, layoutIDs)

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }
}
