package com.kellinreaver.rubricislam.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.kellinreaver.rubricislam.data.local.dao.PrayerTimeDao
import com.kellinreaver.rubricislam.data.local.entity.PrayerTimeEntity
import com.kellinreaver.rubricislam.data.remote.AladhanApiService
import com.kellinreaver.rubricislam.domain.model.PrayerTime
import com.kellinreaver.rubricislam.domain.repository.PrayerTimeRepository
import com.kellinreaver.rubricislam.domain.usecase.LocationModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow

class PrayerTimeRepositoryImpl
@Inject
constructor(
    private val context: Context,
    private val apiService: AladhanApiService,
    private val fusedLocationClient: FusedLocationProviderClient,
    private val prayerTimeDao: PrayerTimeDao
) : PrayerTimeRepository {
    @SuppressLint("MissingPermission")
    override fun getCurrentLocation(): Flow<LocationModel> = callbackFlow {
        // Get last known location for immediate response
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                trySend(LocationModel(it.latitude, it.longitude))
            }
        }

        val locationRequest =
            LocationRequest
                .Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000L)
                .setMinUpdateIntervalMillis(5000L)
                .setMinUpdateDistanceMeters(10f)
                .build()

        val locationCallback =
            object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { location ->
                        trySend(LocationModel(location.latitude, location.longitude))
                    }
                }
            }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            context.mainLooper
        )

        awaitClose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    override fun getPrayerTimes(lat: Double, lon: Double): Flow<List<PrayerTime>> = flow {
        val currentDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())

        // Check cache first
        val cachedData = prayerTimeDao.getPrayerTimesForDate(currentDate).firstOrNull()

        if (cachedData != null && !isCacheStale(cachedData)) {
            Log.i(TAG, "Cache hit for date: $currentDate. Returning cached prayer times.")
            emit(mapEntityToDomain(cachedData))
        } else {
            if (cachedData == null) {
                Log.i(TAG, "No cache found for date: $currentDate. Fetching from API.")
            } else {
                Log.w(
                    TAG,
                    "Cache is stale for date: $currentDate. Last updated: " +
                        "${Date(cachedData.lastUpdated)}. Refreshing from API."
                )
            }

            try {
                // Fetch from API
                val response =
                    apiService.getPrayerTimesByCoords(
                        lat = lat,
                        lon = lon,
                        method = 4
                    )
                val timings = response.data.timings
                Log.i(
                    TAG,
                    "Successfully fetched prayer times from Aladhan API for coords: ($lat, $lon)"
                )

                // Save to cache
                val entity =
                    PrayerTimeEntity(
                        date = currentDate,
                        fajr = timings.fajr,
                        sunrise = timings.sunrise,
                        dhuhr = timings.dhuhr,
                        asr = timings.asr,
                        maghrib = timings.maghrib,
                        isha = timings.isha,
                        latitude = lat,
                        longitude = lon
                    )
                prayerTimeDao.insertPrayerTimes(entity)
                Log.d(TAG, "Saved new prayer times to local database for date: $currentDate")

                emit(mapEntityToDomain(entity))
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching prayer times from API: ${e.message}", e)
                // If network fails, emit cache even if stale as fallback
                cachedData?.let {
                    Log.w(TAG, "Emitting stale cache as fallback for date: $currentDate")
                    emit(mapEntityToDomain(it))
                } ?: Log.e(TAG, "No cached data available to fallback on.")
            }
        }
    }

    private fun isCacheStale(entity: PrayerTimeEntity): Boolean {
        val cacheDuration = 24 * 60 * 60 * 1000L // 24 hours
        val isStale = System.currentTimeMillis() - entity.lastUpdated > cacheDuration
        return isStale
    }

    companion object {
        private const val TAG = "PrayerTimeRepo"
    }

    private fun mapEntityToDomain(entity: PrayerTimeEntity): List<PrayerTime> = listOf(
        PrayerTime("Fajr", entity.fajr),
        PrayerTime("Sunrise", entity.sunrise),
        PrayerTime("Dhuhr", entity.dhuhr),
        PrayerTime("Asr", entity.asr),
        PrayerTime("Maghrib", entity.maghrib),
        PrayerTime("Isha", entity.isha)
    )
}
