package io.horizontalsystems.bankwallet.entities

import android.os.Parcelable
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.managers.PassphraseValidator
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.shorten
import io.horizontalsystems.ethereumkit.core.signer.Signer
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.hdwalletkit.HDExtendedKey
import io.horizontalsystems.hdwalletkit.HDWallet
import io.horizontalsystems.hdwalletkit.Language
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.horizontalsystems.hdwalletkit.WordList
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.math.BigInteger
import java.text.Normalizer

@Parcelize
data class Account(
    val id: String,
    val name: String,
    val type: AccountType,
    val origin: AccountOrigin,
    val level: Int,
    val isBackedUp: Boolean = false,
    val isFileBackedUp: Boolean = false,
) : Parcelable {

    @IgnoredOnParcel
    val hasAnyBackup = isBackedUp || isFileBackedUp

    @IgnoredOnParcel
    val isWatchAccount: Boolean
        get() = type.isWatchAccountType

    @IgnoredOnParcel
    val nonStandard: Boolean by lazy {
        if (type is AccountType.Mnemonic) {
            val words = type.words.joinToString(separator = " ")
            val passphrase = type.passphrase
            val normalizedWords = words.normalizeNFKD()
            val normalizedPassphrase = passphrase.normalizeNFKD()

            when {
                words != normalizedWords -> true
                passphrase != normalizedPassphrase -> true
                else -> try {
                    Mnemonic().validateStrict(type.words)
                    false
                } catch (exception: Exception) {
                    true
                }
            }
        } else {
            false
        }
    }

    @IgnoredOnParcel
    val nonRecommended: Boolean by lazy {
        if (type is AccountType.Mnemonic) {
            val englishWords = WordList.wordList(Language.English).validWords(type.words)
            val standardPassphrase = PassphraseValidator().containsValidCharacters(type.passphrase)
            !englishWords || !standardPassphrase
        } else {
            false
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other is Account) {
            return id == other.id
        }

        return false
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

@Parcelize
sealed class CexType : Parcelable {
    @Parcelize
    class Binance(val apiKey: String, val secretKey: String) : CexType()

    fun serialized() = when (this) {
        is Binance -> listOf("binance", apiKey, secretKey).joinToString(dataSeparator)
    }

    fun sameType(other: CexType): Boolean {
        return when (this) {
            is Binance -> other is Binance
        }
    }

    fun name(): String {
        return when (this) {
            is Binance -> "Binance"
        }
    }

    companion object {
        fun deserialize(value: String): CexType? {
            val parts = value.split(dataSeparator)

            return when (parts[0]) {
                "binance" -> Binance(parts[1], parts[2])
                else -> null
            }

        }

        private val dataSeparator = "@"
    }
}

@Parcelize
sealed class AccountType : Parcelable {
    @Parcelize
    data class Cex(val cexType: CexType) : AccountType()

    @Parcelize
    data class EvmAddress(val address: String) : AccountType()

    @Parcelize
    data class SolanaAddress(val address: String) : AccountType()

    @Parcelize
    data class TronAddress(val address: String) : AccountType()

    @Parcelize
    data class TonAddress(val address: String) : AccountType()

    @Parcelize
    data class BitcoinAddress(val address: String, val blockchainType: BlockchainType, val tokenType: TokenType) : AccountType() {

        val serialized: String
            get() = "$address|${blockchainType.uid}|${tokenType.id}"

        companion object {
            fun fromSerialized(serialized: String): BitcoinAddress {
                val split = serialized.split("|")
                return BitcoinAddress(
                    split[0],
                    BlockchainType.fromUid(split[1]),
                    TokenType.fromId(split[2])!!
                )
            }
        }
    }

    @Parcelize
    data class Mnemonic(val words: List<String>, val passphrase: String) : AccountType() {
        @IgnoredOnParcel
        val seed by lazy { Mnemonic().toSeed(words, passphrase) }

        override fun equals(other: Any?): Boolean {
            return other is Mnemonic
                    && words.toTypedArray().contentEquals(other.words.toTypedArray())
                    && passphrase == other.passphrase
        }

        override fun hashCode(): Int {
            return words.toTypedArray().contentHashCode() + passphrase.hashCode()
        }
    }

    @Parcelize
    data class EvmPrivateKey(val key: BigInteger) : AccountType() {
        override fun equals(other: Any?): Boolean {
            return other is EvmPrivateKey && key == other.key
        }

        override fun hashCode(): Int {
            return key.hashCode()
        }
    }

    @Parcelize
    data class HdExtendedKey(val keySerialized: String) : AccountType() {
        val hdExtendedKey: HDExtendedKey
            get() = HDExtendedKey(keySerialized)

        override fun equals(other: Any?): Boolean {
            return other is HdExtendedKey && keySerialized.contentEquals(other.keySerialized)
        }

        override fun hashCode(): Int {
            return keySerialized.hashCode()
        }
    }

    @Parcelize
    enum class Derivation(val value: String) : Parcelable {
        bip44("bip44"),
        bip49("bip49"),
        bip84("bip84"),
        bip86("bip86");

        val addressType: String
            get() = when (this) {
                bip44 -> "Legacy"
                bip49 -> "SegWit"
                bip84 -> "Native SegWit"
                bip86 -> "Taproot"
            }

        val rawName: String
            get() = when (this) {
                bip44 -> "BIP 44"
                bip49 -> "BIP 49"
                bip84 -> "BIP 84"
                bip86 -> "BIP 86"
            }

        val purpose: HDWallet.Purpose
            get() = when (this) {
                bip44 -> HDWallet.Purpose.BIP44
                bip49 -> HDWallet.Purpose.BIP49
                bip84 -> HDWallet.Purpose.BIP84
                bip86 -> HDWallet.Purpose.BIP86
            }

        companion object {
            val default = bip84
            private val map = values().associateBy(Derivation::value)

            fun fromString(value: String?): Derivation? = map[value]
        }
    }

    val description: String
        get() = when (this) {
            is Mnemonic -> {
                val count = words.size

                if (passphrase.isNotBlank()) {
                    Translator.getString(R.string.ManageAccount_NWordsWithPassphrase, count)
                } else {
                    Translator.getString(R.string.ManageAccount_NWords, count)
                }
            }
            is BitcoinAddress -> "BTC Address"
            is EvmAddress -> "EVM Address"
            is SolanaAddress -> "Solana Address"
            is TronAddress -> "Tron Address"
            is TonAddress -> "Ton Address"
            is EvmPrivateKey -> "EVM Private Key"
            is HdExtendedKey -> {
                when (this.hdExtendedKey.derivedType) {
                    HDExtendedKey.DerivedType.Master -> "BIP32 Root Key"
                    HDExtendedKey.DerivedType.Account -> {
                        if (hdExtendedKey.isPublic) {
                            "Account xPubKey"
                        } else {
                            "Account xPrivKey"
                        }
                    }
                    else -> ""
                }
            }
            is Cex -> "Cex"
        }

    val supportedDerivations: List<Derivation>
        get() = when (this) {
            is Mnemonic -> {
                listOf(Derivation.bip44, Derivation.bip49, Derivation.bip84, Derivation.bip86)
            }
            is HdExtendedKey -> {
                hdExtendedKey.purposes.map { it.derivation }
            }
            else -> emptyList()
        }

    val hideZeroBalances: Boolean
        get() = false

    val detailedDescription: String
        get() = when (this) {
            is EvmAddress -> this.address.shorten()
            is SolanaAddress -> this.address.shorten()
            is TronAddress -> this.address.shorten()
            is TonAddress -> this.address.shorten()
            is BitcoinAddress -> this.address.shorten()
            else -> this.description
        }

    val canAddTokens: Boolean
        get() = when (this) {
            is Mnemonic, is EvmPrivateKey -> true
            else -> false
        }

    val supportsWalletConnect: Boolean
        get() = when (this) {
            is Mnemonic, is EvmPrivateKey -> true
            else -> false
        }

    val isWatchAccountType: Boolean
        get() = when (this) {
            is EvmAddress -> true
            is SolanaAddress -> true
            is TronAddress -> true
            is TonAddress -> true
            is BitcoinAddress -> true
            is HdExtendedKey -> hdExtendedKey.isPublic
            else -> false
        }

    fun evmAddress(chain: Chain) = when (this) {
        is Mnemonic -> Signer.address(seed, chain)
        is EvmPrivateKey -> Signer.address(key)
        else -> null
    }

    fun sign(message: ByteArray, isLegacy: Boolean = false): ByteArray? {
        val signer = when (this) {
            is Mnemonic -> {
                Signer.getInstance(seed, App.evmBlockchainManager.getChain(BlockchainType.Ethereum))
            }
            is EvmPrivateKey -> {
                Signer.getInstance(key, App.evmBlockchainManager.getChain(BlockchainType.Ethereum))
            }
            else -> null
        } ?: return null

        return if (isLegacy) {
            signer.signByteArrayLegacy(message)
        } else {
            signer.signByteArray(message)
        }
    }
}

val HDWallet.Purpose.derivation: AccountType.Derivation
    get() = when (this) {
        HDWallet.Purpose.BIP44 -> AccountType.Derivation.bip44
        HDWallet.Purpose.BIP49 -> AccountType.Derivation.bip49
        HDWallet.Purpose.BIP84 -> AccountType.Derivation.bip84
        HDWallet.Purpose.BIP86 -> AccountType.Derivation.bip86
    }

val HDWallet.Purpose.tokenTypeDerivation: TokenType.Derivation
    get() = when (this) {
        HDWallet.Purpose.BIP44 -> TokenType.Derivation.Bip44
        HDWallet.Purpose.BIP49 -> TokenType.Derivation.Bip49
        HDWallet.Purpose.BIP84 -> TokenType.Derivation.Bip84
        HDWallet.Purpose.BIP86 -> TokenType.Derivation.Bip86
    }

@Parcelize
enum class AccountOrigin(val value: String) : Parcelable {
    Created("Created"),
    Restored("Restored");
}

fun String.normalizeNFKD(): String = Normalizer.normalize(this, Normalizer.Form.NFKD)
