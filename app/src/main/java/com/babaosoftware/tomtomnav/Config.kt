package com.babaosoftware.tomtomnav

var configInstance = Config()

data class Config(
    var isSimulated: Boolean = true,
    var isMapMatched: Boolean = true,
    val startZoom: Double = 10.0,
    val simulatedSpeed: Double = 13.0, // 30 mph; 50 km/h
    var showZoomControl: Boolean = true,
    var showCompass: Boolean = true,
    var enableDarkStyleMode: Boolean = true,
)
