package com.app.translation.navigation

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface DirectionService {
    @GET("json")
    suspend fun getDirection(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("mode") mode: String,
        @Query("alternatives") alternatives: String,
        @Query("language") language: String,
        @Query("key") key: String
    ): Response<Direction>
}