package com.letsplay.auth

data class BasicLoginRequest(
    val username: String,
    val password: String
) : LoginRequest(username, password)
