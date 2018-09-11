package bitcoin.wallet.modules.transactionInfo

import bitcoin.wallet.core.IDatabaseManager
import bitcoin.wallet.core.managers.CoinManager
import bitcoin.wallet.entities.TransactionRecord

class TransactionInfoInteractor(private val databaseManager: IDatabaseManager, private val coinManager: CoinManager) : TransactionInfoModule.IInteractor {

    var delegate: TransactionInfoModule.IInteractorDelegate? = null

    private var latestBlockHeights = mapOf<String, Long>()
    private var transactionRecord: TransactionRecord? = null

    override fun getTransactionInfo(coinCode: String, txHash: String) {
        databaseManager.getBlockchainInfos().subscribe {
            latestBlockHeights = it.array.map { it.coinCode to it.latestBlockHeight }.toMap()

            refresh()
        }

        databaseManager.getTransactionRecord(coinCode, txHash).subscribe {
            transactionRecord = it

            refresh()
        }
    }

    private fun refresh() {
        transactionRecord?.let { transactionRecord ->

            latestBlockHeights[transactionRecord.coinCode]?.let { latestBlockHeight ->

                coinManager.getCoinByCode(transactionRecord.coinCode)?.let { coin ->

                    databaseManager.getExchangeRates().subscribe {

                        val exchangeRate = it.array.find { it.code == transactionRecord.coinCode }?.value
//                        transactionConverter.convertToTransactionRecordViewItem(coin, transactionRecord, latestBlockHeight, exchangeRate).let { transactionRecordViewItem ->
//
//                            delegate?.didGetTransactionInfo(transactionRecordViewItem)
//                        }
                    }

                }
            }
        }
    }

}
