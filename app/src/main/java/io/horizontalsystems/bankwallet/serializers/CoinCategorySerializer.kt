package io.horizontalsystems.bankwallet.serializers

import io.horizontalsystems.marketkit.models.CoinCategory
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object CoinCategorySerializer : KSerializer<CoinCategory> {
    override val descriptor: SerialDescriptor
        get() = TODO("Not yet implemented")

    override fun serialize(
        encoder: Encoder,
        value: CoinCategory
    ) {
        TODO("Not yet implemented")
    }

    override fun deserialize(decoder: Decoder): CoinCategory {
        TODO("Not yet implemented")
    }

}