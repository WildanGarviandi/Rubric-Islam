package com.kellinreaver.rubricislam.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Service that fuses accelerometer and magnetometer data to provide
 * the current device heading (azimuth) in degrees.
 */
@Singleton
class CompassSensorManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val _deviceHeading = MutableStateFlow(0f)
    val deviceHeading: StateFlow<Float> = _deviceHeading.asStateFlow()

    private var accelerometerReading = FloatArray(3)
    private var magnetometerReading = FloatArray(3)
    private var rotationMatrix = FloatArray(9)
    private var orientationAngles = FloatArray(3)
    private var lastHeading = 0f
    private val lock = Any()

    fun startListening() {
        synchronized(lock) {
            val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

            if (accelerometer != null) {
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
            }
            if (magnetometer != null) {
                sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI)
            }
        }
    }

    fun stopListening() {
        synchronized(lock) {
            sensorManager.unregisterListener(this)
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        synchronized(lock) {
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    accelerometerReading[0] =
                        accelerometerReading[0] +
                        ALPHA * (event.values[0] - accelerometerReading[0])
                    accelerometerReading[1] =
                        accelerometerReading[1] +
                        ALPHA * (event.values[1] - accelerometerReading[1])
                    accelerometerReading[2] =
                        accelerometerReading[2] +
                        ALPHA * (event.values[2] - accelerometerReading[2])
                }

                Sensor.TYPE_MAGNETIC_FIELD -> {
                    magnetometerReading[0] =
                        magnetometerReading[0] + ALPHA * (event.values[0] - magnetometerReading[0])
                    magnetometerReading[1] =
                        magnetometerReading[1] + ALPHA * (event.values[1] - magnetometerReading[1])
                    magnetometerReading[2] =
                        magnetometerReading[2] + ALPHA * (event.values[2] - magnetometerReading[2])
                }
            }
            updateHeading()
        }
    }

    private fun updateHeading() {
        if (SensorManager.getRotationMatrix(
                rotationMatrix,
                null,
                accelerometerReading,
                magnetometerReading
            )
        ) {
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            // azimuth is orientationAngles[0] in radians
            val azimuthDegrees = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()

            // Normalize to 0-360
            val normalizedHeading = (azimuthDegrees + 360) % 360

            // Only update if change is significant enough to reduce micro-jiggle
            if (abs(normalizedHeading - lastHeading) > 0.5f) {
                _deviceHeading.value = normalizedHeading
                lastHeading = normalizedHeading
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.i(LOG_TAG, "onAccuracyChanged ${sensor?.name}, accuracy $accuracy")
    }

    companion object {
        private const val LOG_TAG = "CompassSensorManager"

        private const val ALPHA = 0.15f // Smoothing factor
    }
}
