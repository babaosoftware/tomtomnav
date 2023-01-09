package com.babaosoftware.tomtomnav

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.babaosoftware.tomtomnav.MainActivity.Companion.ZOOM_TO_ROUTE_PADDING
import com.babaosoftware.tomtomnav.databinding.ActivityMainBinding
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.tomtom.kotlin.quantity.Distance
import com.tomtom.sdk.common.location.GeoLocation
import com.tomtom.sdk.common.location.GeoPoint
import com.tomtom.sdk.common.vehicle.Vehicle
import com.tomtom.sdk.location.android.AndroidLocationProvider
import com.tomtom.sdk.location.android.AndroidLocationProviderConfig
import com.tomtom.sdk.location.mapmatched.MapMatchedLocationProvider
import com.tomtom.sdk.location.simulation.SimulationLocationProvider
import com.tomtom.sdk.location.simulation.strategy.InterpolationStrategy
import com.tomtom.sdk.map.display.MapOptions
import com.tomtom.sdk.map.display.TomTomMap
import com.tomtom.sdk.map.display.camera.CameraOptions
import com.tomtom.sdk.map.display.camera.CameraTrackingMode
import com.tomtom.sdk.map.display.camera.OnCameraChangeListener
import com.tomtom.sdk.map.display.common.screen.Padding
import com.tomtom.sdk.map.display.gesture.OnMapLongClickListener
import com.tomtom.sdk.map.display.image.ImageFactory
import com.tomtom.sdk.map.display.location.LocationMarkerOptions
import com.tomtom.sdk.map.display.marker.MarkerOptions
import com.tomtom.sdk.map.display.route.Instruction
import com.tomtom.sdk.map.display.route.RouteOptions
import com.tomtom.sdk.map.display.ui.MapFragment
import com.tomtom.sdk.map.display.ui.compass.CompassButton
import com.tomtom.sdk.navigation.NavigationConfiguration
import com.tomtom.sdk.navigation.NavigationError
import com.tomtom.sdk.navigation.OnProgressUpdateListener
import com.tomtom.sdk.navigation.OnRouteDeviationListener
import com.tomtom.sdk.navigation.OnRouteUpdatedListener
import com.tomtom.sdk.navigation.RoutePlan
import com.tomtom.sdk.navigation.TomTomNavigation
import com.tomtom.sdk.navigation.routereplanner.DefaultRouteReplanner
import com.tomtom.sdk.navigation.routereplanner.RouteReplanner
import com.tomtom.sdk.navigation.ui.NavigationFragment
import com.tomtom.sdk.navigation.ui.NavigationUiOptions
import com.tomtom.sdk.route.Route
import com.tomtom.sdk.routing.RoutePlanner
import com.tomtom.sdk.routing.RoutePlanningCallback
import com.tomtom.sdk.routing.RoutePlanningResult
import com.tomtom.sdk.routing.common.RoutingError
import com.tomtom.sdk.routing.common.options.Itinerary
import com.tomtom.sdk.routing.common.options.RoutePlanningOptions
import com.tomtom.sdk.routing.common.options.guidance.*
import com.tomtom.sdk.routing.online.OnlineRoutePlanner
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

enum class AppState{
    INITIAL, ROUTE, NAVIGATION,
}

class MainActivity : AppCompatActivity(), SettingsCallback {

    private val mapKey = BuildConfig.MAP_KEY
    private lateinit var mapFragment: MapFragment
    private lateinit var tomTomMap: TomTomMap
    private lateinit var locationProvider: AndroidLocationProvider
    private lateinit var routePlanner: RoutePlanner
    private lateinit var routeReplanner: RouteReplanner
    private lateinit var routePlanningOptions: RoutePlanningOptions
    private lateinit var route: Route
    private lateinit var tomTomNavigation: TomTomNavigation
    private lateinit var navigationFragment: NavigationFragment

    private lateinit var binding: ActivityMainBinding
    private lateinit var routeStart: ExtendedFloatingActionButton
    private lateinit var navStart: ExtendedFloatingActionButton
    private lateinit var navStop: ExtendedFloatingActionButton
    private lateinit var settings: ExtendedFloatingActionButton


//    private val myRoute = listOf(
//        GeoPoint(41.416841, -73.4737747),
//        GeoPoint(41.415115, -73.477221),
//        GeoPoint(41.409467, -73.501574),
//    )

    // private val myRoute = listOf(
    //     GeoPoint(41.426222, -73.491336),
    //     GeoPoint(40.765385, -73.989570),
    // )

