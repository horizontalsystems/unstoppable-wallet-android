package io.horizontalsystems.bankwallet.serializers

import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object BlockchainTypeSerializer : KSerializer<BlockchainType> {
    override val descriptor = PrimitiveSerialDescriptor("BlockchainType", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: BlockchainType) {
        encoder.encodeString(value.uid)
    }

    override fun deserialize(decoder: Decoder): BlockchainType {
        return BlockchainType.fromUid(decoder.decodeString())
    }
}
