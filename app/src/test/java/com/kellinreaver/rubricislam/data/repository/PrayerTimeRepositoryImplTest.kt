package com.kellinreaver.rubricislam.data.repository

import android.content.Context
import android.location.Location
import android.os.Looper
import app.cash.turbine.test
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.kellinreaver.rubricislam.data.local.dao.PrayerTimeDao
import com.kellinreaver.rubricislam.data.local.entity.PrayerTimeEntity
import com.kellinreaver.rubricislam.data.remote.AladhanApiService
import com.kellinreaver.rubricislam.data.remote.AladhanData
import com.kellinreaver.rubricislam.data.remote.AladhanResponse
import com.kellinreaver.rubricislam.data.remote.PrayerTimings
import com.kellinreaver.rubricislam.domain.model.PrayerTime
import com.kellinreaver.rubricislam.domain.usecase.LocationModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PrayerTimeRepositoryImplTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private val apiService: AladhanApiService = mockk()
    private val fusedLocationClient: FusedLocationProviderClient = mockk(relaxed = true)
    private val prayerTimeDao: PrayerTimeDao = mockk(relaxed = true)

    private val repository =
        PrayerTimeRepositoryImpl(context, apiService, fusedLocationClient, prayerTimeDao)

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun makeTimings(): PrayerTimings = PrayerTimings(
        fajr = "04:30",
        sunrise = "05:55",
        dhuhr = "12:15",
        asr = "15:35",
        maghrib = "18:10",
        isha = "19:35",
        imsak = "04:20",
        sunset = "18:09"
    )

    private fun makeResponse(): AladhanResponse =
        AladhanResponse(code = 200, status = "OK", data = AladhanData(timings = makeTimings()))

    private fun makeEntity(
        date: String = "06-09-2026",
        lastUpdated: Long = System.currentTimeMillis(),
        lat: Double = 21.4225,
        lon: Double = 39.8262
    ): PrayerTimeEntity = PrayerTimeEntity(
        date = date,
        fajr = "04:30",
        sunrise = "05:55",
        dhuhr = "12:15",
        asr = "15:35",
        maghrib = "18:10",
        isha = "19:35",
        latitude = lat,
        longitude = lon,
        lastUpdated = lastUpdated
    )

    /** Wires the DAO to emit [entity] (or null) when asked for any date. */
    private fun stubDaoForDate(entity: PrayerTimeEntity?) {
        every { prayerTimeDao.getPrayerTimesForDate(any()) } returns flowOf(entity)
    }

    private fun expectedDomainFromEntity(entity: PrayerTimeEntity): List<PrayerTime> = listOf(
        PrayerTime("Fajr", entity.fajr),
        PrayerTime("Sunrise", entity.sunrise),
        PrayerTime("Dhuhr", entity.dhuhr),
        PrayerTime("Asr", entity.asr),
        PrayerTime("Maghrib", entity.maghrib),
        PrayerTime("Isha", entity.isha)
    )

    // -------------------------------------------------------------------------
    // getCurrentLocation()
    // -------------------------------------------------------------------------

    @Test
    fun `getCurrentLocation emits last known location immediately`() = runTest {
        val lastLocation = Location("test").apply {
            latitude = 21.4225
            longitude = 39.8262
        }

        // Wire lastLocation to a fake Task that delivers the value to any registered
        // OnSuccessListener synchronously when addOnSuccessListener is called.
        val fakeTask: Task<Location> = mockk(relaxed = true)
        every { fusedLocationClient.lastLocation } returns fakeTask
        every {
            fakeTask.addOnSuccessListener(
                any<com.google.android.gms.tasks.OnSuccessListener<in Location>>()
            )
        } answers {
            val listener = firstArg<com.google.android.gms.tasks.OnSuccessListener<in Location>>()
            listener.onSuccess(lastLocation)
            fakeTask
        }

        every {
            fusedLocationClient.requestLocationUpdates(
                any<LocationRequest>(),
                any<LocationCallback>(),
                any<Looper>()
            )
        } returns Tasks.forResult(null)

        repository.getCurrentLocation().test {
            val item = awaitItem()
            assertEquals(LocationModel(21.4225, 39.8262), item)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getCurrentLocation emits via LocationCallback when last location is null`() = runTest {
        val nullTask: Task<Location> = Tasks.forResult(null)
        every { fusedLocationClient.lastLocation } returns nullTask

        val capturedCallback = slot<LocationCallback>()
        every {
            fusedLocationClient.requestLocationUpdates(
                any<LocationRequest>(),
                capture(capturedCallback),
                any<Looper>()
            )
        } returns Tasks.forResult(null)

        val emitted = mutableListOf<LocationModel>()
        val collectorJob: Job = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.getCurrentLocation().collect { emitted += it }
        }

        // Simulate a live location update from the FusedLocationProviderClient.
        val newLocation = Location("gps").apply {
            latitude = 24.7136
            longitude = 46.6753
        }
        capturedCallback.captured.onLocationResult(LocationResult.create(listOf(newLocation)))

        assertEquals(1, emitted.size)
        assertEquals(24.7136, emitted.first().latitude, 0.0)
        assertEquals(46.6753, emitted.first().longitude, 0.0)

        collectorJob.cancelAndJoin()
    }

    @Test
    fun `getCurrentLocation removes location updates when flow is closed`() = runTest {
        val nullTask: Task<Location> = Tasks.forResult(null)
        every { fusedLocationClient.lastLocation } returns nullTask

        val capturedCallback = slot<LocationCallback>()
        every {
            fusedLocationClient.requestLocationUpdates(
                any<LocationRequest>(),
                capture(capturedCallback),
                any<Looper>()
            )
        } returns Tasks.forResult(null)

        val collectorJob: Job = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.getCurrentLocation().collect { /* swallow emissions */ }
        }
        yield()
        collectorJob.cancelAndJoin()

        verify { fusedLocationClient.removeLocationUpdates(capturedCallback.captured) }
    }

    @Test
    fun `getCurrentLocation requests updates with HIGH_ACCURACY and configured intervals`() =
        runTest {
            val nullTask: Task<Location> = Tasks.forResult(null)
            every { fusedLocationClient.lastLocation } returns nullTask

            val capturedRequest = slot<LocationRequest>()
            every {
                fusedLocationClient.requestLocationUpdates(
                    capture(capturedRequest),
                    any<LocationCallback>(),
                    any<Looper>()
                )
            } returns Tasks.forResult(null)

            val collectorJob: Job = launch(UnconfinedTestDispatcher(testScheduler)) {
                repository.getCurrentLocation().collect { /* no-op */ }
            }
            yield()
            collectorJob.cancelAndJoin()

            val req = capturedRequest.captured
            assertEquals(10000L, req.intervalMillis)
            assertEquals(5000L, req.minUpdateIntervalMillis)
            assertEquals(10f, req.minUpdateDistanceMeters)
            assertEquals(
                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                req.priority
            )
        }

    @Test
    fun `getCurrentLocation callback ignores empty LocationResult`() = runTest {
        val nullTask: Task<Location> = Tasks.forResult(null)
        every { fusedLocationClient.lastLocation } returns nullTask

        val capturedCallback = slot<LocationCallback>()
        every {
            fusedLocationClient.requestLocationUpdates(
                any<LocationRequest>(),
                capture(capturedCallback),
                any<Looper>()
            )
        } returns Tasks.forResult(null)

        val emitted = mutableListOf<LocationModel>()
        val collectorJob: Job = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.getCurrentLocation().collect { emitted += it }
        }

        // Empty result: lastLocation is null so nothing should be emitted.
        capturedCallback.captured.onLocationResult(LocationResult.create(emptyList()))

        assertEquals(0, emitted.size)
        collectorJob.cancelAndJoin()
    }

    @Test
    fun `getCurrentLocation emits multiple updates from LocationCallback`() = runTest {
        val nullTask: Task<Location> = Tasks.forResult(null)
        every { fusedLocationClient.lastLocation } returns nullTask

        val capturedCallback = slot<LocationCallback>()
        every {
            fusedLocationClient.requestLocationUpdates(
                any<LocationRequest>(),
                capture(capturedCallback),
                any<Looper>()
            )
        } returns Tasks.forResult(null)

        val emitted = mutableListOf<LocationModel>()
        val collectorJob: Job = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.getCurrentLocation().collect { emitted += it }
        }

        val first = Location("gps").apply {
            latitude = 1.0
            longitude = 2.0
        }
        val second = Location("gps").apply {
            latitude = 3.0
            longitude = 4.0
        }
        capturedCallback.captured.onLocationResult(LocationResult.create(listOf(first)))
        capturedCallback.captured.onLocationResult(LocationResult.create(listOf(second)))

        assertEquals(2, emitted.size)
        assertEquals(LocationModel(1.0, 2.0), emitted[0])
        assertEquals(LocationModel(3.0, 4.0), emitted[1])

        collectorJob.cancelAndJoin()
    }

    // -------------------------------------------------------------------------
    // getPrayerTimes() - cache hit
    // -------------------------------------------------------------------------

    @Test
    fun `getPrayerTimes returns cached data when cache is fresh`() = runTest {
        val entity = makeEntity()
        stubDaoForDate(entity)

        repository.getPrayerTimes(21.4225, 39.8262).test {
            val result = awaitItem()
            assertEquals(expectedDomainFromEntity(entity), result)
            awaitComplete()
        }
    }

    @Test
    fun `getPrayerTimes does not call API when cache is fresh`() = runTest {
        val entity = makeEntity()
        stubDaoForDate(entity)

        repository.getPrayerTimes(21.4225, 39.8262).test {
            awaitItem()
            awaitComplete()
        }

        coVerify(exactly = 0) {
            apiService.getPrayerTimesByCoords(lat = any(), lon = any(), method = any())
        }
        coVerify(exactly = 0) { prayerTimeDao.insertPrayerTimes(any()) }
    }

    // -------------------------------------------------------------------------
    // getPrayerTimes() - cache miss / stale
    // -------------------------------------------------------------------------

    @Test
    fun `getPrayerTimes fetches from API when cache is missing`() = runTest {
        stubDaoForDate(null)
        val response = makeResponse()
        coEvery {
            apiService.getPrayerTimesByCoords(lat = any(), lon = any(), method = any())
        } returns response

        repository.getPrayerTimes(21.4225, 39.8262).test {
            val result = awaitItem()
            assertEquals(expectedDomainFromEntity(makeEntity()), result)
            awaitComplete()
        }

        coVerify(exactly = 1) {
            apiService.getPrayerTimesByCoords(lat = 21.4225, lon = 39.8262, method = 4)
        }
        coVerify(exactly = 1) { prayerTimeDao.insertPrayerTimes(any()) }
    }

    @Test
    fun `getPrayerTimes refreshes from API when cache is stale`() = runTest {
        val staleEntity = makeEntity(
            lastUpdated = System.currentTimeMillis() - (25 * 60 * 60 * 1000L)
        )
        stubDaoForDate(staleEntity)

        val response = makeResponse()
        coEvery {
            apiService.getPrayerTimesByCoords(lat = any(), lon = any(), method = any())
        } returns response

        repository.getPrayerTimes(21.4225, 39.8262).test {
            val result = awaitItem()
            assertEquals(expectedDomainFromEntity(makeEntity()), result)
            awaitComplete()
        }

        coVerify(exactly = 1) {
            apiService.getPrayerTimesByCoords(lat = any(), lon = any(), method = any())
        }
        coVerify(exactly = 1) { prayerTimeDao.insertPrayerTimes(any()) }
    }

    @Test
    fun `getPrayerTimes persists API result to cache with queried coordinates`() = runTest {
        stubDaoForDate(null)
        val response = makeResponse()
        coEvery {
            apiService.getPrayerTimesByCoords(lat = any(), lon = any(), method = any())
        } returns response

        val lat = 10.0
        val lon = 20.0
        repository.getPrayerTimes(lat, lon).test {
            awaitItem()
            awaitComplete()
        }

        val persisted = slot<PrayerTimeEntity>()
        coVerify { prayerTimeDao.insertPrayerTimes(capture(persisted)) }
        val entity = persisted.captured
        assertEquals(lat, entity.latitude, 0.0)
        assertEquals(lon, entity.longitude, 0.0)
        assertEquals("04:30", entity.fajr)
        assertEquals("05:55", entity.sunrise)
        assertEquals("12:15", entity.dhuhr)
        assertEquals("15:35", entity.asr)
        assertEquals("18:10", entity.maghrib)
        assertEquals("19:35", entity.isha)
        assertTrue(entity.lastUpdated > 0L)
    }

    @Test
    fun `getPrayerTimes emits ordered prayer list after API success`() = runTest {
        stubDaoForDate(null)
        val response = makeResponse()
        coEvery {
            apiService.getPrayerTimesByCoords(lat = any(), lon = any(), method = any())
        } returns response

        repository.getPrayerTimes(0.0, 0.0).test {
            val item = awaitItem()
            assertEquals(6, item.size)
            assertEquals(PrayerTime("Fajr", "04:30"), item[0])
            assertEquals(PrayerTime("Sunrise", "05:55"), item[1])
            assertEquals(PrayerTime("Dhuhr", "12:15"), item[2])
            assertEquals(PrayerTime("Asr", "15:35"), item[3])
            assertEquals(PrayerTime("Maghrib", "18:10"), item[4])
            assertEquals(PrayerTime("Isha", "19:35"), item[5])
            awaitComplete()
        }
    }

    // -------------------------------------------------------------------------
    // getPrayerTimes() - error handling / stale fallback
    // -------------------------------------------------------------------------

    @Test
    fun `getPrayerTimes falls back to stale cache when API throws`() = runTest {
        val staleEntity = makeEntity(
            lastUpdated = System.currentTimeMillis() - (48 * 60 * 60 * 1000L)
        )
        stubDaoForDate(staleEntity)

        coEvery {
            apiService.getPrayerTimesByCoords(lat = any(), lon = any(), method = any())
        } throws RuntimeException("network down")

        repository.getPrayerTimes(21.4225, 39.8262).test {
            val result = awaitItem()
            assertEquals(expectedDomainFromEntity(staleEntity), result)
            awaitComplete()
        }

        // Failed attempt should not be persisted to the cache.
        coVerify(exactly = 0) { prayerTimeDao.insertPrayerTimes(any()) }
    }

    @Test
    fun `getPrayerTimes emits nothing when API fails and no cache exists`() = runTest {
        stubDaoForDate(null)
        coEvery {
            apiService.getPrayerTimesByCoords(lat = any(), lon = any(), method = any())
        } throws RuntimeException("network down")

        var emitted = 0
        var collected: List<PrayerTime>? = null
        repository.getPrayerTimes(0.0, 0.0).collect {
            emitted++
            collected = it
        }

        assertEquals(0, emitted)
        assertNull(collected)
    }

    @Test
    fun `getPrayerTimes does not overwrite fresh cache with API result`() = runTest {
        val freshEntity = makeEntity(lastUpdated = System.currentTimeMillis())
        stubDaoForDate(freshEntity)

        repository.getPrayerTimes(0.0, 0.0).test {
            val item = awaitItem()
            assertEquals(expectedDomainFromEntity(freshEntity), item)
            awaitComplete()
        }

        coVerify(exactly = 0) {
            apiService.getPrayerTimesByCoords(lat = any(), lon = any(), method = any())
        }
        coVerify(exactly = 0) { prayerTimeDao.insertPrayerTimes(any()) }
    }

    // -------------------------------------------------------------------------
    // getPrayerTimes() - stale-threshold edge cases
    // -------------------------------------------------------------------------

    @Test
    fun `getPrayerTimes treats cache exactly 24h plus 1ms old as stale`() = runTest {
        val justOverStale = makeEntity(
            lastUpdated = System.currentTimeMillis() - (24L * 60L * 60L * 1000L) - 1L
        )
        stubDaoForDate(justOverStale)
        coEvery {
            apiService.getPrayerTimesByCoords(lat = any(), lon = any(), method = any())
        } returns makeResponse()

        repository.getPrayerTimes(0.0, 0.0).test {
            awaitItem()
            awaitComplete()
        }

        coVerify(exactly = 1) {
            apiService.getPrayerTimesByCoords(lat = any(), lon = any(), method = any())
        }
    }

    @Test
    fun `getPrayerTimes treats cache under 24h old as fresh`() = runTest {
        val justUnderStale = makeEntity(
            lastUpdated =
            System.currentTimeMillis() - ((23L * 60L * 60L * 1000L) + (59L * 60L * 1000L))
        )
        stubDaoForDate(justUnderStale)

        repository.getPrayerTimes(0.0, 0.0).test {
            val item = awaitItem()
            assertEquals(expectedDomainFromEntity(justUnderStale), item)
            awaitComplete()
        }

        coVerify(exactly = 0) {
            apiService.getPrayerTimesByCoords(lat = any(), lon = any(), method = any())
        }
    }

    // -------------------------------------------------------------------------
    // getPrayerTimes() - date format
    // -------------------------------------------------------------------------

    @Test
    fun `getPrayerTimes queries DAO with date in dd-MM-yyyy format`() = runTest {
        stubDaoForDate(null)
        coEvery {
            apiService.getPrayerTimesByCoords(lat = any(), lon = any(), method = any())
        } returns makeResponse()

        repository.getPrayerTimes(0.0, 0.0).test {
            awaitItem()
            awaitComplete()
        }

        val queriedDate = slot<String>()
        verify { prayerTimeDao.getPrayerTimesForDate(capture(queriedDate)) }
        val date = queriedDate.captured
        assertTrue(
            "Expected dd-MM-yyyy but got '$date'",
            date.matches(Regex("""\d{2}-\d{2}-\d{4}"""))
        )
    }

    // -------------------------------------------------------------------------
    // getPrayerTimes() - mapping verification
    // -------------------------------------------------------------------------

    @Test
    fun `mapped domain list preserves order and disables isNext flag`() = runTest {
        val entity = makeEntity()
        stubDaoForDate(entity)

        repository.getPrayerTimes(0.0, 0.0).test {
            val item = awaitItem()
            val names = item.map { it.name }
            assertEquals(
                listOf("Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha"),
                names
            )
            item.forEach { prayer ->
                assertEquals(false, prayer.isNext)
                assertNotNull(prayer.time)
            }
            awaitComplete()
        }
    }
}
