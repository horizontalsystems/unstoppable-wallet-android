package io.horizontalsystems.bankwallet.core

import io.horizontalsystems.bankwallet.entities.SyncMode
import java.util.*

class Account(val id: String,
              val name: String,
              val type: AccountType,
              val defaultSyncMode: SyncMode,
              var isBackedUp: Boolean = false) {

    override fun equals(other: Any?): Boolean {
        if (other is Account) {
            return name == other.name && type == other.type
        }

        return false
    }

    override fun hashCode(): Int {
        return Objects.hash(name, type)
    }
}

sealed class AccountType {
    data class Mnemonic(val words: List<String>, val derivation: Derivation, val salt: String) : AccountType()
    data class MasterKey(val data: ByteArray) : AccountType() {
        override fun equals(other: Any?): Boolean {
            if (other is MasterKey) {
                return data.contentEquals(other.data)
            }

            return false
        }

        override fun hashCode(): Int {
            return data.contentHashCode()
        }
    }

    data class HDMasterKey(val data: ByteArray, val derivation: Derivation) : AccountType() {
        override fun equals(other: Any?): Boolean {
            if (other is HDMasterKey) {
                return derivation == other.derivation && data.contentEquals(other.data)
            }

            return false
        }

        override fun hashCode(): Int {
            return Objects.hash(data, derivation)
        }
    }

    data class Eos(val account: String, val key: String) : AccountType()
    enum class Derivation {
        bip39,
        bip44
    }
}
