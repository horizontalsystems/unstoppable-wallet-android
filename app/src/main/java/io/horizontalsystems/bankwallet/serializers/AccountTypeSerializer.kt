package io.horizontalsystems.bankwallet.serializers

import android.util.Base64
import io.horizontalsystems.bankwallet.entities.AccountType
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object AccountTypeSerializer : KSerializer<AccountType> {
    override val descriptor = PrimitiveSerialDescriptor("AccountType", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: AccountType) {
        val encoded = when (value) {
            is AccountType.EvmAddress -> "EvmAddress|${value.address}"
            is AccountType.SolanaAddress -> "SolanaAddress|${value.address}"
            is AccountType.TronAddress -> "TronAddress|${value.address}"
            is AccountType.TonAddress -> "TonAddress|${value.address}"
            is AccountType.StellarAddress -> "StellarAddress|${value.address}"
            is AccountType.BitcoinAddress -> "BitcoinAddress|${value.serialized}"
            is AccountType.MoneroWatchAccount -> "MoneroWatchAccount|${value.serialized}"
            is AccountType.HdExtendedKey -> "HdExtendedKey|${value.keySerialized}"
            is AccountType.StellarSecretKey -> "StellarSecretKey|${value.key}"
            is AccountType.EvmPrivateKey -> "EvmPrivateKey|${value.key}"
            is AccountType.TronPrivateKey -> "TronPrivateKey|${value.key}"
            is AccountType.Mnemonic -> {
                val words = value.words.joinToString(" ")
                val passphrase = Base64.encodeToString(value.passphrase.toByteArray(), Base64.NO_WRAP)
                "Mnemonic|$words|$passphrase"
            }
        }
        encoder.encodeString(encoded)
    }

    override fun deserialize(decoder: Decoder): AccountType {
        val encoded = decoder.decodeString()
        val separatorIdx = encoded.indexOf('|')
        val type = encoded.substring(0, separatorIdx)
        val data = encoded.substring(separatorIdx + 1)
        return when (type) {
            "EvmAddress" -> AccountType.EvmAddress(data)
            "SolanaAddress" -> AccountType.SolanaAddress(data)
            "TronAddress" -> AccountType.TronAddress(data)
            "TonAddress" -> AccountType.TonAddress(data)
            "StellarAddress" -> AccountType.StellarAddress(data)
            "BitcoinAddress" -> AccountType.BitcoinAddress.fromSerialized(data)
            "MoneroWatchAccount" -> AccountType.MoneroWatchAccount.fromSerialized(data)
            "HdExtendedKey" -> AccountType.HdExtendedKey(data)
            "StellarSecretKey" -> AccountType.StellarSecretKey(data)
            "EvmPrivateKey" -> AccountType.EvmPrivateKey(data.toBigInteger())
            "TronPrivateKey" -> AccountType.TronPrivateKey(data.toBigInteger())
            "Mnemonic" -> {
                val parts = data.split("|", limit = 2)
                val words = parts[0].split(" ")
                val passphrase = String(Base64.decode(parts[1], Base64.NO_WRAP))
                AccountType.Mnemonic(words, passphrase)
            }
            else -> throw IllegalArgumentException("Unknown AccountType: $type")
        }
    }
}
