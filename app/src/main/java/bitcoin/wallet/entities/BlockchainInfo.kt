package bitcoin.wallet.entities

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class BlockchainInfo : RealmObject() {

    @PrimaryKey
    var coinCode = ""

    var latestBlockHeight = 0L

    var syncing = false

}
