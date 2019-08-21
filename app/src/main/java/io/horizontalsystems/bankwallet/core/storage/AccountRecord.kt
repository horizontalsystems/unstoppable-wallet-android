package io.horizontalsystems.bankwallet.core.storage

import androidx.room.Entity
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.SyncMode

class SecretString(val value: String)

class SecretList(val list: List<String>)

@Entity(primaryKeys = ["id"])
data class AccountRecord(var id: String,
                         var name: String,
                         var type: String,
                         var isBackedUp: Boolean,
                         var syncMode: SyncMode?,
                         var words: SecretList?,
                         var derivation: AccountType.Derivation?,
                         var salt: SecretString?,
                         var key: SecretString?,
                         var eosAccount: String?) {

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
