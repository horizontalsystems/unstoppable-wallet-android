package io.horizontalsystems.bankwallet.core

import io.horizontalsystems.bankwallet.entities.SyncMode
import java.util.*

class Account(val id: String,
              val name: String,
              val type: AccountType,
              var isBackedUp: Boolean = false,
              val defaultSyncMode: SyncMode = SyncMode.FAST) {

    override fun equals(other: Any?): Boolean {
        if (other is Account) {
            return name == other.name && type == other.type
        }

        return false
    }

    override fun hashCode(): Int {
        return Objects.hash(name, type)
    }

    override fun toString(): String {
        val accountType = when (type) {
            is AccountType.Mnemonic -> "Mnemonic [words: ${type.words.joinToString(separator = ", ")}, derivation: ${type.derivation.name}, salt: ${type.salt}]"
            is AccountType.PrivateKey -> "PrivateKey [key: ${type.key.toHexString()}]"
            is AccountType.HDMasterKey -> "HDMasterKey [key: ${type.key.toHexString()}, derivation: ${type.derivation}]"
            is AccountType.Eos -> "Eos [activePrivateKey: ${type.activePrivateKey.toHexString()}, account: ${type.account}]"
        }
        return "Account: [id: $id, name: $name, type: $accountType, isBackedUp: $isBackedUp, defaultSyncMode: $defaultSyncMode]"
    }
}

sealed class AccountType {
    data class Mnemonic(val words: List<String>, val derivation: Derivation, val salt: String) : AccountType()

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

    data class Eos(val account: String, val activePrivateKey: ByteArray) : AccountType()

    enum class Derivation {
        bip39,
        bip44
    }
}
