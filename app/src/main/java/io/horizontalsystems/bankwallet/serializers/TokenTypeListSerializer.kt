package io.horizontalsystems.bankwallet.serializers

import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object TokenTypeListSerializer : KSerializer<List<TokenType>> {
    override val descriptor = PrimitiveSerialDescriptor("TokenTypeList", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: List<TokenType>) {
        encoder.encodeString(value.joinToString(",") { it.id })
    }

    override fun deserialize(decoder: Decoder): List<TokenType> {
        val encoded = decoder.decodeString()
        if (encoded.isEmpty()) return emptyList()
        return encoded.split(",").map { TokenType.fromId(it)!! }
    }
}
