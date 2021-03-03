package io.horizontalsystems.bankwallet.entities

import android.os.Parcelable
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.coinkit.models.CoinType
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
class Account(val id: String,
              val name: String,
              val type: AccountType,
              val origin: AccountOrigin,
              var isBackedUp: Boolean = false
) : Parcelable {

    override fun equals(other: Any?): Boolean {
        if (other is Account) {
            return id == other.id
        }

        return false
    }

    override fun hashCode(): Int {
        return Objects.hash(name, type)
    }
}

@Parcelize
open class AccountType : Parcelable {
    @Parcelize
    data class Mnemonic(val words: List<String>, val salt: String? = null) : AccountType() {
        override fun equals(other: Any?): Boolean {
            return other is Mnemonic && words.toTypedArray().contentEquals(other.words.toTypedArray()) && salt == other.salt
        }

        override fun hashCode(): Int {
            return words.toTypedArray().contentHashCode() + salt.hashCode()
        }
    }

    @Parcelize
    data class PrivateKey(val key: ByteArray) : AccountType() {
        override fun equals(other: Any?): Boolean {
            return other is PrivateKey && key.contentEquals(other.key)
        }

        override fun hashCode(): Int {
            return key.contentHashCode()
        }
    }

    @Parcelize
    data class Zcash(val words: List<String>, val birthdayHeight: Long? = null) : AccountType() {
        override fun equals(other: Any?): Boolean {
            return other is Zcash && words.toTypedArray().contentEquals(other.words.toTypedArray())
        }

        override fun hashCode(): Int {
            return words.toTypedArray().contentHashCode()
        }
    }

    @Parcelize
    enum class Derivation(val value: String) : Parcelable {
        bip44("bip44"),
        bip49("bip49"),
        bip84("bip84");
    }
}

fun AccountType.Derivation.addressType(): String = when (this) {
    AccountType.Derivation.bip44 -> "Legacy"
    AccountType.Derivation.bip49 -> "SegWit"
    AccountType.Derivation.bip84 -> "Native SegWit"
}

fun AccountType.Derivation.title(): String = when (this) {
    AccountType.Derivation.bip44 -> "BIP 44"
    AccountType.Derivation.bip49 -> "BIP 49"
    AccountType.Derivation.bip84 -> "BIP 84"
}

fun AccountType.Derivation.longTitle(): String =
        "${this.addressType()} - ${this.title()}"

fun AccountType.Derivation.description(): Int = when (this) {
    AccountType.Derivation.bip44 -> R.string.CoinOption_bip44_Subtitle
    AccountType.Derivation.bip84 -> R.string.CoinOption_bip84_Subtitle
    AccountType.Derivation.bip49 -> R.string.CoinOption_bip49_Subtitle
}

fun AccountType.Derivation.addressPrefix(coinType: CoinType): String? {
    return when (coinType) {
        CoinType.Bitcoin -> {
            when (this) {
                AccountType.Derivation.bip44 -> "1"
                AccountType.Derivation.bip49 -> "3"
                AccountType.Derivation.bip84 -> "bc1"
            }
        }
        CoinType.Litecoin -> {
            when (this) {
                AccountType.Derivation.bip44 -> "L"
                AccountType.Derivation.bip49 -> "M"
                AccountType.Derivation.bip84 -> "ltc1"
            }
        }
        else -> null
    }

}

@Parcelize
enum class AccountOrigin(val value: String) : Parcelable {
    Created("Created"),
    Restored("Restored");
}
