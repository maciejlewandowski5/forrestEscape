package com.example.forestescape

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.google.ar.core.ArCoreApk

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        finishIfARNotAvaliable();
    }

    private fun finishIfARNotAvaliable() {
        val availability = ArCoreApk.getInstance().checkAvailability(this)
        if (availability.isTransient) {
            Handler(Looper.getMainLooper()).postDelayed({
                finishIfARNotAvaliable()
            }, 200)
        }
        if (!availability.isSupported) {
            finish()
        }
    }
}