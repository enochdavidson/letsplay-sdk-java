package com.letsplay.realtime

import com.letsplay.Client
import com.letsplay.auth.BasicLoginRequest
import reactor.util.retry.Retry
import java.time.Duration

fun main() {
    val client = Client(
        "http://localhost:8080/api/",
        "ws://localhost:9898/rsocket",
        Retry.backoff(1, Duration.ofMillis(1000))
    )
    val socket = client.connect(BasicLoginRequest("john", "pass"))

    val response1 = socket.request("hello", "Client SDK 1").block()
    println(response1)

    Thread.sleep(20000)

    val response2 = socket.request("hello", "Client SDK 2").block()
    println(response2)
}