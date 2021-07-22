package com.sztorm.timepickerdemo

import android.os.Bundle
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.sztorm.timepickerdemo.timepickerdemo.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        title = resources.getString(R.string.appTitle)
        val viewPager: ViewPager2 = findViewById(R.id.viewPager)
        val layoutIDs = intArrayOf(
            R.layout.layout_24h_picker,
            R.layout.layout_12h_picker,
            R.layout.layout_12h_twosteppicker,
            R.layout.layout_24h_twosteppicker)

        viewPager.adapter = ActivityAdapter(this, layoutIDs)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }
}
