package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.ISendDashAdapter
import io.horizontalsystems.bankwallet.core.UnsupportedAccountException
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.SyncMode
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.models.BalanceInfo
import io.horizontalsystems.bitcoincore.models.BlockInfo
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.dashkit.DashKit
import io.horizontalsystems.dashkit.DashKit.NetworkType
import io.horizontalsystems.dashkit.models.DashTransactionInfo
import io.reactivex.Single
import java.math.BigDecimal

class DashAdapter(
        override val kit: DashKit,
        syncMode: SyncMode?,
        backgroundManager: BackgroundManager
) : BitcoinBaseAdapter(kit, syncMode = syncMode, backgroundManager = backgroundManager), DashKit.Listener, ISendDashAdapter {

    constructor(wallet: Wallet, syncMode: SyncMode?, testMode: Boolean, backgroundManager: BackgroundManager) :
            this(createKit(wallet, syncMode, testMode), syncMode, backgroundManager)

    init {
        kit.listener = this
    }

    //
    // BitcoinBaseAdapter
    //

    override val satoshisInBitcoin: BigDecimal = BigDecimal.valueOf(Math.pow(10.0, decimal.toDouble()))

    //
    // DashKit Listener
    //

    override fun onBalanceUpdate(balance: BalanceInfo) {
        balanceUpdatedSubject.onNext(Unit)
    }

    override fun onLastBlockInfoUpdate(blockInfo: BlockInfo) {
        lastBlockUpdatedSubject.onNext(Unit)
    }

    override fun onKitStateUpdate(state: BitcoinCore.KitState) {
        setState(state)
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

    // ISendDashAdapter

    override fun availableBalance(address: String?): BigDecimal {
        return availableBalance(feeRate, address, mapOf())
    }

    override fun fee(amount: BigDecimal, address: String?): BigDecimal {
        return fee(amount, feeRate, address, mapOf())
    }

    override fun validate(address: String) {
        validate(address, mapOf())
    }

    override fun send(amount: BigDecimal, address: String, logger: AppLogger): Single<Unit> {
        return send(amount, address, feeRate, mapOf(), null, logger)
    }

    companion object {

        private const val feeRate = 1L

        private fun getNetworkType(testMode: Boolean) =
                if (testMode) NetworkType.TestNet else NetworkType.MainNet

        private fun createKit(wallet: Wallet, syncMode: SyncMode?, testMode: Boolean): DashKit {
            val account = wallet.account
            val accountType = account.type
            if (accountType is AccountType.Mnemonic && accountType.words.size == 12) {
                return DashKit(context = App.instance,
                        words = accountType.words,
                        walletId = account.id,
                        syncMode = getSyncMode(syncMode),
                        networkType = getNetworkType(testMode),
                        confirmationsThreshold = confirmationsThreshold)
            }

            throw UnsupportedAccountException()
        }

        fun clear(walletId: String, testMode: Boolean) {
            DashKit.clear(App.instance, getNetworkType(testMode), walletId)
        }
    }
}
