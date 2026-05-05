package io.horizontalsystems.bankwallet.serializers

import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object BlockchainTypeListSerializer : KSerializer<List<BlockchainType>> {
    override val descriptor = PrimitiveSerialDescriptor("BlockchainTypeList", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: List<BlockchainType>) {
        encoder.encodeString(value.joinToString(",") { it.uid })
    }

    override fun deserialize(decoder: Decoder): List<BlockchainType> {
        val encoded = decoder.decodeString()
        if (encoded.isEmpty()) return emptyList()
        return encoded.split(",").map { BlockchainType.fromUid(it) }
    }
}
