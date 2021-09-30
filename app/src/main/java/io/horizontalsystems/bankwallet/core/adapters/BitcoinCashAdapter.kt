package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.AdapterErrorWrongParameters
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISendBitcoinAdapter
import io.horizontalsystems.bankwallet.core.UnsupportedAccountException
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.BitcoinCashCoinType
import io.horizontalsystems.bankwallet.entities.SyncMode
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bitcoincash.BitcoinCashKit
import io.horizontalsystems.bitcoincash.BitcoinCashKit.NetworkType
import io.horizontalsystems.bitcoincash.MainNetBitcoinCash
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.models.BalanceInfo
import io.horizontalsystems.bitcoincore.models.BlockInfo
import io.horizontalsystems.bitcoincore.models.TransactionInfo
import io.horizontalsystems.core.BackgroundManager
import java.math.BigDecimal

class BitcoinCashAdapter(
        override val kit: BitcoinCashKit,
        syncMode: SyncMode?,
        backgroundManager: BackgroundManager,
        wallet: Wallet,
        testMode: Boolean
) : BitcoinBaseAdapter(kit, syncMode, backgroundManager, wallet, testMode), BitcoinCashKit.Listener, ISendBitcoinAdapter {

    constructor(wallet: Wallet, syncMode: SyncMode?, testMode: Boolean, backgroundManager: BackgroundManager) :
            this(createKit(wallet, syncMode, testMode), syncMode, backgroundManager, wallet, testMode)

    init {
        kit.listener = this
    }

    //
    // BitcoinBaseAdapter
    //

    override val satoshisInBitcoin: BigDecimal = BigDecimal.valueOf(Math.pow(10.0, decimal.toDouble()))

    // ITransactionsAdapter

    override val explorerTitle: String = "btc.com"

    override fun explorerUrl(transactionHash: String): String? {
        return if (testMode) null else "https://bch.btc.com/$transactionHash"
    }

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
        setState(state)
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

    companion object {

        private fun createKit(wallet: Wallet, syncMode: SyncMode?, testMode: Boolean): BitcoinCashKit {
            val account = wallet.account
            val accountType = (account.type as? AccountType.Mnemonic) ?: throw UnsupportedAccountException()
            val bchCoinType = wallet.coinSettings.bitcoinCashCoinType ?: throw AdapterErrorWrongParameters("BitcoinCashCoinType is null")
            val kitCoinType = when(bchCoinType){
                BitcoinCashCoinType.type145 -> MainNetBitcoinCash.CoinType.Type145
                BitcoinCashCoinType.type0 -> MainNetBitcoinCash.CoinType.Type0
            }

            return BitcoinCashKit(context = App.instance,
                    words = accountType.words,
                    passphrase = accountType.passphrase,
                    walletId = account.id,
                    syncMode = getSyncMode(syncMode ?: SyncMode.Slow),
                    networkType = getNetworkType(testMode, kitCoinType),
                    confirmationsThreshold = confirmationsThreshold)
        }

        fun clear(walletId: String, testMode: Boolean) {
            BitcoinCashKit.clear(App.instance, getNetworkType(testMode), walletId)
        }

        private fun getNetworkType(testMode: Boolean, kitCoinType: MainNetBitcoinCash.CoinType = MainNetBitcoinCash.CoinType.Type145) =
                if (testMode) NetworkType.TestNet else NetworkType.MainNet(kitCoinType)
    }
}
