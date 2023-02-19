package com.babaosoftware.tomtomnav

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.babaosoftware.tomtomnav.databinding.ActivityMainBinding
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.tomtom.quantity.Distance
import com.tomtom.quantity.Speed
import com.tomtom.sdk.location.GeoLocation
import com.tomtom.sdk.location.GeoPoint
import com.tomtom.sdk.location.LocationProvider
import com.tomtom.sdk.vehicle.Vehicle
import com.tomtom.sdk.location.android.AndroidLocationProvider
import com.tomtom.sdk.location.mapmatched.MapMatchedLocationProvider
import com.tomtom.sdk.location.simulation.SimulationLocationProvider
import com.tomtom.sdk.location.simulation.strategy.InterpolationStrategy
import com.tomtom.sdk.map.display.MapOptions
import com.tomtom.sdk.map.display.TomTomMap
import com.tomtom.sdk.map.display.camera.CameraChangeListener
import com.tomtom.sdk.map.display.camera.CameraOptions
import com.tomtom.sdk.map.display.camera.CameraTrackingMode
import com.tomtom.sdk.map.display.common.screen.Padding
import com.tomtom.sdk.map.display.gesture.MapLongClickListener
import com.tomtom.sdk.map.display.image.ImageFactory
import com.tomtom.sdk.map.display.location.LocationMarkerOptions
import com.tomtom.sdk.map.display.marker.MarkerOptions
import com.tomtom.sdk.map.display.route.Instruction
import com.tomtom.sdk.map.display.route.RouteOptions
import com.tomtom.sdk.map.display.ui.MapFragment
import com.tomtom.sdk.map.display.ui.compass.CompassButton
import com.tomtom.sdk.navigation.*
import com.tomtom.sdk.navigation.routereplanner.DefaultRouteReplanner
import com.tomtom.sdk.navigation.routereplanner.RouteReplanner
import com.tomtom.sdk.navigation.ui.NavigationFragment
import com.tomtom.sdk.navigation.ui.NavigationUiOptions
import com.tomtom.sdk.routing.route.Route
import com.tomtom.sdk.routing.RoutePlanner
import com.tomtom.sdk.routing.RoutePlanningCallback
import com.tomtom.sdk.routing.RoutePlanningResponse
import com.tomtom.sdk.routing.RoutingFailure
import com.tomtom.sdk.routing.online.OnlineRoutePlanner
import com.tomtom.sdk.routing.options.Itinerary
import com.tomtom.sdk.routing.options.RouteLegOptions
import com.tomtom.sdk.routing.options.RoutePlanningOptions
import com.tomtom.sdk.routing.options.calculation.*
import com.tomtom.sdk.routing.options.guidance.*
import com.tomtom.sdk.vehicle.VehicleDimensions
import org.json.JSONArray

enum class AppState{
    INITIAL, ROUTE, NAVIGATION,
}

class MainActivity : AppCompatActivity(), SettingsCallback {

    private val mapKey = BuildConfig.MAP_KEY
    private lateinit var mapFragment: MapFragment
    private lateinit var tomTomMap: TomTomMap
    private lateinit var locationProvider: LocationProvider
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

//    private val myRoute = listOf(
//        GeoPoint(41.426222, -73.491336),
//        GeoPoint(40.765385, -73.989570),
//    )

//    private val myRoute = listOf(
//        GeoPoint(41.385473, -73.434461),
//        GeoPoint(41.385026, -73.433252), // ghost
//        GeoPoint(41.384787, -73.434339),
//        GeoPoint(41.383768, -73.434088), // ghost
//        GeoPoint(41.384064, -73.432882),
//        GeoPoint(41.383143, -73.433692), // ghost
//        GeoPoint(41.383047, -73.433184),
//    )

