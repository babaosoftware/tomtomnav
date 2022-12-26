package com.babaosoftware.tomtomnav

var settingsCallback: SettingsCallback? = null

interface SettingsCallback {
    fun simulated(sim: Boolean)
    fun mapMatched(mm: Boolean)
    fun showZoom(zoom: Boolean)
    fun showCompass(compass: Boolean)
    fun darkStyle(ds: Boolean)
}