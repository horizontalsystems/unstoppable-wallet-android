package io.horizontalsystems.bankwallet.core.adapters

import android.content.Context
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.utils.AddressParser
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.SyncMode
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.viewHelpers.DateHelper
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.models.BlockInfo
import io.horizontalsystems.bitcoincore.models.TransactionInfo
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType
import io.horizontalsystems.bitcoinkit.BitcoinKit
import io.horizontalsystems.bitcoinkit.BitcoinKit.NetworkType
import io.reactivex.Single
import java.math.BigDecimal
import java.util.*

class BitcoinAdapter(wallet: Wallet, override val kit: BitcoinKit, addressParser: AddressParser, private val feeRateProvider: IFeeRateProvider)
    : BitcoinBaseAdapter(wallet, kit, addressParser), BitcoinKit.Listener {

    constructor(wallet: Wallet, testMode: Boolean, feeRateProvider: IFeeRateProvider) :
            this(wallet, createKit(wallet, testMode), AddressParser("bitcoin", true), feeRateProvider)

    init {
        kit.listener = this
    }

    //
    // BitcoinBaseAdapter
    //

    override val receiveScriptType = ScriptType.P2WPKHSH
    override val changeScriptType = ScriptType.P2WPKH
    override val satoshisInBitcoin: BigDecimal = BigDecimal.valueOf(Math.pow(10.0, decimal.toDouble()))

    override fun getFeeRate(feeRatePriority: FeeRatePriority): Long {
        return feeRateProvider.bitcoinFeeRate(feeRatePriority)
    }

    //
    // BitcoinKit Listener
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

    override fun getTransactions(from: Pair<String, Int>?, limit: Int): Single<List<TransactionRecord>> {
        return kit.transactions(from?.first, limit).map { it.map { tx -> transactionRecord(tx) } }
    }

    companion object {

        private fun getNetworkType(testMode: Boolean) =
                if (testMode) NetworkType.TestNet else NetworkType.MainNet


        private fun createKit(wallet: Wallet, testMode: Boolean): BitcoinKit {
            val account = wallet.account
            if (account.type is AccountType.Mnemonic) {
                return BitcoinKit(App.instance, account.type.words, account.id, syncMode = SyncMode.fromSyncMode(account.defaultSyncMode), networkType = getNetworkType(testMode))
            }

            throw UnsupportedAccountException()
        }

        fun clear(context: Context, walletId: String, testMode: Boolean) {
            BitcoinKit.clear(context, getNetworkType(testMode), walletId)
        }
    }
}
