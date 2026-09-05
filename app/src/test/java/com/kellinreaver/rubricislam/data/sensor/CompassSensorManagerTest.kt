package com.kellinreaver.rubricislam.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CompassSensorManagerTest {

    private val context: Context = RuntimeEnvironment.getApplication()

    @Test
    fun `startListening registers accelerometer and magnetometer listeners`() {
        val sensorManager: SensorManager = mockk(relaxed = true)
        val accelerometer: Sensor = mockk(relaxed = true)
        val magnetometer: Sensor = mockk(relaxed = true)
        every { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) } returns accelerometer
        every { sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) } returns magnetometer

        val compassManager = CompassSensorManager(context)
        // Replace the internal sensorManager via reflection to use our mock
        val field = CompassSensorManager::class.java.getDeclaredField("sensorManager")
        field.isAccessible = true
        field.set(compassManager, sensorManager)

        compassManager.startListening()

        verify {
            sensorManager.registerListener(
                compassManager,
                accelerometer,
                SensorManager.SENSOR_DELAY_UI
            )
        }
        verify {
            sensorManager.registerListener(
                compassManager,
                magnetometer,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    @Test
    fun `stopListening unregisters all listeners`() {
        val sensorManager: SensorManager = mockk(relaxed = true)

        val compassManager = CompassSensorManager(context)
        val field = CompassSensorManager::class.java.getDeclaredField("sensorManager")
        field.isAccessible = true
        field.set(compassManager, sensorManager)

        compassManager.stopListening()

        verify {
            sensorManager.unregisterListener(compassManager)
        }
    }

    @Test
    fun `onSensorChanged updates device heading`() = runTest {
        val compassManager = CompassSensorManager(context)

        // Simulate accelerometer pointing flat and magnetometer pointing north
        sendSensorEvent(compassManager, Sensor.TYPE_ACCELEROMETER, floatArrayOf(0f, 0f, 9.81f))
        sendSensorEvent(compassManager, Sensor.TYPE_MAGNETIC_FIELD, floatArrayOf(0f, 25f, -41f))

        val heading = compassManager.deviceHeading.first()
        assertTrue(heading in 0.0f..360.0f)
    }

    private fun sendSensorEvent(listener: CompassSensorManager, type: Int, values: FloatArray) {
        val constructor = SensorEvent::class.java.getDeclaredConstructor(Int::class.java)
        constructor.isAccessible = true
        val sensorEvent = constructor.newInstance(values.size)
        System.arraycopy(values, 0, sensorEvent.values, 0, values.size)
        sensorEvent.sensor = mockk {
            every { this@mockk.type } returns type
        }
        listener.onSensorChanged(sensorEvent)
    }
}
