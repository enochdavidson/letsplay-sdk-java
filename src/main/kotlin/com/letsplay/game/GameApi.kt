package com.letsplay.game

import com.letsplay.Client
import reactor.core.publisher.Mono

class GameApi(private val client: Client) {

    fun create(request: GameCreateRequest): Mono<String> {
        return client.getSocket().request("games/create", request)
    }

    fun join(gameId: String, password: String): Mono<Boolean> {
        return client.getSocket().request("games/$gameId/join", password)
    }
}