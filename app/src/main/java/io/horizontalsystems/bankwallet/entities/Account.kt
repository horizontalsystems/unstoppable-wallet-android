package io.horizontalsystems.bankwallet.entities

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
class Account(val id: String,
              val name: String,
              val type: AccountType,
              val origin: AccountOrigin,
              var isBackedUp: Boolean = false) : Parcelable {

    override fun equals(other: Any?): Boolean {
        if (other is Account) {
            return id == other.id && type == other.type
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
    data class Mnemonic(val words: List<String>, val salt: String?) : AccountType()

    @Parcelize
    data class PrivateKey(val key: ByteArray) : AccountType() {
        override fun equals(other: Any?): Boolean {
            if (other is PrivateKey) {
                return key.contentEquals(other.key)
            }

            return false
        }

        override fun hashCode(): Int {
            return key.contentHashCode()
        }
    }

    @Parcelize
    data class Eos(val account: String, val activePrivateKey: String) : AccountType()

    @Parcelize
    enum class Derivation(val value: String) : Parcelable {
        bip44("bip44"),
        bip49("bip49"),
        bip84("bip84");

        val addressType: String
            get() = when (this) {
                bip44 -> "Legacy"
                bip49 -> "SegWit"
                bip84 -> "Native SegWit"
            }
    }

    companion object{

        fun getDerivationLongTitle(derivation: Derivation): String {
            return "${derivation.addressType} - ${getDerivationTitle(derivation)}"
        }

        fun getDerivationTitle(derivation: Derivation): String {
            return when(derivation) {
                Derivation.bip44 -> "BIP 44"
                Derivation.bip49 -> "BIP 49"
                Derivation.bip84 -> "BIP 84"
            }
        }
    }
}

@Parcelize
enum class AccountOrigin(val value: String) : Parcelable {
    Created("Created"),
    Restored("Restored");
}
