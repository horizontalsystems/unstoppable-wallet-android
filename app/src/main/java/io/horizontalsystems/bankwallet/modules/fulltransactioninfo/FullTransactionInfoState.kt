package io.horizontalsystems.bankwallet.modules.fulltransactioninfo

import io.horizontalsystems.bankwallet.entities.FullTransactionRecord
import io.horizontalsystems.bankwallet.entities.Wallet

class FullTransactionInfoState(override val wallet: Wallet, override val transactionHash: String)
    : FullTransactionInfoModule.State {

    override var transactionRecord: FullTransactionRecord? = null
}
