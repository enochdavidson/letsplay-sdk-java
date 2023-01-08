package com.letsplay.user

import com.letsplay.Client
import reactor.core.publisher.Mono

class UserApi(private val client: Client) {
    fun me(): Mono<User> {
        return client.getSocket().request("users/me", "")
    }
}