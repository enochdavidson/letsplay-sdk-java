package com.letsplay.example

import com.letsplay.Client
import com.letsplay.auth.Logins
import com.letsplay.game.GameCreateRequest
import reactor.util.retry.Retry
import java.time.Duration

fun main() {
    val client = Client(
        "http://localhost:8080/api/",
        "ws://localhost:9898/rsocket",
        Retry.backoff(3, Duration.ofMillis(1000))
    )
    val socket = client.connect(Logins.basic("ann", "pass"))

    client.users().me()
        .doOnNext { println(it) }
        .subscribe()

    client.games().create(GameCreateRequest("TicTacToe", emptyMap()))
        .doOnNext {
            println(it)
            client.games(it).sendEvent("move", Message("Client", "My Move")).subscribe()
        }
        .doOnError { println(it) }
        .doFinally { println("Game Created") }
        .subscribe()

    Thread.sleep(60000)
}