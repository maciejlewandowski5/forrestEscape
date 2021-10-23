package com.example.forestescape

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModelProvider
import com.google.ar.core.ArCoreApk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import android.app.ActivityManager
import android.content.Context
import android.provider.Settings


class MainActivity : AppCompatActivity() {
    private lateinit var permissionsViewModel: PermissionsViewModel
    private lateinit var arInstallViewModel: ArSessionViewModel
    private lateinit var gameViewModel: GameViewModel
    private lateinit var internetConnectionViewModel: InternetConnectionViewModel

    private var mUserRequestedInstall: Boolean = true
    private var gameSession: GameSession? = null


    @SuppressLint("HardwareIds") // Id is needed to identity device via management system
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        finishIfArCoreNotAvailable();

        permissionsViewModel = ViewModelProvider(this).get(PermissionsViewModel::class.java)
        finishIfPermissionIsDeniedNTimes(3)
        internetConnectionViewModel =
            ViewModelProvider(this).get(InternetConnectionViewModel::class.java)
        arInstallViewModel = ViewModelProvider(this).get(ArSessionViewModel::class.java)
        finishIfUserDeniedArCoreInstallation()
        gameViewModel = ViewModelProvider(this, GameViewModelFactory(FireBaseGameRepository())).get(
            GameViewModel::class.java
        )
        gameViewModel.initializeGameSession(
            Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ANDROID_ID
            )
        )
        gameViewModel.gameSession.observe(this) {
            gameSession = it
        }

        updateUserInstall()


    }


    override fun onResume() {
        super.onResume()
        if (!permissionsViewModel.hasCameraPermission(this)) {
            permissionsViewModel.requestPermission()
        }
        if (!internetConnectionViewModel.isOnline()) {
            finish()
        }
        arInstallViewModel.requestSessionAndInstall(
            ArCoreApk.getInstance()
                .requestInstall(this, mUserRequestedInstall)!!
        )
        return
    }

    override fun onPause() {
        super.onPause()
        val activityManager = applicationContext
            .getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        activityManager.moveTaskToFront(taskId, 0)
    }

    override fun onBackPressed() {
        // No super.onBackPressed()
        // Because we do not want user to be able to exit app. There is a game. Exiting app is
        // possible via firebase setting
    }

    private fun updateUserInstall() {
        arInstallViewModel.userRequestedInstall.observe(this) {
            mUserRequestedInstall = it
        }
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

    private fun finishIfUserDeniedArCoreInstallation() {
        arInstallViewModel.userDeclinedInstallation.observe(this) {
            if (it == true) {
                finish()
            }
        }
    }

    private fun finishIfPermissionIsDeniedNTimes(n: Int) {
        permissionsViewModel.getCanceled().observe(this) {
            if (it == n) {
                this.finish()
            }
        }
    }
}


