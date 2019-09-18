package io.horizontalsystems.bankwallet.entities

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
class Account(val id: String,
              val name: String,
              val type: AccountType,
              var isBackedUp: Boolean = false,
              val defaultSyncMode: SyncMode?) : Parcelable {

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
    data class Mnemonic(val words: List<String>, val derivation: Derivation, val salt: String?) : AccountType()

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
    data class HDMasterKey(val key: ByteArray, val derivation: Derivation) : AccountType() {
        override fun equals(other: Any?): Boolean {
            if (other is HDMasterKey) {
                return derivation == other.derivation && key.contentEquals(other.key)
            }

            return false
        }

        override fun hashCode(): Int {
            return Objects.hash(key, derivation)
        }
    }

    @Parcelize
    data class Eos(val account: String, val activePrivateKey: String) : AccountType()

    @Parcelize
    enum class Derivation : Parcelable {
        bip44,
        bip49,
        bip84
    }
}
