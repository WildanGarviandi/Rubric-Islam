package com.kellinreaver.rubricislam.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import com.kellinreaver.rubricislam.domain.model.Qiblat
import com.kellinreaver.rubricislam.domain.repository.QiblatRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import kotlin.math.*

class QiblatRepositoryImpl @Inject constructor(
    private val context: Context
) : QiblatRepository {

    private val meccaLat = 21.4225
    private val meccaLon = 39.8262

    @SuppressLint("MissingPermission")
    override fun getQiblatDirection(lat: Double, lon: Double): Flow<Qiblat> = callbackFlow {
        // For simplicity, we'll just calculate it immediately based on the provided lat/lon.
        // In a real app, we would listen to location updates.
        val direction = calculateBearing(lat, lon, meccaLat, meccaLon)
        trySend(Qiblat(direction))
        close()
    }

    private fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaLambda = Math.toRadians(lon2 - lon1)

        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        val bearing = Math.toDegrees(atan2(y, x))

        return ((bearing + 360) % 360).toFloat()
    }
}
