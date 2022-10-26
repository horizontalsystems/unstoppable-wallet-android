package io.horizontalsystems.bankwallet.entities

import android.os.Parcelable
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.shorten
import io.horizontalsystems.hdwalletkit.HDExtendedKey
import io.horizontalsystems.hdwalletkit.HDWallet
import io.horizontalsystems.hdwalletkit.Mnemonic
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.math.BigInteger

@Parcelize
data class Account(
    val id: String,
    val name: String,
    val type: AccountType,
    val origin: AccountOrigin,
    val isBackedUp: Boolean = false
) : Parcelable {

    @IgnoredOnParcel
    val isWatchAccount: Boolean
        get() = when (this.type) {
            is AccountType.EvmAddress -> true
            is AccountType.HdExtendedKey -> this.type.hdExtendedKey.info.isPublic
            else -> false
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
sealed class AccountType : Parcelable {
    @Parcelize
    data class EvmAddress(val address: String) : AccountType()

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
        bip84("bip84");

        companion object {
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
            is EvmAddress -> "EVM Address"
            is EvmPrivateKey -> "EVM Private Key"
            is HdExtendedKey -> {
                when (this.hdExtendedKey.derivedType) {
                    HDExtendedKey.DerivedType.Master -> "BIP32 Root Key"
                    HDExtendedKey.DerivedType.Account -> {
                        if (hdExtendedKey.info.isPublic) {
                            "Account xPubKey"
                        } else {
                            "Account xPrivKey"
                        }
                    }
                    else -> ""
                }
            }
            else -> ""
        }

    val supportedDerivations: List<Derivation>
        get() = when (this) {
            is Mnemonic -> {
                listOf(Derivation.bip44, Derivation.bip49, Derivation.bip84)
            }
            is HdExtendedKey -> {
                listOf(this.hdExtendedKey.info.purpose.derivation)
            }
            else -> emptyList()
        }

    val hideZeroBalances = this is EvmAddress

    val detailedDescription: String
        get() = when (this) {
            is EvmAddress -> this.address.shorten()
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
}

val HDWallet.Purpose.derivation: AccountType.Derivation
    get() = when (this) {
        HDWallet.Purpose.BIP44 -> AccountType.Derivation.bip44
        HDWallet.Purpose.BIP49 -> AccountType.Derivation.bip49
        HDWallet.Purpose.BIP84 -> AccountType.Derivation.bip84
    }

val AccountType.Derivation.addressType: String
    get() = when (this) {
        AccountType.Derivation.bip44 -> "Legacy"
        AccountType.Derivation.bip49 -> "SegWit"
        AccountType.Derivation.bip84 -> "Native SegWit"
    }

val AccountType.Derivation.rawName: String
    get() = when (this) {
        AccountType.Derivation.bip44 -> "BIP 44"
        AccountType.Derivation.bip49 -> "BIP 49"
        AccountType.Derivation.bip84 -> "BIP 84"
    }

val AccountType.Derivation.title: String
    get() = when (this) {
        AccountType.Derivation.bip44 -> Translator.getString(R.string.CoinOption_bip44_Title)
        AccountType.Derivation.bip49 -> Translator.getString(R.string.CoinOption_bip49_Title)
        AccountType.Derivation.bip84 -> Translator.getString(R.string.CoinOption_bip84_Title)
    }

val AccountType.Derivation.description: String
    get() = when (this) {
        AccountType.Derivation.bip44 -> rawName
        AccountType.Derivation.bip84,
        AccountType.Derivation.bip49 -> "$rawName - $addressType"
    }

@Parcelize
enum class AccountOrigin(val value: String) : Parcelable {
    Created("Created"),
    Restored("Restored");
}
