package com.letsplay.auth

data class LoginRequest(
    val provider: String,
    val identity: String?,
    val credential: String
)
