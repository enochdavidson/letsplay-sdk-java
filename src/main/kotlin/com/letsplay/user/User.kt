package com.letsplay.user

data class User(
    var id: String,
    var name: String?,
    var avatarUrl: String? = null,
    var data: Map<String, String> = mutableMapOf()
)
