package com.letsplay

import com.letsplay.auth.AuthService
import com.letsplay.auth.LoginRequest
import com.letsplay.auth.Session
import com.letsplay.exception.ConnectionException
import com.letsplay.realtime.Socket
import com.letsplay.user.UserApi
import reactor.util.retry.Retry
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create


class Client(
    private val apiUrl: String,
    private val realtimeUrl: String,
    private val retryStrategy: Retry
) {
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(apiUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val authService: AuthService = retrofit.create()

    private lateinit var session: Session
    private lateinit var socket: Socket

    val userApi = UserApi(this)

    fun connect(credential: LoginRequest): Socket {
        login(credential)
        if (::socket.isInitialized) {
            socket.dispose()
        }
        socket = Socket(realtimeUrl, retryStrategy)
        socket.connect(session)
        return socket
    }

    private fun login(request: LoginRequest): Session {
        val call = authService.login(request)
        val response = call.execute()

        if (response.isSuccessful) {
            session = response.body()!!
            return session
        } else {
            throw ConnectionException(response.code(), response.toString())
        }
    }

    fun getSession(): Session {
        return session
    }

    fun getSocket(): Socket {
        return socket
    }
}