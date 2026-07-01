package io.horizontalsystems.bankwallet.modules.profeatures.storage

import androidx.room.Entity
import io.horizontalsystems.bankwallet.core.storage.SecretString
import java.util.*

@Entity(primaryKeys = ["nftName", "accountId"])
data class ProFeaturesSessionKey(var nftName: String,
                                 var accountId: String,
                                 var address: String,
                                 var key: SecretString) {

    override fun equals(other: Any?): Boolean {
        if (other is ProFeaturesSessionKey) {
            return nftName == other.nftName && accountId == other.accountId
        }

        return false
    }

    override fun hashCode(): Int {
        return Objects.hash(nftName, accountId)
    }

}
