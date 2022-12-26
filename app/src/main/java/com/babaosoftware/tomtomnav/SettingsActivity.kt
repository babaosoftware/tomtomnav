package com.babaosoftware.tomtomnav

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.RadioGroup
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.babaosoftware.tomtomnav.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var simulated: SwitchCompat
    private lateinit var mapMatched: SwitchCompat
    private lateinit var zoom: SwitchCompat
    private lateinit var compass: SwitchCompat
    private lateinit var dark: SwitchCompat

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        simulated = binding.simulated
        simulated.isChecked = configInstance.isSimulated
        simulated.setOnClickListener {
            settingsCallback!!.simulated(simulated.isChecked)
        }

        mapMatched = binding.mapmatched
        mapMatched.isChecked = configInstance.isMapMatched
        mapMatched.setOnClickListener {
            settingsCallback!!.mapMatched(mapMatched.isChecked)
        }

        zoom = binding.zoom
        zoom.isChecked = configInstance.showZoomControl
        zoom.setOnClickListener { settingsCallback!!.showZoom(zoom.isChecked) }

        compass = binding.compass
        compass.isChecked = configInstance.showCompass
        compass.setOnClickListener { settingsCallback!!.showCompass(compass.isChecked) }

        dark = binding.dark
        dark.isChecked = configInstance.enableDarkStyleMode
        dark.setOnClickListener { settingsCallback!!.darkStyle(dark.isChecked) }
    }
}