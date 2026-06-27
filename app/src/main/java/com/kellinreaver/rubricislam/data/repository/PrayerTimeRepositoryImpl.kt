package com.kellinreaver.rubricislam.data.repository

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.kellinreaver.rubricislam.data.remote.AladhanApiService
import com.kellinreaver.rubricislam.domain.model.PrayerTime
import com.kellinreaver.rubricislam.domain.repository.PrayerTimeRepository
import com.kellinreaver.rubricislam.domain.usecase.LocationModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class PrayerTimeRepositoryImpl @Inject constructor(
    private val context: Context,
    private val apiService: AladhanApiService,
    private val fusedLocationClient: FusedLocationProviderClient
) : PrayerTimeRepository {

    @SuppressLint("MissingPermission")
    override fun getCurrentLocation(): Flow<LocationModel> = callbackFlow {
        // Get last known location for immediate response
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                trySend(LocationModel(it.latitude, it.longitude))
            }
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000L)
            .setMinUpdateIntervalMillis(5000L)
            .setMinUpdateDistanceMeters(10f)
            .build()

        val locationCallback = object : LocationCallback() {
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
        val response = apiService.getPrayerTimesByCoords(
            lat = lat,
            lon = lon,
            method = 4
        )
        val timings = response.data.timings
        val prayerTimes = listOf(
            PrayerTime("Fajr", timings.fajr),
            PrayerTime("Sunrise", timings.sunrise),
            PrayerTime("Dhuhr", timings.dhuhr),
            PrayerTime("Asr", timings.asr),
            PrayerTime("Maghrib", timings.maghrib),
            PrayerTime("Isha", timings.isha)
        )
        emit(prayerTimes)
    }
}
