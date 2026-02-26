package io.horizontalsystems.bankwallet.serializers

import io.horizontalsystems.marketkit.models.Blockchain
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object BlockchainSerializer : KSerializer<Blockchain> {
    override val descriptor: SerialDescriptor
        get() = TODO("Not yet implemented")

    override fun serialize(
        encoder: Encoder,
        value: Blockchain
    ) {
        TODO("Not yet implemented")
    }

    override fun deserialize(decoder: Decoder): Blockchain {
        TODO("Not yet implemented")
    }

}