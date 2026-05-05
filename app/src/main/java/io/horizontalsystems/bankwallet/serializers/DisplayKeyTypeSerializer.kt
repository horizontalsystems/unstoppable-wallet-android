package io.horizontalsystems.bankwallet.serializers

import io.horizontalsystems.bankwallet.modules.manageaccount.showextendedkey.ShowExtendedKeyModule
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object DisplayKeyTypeSerializer : KSerializer<ShowExtendedKeyModule.DisplayKeyType> {
    override val descriptor = PrimitiveSerialDescriptor("DisplayKeyType", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ShowExtendedKeyModule.DisplayKeyType) {
        val encoded = when (value) {
            is ShowExtendedKeyModule.DisplayKeyType.Bip32RootKey -> "Bip32RootKey"
            is ShowExtendedKeyModule.DisplayKeyType.AccountPrivateKey -> "AccountPrivateKey|${value.derivable}"
            is ShowExtendedKeyModule.DisplayKeyType.AccountPublicKey -> "AccountPublicKey|${value.derivable}"
        }
        encoder.encodeString(encoded)
    }

    override fun deserialize(decoder: Decoder): ShowExtendedKeyModule.DisplayKeyType {
        val encoded = decoder.decodeString()
        return when {
            encoded == "Bip32RootKey" -> ShowExtendedKeyModule.DisplayKeyType.Bip32RootKey
            encoded.startsWith("AccountPrivateKey|") -> {
                val derivable = encoded.substringAfter("|").toBoolean()
                ShowExtendedKeyModule.DisplayKeyType.AccountPrivateKey(derivable)
            }
            encoded.startsWith("AccountPublicKey|") -> {
                val derivable = encoded.substringAfter("|").toBoolean()
                ShowExtendedKeyModule.DisplayKeyType.AccountPublicKey(derivable)
            }
            else -> throw IllegalArgumentException("Unknown DisplayKeyType: $encoded")
        }
    }
}
