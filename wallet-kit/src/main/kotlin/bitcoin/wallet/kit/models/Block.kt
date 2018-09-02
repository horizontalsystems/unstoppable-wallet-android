package bitcoin.wallet.kit.models

import io.realm.RealmObject
import io.realm.RealmResults
import io.realm.annotations.LinkingObjects

/**
 * Block
 *
 *  Size        Field           Description
 *  ====        =====           ===========
 *  80 bytes    Header          Consists of 6 fields that are hashed to calculate the block hash
 *  VarInt      TxCount         Number of transactions in the block
 *  Variable    Transactions    The transactions in the block
 */
open class Block : RealmObject() {

    var synced = false
    var height: Int = 0
    var header: Header? = null
    var previousBlock: Block? = null

    @LinkingObjects("block")
    val transactions: RealmResults<Transaction>? = null

}
