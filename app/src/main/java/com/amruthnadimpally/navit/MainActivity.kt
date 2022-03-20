package com.amruthnadimpally.navit

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.api.directions.v5.models.Bearing
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.Expected
import com.mapbox.geojson.Point
import com.mapbox.maps.*
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.TimeFormat
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.RouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.core.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.ui.maneuver.api.RoadShieldCallback
import com.mapbox.navigation.ui.maneuver.model.Maneuver
import com.mapbox.navigation.ui.maneuver.model.RoadShield
import com.mapbox.navigation.ui.maneuver.model.RoadShieldError
import com.mapbox.navigation.ui.maneuver.view.MapboxManeuverView
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.camera.lifecycle.NavigationBasicGesturesHandler
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import com.mapbox.navigation.ui.maps.camera.transition.NavigationCameraTransitionOptions
import com.mapbox.navigation.ui.maps.camera.view.MapboxRecenterButton
import com.mapbox.navigation.ui.maps.camera.view.MapboxRouteOverviewButton
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import com.mapbox.navigation.ui.tripprogress.api.MapboxTripProgressApi
import com.mapbox.navigation.ui.tripprogress.model.*
import com.mapbox.navigation.ui.tripprogress.view.MapboxTripProgressView
import com.mapbox.navigation.ui.voice.api.MapboxSpeechApi
import com.mapbox.navigation.ui.voice.api.MapboxVoiceInstructionsPlayer
import com.mapbox.navigation.ui.voice.model.SpeechAnnouncement
import com.mapbox.navigation.ui.voice.model.SpeechError
import com.mapbox.navigation.ui.voice.model.SpeechValue
import com.mapbox.navigation.ui.voice.model.SpeechVolume
import com.mapbox.navigation.ui.voice.view.MapboxSoundButton
import java.util.*

var mapView : MapView? = null

var shouldNav : Boolean = false

