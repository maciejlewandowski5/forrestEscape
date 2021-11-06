package com.example.forestescape

import android.content.ContentValues.TAG
import android.net.Uri
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.example.forestescape.databinding.FragmentScanGameBinding
import com.example.forestescape.model.CurrentGame
import com.example.forestescape.renderer.AugmentedImageRenderer
import com.example.forestescape.renderer.BackgroundRenderer
import com.example.forestescape.viewmodel.ArSessionViewModel
import com.example.forestescape.viewmodel.CurrentGameSharedViewModel
import com.example.forestescape.viewmodel.SurfaceSizeViewModel
import com.google.ar.core.*
import java.io.IOException
import java.util.HashMap
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


class ScanGame : Fragment(), Observer<CurrentGame>, GLSurfaceView.Renderer {
    private lateinit var currentGameSharedViewModelViewModel: CurrentGameSharedViewModel
    private lateinit var surfaceSizeViewModel: SurfaceSizeViewModel
    private lateinit var arSessionViewModel: ArSessionViewModel

    private var dummyButton: Button? = null
    private var _binding: FragmentScanGameBinding? = null

    private var surfaceView: GLSurfaceView? = null
    private var fitToScanView: ImageView? = null

    private val backgroundRenderer = BackgroundRenderer()
    private val augmentedImageRenderer: AugmentedImageRenderer = AugmentedImageRenderer()


    private var session: Session? = null


    private val augmentedImageMap: MutableMap<Int, Pair<AugmentedImage, Anchor>> = HashMap()


    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanGameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRenderer()
        dummyButton?.setOnClickListener {
            dummyButton?.setOnClickListener {
                val action = MapGameDirections.actionMapGameToNoGame()
                findNavController().navigate(action)
            }
        }
        setupViewModels()

    }

    private fun setupViewModels() {
        arSessionViewModel =
            ViewModelProvider(requireActivity()).get(ArSessionViewModel::class.java)
        arSessionViewModel.session.observe(requireActivity()) { it?.let { session = it } }
        surfaceSizeViewModel =
            ViewModelProvider(requireActivity()).get(SurfaceSizeViewModel::class.java)
        surfaceSizeViewModel.surfaceSizeLiveData.observe(viewLifecycleOwner) {
            session?.setDisplayGeometry(it.rotation, it.width, it.height)

        }
    }

    private fun setupRenderer() {
        surfaceView = binding.surfaceview
        surfaceView!!.preserveEGLContextOnPause = true
        surfaceView!!.setEGLContextClientVersion(2)
        surfaceView!!.setEGLConfigChooser(8, 8, 8, 8, 16, 0) // Alpha used for plane blending.
        surfaceView!!.setRenderer(this)
        surfaceView!!.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        surfaceView!!.setWillNotDraw(false)

        fitToScanView = binding.imageViewFitToScan
    }

    override fun onResume() {
        super.onResume()
        currentGameSharedViewModelViewModel =
            ViewModelProvider(requireActivity()).get(CurrentGameSharedViewModel::class.java)
        currentGameSharedViewModelViewModel.currentGame.observe(requireActivity(), this)

        session!!.resume()
        surfaceView!!.onResume()
        fitToScanView!!.visibility = View.VISIBLE
    }

    override fun onPause() {
        super.onPause()
        currentGameSharedViewModelViewModel.currentGame.removeObserver(this)

        if (session != null) {
            surfaceView!!.onPause()
            session!!.pause()
        }
    }

    override fun onDestroy() {
        if (session != null) {
            session!!.close()
            session = null
        }
        super.onDestroy()

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onChanged(it: CurrentGame?) {

        when (it) {
            CurrentGame.MAP -> {
                findNavController().navigate(ScanGameDirections.actionScanGameToMapGame())
            }
            CurrentGame.PASSWORD -> {
                findNavController().navigate(ScanGameDirections.actionScanGameToPasswordGame())
            }
            CurrentGame.NO_GAME -> {
                findNavController().navigate(ScanGameDirections.actionScanGameToNoGame())
            }
            CurrentGame.CHARGE -> {

            }
            CurrentGame.SCAN -> {
                // do nothing
            }
        }
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)
        try {
            backgroundRenderer.createOnGlThread(requireContext())
            augmentedImageRenderer.createOnGlThread(requireContext())
        } catch (e: IOException) {
            Log.e(
                TAG,
                "Failed to read an asset file",
                e
            )
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        surfaceSizeViewModel.onSurfaceChanged(width, height)
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        if (session == null) {
            return
        }
        try {
            session!!.setCameraTextureName(backgroundRenderer.textureId)

            val frame = session!!.update()
            val camera = frame.camera

            backgroundRenderer.draw(frame)

            val projmtx = FloatArray(16)
            camera.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f)

            val viewmtx = FloatArray(16)
            camera.getViewMatrix(viewmtx, 0)

            val colorCorrectionRgba = FloatArray(4)
            frame.lightEstimate.getColorCorrection(colorCorrectionRgba, 0)

            drawAugmentedImages(frame, projmtx, viewmtx, colorCorrectionRgba)
        } catch (t: Throwable) {
            // Avoid crashing the application due to unhandled exceptions.
            Log.e(
                TAG,
                "Exception on the OpenGL thread",
                t
            )
        }
    }

    private fun drawAugmentedImages(
        frame: Frame, projmtx: FloatArray, viewmtx: FloatArray, colorCorrectionRgba: FloatArray
    ) {
        val updatedAugmentedImages = frame.getUpdatedTrackables(
            AugmentedImage::class.java
        )

        for (augmentedImage in updatedAugmentedImages) {
            when (augmentedImage.trackingState) {
                TrackingState.PAUSED -> {
                    val text = String.format("Detected Image %d", augmentedImage.index)
                    Log.i(TAG, text)
                    //requireActivity().runOnUiThread { fitToScanView!!.visibility = View.VISIBLE }
                }
                TrackingState.TRACKING -> {
                    requireActivity().runOnUiThread { fitToScanView!!.visibility = View.INVISIBLE }
                    Log.i(TAG, "TRACKING ${augmentedImage.name}")
                    if (!augmentedImageMap.containsKey(augmentedImage.index)) {
                        val centerPoseAnchor =
                            augmentedImage.createAnchor(augmentedImage.centerPose)
                        augmentedImageMap[augmentedImage.index] =
                            Pair.create(augmentedImage, centerPoseAnchor)
                    }
                }
                TrackingState.STOPPED -> {
                    Log.i(TAG, "STOPPED TRACKING ${augmentedImage.name}")
                    augmentedImageMap.remove(augmentedImage.index)
                    requireActivity().runOnUiThread { fitToScanView!!.visibility = View.VISIBLE }
                }
                else -> {
                }
            }
        }
        for (pair in augmentedImageMap.values) {
            val augmentedImage = pair.first
            val centerAnchor = augmentedImageMap[augmentedImage.index]!!.second
            when (augmentedImage.trackingState) {
                TrackingState.TRACKING -> {
                    Log.i(TAG, "DRAWING  ${augmentedImage.name}")
                    augmentedImageRenderer.draw(
                        viewmtx, projmtx, augmentedImage, centerAnchor, colorCorrectionRgba
                    )
                }
                else -> {
                }
            }
        }
    }
}