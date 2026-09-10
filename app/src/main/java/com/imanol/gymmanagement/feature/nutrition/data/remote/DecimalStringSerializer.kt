package com.imanol.gymmanagement.feature.nutrition.data.remote

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonUnquotedLiteral
import kotlinx.serialization.json.jsonPrimitive

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
object DecimalStringSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("DecimalString", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: String) {
        if (encoder is JsonEncoder) {
            value.toBigDecimal()
            encoder.encodeJsonElement(JsonUnquotedLiteral(value))
        } else {
            encoder.encodeString(value)
        }
    }

    override fun deserialize(decoder: Decoder): String =
        if (decoder is JsonDecoder) {
            decoder.decodeJsonElement().jsonPrimitive.content
        } else {
            decoder.decodeString()
        }
}
