package io.horizontalsystems.bankwallet.serializers

import io.horizontalsystems.marketkit.models.Coin
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object CoinSerializer : KSerializer<Coin> {
    override val descriptor: SerialDescriptor
        get() = TODO("Not yet implemented")

    override fun serialize(
        encoder: Encoder,
        value: Coin
    ) {
        TODO("Not yet implemented")
    }

    override fun deserialize(decoder: Decoder): Coin {
        TODO("Not yet implemented")
    }

}