    private val myRoute = listOf(
        GeoPoint(longitude = -82.84833591279993, latitude = 27.898096290443704),
        GeoPoint(longitude = -82.84783574652216, latitude = 27.89974529768338),
        GeoPoint(longitude = -82.84732139086672, latitude = 27.899105761534557),
        GeoPoint(longitude = -82.84636362505866, latitude = 27.898720157005656),
        GeoPoint(longitude = -82.84616852564793, latitude = 27.898046130422415),
        GeoPoint(longitude = -82.84765128914837, latitude = 27.897437934767808),
        GeoPoint(longitude = -82.84560293515356, latitude = 27.904861813289386),
    )

//    private val myRoute = listOf(
//        GeoPoint(43.636895, -70.232080),
////        GeoPoint(43.636948, -70.232312),
//        GeoPoint(43.636807262052926, -70.23225650720302),
//        GeoPoint(43.6365476672693, -70.2341996175512),
//        GeoPoint(43.6357886532729, -70.23536101404584),
//    )

    private lateinit var myRouteOptions: List<GeoPoint>

    val jRoute = JSONArray("[[-82.848406,27.898105],[-82.848393,27.898189],[-82.848338,27.898532],[-82.848287,27.898865],[-82.848273,27.898955],[-82.84827,27.898977],[-82.848141,27.899788],[-82.848059,27.899778],[-82.847834,27.899755],[-82.847461,27.899717],[-82.847228,27.899676],[-82.847309,27.899104],[-82.847345,27.898847],[-82.847019,27.898811],[-82.846362,27.898734],[-82.846058,27.898699],[-82.846132,27.898145],[-82.846146,27.898044],[-82.846146,27.898044],[-82.846237,27.897396],[-82.845569,27.8974],[-82.844915,27.897404],[-82.844465,27.897375],[-82.843966,27.897254],[-82.843897,27.897482],[-82.843852,27.897632],[-82.843869,27.897744],[-82.843985,27.897836],[-82.844236,27.897924],[-82.844562,27.898039],[-82.844781,27.898175],[-82.844858,27.898223],[-82.845387,27.898613],[-82.845425,27.89836],[-82.845469,27.898063],[-82.845569,27.8974],[-82.846237,27.897396],[-82.847153,27.897391],[-82.847218,27.89739],[-82.847651,27.897388],[-82.847651,27.897388],[-82.848035,27.897386],[-82.848439,27.897383],[-82.848522,27.897383],[-82.848453,27.897816],[-82.848393,27.898189],[-82.848338,27.898532],[-82.848287,27.898865],[-82.848273,27.898955],[-82.84827,27.898977],[-82.848141,27.899788],[-82.848072,27.900229],[-82.848013,27.900601],[-82.847885,27.901423],[-82.847751,27.902239],[-82.847686,27.902663],[-82.847625,27.903058],[-82.847514,27.903708],[-82.847458,27.904016],[-82.847301,27.904963],[-82.846344,27.904842],[-82.845621,27.90475]]")

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

        myRouteOptions = mutableListOf()

