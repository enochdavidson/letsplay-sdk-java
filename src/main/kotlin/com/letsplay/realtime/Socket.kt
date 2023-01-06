package com.letsplay.realtime

import com.google.gson.Gson
import com.letsplay.auth.Session
import io.netty.buffer.ByteBufAllocator
import io.rsocket.Payload
import io.rsocket.SocketAcceptor
import io.rsocket.core.RSocketClient
import io.rsocket.core.RSocketConnector
import io.rsocket.metadata.AuthMetadataCodec
import io.rsocket.metadata.CompositeMetadataCodec
import io.rsocket.metadata.TaggingMetadataCodec
import io.rsocket.metadata.WellKnownMimeType
import io.rsocket.transport.netty.client.WebsocketClientTransport
import io.rsocket.util.DefaultPayload
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import java.net.URI

class Socket(
    private val realtimeUrl: String,
    private val retryStrategy: Retry
) {
    private lateinit var client: RSocketClient
    private val gson = Gson()
    private val receiveHandler = ReceiveMessageHandler(gson)

    fun connect(session: Session) {
        val ws = WebsocketClientTransport.create(URI.create(realtimeUrl))
        val rSocket = RSocketConnector.create()
            .metadataMimeType(WellKnownMimeType.MESSAGE_RSOCKET_COMPOSITE_METADATA.string)
            .dataMimeType(WellKnownMimeType.APPLICATION_JSON.string)
            .setupPayload(createSetupPayload(session.token))
            .acceptor(SocketAcceptor.forFireAndForget { receiveHandler.handle(it) })
            .reconnect(retryStrategy)
            .connect(ws)
        client = RSocketClient.from(rSocket)
    }

    inline fun <reified T> request(route: String, request: Any): Mono<T> {
        return request(route, request, T::class.java)
    }

    fun <T> request(route: String, request: Any, type: Class<T>): Mono<T> {
        val response = client.requestResponse(Mono.just(createPayload(route, request)))
        return response.map { gson.fromJson(it.dataUtf8, type) }
    }

    fun send(route: String, request: Any): Mono<Void> {
        return client.fireAndForget(Mono.just(createPayload(route, request)))
    }

    inline fun <reified T> receive(route: String): Flux<T> {
        return receive(route, T::class.java)
    }

    fun <T> receive(route: String, type: Class<T>): Flux<T> {
        return receiveHandler.register(route, type)
    }

    fun dispose() {
        if (::client.isInitialized && !client.isDisposed) {
            client.dispose()
            receiveHandler.dispose()
        }
    }

    private fun createPayload(route: String, message: Any): Payload {
        val metadata = ByteBufAllocator.DEFAULT.compositeBuffer()
        val routingMetadata =
            TaggingMetadataCodec.createRoutingMetadata(ByteBufAllocator.DEFAULT, listOf(route))

        CompositeMetadataCodec.encodeAndAddMetadata(
            metadata,
            ByteBufAllocator.DEFAULT,
            WellKnownMimeType.MESSAGE_RSOCKET_ROUTING,
            routingMetadata.content
        )

        val data = ByteBufAllocator.DEFAULT.buffer().writeBytes(gson.toJson(message).toByteArray())
        return DefaultPayload.create(data, metadata)
    }

    private fun createSetupPayload(token: String): Payload {
        val metadata = ByteBufAllocator.DEFAULT.compositeBuffer()
        val authMetadata = AuthMetadataCodec.encodeBearerMetadata(ByteBufAllocator.DEFAULT, token.toCharArray())

        CompositeMetadataCodec.encodeAndAddMetadata(
            metadata,
            ByteBufAllocator.DEFAULT,
            WellKnownMimeType.MESSAGE_RSOCKET_AUTHENTICATION,
            authMetadata
        )

        val data = ByteBufAllocator.DEFAULT.buffer().writeBytes("".toByteArray())
        return DefaultPayload.create(data, metadata)
    }
}