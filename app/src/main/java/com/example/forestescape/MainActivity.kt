package com.example.forestescape

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModelProvider
import com.google.ar.core.ArCoreApk

class MainActivity : AppCompatActivity() {
    private lateinit var permissionsViewModel: PermissionsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        finishIfArCoreNotAvailable();

        permissionsViewModel = ViewModelProvider(this).get(PermissionsViewModel::class.java)
        finishIfPermissionIsDeniedNTimes(3)
    }

    private fun finishIfPermissionIsDeniedNTimes(n:Int) {
        permissionsViewModel.getCanceled().observe(this) {
            if (it == n) {
                this.finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!permissionsViewModel.hasCameraPermission(this)) {
            permissionsViewModel.requestPermission()
        }
        return
    }

    private fun finishIfArCoreNotAvailable() {
        val availability = ArCoreApk.getInstance().checkAvailability(this)
        if (availability.isTransient) {
            Handler(Looper.getMainLooper()).postDelayed({
                finishIfArCoreNotAvailable()
            }, 200)
        }
        if (!availability.isSupported) {
            finish()
        }
    }
}


