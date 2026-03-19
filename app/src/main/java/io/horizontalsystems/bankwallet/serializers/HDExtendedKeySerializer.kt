package io.horizontalsystems.bankwallet.serializers

import io.horizontalsystems.hdwalletkit.HDExtendedKey
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object HDExtendedKeySerializer : KSerializer<HDExtendedKey> {
    override val descriptor: SerialDescriptor
        get() = TODO("Not yet implemented")

    override fun serialize(
        encoder: Encoder,
        value: HDExtendedKey
    ) {
        TODO("Not yet implemented")
    }

    override fun deserialize(decoder: Decoder): HDExtendedKey {
        TODO("Not yet implemented")
    }
}
