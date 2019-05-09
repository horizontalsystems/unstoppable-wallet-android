package io.horizontalsystems.bankwallet.core

import android.content.Context
import io.horizontalsystems.bankwallet.core.utils.AddressParser
import io.horizontalsystems.bankwallet.entities.AuthData
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.bankwallet.viewHelpers.DateHelper
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.models.BlockInfo
import io.horizontalsystems.dashkit.DashKit
import io.horizontalsystems.dashkit.DashKit.NetworkType
import io.horizontalsystems.dashkit.models.DashTransactionInfo
import io.reactivex.Single
import java.math.BigDecimal
import java.util.*

class DashAdapter(coin: Coin, override val kit: DashKit, addressParser: AddressParser, private val feeRateProvider: IFeeRateProvider) :
        BitcoinBaseAdapter(coin, kit, addressParser), DashKit.Listener {

    constructor(coin: Coin, authData: AuthData, newWallet: Boolean, testMode: Boolean, feeRateProvider: IFeeRateProvider) :
            this(coin, createKit(authData, newWallet, testMode), AddressParser("dash", true), feeRateProvider)


    init {
        kit.listener = this
    }

    //
    // BitcoinBaseAdapter
    //

    override val satoshisInBitcoin: BigDecimal = BigDecimal.valueOf(Math.pow(10.0, decimal.toDouble()))

    override fun feeRate(feePriority: FeeRatePriority): Int {
        return feeRateProvider.dashFeeRate(feePriority).toInt()
    }

    //
    // DashKit Listener
    //

    override fun onBalanceUpdate(balance: Long) {
        balanceUpdatedSubject.onNext(Unit)
    }

    override fun onLastBlockInfoUpdate(blockInfo: BlockInfo) {
        lastBlockHeightUpdatedSubject.onNext(Unit)
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
                    this.state = AdapterState.NotSynced
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

    override fun onTransactionsUpdate(inserted: List<DashTransactionInfo>, updated: List<DashTransactionInfo>) {
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

    override fun getTransactions(from: Pair<String, Int>?, limit: Int): Single<List<TransactionRecord>> {
        return kit.transactions(from?.first, limit).map { it.map { tx -> transactionRecord(tx) } }
    }

    companion object {

        private fun getNetworkType(testMode: Boolean) =
                if (testMode) NetworkType.TestNet else NetworkType.MainNet

        private fun createKit(authData: AuthData, newWallet: Boolean, testMode: Boolean): DashKit {
            return DashKit(App.instance, authData.words, authData.walletId, newWallet = newWallet, networkType = getNetworkType(testMode))
        }

        fun clear(context: Context, walletId: String, testMode: Boolean) {
            DashKit.clear(context, getNetworkType(testMode), walletId)
        }
    }
}
