package com.example.forestescape.viewmodel

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2

class SensorViewModel(
    application: Application,
) : AndroidViewModel(application) {
    val sensorLiveData: SensorLiveData = SensorLiveData()

    inner class SensorLiveData : LiveData<SensorsData>(), SensorEventListener {
        private val mRotHist: MutableList<FloatArray> = ArrayList()
        private var mRotHistIndex = 0

        private val mHistoryMaxLength = 40
        private var mGravity: FloatArray? = null
        private var mMagnetic: FloatArray? = null
        private var mRotation: FloatArray? = null
        private var mRotationMatrix = FloatArray(9)
        private var rotationPreviousValues: FloatArray? = null
        private var rotationX: Float = 0f
        private var rotationY: Float = 0f
        private var rotationZ: Float = 0f
        private var light: Float = 0f

        private var mFacing: Float? = null

        private val TWENTY_FIVE_DEGREE_IN_RADIAN = 0.436332313f
        private val ONE_FIFTY_FIVE_DEGREE_IN_RADIAN: Float = 2.7052603f


        private val sensorManager: SensorManager
            get() =
                getApplication<Application>()
                    .getSystemService(Context.SENSOR_SERVICE) as SensorManager

        override fun observe(owner: LifecycleOwner, observer: Observer<in SensorsData?>) {
            super.observe(owner, observer)
        }

        override fun onSensorChanged(event: SensorEvent?) {
            if (event != null) {

                when (event.sensor.type) {
                    Sensor.TYPE_GRAVITY -> {
                        mGravity = event.values.clone()
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        mMagnetic = event.values.clone()
                    }
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        mRotation = event.values.clone()
                    }
                    Sensor.TYPE_GAME_ROTATION_VECTOR -> {
                        if (rotationPreviousValues != null) {
                            if (abs(event.values[0]) > abs(rotationPreviousValues!![0]) * SENSITIVITY_COEFFICIENT) {
                                rotationX = event.values[0]
                            }
                            if (abs(event.values[1]) > abs(rotationPreviousValues!![1]) * SENSITIVITY_COEFFICIENT) {
                                rotationY = event.values[1]
                            }
                            if (abs(event.values[2]) > abs(
                                    rotationPreviousValues!![2]
                                ) * SENSITIVITY_COEFFICIENT
                            ) {
                                rotationZ = event.values[2]
                            }

                        }
                        rotationPreviousValues = event.values.clone()
                    }
                    Sensor.TYPE_LIGHT -> {
                        light = event.values[0]
                    }
                }

                if (mGravity != null && mMagnetic != null) {
                    if (SensorManager.getRotationMatrix(
                            mRotationMatrix,
                            null,
                            mGravity,
                            mMagnetic
                        )
                    ) {
                        val inclination = acos(mRotationMatrix[8].toDouble()).toFloat()
                        mFacing = if (inclination < TWENTY_FIVE_DEGREE_IN_RADIAN ||
                            inclination > ONE_FIFTY_FIVE_DEGREE_IN_RADIAN
                        ) {
                            clearRotHist()
                            Float.NaN
                        } else {
                            setRotHist()
                            findFacing()
                        }
                    }
                }
            }

            var azimuth =
                mRotation?.get(2)?.times(2 * 180)?.div(Math.PI)?.plus(0)?.toDouble() ?: 0.0
            //(mFacing?.toDouble()?.times( 180))?.div(Math.PI) ?: 0.0
            azimuth = if (azimuth < 0) {
                360 + azimuth
            } else {
                azimuth
            }
            postValue(
                SensorsData(
                    azimuth,
                    light,
                    rotationX,
                    rotationY,
                    rotationZ
                )
            )
            /*postValue(
                SensorsData(
                    asin(rotationZ.toDouble()) * 2 / Math.PI * 180 + 180,
                    light,
                    rotationX,
                    rotationY,
                    rotationZ
                )
            )

             */
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            //Not used
        }

        override fun onActive() {
            sensorManager.let { sm ->
                sm.getDefaultSensor(Sensor.TYPE_LIGHT).let {
                    sm.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
                }
            }
            sensorManager.let { sm ->
                sm.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR).let {
                    sm.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
                }
            }
            sensorManager.let { sm ->
                sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD).let {
                    sm.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
                }
            }
            sensorManager.let { sm ->
                sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR).let {
                    sm.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
                }
            }
            sensorManager.let { sm ->
                sm.getDefaultSensor(Sensor.TYPE_GRAVITY).let {
                    sm.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
                }
            }
        }

        override fun onInactive() {
            sensorManager.unregisterListener(this)
        }

        private fun clearRotHist() {

            mRotHist.clear()
            mRotHistIndex = 0
        }

        private fun setRotHist() {
            val hist = mRotationMatrix.clone()
            if (mRotHist.size == mHistoryMaxLength) {
                mRotHist.removeAt(mRotHistIndex)
            }
            mRotHist.add(mRotHistIndex++, hist)
            mRotHistIndex %= mHistoryMaxLength
        }

        private fun findFacing(): Float {
            val averageRotHist = average(mRotHist)
            return atan2(
                (-averageRotHist[2]).toDouble(),
                (-averageRotHist[5]).toDouble()
            ).toFloat()
        }

        private fun average(values: List<FloatArray>): FloatArray {
            val result = FloatArray(9)
            for (value in values) {
                for (i in 0..8) {
                    result[i] += value[i]
                }
            }
            for (i in 0..8) {
                result[i] = result[i] / values.size
            }
            return result
        }
    }

    companion object {
        private const val SENSITIVITY_COEFFICIENT = 1.15
    }
}

data class SensorsData(
    val azimuth: Double,
    val light: Float,
    val deviceRotationX: Float,
    val deviceRotationY: Float,
    val deviceRotationZ: Float,
)

class AzimuthCalculator {

}