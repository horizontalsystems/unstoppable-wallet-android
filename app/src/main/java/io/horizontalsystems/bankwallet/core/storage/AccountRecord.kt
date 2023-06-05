package io.horizontalsystems.bankwallet.core.storage

import androidx.room.Entity

class SecretString(val value: String)

class SecretList(val list: List<String>)

@Entity(primaryKeys = ["id"])
data class AccountRecord(var id: String,
                         var name: String,
                         var type: String,
                         var origin: String,
                         var isBackedUp: Boolean,
                         var isFileBackedUp: Boolean,
                         var words: SecretList?,
                         var passphrase: SecretString?,
                         var key: SecretString?) {

    var deleted = false

    override fun equals(other: Any?): Boolean {
        if (other is AccountRecord) {
            return id == other.id
        }

        return false
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

}