        for (i in 0 until jRoute.length()){
            val p = jRoute[i] as JSONArray
            val longitude = p[0] as Double
            val latitude = p[1] as Double
            myRouteOptions += GeoPoint(latitude, longitude)
        }

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

//    override fun onConfigurationChanged(newConfig: Configuration) {
//        super.onConfigurationChanged(newConfig)
//        if (::navigationFragment.isInitialized) {
//            navigationFragment.navigationView.hideGuidanceView()
//            Handler().postDelayed({
//                navigationFragment.navigationView.showGuidanceView()
//            }, 1000)
//        }
//    }

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
            itinerary = if (configInstance.useWaypoints)
                Itinerary(route[0], route[size - 1], wayPoints)
            else
                Itinerary(route[0], route[size - 1])
            addWayPointsMarkers(wayPoints)
        }
        else{
            itinerary = Itinerary(route[0], route[size - 1])
        }

        routePlanner =
            OnlineRoutePlanner.create(context = this, apiKey = mapKey)
        routeReplanner = DefaultRouteReplanner.create(routePlanner)

        routePlanningOptions = if (configInstance.useWaypoints)

            RoutePlanningOptions(
            itinerary = itinerary,
            costModel = CostModel(RouteType.Short, ConsiderTraffic.No, AvoidOptions(setOf(AvoidType.AlreadyUsedRoads))),
            guidanceOptions = GuidanceOptions(
                instructionType = InstructionType.Text,
                phoneticsType = InstructionPhoneticsType.Ipa,
                announcementPoints = AnnouncementPoints.All,
                extendedSections = ExtendedSections.All,
                progressPoints = ProgressPoints.All
            ),
            vehicle = Vehicle.Bus(dimensions = VehicleDimensions(length = Distance.Companion.feet(50)))
        )
        else
            RoutePlanningOptions(
                itinerary = itinerary,
                routeLegOptions = listOf(RouteLegOptions(supportingPoints = myRouteOptions))
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
        tomTomMap.enableLocationMarker(LocationMarkerOptions(LocationMarkerOptions.Type.Pointer))
    }

    private fun setupMapListeners() {
        tomTomMap.addMapLongClickListener(listener = MapLongClickListener {
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
        override fun onSuccess(result: RoutePlanningResponse) {
            route = result.routes.first()
            drawRoute(route!!)
        }

        override fun onFailure(failure: RoutingFailure) {
            Toast.makeText(this@MainActivity, failure.message, Toast.LENGTH_SHORT).show()
        }

        override fun onRoutePlanned(route: Route) = Unit
    }

    private fun addRoute(route: Route){
        tomTomMap.removeRoutes()
        val instructions = route.mapInstructions()
        val routeOptions = RouteOptions(
            geometry = route.geometry,
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
        tomTomNavigation = TomTomNavigationFactory.create(navigationConfiguration)

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
        tomTomNavigation.addProgressUpdatedListener(onProgressUpdateListener)
        tomTomNavigation.addRouteUpdatedListener(onRouteUpdatedListener)
//        navigationFragment.navigationView.addGuidanceViewBoundariesChangeListener {
//            Log.v("guidanceview", "top:${it.top} bottom:${it.bottom} height:${it.height}")
//
//        }
    }

    private val navigationListener = object : NavigationFragment.NavigationListener {
        override fun onStarted() {
            tomTomMap.addCameraChangeListener(onCameraChangeListener)
            tomTomMap.cameraTrackingMode = CameraTrackingMode.FollowRoute
            tomTomMap.enableLocationMarker(LocationMarkerOptions(LocationMarkerOptions.Type.Chevron))
            if (configInstance.isMapMatched)
                setMapMatchedLocationProvider()
            if (configInstance.isSimulated)
                setSimulationLocationProviderToNavigation()
            setMapNavigationPadding()
        }

        override fun onFailed(error: NavigationFailure) {
            Toast.makeText(this@MainActivity, error.message, Toast.LENGTH_SHORT).show()
            stopNavigation()
        }

        override fun onStopped() {
            stopNavigation()
        }
    }

    private val onProgressUpdateListener = ProgressUpdatedListener {
        tomTomMap.routes.first().progress = it.distanceAlongRoute
    }
    private val onCameraChangeListener by lazy {
        CameraChangeListener {
            val cameraTrackingMode = tomTomMap.cameraTrackingMode
            if (cameraTrackingMode == CameraTrackingMode.FollowRoute) {
                navigationFragment.navigationView.showSpeedView()
            } else {
                navigationFragment.navigationView.hideSpeedView()
            }
        }
    }

    private val onRouteUpdatedListener = RouteUpdatedListener { route, updateReason ->
        addRoute(route)
    }

    private fun setSimulationLocationProviderToNavigation() {
        val interpolationStrategy = InterpolationStrategy(
            locations = route.geometry.map { GeoLocation(it) },
            currentSpeed = Speed.Companion.milesPerHour(configInstance.simulatedSpeed)
        )
        locationProvider = SimulationLocationProvider.create(strategy = interpolationStrategy)
        tomTomNavigation.navigationEngineRegistry.updateEngines(
            locationProvider = locationProvider
        )
        locationProvider.enable()
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
            tomTomNavigation.removeProgressUpdatedListener(onProgressUpdateListener)
        }
        tomTomMap.cameraTrackingMode = CameraTrackingMode.None
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
        mapFragment.compassButton.visibilityPolicy = if (configInstance.showCompass) CompassButton.VisibilityPolicy.Visible else CompassButton.VisibilityPolicy.Invisible
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