class MainActivity : AppCompatActivity() {
    //Regular Map Declarers
    lateinit var onIndicatorBearingChangedListener : OnIndicatorBearingChangedListener
    lateinit var onIndicatorPositionChangedListener : OnIndicatorPositionChangedListener
    lateinit var onMoveListener : OnMoveListener
    //Navigation Declarers
    lateinit var mapboxNavigation : MapboxNavigation
    lateinit var navigationCamera : NavigationCamera
    lateinit var viewportDataSource: MapboxNavigationViewportDataSource
    private val pixelDensity = Resources.getSystem().displayMetrics.density
    private val overviewPadding: EdgeInsets by lazy {
        EdgeInsets(
            140.0 * pixelDensity,
            40.0 * pixelDensity,
            120.0 * pixelDensity,
            40.0 * pixelDensity
        )
    }
    private val landscapeOverviewPadding: EdgeInsets by lazy {
        EdgeInsets(
            30.0 * pixelDensity,
            380.0 * pixelDensity,
            110.0 * pixelDensity,
            20.0 * pixelDensity
        )
    }
    private val followingPadding: EdgeInsets by lazy {
        EdgeInsets(
            180.0 * pixelDensity,
            40.0 * pixelDensity,
            150.0 * pixelDensity,
            40.0 * pixelDensity
        )
    }
    private val landscapeFollowingPadding: EdgeInsets by lazy {
        EdgeInsets(
            30.0 * pixelDensity,
            380.0 * pixelDensity,
            110.0 * pixelDensity,
            40.0 * pixelDensity
        )
    }
    val displayMetrics = DisplayMetrics()
    lateinit var maneuverApi: MapboxManeuverApi
    lateinit var tripProgressApi: MapboxTripProgressApi
    lateinit var routeLineApi: MapboxRouteLineApi
    lateinit var routeLineView: MapboxRouteLineView
    lateinit var routeArrowApi: MapboxRouteArrowApi
    lateinit var routeArrowView: MapboxRouteArrowView
    val soundButton : MapboxSoundButton
    get() = findViewById(R.id.soundButton)
    val tripProgressCard : CardView
    get() = findViewById(R.id.tripProgressCard)
    val maneuverView : MapboxManeuverView
    get() = findViewById(R.id.maneuverView)
    val tripProgressView : MapboxTripProgressView
    get() = findViewById(R.id.tripProgressView)
    val recenter : MapboxRecenterButton
    get() = findViewById(R.id.recenter)
    val routeOverview : MapboxRouteOverviewButton
    get() = findViewById(R.id.routeOverview)
    lateinit var speechApi : MapboxSpeechApi
    lateinit var voiceInstructionsPlayer: MapboxVoiceInstructionsPlayer
    lateinit var speechCallback : MapboxNavigationConsumer<Expected<SpeechError, SpeechValue>>
    lateinit var voiceInstructionsPlayerCallback : MapboxNavigationConsumer<SpeechAnnouncement>
    lateinit var voiceInstructionsObserver: VoiceInstructionsObserver
    lateinit var navigationLocationProvider : NavigationLocationProvider
    lateinit var locationObserver: LocationObserver
    lateinit var routeProgressObserver: RouteProgressObserver
    lateinit var routesObserver : RoutesObserver
    lateinit var replayProgressObserver : ReplayProgressObserver
    lateinit var mapboxReplayer : MapboxReplayer
    lateinit var roadShieldCallback : RoadShieldCallback
    private var isVoiceInstructionsMuted = false
        set(value) {
            field = value
            if (value) {
                soundButton.muteAndExtend(1500L)
                voiceInstructionsPlayer.volume(SpeechVolume(0f))
            } else {
                soundButton.unmuteAndExtend(1500L)
                voiceInstructionsPlayer.volume(SpeechVolume(1f))
            }
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mapView = findViewById(R.id.mapView)
        soundButton.visibility = View.GONE
        maneuverView.visibility = View.GONE
        tripProgressCard.visibility = View.GONE
        tripProgressView.visibility = View.GONE
        recenter.visibility = View.GONE
        if (!shouldNav) {
            onIndicatorBearingChangedListener = OnIndicatorBearingChangedListener {
                mapView!!.getMapboxMap().setCamera(CameraOptions.Builder().bearing(it).build())
            }

            onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener {
                mapView!!.getMapboxMap().setCamera(CameraOptions.Builder().center(it).build())
                mapView!!.gestures.focalPoint = mapView!!.getMapboxMap().pixelForCoordinate(it)
            }
            fun onCameraTrackingDismissed() {
                mapView!!.location
                    .removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
                mapView!!.location
                    .removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
                mapView!!.gestures.removeOnMoveListener(onMoveListener)
            }
            onMoveListener = object : OnMoveListener {
                override fun onMoveBegin(detector: MoveGestureDetector) {
                    onCameraTrackingDismissed()
                }

                override fun onMove(detector: MoveGestureDetector): Boolean {
                    return false
                }

                override fun onMoveEnd(detector: MoveGestureDetector) {}
            }
            fun initGesturesListener(){
                mapView!!.gestures.addOnMoveListener(onMoveListener)
            }
            fun startLocationComponent(){
                mapView!!.location.updateSettings {
                    enabled = true
                    locationPuck = LocationPuck2D(
                            bearingImage = AppCompatResources.getDrawable(
                                this@MainActivity,
                                com.mapbox.navigation.R.drawable.mapbox_navigation_puck_icon,
                            ),
                    shadowImage = AppCompatResources.getDrawable(
                        this@MainActivity,
                        com.mapbox.maps.R.drawable.mapbox_user_icon_shadow,
                    ),
                    scaleExpression = interpolate {
                        linear()
                        zoom()
                        stop {
                            literal(0.0)
                            literal(0.6)
                        }
                        stop {
                            literal(20.0)
                            literal(1.0)
                        }
                    }.toJson()
                    )
                }
                mapView!!.location.addOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
                mapView!!.location.addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
            }
            mapView!!.getMapboxMap().setCamera(
                CameraOptions.Builder()
                    .zoom(14.0)
                    .build()
            )
            mapView?.getMapboxMap()?.loadStyleUri(Style.MAPBOX_STREETS, object : Style.OnStyleLoaded {
                override fun onStyleLoaded(style: Style) {
                    initGesturesListener()
                    startLocationComponent()
                }
            })
        } else {
            //Navigation
            soundButton.visibility = View.VISIBLE
            maneuverView.visibility = View.VISIBLE
            tripProgressCard.visibility = View.VISIBLE
            tripProgressView.visibility = View.VISIBLE
            recenter.visibility = View.VISIBLE
            mapboxReplayer = MapboxReplayer()
            val replayLocationEngine = ReplayLocationEngine(mapboxReplayer)
            replayProgressObserver = ReplayProgressObserver(mapboxReplayer)
            val mapboxMap : MapboxMap = mapView!!.getMapboxMap()
            navigationLocationProvider = NavigationLocationProvider()
            routeArrowApi = MapboxRouteArrowApi()
            locationObserver = object : LocationObserver {
                var firstLocationUpdateReceived = false
                override fun onNewRawLocation(rawLocation: Location) {

                }

                override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
                    val enhancedLocation = locationMatcherResult.enhancedLocation
                    navigationLocationProvider.changePosition(
                        location = enhancedLocation,
                        keyPoints = locationMatcherResult.keyPoints,
                    )
                    viewportDataSource.onLocationChanged(enhancedLocation)
                    viewportDataSource.evaluate()
                    if (!firstLocationUpdateReceived) {
                        firstLocationUpdateReceived = true
                        navigationCamera.requestNavigationCameraToOverview(
                            stateTransitionOptions = NavigationCameraTransitionOptions.Builder()
                                .maxDuration(0)
                                .build()
                        )
                    }
                }
            }
            routeProgressObserver = RouteProgressObserver { routeProgress ->
                viewportDataSource.onRouteProgressChanged(routeProgress)
                viewportDataSource.evaluate()
                val style = mapboxMap.getStyle()
                if (style != null) {
                    val maneuverArrowResult = routeArrowApi.addUpcomingManeuverArrow(routeProgress)
                    routeArrowView.renderManeuverUpdate(style, maneuverArrowResult)
                }
                val maneuvers = maneuverApi.getManeuvers(routeProgress)
                maneuvers.fold(
                    { error ->
                        Toast.makeText(
                            this@MainActivity,
                            error.errorMessage,
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    { maneuverList ->
                        maneuverView.visibility = View.VISIBLE
                        maneuverView.renderManeuvers(maneuvers)
                        maneuverApi.getRoadShields(maneuverList, roadShieldCallback)
                    }
                )
                tripProgressView.render(
                    tripProgressApi.getTripProgress(routeProgress)
                )
            }
            roadShieldCallback = object : RoadShieldCallback {
                override fun onRoadShields(
                    maneuvers: List<Maneuver>,
                    shields: Map<String, RoadShield?>,
                    errors: Map<String, RoadShieldError>
                ) {
                    maneuverView.renderManeuverShields(shields)
                }
            }
            routesObserver = RoutesObserver { routeUpdateResult: RoutesUpdatedResult ->
                if (routeUpdateResult.routes.isNotEmpty()) {
                    val routeLines = routeUpdateResult.routes.map { RouteLine(it, null) }
                    routeLineApi.setRoutes(
                        routeLines
                    ) { value ->
                        mapboxMap.getStyle()?.apply {
                            routeLineView.renderRouteDrawData(this, value)
                        }
                    }
                    viewportDataSource.onRouteChanged(routeUpdateResult.routes.first())
                    viewportDataSource.evaluate()
                } else {
                    val style = mapboxMap.getStyle()
                    if (style != null) {
                        routeLineApi.clearRouteLine {value ->
                            routeLineView.renderClearRouteLineValue(
                                style,
                                value
                            )
                        }
                        routeArrowView.render(style, routeArrowApi.clearArrows())
                    }
                    viewportDataSource.clearRouteData()
                    viewportDataSource.evaluate()
                }
            }
            voiceInstructionsPlayerCallback = MapboxNavigationConsumer<SpeechAnnouncement> { value ->
                speechApi.clean(value)
            }
            speechCallback = MapboxNavigationConsumer { expected ->
                expected.fold(
                    { error ->
                        voiceInstructionsPlayer.play(
                            error.fallback,
                            voiceInstructionsPlayerCallback
                        )
                    },
                    { value ->
                        voiceInstructionsPlayer.play(
                            value.announcement,
                            voiceInstructionsPlayerCallback
                        )
                    }
                )
            }
            voiceInstructionsObserver = VoiceInstructionsObserver { voiceInstructions ->
                speechApi.generate(voiceInstructions, speechCallback)
            }
            mapView!!.location.apply {
                this.locationPuck = LocationPuck2D(
                    bearingImage = ContextCompat.getDrawable(
                        this@MainActivity,
                        com.mapbox.navigation.R.drawable.mapbox_navigation_puck_icon
                    )
                )
                setLocationProvider(navigationLocationProvider)
                enabled = true
            }
            mapboxNavigation = if (MapboxNavigationProvider.isCreated()) {
                MapboxNavigationProvider.retrieve()
            } else {
                MapboxNavigationProvider.create(
                    NavigationOptions.Builder(this.applicationContext)
                        .accessToken(getString(R.string.mapbox_access_token))
                        //.locationEngine(replayLocationEngine)
                        .build()
                )
            }
            viewportDataSource = MapboxNavigationViewportDataSource(mapboxMap)
            navigationCamera = NavigationCamera(
                mapboxMap,
                mapView!!.camera,
                viewportDataSource
            )
            mapView!!.camera.addCameraAnimationsLifecycleListener(
                NavigationBasicGesturesHandler(navigationCamera)
            )
            navigationCamera.registerNavigationCameraStateChangeObserver { navigationCameraState ->
                when (navigationCameraState) {
                    NavigationCameraState.TRANSITION_TO_FOLLOWING,
                    NavigationCameraState.FOLLOWING -> recenter.visibility = View.INVISIBLE
                    NavigationCameraState.TRANSITION_TO_OVERVIEW,
                    NavigationCameraState.OVERVIEW,
                    NavigationCameraState.IDLE -> recenter.visibility = View.VISIBLE
                }
            }
            if (this.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                viewportDataSource.overviewPadding = landscapeOverviewPadding
            } else {
                viewportDataSource.overviewPadding = overviewPadding
            }
            if (this.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                viewportDataSource.followingPadding = landscapeFollowingPadding
            } else {
                viewportDataSource.followingPadding = followingPadding
            }
            val distanceFormatterOptions = mapboxNavigation.navigationOptions.distanceFormatterOptions
            maneuverApi = MapboxManeuverApi(
                MapboxDistanceFormatter(distanceFormatterOptions)
            )
            tripProgressApi = MapboxTripProgressApi(
                TripProgressUpdateFormatter.Builder(this)
                    .distanceRemainingFormatter(
                        DistanceRemainingFormatter(distanceFormatterOptions)
                    )
                    .timeRemainingFormatter(
                        TimeRemainingFormatter(this)
                    )
                    .percentRouteTraveledFormatter(
                        PercentDistanceTraveledFormatter()
                    )
                    .estimatedTimeToArrivalFormatter(
                        EstimatedTimeToArrivalFormatter(this, TimeFormat.NONE_SPECIFIED)
                    )
                    .build()
            )
            speechApi = MapboxSpeechApi(
                this,
                getString(R.string.mapbox_access_token),
                Locale.US.language
            )
            voiceInstructionsPlayer = MapboxVoiceInstructionsPlayer(
                this,
                getString(R.string.mapbox_access_token),
                Locale.US.language
            )
            val mapboxRouteLineOptions = MapboxRouteLineOptions.Builder(this)
                .withRouteLineBelowLayerId("road-label")
                .build()
            routeLineApi = MapboxRouteLineApi(mapboxRouteLineOptions)
            routeLineView = MapboxRouteLineView(mapboxRouteLineOptions)
            val routeArrowOptions = RouteArrowOptions.Builder(this).build()
            routeArrowView = MapboxRouteArrowView(routeArrowOptions)
            mapboxMap.loadStyleUri(
                Style.MAPBOX_STREETS
            ) {
                findRoute(Point.fromLngLat(-74.1775831, 40.6951032))
            }
            recenter.setOnClickListener {
                navigationCamera.requestNavigationCameraToFollowing()
                routeOverview.showTextAndExtend(1500L)
            }
            routeOverview.setOnClickListener {
                navigationCamera.requestNavigationCameraToOverview()
                recenter.showTextAndExtend(1500L)
            }
            soundButton.setOnClickListener {
                isVoiceInstructionsMuted = !isVoiceInstructionsMuted
            }
            soundButton.unmute()
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                //Check Permissions
                return
            }
            mapboxNavigation.startTripSession()
        }
    }
    override fun onStart() {
        super.onStart()
        mapView?.onStart()
        if (shouldNav) {
            mapboxNavigation.registerRoutesObserver(routesObserver)
            mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
            mapboxNavigation.registerLocationObserver(locationObserver)
            mapboxNavigation.registerVoiceInstructionsObserver(voiceInstructionsObserver)
            mapboxNavigation.registerRouteProgressObserver(replayProgressObserver)
            if (mapboxNavigation.getRoutes().isEmpty()) {
                mapboxReplayer.pushEvents(
                    listOf(
                        ReplayRouteMapper.mapToUpdateLocation(
                            eventTimestamp = 0.0,
                            point = Point.fromLngLat(-74.3470166, 40.5982555)
                        )
                    )
                )
                mapboxReplayer.playFirstLocation()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
        if (!shouldNav) {
            mapView?.location?.removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
            mapView?.location?.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
            mapView?.gestures?.removeOnMoveListener(onMoveListener)
        }
        if (shouldNav) {
            MapboxNavigationProvider.destroy()
            mapboxReplayer.finish()
            maneuverApi.cancel()
            routeLineApi.cancel()
            routeLineView.cancel()
            speechApi.cancel()
            voiceInstructionsPlayer.shutdown()
        }
    }

    private fun findRoute(destination: Point) {
        val originLocation = navigationLocationProvider.lastLocation
        val originPoint = originLocation?.let {
            Point.fromLngLat(it.longitude, it.latitude)
        } ?: return
        mapboxNavigation.requestRoutes(
            RouteOptions.builder()
                .applyDefaultNavigationOptions()
                .applyLanguageAndVoiceUnitOptions(this)
                .coordinatesList(listOf(originPoint, destination))
                .bearingsList(
                    listOf(
                        Bearing.builder()
                            .angle(originLocation.bearing.toDouble())
                            .degrees(45.0)
                            .build(),
                        null
                    )
                )
                .layersList(listOf(mapboxNavigation.getZLevel(), null))
                .build(),
            object : RouterCallback {
                override fun onRoutesReady(
                    routes: List<DirectionsRoute>,
                    routerOrigin: RouterOrigin
                ) {
                    setRouteAndStartNavigation(routes)
                }

                override fun onFailure(
                    reasons: List<RouterFailure>,
                    routeOptions: RouteOptions
                ) {
                }

                override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) {
                }
            }
        )
    }
    override fun onConfigurationChanged(newConfig: Configuration) {
        //Custom code to fix camera not framing properly in certain devices
        super.onConfigurationChanged(newConfig)
        if (shouldNav) {
            navigationCamera.requestNavigationCameraToFollowing()
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                viewportDataSource.followingPadding = landscapeFollowingPadding
                maneuverView.layoutParams.width = ((displayMetrics.widthPixels / 2) + (displayMetrics.widthPixels/12))
                val params = maneuverView.layoutParams as ViewGroup.MarginLayoutParams
                params.setMargins(displayMetrics.widthPixels/30, 10, 580, 600)
                maneuverView.layoutParams = params
            } else {
                viewportDataSource.followingPadding = followingPadding
                viewportDataSource.followingPadding = followingPadding
                maneuverView.layoutParams.width = (displayMetrics.widthPixels - 20)
                val params = maneuverView.layoutParams as ViewGroup.MarginLayoutParams
                params.setMargins(10, 10, 10, 10)
            }
        } else {
            //No Need For Custom Code
        }
    }
    private fun startSimulation(route: DirectionsRoute) {
        mapboxReplayer.run {
            stop()
            clearEvents()
            val replayEvents = ReplayRouteMapper().mapDirectionsRouteGeometry(route)
            pushEvents(replayEvents)
            seekTo(replayEvents.first())
            play()
        }
    }
    private fun setRouteAndStartNavigation(routes: List<DirectionsRoute>){
        mapboxNavigation.setRoutes(routes)
        startSimulation(routes.first())
        soundButton.visibility = View.VISIBLE
        routeOverview.visibility = View.VISIBLE
        tripProgressCard.visibility = View.VISIBLE
        navigationCamera.requestNavigationCameraToOverview()

    }
}