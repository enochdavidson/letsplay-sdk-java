package com.letsplay.auth

abstract class LoginRequest(
    open val identity: String,
    open val credential: String?
)
