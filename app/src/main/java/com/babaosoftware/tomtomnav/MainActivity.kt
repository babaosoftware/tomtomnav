package com.babaosoftware.tomtomnav

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import com.tomtom.sdk.map.display.location.LocationMarkerOptions
import com.tomtom.sdk.map.display.route.RouteOptions
import com.tomtom.sdk.map.display.ui.MapFragment
import com.tomtom.sdk.navigation.NavigationConfiguration
import com.tomtom.sdk.navigation.NavigationError
import com.tomtom.sdk.navigation.OnProgressUpdateListener
import com.tomtom.sdk.navigation.RoutePlan
import com.tomtom.sdk.navigation.TomTomNavigation
import com.tomtom.sdk.navigation.routereplanner.DefaultRouteReplanner
import com.tomtom.sdk.navigation.routereplanner.RouteReplanner
import com.tomtom.sdk.navigation.ui.NavigationFragment
import com.tomtom.sdk.navigation.ui.NavigationUiOptions
import com.tomtom.sdk.route.Route
import com.tomtom.sdk.route.instruction.Instruction
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
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds


class MainActivity : AppCompatActivity() {

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

    private val startZoom = 10.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initMap()

        findViewById<ExtendedFloatingActionButton>(R.id.route_start).setOnClickListener {
            initRouting()
        }

