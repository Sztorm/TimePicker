package com.sztorm.timepickerdemo

import android.os.Bundle
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import com.sztorm.timepickerdemo.timepickerdemo.R

class Main : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)

        title = resources.getString(R.string.appTitle)

        val viewPager: ViewPager = findViewById(R.id.viewPager)
        viewPager.adapter = ActivityAdapter(supportFragmentManager)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }
}