    private val myRoute = listOf(
        GeoPoint(41.385473, -73.434461),
        GeoPoint(41.385026, -73.433252), // ghost
        GeoPoint(41.384787, -73.434339),
        GeoPoint(41.383768, -73.434088), // ghost
        GeoPoint(41.384064, -73.432882),
        GeoPoint(41.383143, -73.433692), // ghost
        GeoPoint(41.383047, -73.433184),
    )


    companion object {
        private const val ZOOM_TO_ROUTE_PADDING = 100
        private var appState = AppState.INITIAL
    }

    private fun changeState(){
        when (appState){
            AppState.INITIAL -> {
                routeStart.visibility = View.VISIBLE
                navStart.visibility = View.GONE
                navStop.visibility = View.GONE
                settings.visibility = View.VISIBLE
            }
            AppState.ROUTE -> {
                routeStart.visibility = View.GONE
                navStart.visibility = View.VISIBLE
                navStop.visibility = View.VISIBLE
                settings.visibility = View.GONE
            }
            AppState.NAVIGATION -> {
                routeStart.visibility = View.GONE
                navStart.visibility = View.GONE
                navStop.visibility = View.VISIBLE
                settings.visibility = View.GONE
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        settingsCallback = this
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        routeStart = binding.routeStart
        navStart = binding.navStart
        navStop = binding.navStop
        settings = binding.settings

        changeState()
        initMap()

        routeStart.setOnClickListener {
            appState = AppState.ROUTE
            changeState()
            initRouting(myRoute)
        }

        navStart.setOnClickListener {
            appState = AppState.NAVIGATION
            changeState()
            initNavigation()
        }

        navStop.setOnClickListener {
            appState = AppState.INITIAL
            changeState()
            stopNavigation()
        }

        settings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun initMap() {
        val mapOptions = MapOptions(mapKey = mapKey, cameraOptions = CameraOptions(zoom = configInstance.startZoom))
        mapFragment = MapFragment.newInstance(mapOptions)
        supportFragmentManager.beginTransaction()
            .replace(R.id.map_container, mapFragment)
            .commit()
        mapFragment.getMapAsync { map ->
            tomTomMap = map
            setupPermissions()
            showZoom(configInstance.showZoomControl)
            showCompass(configInstance.showCompass)
            showTrafficFlow(configInstance.showTrafficFlow)
            showTrafficIncidents(configInstance.showTrafficIncidents)
            setupMapListeners()
        }
    }

    private fun initRouting(route: List<GeoPoint>) {

        val size = route.size
        var itinerary: Itinerary
        if (size > 2){
            var wayPoints = listOf<GeoPoint>()
            for (i in 1..size - 2){
                wayPoints += route[i]
            }
            itinerary = Itinerary(route[0], route[size - 1], wayPoints)
            addWayPointsMarkers(wayPoints)
        }
        else{
            itinerary = Itinerary(route[0], route[size - 1])
        }

        routePlanner =
            OnlineRoutePlanner.create(context = this, apiKey = mapKey)
        routeReplanner = DefaultRouteReplanner.create(routePlanner)

        routePlanningOptions = RoutePlanningOptions(
            itinerary = itinerary,
            guidanceOptions = GuidanceOptions(
                instructionType = InstructionType.Text,
                phoneticsType = InstructionPhoneticsType.Ipa,
                announcementPoints = AnnouncementPoints.All,
                extendedSections = ExtendedSections.All,
                progressPoints = ProgressPoints.All
            ),
            vehicle = Vehicle.Car()
        )
        routePlanner.planRoute(routePlanningOptions, routePlanningCallback)
    }

    private fun addWayPointsMarkers(wayPoints: List<GeoPoint>){
        for (i in wayPoints.indices){
            val wayPoint = wayPoints[i]
            val markerOptions = MarkerOptions(
                coordinate = wayPoint,
                pinImage = ImageFactory.fromBitmap(buildMarkerBitmap(i+1,)))
            tomTomMap.addMarker(markerOptions)
        }
    }

    private fun buildMarkerBitmap(position: Int): Bitmap {
        val view = LayoutInflater.from(this).inflate(
            R.layout.rounded_button,
            null,
            false
        ) as FrameLayout
        val textView = view.findViewById(R.id.btn_pin) as TextView
        textView.text = "$position"
        val spec = View.MeasureSpec.makeMeasureSpec(convertDpToPx(48.0, resources.displayMetrics.density), View.MeasureSpec.EXACTLY)
        view.measure(spec, spec)
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        val bm =
            Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bm)
        view.draw(canvas)
        return bm
    }


    private fun setupPermissions() {
        val permission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        if (permission != PackageManager.PERMISSION_GRANTED) {
            makeRequest()
        } else {
            showUserLocation()
        }
    }

    private fun makeRequest() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            101
        )
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        showUserLocation()
    }

