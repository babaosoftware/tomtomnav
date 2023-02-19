package com.babaosoftware.tomtomnav

var configInstance = Config()

data class Config(
    var useWaypoints: Boolean = true,
    var isSimulated: Boolean = true,
    var isMapMatched: Boolean = false,
    val startZoom: Double = 10.0,
    val simulatedSpeed: Double = 13.0, // 30 mph; 50 km/h
    var showZoomControl: Boolean = true,
    var showCompass: Boolean = true,
    var enableDarkStyleMode: Boolean = true,
    var showTrafficFlow: Boolean = true,
    var showTrafficIncidents: Boolean = true,
)
