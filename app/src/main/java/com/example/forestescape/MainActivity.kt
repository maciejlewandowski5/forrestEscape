package com.example.forestescape

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModelProvider
import com.google.ar.core.ArCoreApk
import android.app.ActivityManager
import android.content.Context
import android.provider.Settings
import com.example.forestescape.databinding.ActivityMainBinding
import com.example.forestescape.model.CurrentGame
import com.example.forestescape.model.Game
import com.example.forestescape.model.State
import com.example.forestescape.repository.FireBaseGameRepository
import com.example.forestescape.viewmodel.*


class MainActivity : AppCompatActivity() {
    private lateinit var permissionsViewModel: PermissionsViewModel
    private lateinit var arInstallViewModel: ArSessionViewModel
    private lateinit var gameViewModel: GameViewModel
    private lateinit var internetConnectionViewModel: InternetConnectionViewModel
    private lateinit var sensorsViewModel: SensorViewModel
    private lateinit var batteryLevelViewModel: BatteryLevelViewModel
    private lateinit var locationViewModel: LocationViewModel
    private lateinit var currentGameSharedViewModel: CurrentGameSharedViewModel

    private var mUserRequestedInstall: Boolean = true

    private lateinit var game: Game

    private lateinit var binding: ActivityMainBinding


    @SuppressLint("HardwareIds") // Id is needed to identity device via management system
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.constraint)

        finishIfArCoreNotAvailable()

        setupViewModels()

    }

    private fun setupViewModels() {
        setupPermissionsViewModel()
        setupInternetConnectionViewModel()
        setupArInstallViewModel()
        setUpGameViewModel()
        setupSensorsViewModel()
        setupBatteryLevelViewModel()
        setupLocationViewModel()
    }

    private fun setupPermissionsViewModel() {
        permissionsViewModel = ViewModelProvider(this).get(PermissionsViewModel::class.java)
        finishIfPermissionIsDeniedNTimes(3)
    }

    private fun setupInternetConnectionViewModel() {
        internetConnectionViewModel =
            ViewModelProvider(this).get(InternetConnectionViewModel::class.java)
    }

    private fun setupArInstallViewModel() {
        arInstallViewModel = ViewModelProvider(this).get(ArSessionViewModel::class.java)
        finishIfUserDeniedArCoreInstallation()
        updateUserInstall()
    }

    private fun setupSensorsViewModel() {
        sensorsViewModel =
            ViewModelProvider(this).get(SensorViewModel::class.java)
        sensorsViewModel.sensorLiveData.observe(this) { value ->
            value?.let { gameViewModel.setSensorsData(it) }
        }
    }

    private fun setupLocationViewModel() {
        locationViewModel = ViewModelProvider(this).get(LocationViewModel::class.java)
        locationViewModel.location.observe(this) { value ->
            gameViewModel.setLocation(value)
        }
    }

    private fun setupBatteryLevelViewModel() {
        batteryLevelViewModel = ViewModelProvider(this).get(BatteryLevelViewModel::class.java)
        batteryLevelViewModel.batteryLevel.observe(this) { value ->
            gameViewModel.setBatteryLevel(value)
        }
    }

    private fun setUpGameViewModel() {
        currentGameSharedViewModel =
            ViewModelProvider(this).get(CurrentGameSharedViewModel::class.java)
        gameViewModel = ViewModelProvider(this, GameViewModelFactory(FireBaseGameRepository())).get(
            GameViewModel::class.java
        )
        gameViewModel.initializeGameSession(
            Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ANDROID_ID
            )
        )
        gameViewModel.gameModel.observe(this) { value -> observeViewModel(value) }

        gameViewModel.currentGame.observe(this) { value ->
            when (value.dataStatus) {
                State.DataStatus.SUCCESS -> {
                    game.id?.let {
                        sendCurrentGameStateToSharedViewModel(value)
                    }
                }
                State.DataStatus.ERROR -> {

                }
                State.DataStatus.LOADING -> {
                }
            }
        }
    }

    private fun observeViewModel(value: State<Game>) {
        when (value.dataStatus) {
            State.DataStatus.SUCCESS -> {
                game = value.dataValue!!
            }
            State.DataStatus.ERROR -> {

            }
            State.DataStatus.LOADING -> {
            }
        }
    }

    private fun sendCurrentGameStateToSharedViewModel(value: State<CurrentGame>?) {
        value!!.dataValue?.let { it1 ->
            currentGameSharedViewModel.currentGame.value = it1
        }
    }


    override fun onResume() {
        super.onResume()
        if (!permissionsViewModel.hasCameraPermission(this) ||
            !permissionsViewModel.hasCoarseLocationPermission(this) ||
            !permissionsViewModel.hasFineLocationPermission(this)
        ) {
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
        super.onBackPressed()
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


