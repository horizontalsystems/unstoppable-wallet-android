package io.horizontalsystems.bankwallet.serializers

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.marketkit.models.Blockchain
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object BlockchainListSerializer : KSerializer<List<Blockchain>> {
    override val descriptor = PrimitiveSerialDescriptor("BlockchainList", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: List<Blockchain>) {
        encoder.encodeString(value.joinToString(",") { it.uid })
    }

    override fun deserialize(decoder: Decoder): List<Blockchain> {
        val encoded = decoder.decodeString()
        if (encoded.isEmpty()) return emptyList()
        val uids = encoded.split(",")
        return App.marketKit.blockchains(uids)
    }
}
