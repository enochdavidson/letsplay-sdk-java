package com.letsplay.auth

object Logins {
    fun basic(username: String, password: String): LoginRequest {
        return LoginRequest("basic", username, password)
    }
}