    private fun initLocationProvider() {
        locationProvider = AndroidLocationProvider(context = this)
    }

    private fun showUserLocation(){
        initLocationProvider()
        enableUserLocation()
        tomTomMap.moveCamera(CameraOptions(position = locationProvider.lastKnownLocation?.position))
    }

    @SuppressLint("MissingPermission")
    private fun enableUserLocation() {
        locationProvider.enable()
        tomTomMap.setLocationProvider(locationProvider)
        val locationMarker = LocationMarkerOptions(type = LocationMarkerOptions.Type.POINTER)
        tomTomMap.enableLocationMarker(locationMarker)
    }

    private fun setupMapListeners() {
        tomTomMap.addOnMapLongClickListener(listener = OnMapLongClickListener {
            if (appState == AppState.INITIAL){
                val start = locationProvider.lastKnownLocation!!.position
                appState = AppState.ROUTE
                changeState()
                initRouting(listOf(start, it))
            }
            true
        })
    }

    private val routePlanningCallback = object : RoutePlanningCallback {
        override fun onSuccess(result: RoutePlanningResult) {
            route = result.routes.first()
            drawRoute(route)
        }

        override fun onError(error: RoutingError) {
            Toast.makeText(this@MainActivity, error.message, Toast.LENGTH_SHORT).show()
        }

        override fun onRoutePlanned(route: Route) = Unit
    }
    private fun addRoute(route: Route){
        tomTomMap.removeRoutes()
        val instructions = route.mapInstructions()
        val geometry = route.legs.flatMap { it.points }
        val routeOptions = RouteOptions(
            geometry = geometry,
            destinationMarkerVisible = true,
            departureMarkerVisible = true,
            instructions = instructions,
        )
        tomTomMap.addRoute(routeOptions)
    }

    private fun drawRoute(route: Route) {
        addRoute(route)
        tomTomMap.zoomToRoutes(ZOOM_TO_ROUTE_PADDING)
    }

    private fun Route.mapInstructions(): List<Instruction> {
        val routeInstructions = legs.flatMap { routeLeg -> routeLeg.instructions }
        return routeInstructions.map {
            Instruction(
                routeOffset = it.routeOffset,
                combineWithNext = it.combineWithNext
            )
        }
    }



    private fun clearMap(){
        tomTomMap.removeMarkers()
        tomTomMap.removeRoutes()
        appState = AppState.INITIAL
        changeState()
    }

    private fun initNavigation() {
        val navigationConfiguration = NavigationConfiguration(
            context = this,
            apiKey = mapKey,
            locationProvider = locationProvider,
            routeReplanner = routeReplanner
        )
        tomTomNavigation = TomTomNavigation.create(navigationConfiguration)

        startNavigation(route)
    }

