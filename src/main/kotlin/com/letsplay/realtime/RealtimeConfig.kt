package com.letsplay.realtime

import io.netty.buffer.ByteBufAllocator
import io.rsocket.Payload
import io.rsocket.core.RSocketConnector
import io.rsocket.metadata.AuthMetadataCodec
import io.rsocket.metadata.CompositeMetadataCodec
import io.rsocket.metadata.TaggingMetadataCodec
import io.rsocket.metadata.WellKnownMimeType
import io.rsocket.transport.netty.client.WebsocketClientTransport
import io.rsocket.util.DefaultPayload
import java.net.URI

class RealtimeConfig {

    fun test(): Unit {

        val ws = WebsocketClientTransport.create(URI.create("ws://localhost:9898/rsocket"))



        val clientRSocket = RSocketConnector.create()
            .metadataMimeType(WellKnownMimeType.MESSAGE_RSOCKET_COMPOSITE_METADATA.string)
            .dataMimeType(WellKnownMimeType.APPLICATION_JSON.string)
            .setupPayload(getSetupPayload(
                "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJqb2huIiwiaWF0IjoxNjcyODM3NTYxLCJleHAiOjE2NzI4MzgxNjF9.9YyU0vpcq3YiDHHWObfBvMhOld7iDbJX1exQGzWHPg_KeUr5CPKEc1bU54H2VRqyousdfG0In5Op0a4Y1Z-Jcw"))
            .connect(ws)
            .block()!!

        val response = clientRSocket.requestResponse(getPayload("hello", "Hello Server"))
        val res = response.block()
        println(res.dataUtf8)
    }

    private fun getPayload(route: String, message: String): Payload {
        val metadata = ByteBufAllocator.DEFAULT.compositeBuffer()
        val routingMetadata =
            TaggingMetadataCodec.createRoutingMetadata(ByteBufAllocator.DEFAULT, listOf(route))
        CompositeMetadataCodec.encodeAndAddMetadata(
            metadata,
            ByteBufAllocator.DEFAULT,
            WellKnownMimeType.MESSAGE_RSOCKET_ROUTING,
            routingMetadata.content
        )

        //val metadata = TaggingMetadataCodec.createRoutingMetadata(ByteBufAllocator.DEFAULT, listOf(route)).content
        val data = ByteBufAllocator.DEFAULT.buffer().writeBytes(message.toByteArray())

        return DefaultPayload.create(data, metadata)
    }

    private fun getSetupPayload(token: String): Payload {

//        val metadata = AuthMetadataCodec.encodeBearerMetadata(ByteBufAllocator.DEFAULT, token.toCharArray())
//        val data = AuthMetadataCodec.encodeBearerMetadata(ByteBufAllocator.DEFAULT, token.toCharArray())

        val metadata = ByteBufAllocator.DEFAULT.compositeBuffer()
        val bearerdata = AuthMetadataCodec.encodeBearerMetadata(ByteBufAllocator.DEFAULT, token.toCharArray())
        CompositeMetadataCodec.encodeAndAddMetadata(
            metadata,
            ByteBufAllocator.DEFAULT,
            WellKnownMimeType.MESSAGE_RSOCKET_AUTHENTICATION,
            bearerdata
        )

        val data = ByteBufAllocator.DEFAULT.buffer().writeBytes("".toByteArray())

        return DefaultPayload.create(data, metadata)
    }
}