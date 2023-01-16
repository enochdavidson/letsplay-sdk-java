package com.letsplay.game

import com.letsplay.Client
import reactor.core.publisher.Mono

class GameInstanceApi(private val gameId: String, private val client: Client) {

    fun sendEvent(eventId: String, data: Any): Mono<Void> {
        return client.getSocket().send("games/$gameId/$eventId", data)
    }
}