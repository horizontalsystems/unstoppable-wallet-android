package io.horizontalsystems.bankwallet.core

class Account(val name: String, val type: AccountType)

class AccountType {
    data class Mnemonic(val words: List<String>, val derivation: Derivation, val salt: String)
    data class MasterKey(val data: ByteArray) {
        override fun equals(other: Any?): Boolean {
            if (other is MasterKey) {
                return data.contentEquals(other.data)
            }

            return super.equals(other)
        }

        override fun hashCode(): Int {
            return data.contentHashCode()
        }
    }

    data class HDMasterKey(val data: ByteArray, val derivation: Derivation) {
        override fun equals(other: Any?): Boolean {
            if (other is HDMasterKey) {
                return derivation == other.derivation && data.contentEquals(other.data)
            }

            return super.equals(other)
        }

        override fun hashCode(): Int {
            var result = data.contentHashCode()
            result = 31 * result + derivation.hashCode()
            return result
        }
    }

    data class Eos(val account: String, val key: String)
    enum class Derivation {
        bip39,
        bip44
    }
}
