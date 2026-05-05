package io.horizontalsystems.bankwallet.serializers

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.marketkit.models.Coin
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object CoinSerializer : KSerializer<Coin> {
    override val descriptor = PrimitiveSerialDescriptor("Coin", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Coin) {
        encoder.encodeString(value.uid)
    }

    override fun deserialize(decoder: Decoder): Coin {
        val uid = decoder.decodeString()
        return App.marketKit.fullCoins(listOf(uid)).firstOrNull()?.coin
            ?: App.marketKit.allCoins().find { it.uid == uid }
            ?: error("Coin not found: $uid")
    }
}
