package com.example.forestescape.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import android.view.Display
import android.view.WindowManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData

class SurfaceSizeViewModel(application: Application) : AndroidViewModel(application) {
    val surfaceSizeLiveData: SurfaceSizeLiveData = SurfaceSizeLiveData()

    fun onSurfaceChanged(width: Int, height: Int) {
        surfaceSizeLiveData.onSurfaceChanged(width, height)
    }

    inner class SurfaceSizeLiveData : LiveData<DisplayGeometry>() {
        private val display: Display? = initializeDisplay()

        private fun initializeDisplay() = //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        //    getApplication<Application>().applicationContext.display
      //  } else {
            (getApplication<Application>()
                .getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
      //  }

        fun onSurfaceChanged(width: Int, height: Int) {
            display?.rotation?.let {
                postValue(
                    DisplayGeometry(
                        rotation = it,
                        width = width,
                        height = height
                    )
                )
            }
        }
    }
}

data class DisplayGeometry(
    val rotation: Int,
    val width: Int,
    val height: Int
)