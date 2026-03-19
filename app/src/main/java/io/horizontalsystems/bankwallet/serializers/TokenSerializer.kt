package io.horizontalsystems.bankwallet.serializers

import io.horizontalsystems.marketkit.models.Token
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object TokenSerializer : KSerializer<Token> {
    override val descriptor: SerialDescriptor
        get() = TODO("Not yet implemented")

    override fun serialize(
        encoder: Encoder,
        value: Token
    ) {
        TODO("Not yet implemented")
    }

    override fun deserialize(decoder: Decoder): Token {
        TODO("Not yet implemented")
    }

}