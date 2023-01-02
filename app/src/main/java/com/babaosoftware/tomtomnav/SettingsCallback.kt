package com.babaosoftware.tomtomnav

var settingsCallback: SettingsCallback? = null

interface SettingsCallback {
    fun simulated(sim: Boolean)
    fun mapMatched(mm: Boolean)
    fun showZoom(zoom: Boolean)
    fun showCompass(compass: Boolean)
    fun showTrafficFlow(flow: Boolean)
    fun showTrafficIncidents(incidents: Boolean)
    fun darkStyle(ds: Boolean)
}