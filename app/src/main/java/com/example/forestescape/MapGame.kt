package com.example.forestescape

import android.content.ContentValues
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
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

    private var _binding: FragmentMapGameBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapGameBinding.inflate(inflater, container, false)
        initMap()
        return _binding!!.root
    }

    private fun initMap() {
        mapView = _binding?.root?.findViewById(R.id.mapView)
        mapView?.getMapboxMap()?.loadStyleUri(STYLE_URL) { initLocationComponent() }
        mapBoxMap = mapView!!.getMapboxMap()
        disableGestures()
    }

    private fun disableGestures() = mapView?.gestures?.apply {
        rotateEnabled = false
        pitchEnabled = false
        doubleTapToZoomInEnabled = false
        doubleTouchToZoomOutEnabled = false
        scrollEnabled = false
        simultaneousRotateAndPinchToZoomEnabled = false
    }

    private fun initLocationComponent() {
        val location = mapView?.location2
        location?.updateSettings {
            this.enabled = true
            this.locationPuck = LOCATION_PUCK
        }
        location?.addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapSharedViewModel =
            ViewModelProvider(requireActivity()).get(MapSharedViewModel::class.java)
        mapSharedViewModel.azimuth.observe(requireActivity()) { updateAzimuthOnMapView(it) }
        onIndicatorPositionChangedListener =
            OnIndicatorPositionChangedListener { updateLocationOnMapView(it) }
        mapView?.compass?.enabled = false
        mapView?.scalebar?.enabled = false
    }

    private fun updateLocationOnMapView(it: Point) {
        mapBoxMap.setCamera(CameraOptions.Builder().zoom(ZOOM_LEVEL).center(it).build())
        mapBoxMap.updateTargetPosition(TARGET_LAT, TARGET_LNG)
    }

    private fun updateAzimuthOnMapView(it: Float) {
        mapBoxMap.setCamera(
            CameraOptions.Builder()
                .bearing(-it.toDouble())
                .zoom(ZOOM_LEVEL)
                .build()
        )
        mapView?.angle = it
        mapBoxMap.updateTargetPosition(TARGET_LAT, TARGET_LNG)
    }

    private fun MapboxMap.updateTargetPosition(lat: Double, lng: Double) {
        mapView?.setTargetPosition(pixelForCoordinate(Point.fromLngLat(lng, lat)))
    }

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
        Log.i(ContentValues.TAG, "MAP GAME OBSERVED CURRENT GAME: $it")
        when (it) {
            CurrentGame.MAP -> Unit
            CurrentGame.PASSWORD ->
                findNavController().navigate(MapGameDirections.actionMapGameToPasswordGame())
            CurrentGame.NO_GAME ->
                findNavController().navigate(MapGameDirections.actionMapGameToNoGame())
            CurrentGame.CHARGE ->
                findNavController().navigate(MapGameDirections.actionMapGameToChargeGame())
            CurrentGame.SCAN ->
                findNavController().navigate(MapGameDirections.actionMapGameToScanGame())
        }
    }

    companion object {
        private const val STYLE_URL =
            "mapbox://styles/maciejlewandowski/cl5b4uyla00g814ll400gdzs2/draft"
        private const val TARGET_LAT = 51.72999676571438
        private const val TARGET_LNG = 19.46115370934121
        private const val ZOOM_LEVEL = 18.0
        private val LOCATION_PUCK = LocationPuck2D(
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
}
