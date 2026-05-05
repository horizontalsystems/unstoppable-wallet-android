package io.horizontalsystems.bankwallet.serializers

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.marketkit.models.Blockchain
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object BlockchainSerializer : KSerializer<Blockchain> {
    override val descriptor = PrimitiveSerialDescriptor("Blockchain", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Blockchain) {
        encoder.encodeString(value.uid)
    }

    override fun deserialize(decoder: Decoder): Blockchain {
        return App.marketKit.blockchain(decoder.decodeString())!!
    }
}