        findViewById<ExtendedFloatingActionButton>(R.id.nav_start).setOnClickListener {
            initNavigation()
        }
    }

    private fun initMap() {
        val mapOptions = MapOptions(mapKey = mapKey, cameraOptions = CameraOptions(zoom = startZoom))
        mapFragment = MapFragment.newInstance(mapOptions)
        supportFragmentManager.beginTransaction()
            .replace(R.id.map_container, mapFragment)
            .commit()
        mapFragment.getMapAsync { map ->
            tomTomMap = map
            setupPermissions()
            mapFragment.zoomControlsView.isVisible = true
        }
    }

    private fun getGeoPointFromJson(obj: JSONObject) : GeoPoint{
        val geometry = obj.getJSONObject("geometry")
        val coordinates = geometry.getJSONArray("coordinates")
        val lon = (coordinates.get(0) as String).toDouble()
        val lat = (coordinates.get(1) as String).toDouble()
        return GeoPoint(lat, lon)
    }

    private fun initRouting() {

        val jRoute = JSONArray("[{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[\"-70.23211\",\"43.636725\"]},\"properties\":{\"stop_id\":\"3805590\",\"address\":\"51 Craggmere Avenue\",\"stop_type\":\"stop\"}},{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[\"-70.23265470376843\",\"43.63716784964882\"]},\"properties\":{\"stop_id\":\"3805586\",\"address\":\"14 Lahave Street\",\"stop_type\":\"stop\"}},{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[\"-70.23394242578969\",\"43.63675640985471\"]},\"properties\":{\"stop_id\":\"3805587\",\"address\":\"7 Bayview Avenue\",\"stop_type\":\"stop\"}},{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[\"-70.23535898190087\",\"43.63752071177362\"]},\"properties\":{\"stop_id\":\"3805588\",\"address\":\"43 Bayview Avenue\",\"stop_type\":\"stop\"}},{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[\"-70.25196136490507\",\"43.633720382528885\"]},\"properties\":{\"stop_id\":\"3805589\",\"address\":\"Mahoney School South Portland\",\"stop_type\":\"stop\"}}]")
        val routeLength = jRoute.length()
        val start = getGeoPointFromJson(jRoute.get(0) as JSONObject)
        val end = getGeoPointFromJson(jRoute.get(routeLength - 1) as JSONObject)

        var itinerary: Itinerary
        if (routeLength > 2){
            var wayPoints = listOf<GeoPoint>()
            for (i in 1..routeLength - 2){
                wayPoints += getGeoPointFromJson(jRoute.get(i) as JSONObject)
            }
            itinerary = Itinerary(start, end, wayPoints)
//            addWayPoints(wayPoints)
        }
        else{
            itinerary = Itinerary(start, end)
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

    private fun setupPermissions() {
        val permission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        if (permission != PackageManager.PERMISSION_GRANTED) {
            makeRequest()
        } else {
            enableUserLocation()
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
        enableUserLocation()
    }

    @SuppressLint("MissingPermission")
    private fun enableUserLocation() {

        val androidLocationProviderConfig = AndroidLocationProviderConfig(
            minTimeInterval = 250L.milliseconds,
            minDistance = Distance.meters(20.0)
        )
        locationProvider = AndroidLocationProvider(
            context = applicationContext,
            config = androidLocationProviderConfig
        )
        tomTomMap.setLocationProvider(locationProvider)
        locationProvider.enable()
        val locationMarker = LocationMarkerOptions(type = LocationMarkerOptions.Type.POINTER)
        tomTomMap.enableLocationMarker(locationMarker)
        tomTomMap.moveCamera(CameraOptions(position = locationProvider.lastKnownLocation?.position))

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
    private fun drawRoute(route: Route) {
        //val instructions = route.mapInstructions()
        val geometry = route.legs.flatMap { it.points }
        val routeOptions = RouteOptions(
            geometry = geometry,
            destinationMarkerVisible = true,
            departureMarkerVisible = true,
            //instructions = instructions
        )
        tomTomMap.addRoute(routeOptions)
        tomTomMap.zoomToRoutes(ZOOM_TO_ROUTE_PADDING)
    }
    companion object {
        private const val ZOOM_TO_ROUTE_PADDING = 100
    }

//    private fun Route.mapInstructions(): List<Instruction> {
//        val routeInstructions = legs.flatMap { routeLeg -> routeLeg.instructions }
//        return routeInstructions.map {
//            Instruction(
//                routeOffset = it.routeOffset,
//                combineWithNext = it.combineWithNext
//            )
//        }
//    }

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
        initNavigationFragment()
        navigationFragment.setTomTomNavigation(tomTomNavigation)
        val routePlan = RoutePlan(route, routePlanningOptions)
        navigationFragment.startNavigation(routePlan)
        navigationFragment.addNavigationListener(navigationListener)
        tomTomNavigation.addOnProgressUpdateListener(onProgressUpdateListener)
    }

    private val navigationListener = object : NavigationFragment.NavigationListener {
        override fun onStarted() {
            tomTomMap.addOnCameraChangeListener(onCameraChangeListener)
            tomTomMap.changeCameraTrackingMode(CameraTrackingMode.FOLLOW_ROUTE)
            tomTomMap.enableLocationMarker(LocationMarkerOptions(LocationMarkerOptions.Type.CHEVRON))
            setSimulationLocationProviderToNavigation()
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
            val cameraTrackingMode = tomTomMap.cameraTrackingMode()
            if (cameraTrackingMode == CameraTrackingMode.FOLLOW_ROUTE) {
                navigationFragment.navigationView.showSpeedView()
            } else {
                navigationFragment.navigationView.hideSpeedView()
            }
        }
    }

    private fun setSimulationLocationProviderToNavigation() {
        var locations = listOf<GeoLocation>()
        for (coordinate in route.routeCoordinates) {
            locations += GeoLocation(coordinate.coordinate)
        }
        //val timestampStrategy = TimestampStrategy(locations = locations)
        val interpolationStrategy = InterpolationStrategy(
            locations = locations,
            startDelay = 1.seconds,
            broadcastDelay = 500.milliseconds,
            currentSpeedInMetersPerSecond = 13.0 // 30 mph; 50 km/h
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
//        val padding = Padding(0, 0, 0, paddingBottom)
//        tomTomMap.setPadding(padding)
    }
    private fun stopNavigation() {
        navigationFragment.stopNavigation()
        tomTomMap.changeCameraTrackingMode(CameraTrackingMode.NONE)
        tomTomMap.enableLocationMarker(LocationMarkerOptions(LocationMarkerOptions.Type.POINTER))
//        resetMapPadding()
        navigationFragment.removeNavigationListener(navigationListener)
        tomTomNavigation.removeOnProgressUpdateListener(onProgressUpdateListener)
//        clearMap()
//        initLocationProvider()
        enableUserLocation()
    }
}