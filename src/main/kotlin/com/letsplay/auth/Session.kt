package com.letsplay.auth

data class Session(
    val sessionId: String,
    val userId: String,
    val token: String,
)
