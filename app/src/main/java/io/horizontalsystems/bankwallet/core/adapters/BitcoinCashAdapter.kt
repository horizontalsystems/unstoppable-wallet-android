package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISendBitcoinAdapter
import io.horizontalsystems.bankwallet.core.UnsupportedAccountException
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.SyncMode
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bitcoincash.BitcoinCashKit
import io.horizontalsystems.bitcoincash.BitcoinCashKit.NetworkType
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.models.BalanceInfo
import io.horizontalsystems.bitcoincore.models.BlockInfo
import io.horizontalsystems.bitcoincore.models.TransactionInfo
import io.horizontalsystems.core.helpers.DateHelper
import io.reactivex.Single
import java.math.BigDecimal
import java.util.*

class BitcoinCashAdapter(
        override val kit: BitcoinCashKit,
        syncMode: SyncMode?
) : BitcoinBaseAdapter(kit, syncMode = syncMode), BitcoinCashKit.Listener, ISendBitcoinAdapter {

    constructor(wallet: Wallet, syncMode: SyncMode?, testMode: Boolean) :
            this(createKit(wallet, syncMode, testMode), syncMode)

    init {
        kit.listener = this
    }

    //
    // BitcoinBaseAdapter
    //

    override val satoshisInBitcoin: BigDecimal = BigDecimal.valueOf(Math.pow(10.0, decimal.toDouble()))

    //
    // BitcoinCashKit Listener
    //

    override fun onBalanceUpdate(balance: BalanceInfo) {
        balanceUpdatedSubject.onNext(Unit)
    }

    override fun onLastBlockInfoUpdate(blockInfo: BlockInfo) {
        lastBlockUpdatedSubject.onNext(Unit)
    }

    override fun onKitStateUpdate(state: BitcoinCore.KitState) {
        when (state) {
            is BitcoinCore.KitState.Synced -> {
                if (this.state !is AdapterState.Synced) {
                    this.state = AdapterState.Synced
                }
            }
            is BitcoinCore.KitState.NotSynced -> {
                if (this.state !is AdapterState.NotSynced) {
                    this.state = AdapterState.NotSynced(state.exception)
                }
            }
            is BitcoinCore.KitState.Syncing -> {
                this.state.let { currentState ->
                    val newProgress = (state.progress * 100).toInt()
                    val newDate = kit.lastBlockInfo?.timestamp?.let { Date(it * 1000) }

                    if (currentState is AdapterState.Syncing && currentState.progress == newProgress) {
                        val currentDate = currentState.lastBlockDate
                        if (newDate != null && currentDate != null && DateHelper.isSameDay(newDate, currentDate)) {
                            return
                        }
                    }

                    this.state = AdapterState.Syncing(newProgress, newDate)
                }
            }
        }
    }

    override fun onTransactionsUpdate(inserted: List<TransactionInfo>, updated: List<TransactionInfo>) {
        val records = mutableListOf<TransactionRecord>()

        for (info in inserted) {
            records.add(transactionRecord(info))
        }

        for (info in updated) {
            records.add(transactionRecord(info))
        }

        transactionRecordsSubject.onNext(records)
    }

    override fun onTransactionsDelete(hashes: List<String>) {
        // ignored for now
    }

    override fun getTransactions(from: TransactionRecord?, limit: Int): Single<List<TransactionRecord>> {
        return kit.transactions(from?.uid, limit).map { it.map { tx -> transactionRecord(tx) } }
    }

    companion object {

        private fun getNetworkType(testMode: Boolean) =
                if (testMode) NetworkType.TestNet else NetworkType.MainNet

        private fun createKit(wallet: Wallet, syncMode: SyncMode?, testMode: Boolean): BitcoinCashKit {
            val account = wallet.account
            val accountType = account.type
            if (accountType is AccountType.Mnemonic && accountType.words.size == 12) {
                return BitcoinCashKit(context = App.instance,
                        words = accountType.words,
                        walletId = account.id,
                        syncMode = getSyncMode(syncMode),
                        networkType = getNetworkType(testMode),
                        confirmationsThreshold = defaultConfirmationsThreshold)
            }

            throw UnsupportedAccountException()
        }

        fun clear(walletId: String, testMode: Boolean) {
            BitcoinCashKit.clear(App.instance, getNetworkType(testMode), walletId)
        }
    }
}
