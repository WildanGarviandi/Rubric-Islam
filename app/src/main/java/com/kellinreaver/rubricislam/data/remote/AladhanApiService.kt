package com.kellinreaver.rubricislam.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface AladhanApiService {
    @GET("v1/timingsByAddress")
    suspend fun getPrayerTimesByAddress(
        @Query("address") address: String,
        @Query("method") method: Int = 4
    ): AladhanResponse

    @GET("v1/timings")
    suspend fun getPrayerTimesByCoords(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("method") method: Int = 4
    ): AladhanResponse
}
