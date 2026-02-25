package io.horizontalsystems.bankwallet.serializers

import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object TokenTypeSerializer : KSerializer<TokenType> {
    override val descriptor = PrimitiveSerialDescriptor("TokenType", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: TokenType
    ) {
        encoder.encodeString(value.id)
    }

    override fun deserialize(decoder: Decoder): TokenType {
        return TokenType.Companion.fromId(decoder.decodeString())!!
    }
}