    private fun initNavigationFragment() {
        val navigationUiOptions = NavigationUiOptions(
            keepInBackground = true
        )
        navigationFragment = NavigationFragment.newInstance(navigationUiOptions)
        supportFragmentManager.beginTransaction()
            .add(R.id.navigation_fragment_container, navigationFragment)
            .commitNow()
    }
    private fun startNavigation(route: Route) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        initNavigationFragment()
        navigationFragment.setTomTomNavigation(tomTomNavigation)
        val routePlan = RoutePlan(route, routePlanningOptions)
        navigationFragment.startNavigation(routePlan)
        navigationFragment.addNavigationListener(navigationListener)
        navigationFragment.navigationView.showSpeedView()
        tomTomNavigation.addOnProgressUpdateListener(onProgressUpdateListener)
        tomTomNavigation.addOnRouteUpdatedListener(onRouteUpdatedListener)
    }

    private val navigationListener = object : NavigationFragment.NavigationListener {
        override fun onStarted() {
            tomTomMap.addOnCameraChangeListener(onCameraChangeListener)
            tomTomMap.changeCameraTrackingMode(CameraTrackingMode.FOLLOW_ROUTE)
            tomTomMap.enableLocationMarker(LocationMarkerOptions(LocationMarkerOptions.Type.CHEVRON))
            if (configInstance.isSimulated)
                setSimulationLocationProviderToNavigation()
            if (configInstance.isMapMatched)
                setMapMatchedLocationProvider()
            setMapNavigationPadding()
        }

        override fun onFailed(error: NavigationError) {
            Toast.makeText(this@MainActivity, error.message, Toast.LENGTH_SHORT).show()
            stopNavigation()
        }

        override fun onStopped() {
            stopNavigation()
        }
    }

    private val onProgressUpdateListener = OnProgressUpdateListener {
        tomTomMap.routes.first().progress = it.distanceAlongRoute
    }
    private val onCameraChangeListener by lazy {
        OnCameraChangeListener {
            // TODO: This will make the speed view to blink - solved in subsequent versions of the sdk
//            val cameraTrackingMode = tomTomMap.cameraTrackingMode()
//            if (cameraTrackingMode == CameraTrackingMode.FOLLOW_ROUTE) {
//                navigationFragment.navigationView.showSpeedView()
//            } else {
//                navigationFragment.navigationView.hideSpeedView()
//            }
        }
    }

    private val onRouteUpdatedListener = OnRouteUpdatedListener { route, updateReason ->
        addRoute(route)
    }

    private fun setSimulationLocationProviderToNavigation() {
        var locations = listOf<GeoLocation>()
        for (coordinate in route.routeCoordinates) {
            locations += GeoLocation(coordinate.coordinate)
        }
        val interpolationStrategy = InterpolationStrategy(
            locations = locations,
            currentSpeedInMetersPerSecond = configInstance.simulatedSpeed
        )
        val simulationLocationProvider = SimulationLocationProvider.create(interpolationStrategy)
        simulationLocationProvider.enable()
        tomTomNavigation.locationProvider = simulationLocationProvider
    }
    private fun setMapMatchedLocationProvider() {
        val mapMatchedLocationProvider = MapMatchedLocationProvider(tomTomNavigation)
        tomTomMap.setLocationProvider(mapMatchedLocationProvider)
        mapMatchedLocationProvider.enable()
    }
    private fun setMapNavigationPadding() {
//        val paddingBottom = resources.getDimensionPixelOffset(R.dimen.map_padding_bottom)
        // Show chevron marker about a third off the bottom of screen
        // TODO: Chevron might be covered on small devices, like phones, by the info window
        val paddingBottom = (resources.displayMetrics.heightPixels * 0.4).toInt()
        val padding = Padding(0, 0, 0, paddingBottom)
        tomTomMap.setPadding(padding)
    }

    private fun resetMapPadding(){
        tomTomMap.setPadding(Padding(0,0,0,0))
    }

    private fun stopNavigation() {
        if (::navigationFragment.isInitialized){
            navigationFragment.stopNavigation()
            navigationFragment.removeNavigationListener(navigationListener)
            tomTomNavigation.removeOnProgressUpdateListener(onProgressUpdateListener)
        }
        tomTomMap.changeCameraTrackingMode(CameraTrackingMode.NONE)
        tomTomMap.enableLocationMarker(LocationMarkerOptions(LocationMarkerOptions.Type.POINTER))
        resetMapPadding()
        clearMap()
        initLocationProvider()
        enableUserLocation()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun simulated(sim: Boolean) {
        configInstance.isSimulated = sim
    }

    override fun mapMatched(mm: Boolean) {
        configInstance.isMapMatched = mm
    }

    override fun showZoom(zoom: Boolean) {
        configInstance.showZoomControl = zoom
        mapFragment.zoomControlsView.isVisible = configInstance.showZoomControl
    }

    override fun showCompass(compass: Boolean) {
        configInstance.showCompass = compass
        mapFragment.compassButton.visibilityPolicy = if (configInstance.showCompass) CompassButton.VisibilityPolicy.VISIBLE else CompassButton.VisibilityPolicy.INVISIBLE
    }

    override fun showTrafficFlow(flow: Boolean) {
        configInstance.showTrafficFlow = flow
        if (flow)
            tomTomMap.showTrafficFlow()
        else
            tomTomMap.hideTrafficFlow()
    }

    override fun showTrafficIncidents(incidents: Boolean) {
        configInstance.showTrafficIncidents = incidents
        if (incidents)
            tomTomMap.showTrafficIncidents()
        else
            tomTomMap.hideTrafficIncidents()
    }

    override fun darkStyle(ds: Boolean) {
        configInstance.enableDarkStyleMode = ds
    }
}