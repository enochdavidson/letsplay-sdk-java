package com.letsplay.example

import com.letsplay.Client
import com.letsplay.auth.Logins
import reactor.util.retry.Retry
import java.time.Duration

fun main() {
    val client = Client(
        "http://localhost:8080/api/",
        "ws://localhost:9898/rsocket",
        Retry.backoff(3, Duration.ofMillis(1000))
    )
    val socket = client.connect(Logins.basic("ann", "pass"))

    client.userApi.me()
        .doOnNext { println(it) }
        .subscribe()

    socket.send("route.send", Message("client", "route.send"))
        .doOnNext { println(it) }
        .doOnError { println(it) }
        .doFinally { println("Finally Sent") }
        .subscribe()

    socket.request<Message>("route.request", Message("client", "route.request"))
        .doOnNext { println(it) }
        .doOnError { println(it) }
        .doFinally { println("Finally Request") }
        .subscribe()

    socket.receive<Message>("route.receive")
        .doOnNext { println(it) }
        .doOnError { println(it) }
        .doFinally { println("Finally Received") }
        .subscribe()

    Thread.sleep(60000)
}