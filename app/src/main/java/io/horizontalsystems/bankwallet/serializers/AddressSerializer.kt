package io.horizontalsystems.bankwallet.serializers

import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object AddressSerializer : KSerializer<Address> {
    override val descriptor = PrimitiveSerialDescriptor("Address", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Address) {
        encoder.encodeString("${value.hex}|${value.domain ?: ""}|${value.blockchainType?.uid ?: ""}")
    }

    override fun deserialize(decoder: Decoder): Address {
        val parts = decoder.decodeString().split("|", limit = 3)
        val hex = parts[0]
        val domain = parts[1].takeIf { it.isNotEmpty() }
        val blockchainType = parts[2].takeIf { it.isNotEmpty() }?.let { BlockchainType.fromUid(it) }
        return Address(hex, domain, blockchainType)
    }
}
