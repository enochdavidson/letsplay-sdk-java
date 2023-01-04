package com.letsplay.auth

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthService {

    @POST("login/basic")
    fun basicLogin(@Body request: BasicLoginRequest): Call<Session>
}