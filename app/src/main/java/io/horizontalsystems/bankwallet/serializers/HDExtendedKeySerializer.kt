package io.horizontalsystems.bankwallet.serializers

import io.horizontalsystems.hdwalletkit.HDExtendedKey
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object HDExtendedKeySerializer : KSerializer<HDExtendedKey> {
    override val descriptor = PrimitiveSerialDescriptor("HDExtendedKey", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: HDExtendedKey) {
        encoder.encodeString(value.serialize())
    }

    override fun deserialize(decoder: Decoder): HDExtendedKey {
        return HDExtendedKey(decoder.decodeString())
    }
}
