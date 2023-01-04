package com.letsplay.realtime

import com.letsplay.auth.Session
import io.netty.buffer.ByteBufAllocator
import io.rsocket.Payload
import io.rsocket.core.RSocketClient
import io.rsocket.core.RSocketConnector
import io.rsocket.metadata.AuthMetadataCodec
import io.rsocket.metadata.CompositeMetadataCodec
import io.rsocket.metadata.TaggingMetadataCodec
import io.rsocket.metadata.WellKnownMimeType
import io.rsocket.transport.netty.client.WebsocketClientTransport
import io.rsocket.util.DefaultPayload
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import java.net.URI

class Socket(
    private val realtimeUrl: String,
    private val retryStrategy: Retry
) {
    private lateinit var client: RSocketClient

    fun connect(session: Session) {
        val ws = WebsocketClientTransport.create(URI.create(realtimeUrl))
        val rSocket = RSocketConnector.create()
            .metadataMimeType(WellKnownMimeType.MESSAGE_RSOCKET_COMPOSITE_METADATA.string)
            .dataMimeType(WellKnownMimeType.APPLICATION_JSON.string)
            .setupPayload(createSetupPayload(session.token))
            .reconnect(retryStrategy)
            .connect(ws)
        client = RSocketClient.from(rSocket)
    }

    fun request(route: String, request: Any): Mono<String> {
        val response = client.requestResponse(Mono.just(createPayload(route, request as String)))
        return response.map { it.dataUtf8 }
    }

    private fun createPayload(route: String, message: String): Payload {
        val metadata = ByteBufAllocator.DEFAULT.compositeBuffer()
        val routingMetadata =
            TaggingMetadataCodec.createRoutingMetadata(ByteBufAllocator.DEFAULT, listOf(route))

        CompositeMetadataCodec.encodeAndAddMetadata(
            metadata,
            ByteBufAllocator.DEFAULT,
            WellKnownMimeType.MESSAGE_RSOCKET_ROUTING,
            routingMetadata.content
        )

        val data = ByteBufAllocator.DEFAULT.buffer().writeBytes(message.toByteArray())
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