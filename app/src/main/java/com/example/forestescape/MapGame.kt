package com.example.forestescape

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.forestescape.databinding.FragmentMapGameBinding
import com.example.forestescape.model.CurrentGame
import com.example.forestescape.viewmodel.CurrentGameSharedViewModel
import com.example.forestescape.viewmodel.MapSharedViewModel
import com.mapbox.geojson.Point
import com.mapbox.maps.*
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.locationcomponent.location2
import com.mapbox.maps.plugin.scalebar.scalebar

class MapGame : Fragment(), Observer<CurrentGame> {
    private lateinit var currentGameSharedViewModelViewModel: CurrentGameSharedViewModel
    private lateinit var mapSharedViewModel: MapSharedViewModel
    var mapView: MapViewCustom? = null
    private lateinit var mapBoxMap: MapboxMap

    private lateinit var onIndicatorPositionChangedListener: OnIndicatorPositionChangedListener
    private lateinit var onIndicatorBearingChangedListener: OnIndicatorBearingChangedListener

    private var _binding: FragmentMapGameBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapGameBinding.inflate(inflater, container, false)
        mapView = _binding?.root?.findViewById(R.id.mapView)
        mapView?.getMapboxMap()
            ?.loadStyleUri("mapbox://styles/maciejlewandowski/cl5b4uyla00g814ll400gdzs2/draft") {
                initLocationComponent()
            }
        mapBoxMap = mapView!!.getMapboxMap()
        mapView?.gestures?.rotateEnabled = false
        mapView?.gestures?.pitchEnabled = false
        mapView?.gestures?.doubleTapToZoomInEnabled = false
        mapView?.gestures?.doubleTouchToZoomOutEnabled = false
        mapView?.gestures?.scrollEnabled = false
        mapView?.gestures?.simultaneousRotateAndPinchToZoomEnabled = false
        return _binding!!.root
    }

    private fun initLocationComponent() {
        val locationComponentPlugin = mapView?.location2
        locationComponentPlugin?.updateSettings {
            this.enabled = true
            this.locationPuck = LocationPuck2D(
                bearingImage = null,
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
        locationComponentPlugin?.addOnIndicatorPositionChangedListener(
            onIndicatorPositionChangedListener
        )
    }

    private fun onCameraTrackingDismissed() {
        Toast.makeText(requireContext(), "onCameraTrackingDismissed", Toast.LENGTH_SHORT).show()
        mapView?.location
            ?.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapSharedViewModel =
            ViewModelProvider(requireActivity()).get(MapSharedViewModel::class.java)
        mapSharedViewModel.azimuth.observe(requireActivity()) {
            val cameraPosition = CameraOptions.Builder()
                .bearing(-it.toDouble())
                .zoom(18.0)
                .build()
            mapBoxMap.setCamera(cameraPosition)
            mapView?.angle = it
            mapBoxMap.getPointsToScreenCoordinates(51.729975, 19.460611)
        }
        mapSharedViewModel.location.observe(requireActivity()) {
            //   val cameraPosition = CameraOptions.Builder()
            //      .center(Point.fromLngLat(it.longitude, it.latitude))
            //      .zoom(20.0).build()
            //  mapBoxMap.setCamera(cameraPosition)
            //   mapView?.gestures?.focalPoint = mapView?.getMapboxMap()
            //       ?.pixelForCoordinate(Point.fromLngLat(it.longitude, it.latitude))
        }
        onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener {
            mapView?.getMapboxMap()
                ?.setCamera(
                    CameraOptions.Builder().zoom(18.0)
                        .center(it).build()
                )
            mapBoxMap.getPointsToScreenCoordinates(51.729975, 19.460611)
        }
        mapView?.compass?.enabled = false
        mapView?.scalebar?.enabled = false
    }

    private fun MapboxMap.getPointsToScreenCoordinates(lat: Double, lng: Double) {
        val screenCoordinates =
            pixelForCoordinate(Point.fromLngLat(lng, lat))
        mapView?.setTargetPosition(screenCoordinates)
    }

    // private fun Point.toCoordinates() =
    //    Coordinates(lat = latitude(), lon = longitude())

    override fun onResume() {
        super.onResume()
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        currentGameSharedViewModelViewModel =
            ViewModelProvider(requireActivity()).get(CurrentGameSharedViewModel::class.java)
        currentGameSharedViewModelViewModel.currentGame.observe(requireActivity(), this)
    }

    override fun onPause() {
        super.onPause()
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        currentGameSharedViewModelViewModel.currentGame.removeObserver(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapView?.location
            ?.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        _binding = null
    }

    override fun onChanged(it: CurrentGame?) {

        println("MAP GAME OBSERVED CURRENT GAME: $it")

        when (it) {
            CurrentGame.MAP -> {
                // do nothing
            }
            CurrentGame.PASSWORD -> {
                findNavController().navigate(MapGameDirections.actionMapGameToPasswordGame())
            }
            CurrentGame.NO_GAME -> {
                findNavController().navigate(MapGameDirections.actionMapGameToNoGame())
            }
            CurrentGame.CHARGE -> {
                findNavController().navigate(MapGameDirections.actionMapGameToChargeGame())
            }
            CurrentGame.SCAN -> {
                findNavController().navigate(MapGameDirections.actionMapGameToScanGame())
            }
        }
    }
}

class CustomMapBoxView : MapView {
    val paint = Paint().apply {
        flags = Paint.ANTI_ALIAS_FLAG
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.neon_green)
        strokeWidth = 10f
    }

    constructor(context: Context) : super(context) {
        init(null, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(attrs, defStyle)
    }

    private fun init(attrs: AttributeSet?, defStyle: Int) {

        val a = context.obtainStyledAttributes(
            attrs, R.styleable.MapView, defStyle, 0
        )
        a.recycle()
        this.postInvalidate()
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        canvas.drawCircle(
            0f, 0f, 50f,
            paint
        )
        canvas.drawCircle(
            canvas.width / 2f, canvas.height / 2f, canvas.width / 3f,
            paint
        )
    }
}
