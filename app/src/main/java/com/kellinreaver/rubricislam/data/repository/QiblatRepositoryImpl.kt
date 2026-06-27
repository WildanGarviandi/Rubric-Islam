package com.kellinreaver.rubricislam.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.kellinreaver.rubricislam.domain.model.Qiblat
import com.kellinreaver.rubricislam.domain.repository.QiblatRepository
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class QiblatRepositoryImpl
@Inject
constructor(
    private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient
) : QiblatRepository {
    @SuppressLint("MissingPermission")
    override fun getQiblatDirection(lat: Double, lon: Double): Flow<Qiblat> = callbackFlow {
        Log.i(TAG, "Starting Qiblat direction flow. Initial coords: ($lat, $lon)")

        // Emit initial value based on passed parameters
        trySend(Qiblat(calculateBearing(lat, lon)))

        val locationRequest =
            LocationRequest
                .Builder(Priority.PRIORITY_HIGH_ACCURACY, INTERVAL_LOCATION_UPDATE)
                .setMinUpdateIntervalMillis(MIN_INTERVAL_LOCATION_UPDATE)
                .build()

        val locationCallback =
            object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { location ->
                        val direction =
                            calculateBearing(
                                location.latitude,
                                location.longitude
                            )
                        Log.d(
                            TAG,
                            "Location update: (${location.latitude}, ${location.longitude}). New Bearing: $direction"
                        )
                        trySend(Qiblat(direction))
                    } ?: Log.w(TAG, "Location result received but lastLocation is null")
                }
            }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            context.mainLooper
        ).addOnFailureListener { e ->
            Log.e(TAG, "Failed to request location updates: ${e.message}", e)
            close(e)
        }

        awaitClose {
            Log.i(TAG, "Closing Qiblat direction flow. Removing location updates.")
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    private fun calculateBearing(lat1: Double, lon1: Double): Float {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(KAABA_LATITUDE)
        val deltaLambda = Math.toRadians(KAABA_LONGITUDE - lon1)

        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        val bearing = Math.toDegrees(atan2(y, x))

        return ((bearing + 360) % 360).toFloat()
    }

    companion object {
        private const val TAG = "QiblatRepo"
        private const val KAABA_LATITUDE = 21.4225
        private const val KAABA_LONGITUDE = 39.8262

        private const val INTERVAL_LOCATION_UPDATE = 5000L
        private const val MIN_INTERVAL_LOCATION_UPDATE = 2000L
    }
}
