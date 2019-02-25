package io.horizontalsystems.bankwallet.modules.fulltransactioninfo

import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.FullTransactionRecord
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode

class FullTransactionInfoState(override val coin: Coin, override val transactionHash: String)
    : FullTransactionInfoModule.State {

    override var transactionRecord: FullTransactionRecord? = null
}
