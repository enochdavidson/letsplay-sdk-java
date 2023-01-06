package com.letsplay.realtime

import com.google.gson.Gson
import io.rsocket.Payload
import io.rsocket.metadata.CompositeMetadata
import io.rsocket.metadata.RoutingMetadata
import io.rsocket.metadata.WellKnownMimeType
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import java.util.concurrent.ConcurrentHashMap

class ReceiveMessageHandler(private val gson: Gson): Disposable {

    private val handlers = ConcurrentHashMap<String, Sinks.Many<*>>()
    private val types = ConcurrentHashMap<String, Class<*>>()

    fun <T> register(event: String, type: Class<T>): Flux<T> {
        val sink = Sinks.many().multicast().directBestEffort<T>()
        handlers[event] = sink
        types[event] = type
        return sink.asFlux()
    }

    fun handle(payload: Payload): Mono<Void> {
        val metadata = CompositeMetadata(payload.sliceMetadata(), true)
        metadata.forEach {
            if (it.mimeType == WellKnownMimeType.MESSAGE_RSOCKET_ROUTING.string) {
                val event = RoutingMetadata(it.content).iterator().next()!!
                val type = types[event]
                if (type != null) {
                    val message = gson.fromJson(payload.dataUtf8, type)
                    val sink = handlers[event] as Sinks.Many<Any>
                    sink.tryEmitNext(message)
                }
            }
        }
        return Mono.empty()
    }

    override fun dispose() {
        handlers.values.forEach {
            it.tryEmitComplete()
        }
    }
}