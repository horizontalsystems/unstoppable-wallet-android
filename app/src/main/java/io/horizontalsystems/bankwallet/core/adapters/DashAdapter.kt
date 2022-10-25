package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISendBitcoinAdapter
import io.horizontalsystems.bankwallet.core.UnsupportedAccountException
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.models.BalanceInfo
import io.horizontalsystems.bitcoincore.models.BlockInfo
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.dashkit.DashKit
import io.horizontalsystems.dashkit.DashKit.NetworkType
import io.horizontalsystems.dashkit.models.DashTransactionInfo
import io.horizontalsystems.marketkit.models.BlockchainType
import java.math.BigDecimal

class DashAdapter(
        override val kit: DashKit,
        syncMode: BitcoinCore.SyncMode,
        backgroundManager: BackgroundManager,
        wallet: Wallet,
        testMode: Boolean
) : BitcoinBaseAdapter(kit, syncMode, backgroundManager, wallet, testMode), DashKit.Listener, ISendBitcoinAdapter {

    constructor(wallet: Wallet, syncMode: BitcoinCore.SyncMode, testMode: Boolean, backgroundManager: BackgroundManager) :
            this(createKit(wallet, syncMode, testMode), syncMode, backgroundManager, wallet, testMode)

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

    override val explorerTitle: String
        get() = "dash.org"

    override fun getTransactionUrl(transactionHash: String): String? =
        if (testMode) null else "https://insight.dash.org/insight/tx/$transactionHash"

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

    override val blockchainType = BlockchainType.Dash

    companion object {

        private fun getNetworkType(testMode: Boolean) =
                if (testMode) NetworkType.TestNet else NetworkType.MainNet

        private fun createKit(wallet: Wallet, syncMode: BitcoinCore.SyncMode, testMode: Boolean): DashKit {
            val account = wallet.account

            when (val accountType = account.type) {
                is AccountType.HdExtendedKey -> {
                    return DashKit(
                        context = App.instance,
                        extendedKey = accountType.hdExtendedKey,
                        walletId = account.id,
                        syncMode = syncMode,
                        networkType = getNetworkType(testMode),
                        confirmationsThreshold = confirmationsThreshold
                    )
                }
                is AccountType.Mnemonic -> {
                    return DashKit(
                        context = App.instance,
                        words = accountType.words,
                        passphrase = accountType.passphrase,
                        walletId = account.id,
                        syncMode = syncMode,
                        networkType = getNetworkType(testMode),
                        confirmationsThreshold = confirmationsThreshold
                    )
                }
                else -> throw UnsupportedAccountException()
            }
        }

        fun clear(walletId: String, testMode: Boolean) {
            DashKit.clear(App.instance, getNetworkType(testMode), walletId)
        }
    }
}
