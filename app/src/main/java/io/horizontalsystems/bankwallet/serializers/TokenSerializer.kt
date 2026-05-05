package io.horizontalsystems.bankwallet.serializers

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object TokenSerializer : KSerializer<Token> {
    override val descriptor = PrimitiveSerialDescriptor("Token", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Token) {
        encoder.encodeString(value.tokenQuery.id)
    }

    override fun deserialize(decoder: Decoder): Token {
        val query = TokenQuery.fromId(decoder.decodeString())!!
        return App.coinManager.getToken(query)!!
    }
}
