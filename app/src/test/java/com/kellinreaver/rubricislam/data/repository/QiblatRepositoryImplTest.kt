package com.kellinreaver.rubricislam.data.repository

import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.tasks.Tasks
import com.kellinreaver.rubricislam.domain.model.Qiblat
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class QiblatRepositoryImplTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private val fusedLocationClient: FusedLocationProviderClient = mockk(relaxed = true)
    private val repository = QiblatRepositoryImpl(context, fusedLocationClient)

    @Test
    fun `getQiblatDirection emits initial bearing for given coordinates`() = runTest {
        val results = mutableListOf<Qiblat>()
        val job = launch(UnconfinedTestDispatcher()) {
            repository.getQiblatDirection(30.0, 39.8262).toList(results)
        }

        assertEquals(1, results.size)
        assertEquals(180.0f, results.first().bearing, 0.1f)

        job.cancel()
    }

    @Test
    fun `getQiblatDirection emits updated bearing on location update`() = runTest {
        val callbackSlot = slot<LocationCallback>()
        every {
            fusedLocationClient.requestLocationUpdates(
                any(),
                capture(callbackSlot),
                any<Looper>()
            )
        } returns Tasks.forResult(null)

        val results = mutableListOf<Qiblat>()
        val job = launch(UnconfinedTestDispatcher()) {
            repository.getQiblatDirection(30.0, 39.8262).toList(results)
        }

        assertEquals(1, results.size)

        val updatedLocation = Location("").apply {
            latitude = 21.4225
            longitude = 50.0
        }
        val locationResult = LocationResult.create(listOf(updatedLocation))
        callbackSlot.captured.onLocationResult(locationResult)

        assertEquals(2, results.size)
        assertTrue(results.last().bearing in 270.0f..280.0f)

        job.cancel()
    }

    @Test
    fun `getQiblatDirection removes location updates on close`() = runTest {
        val callbackSlot = slot<LocationCallback>()
        every {
            fusedLocationClient.requestLocationUpdates(
                any(),
                capture(callbackSlot),
                any<Looper>()
            )
        } returns Tasks.forResult(null)

        val job = launch(UnconfinedTestDispatcher()) {
            repository.getQiblatDirection(0.0, 0.0).toList()
        }

        job.cancel()

        verify { fusedLocationClient.removeLocationUpdates(callbackSlot.captured) }
    }
}
