package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.AdapterErrorWrongParameters
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISendBitcoinAdapter
import io.horizontalsystems.bankwallet.core.UnsupportedAccountException
import io.horizontalsystems.bankwallet.entities.*
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
        backgroundManager: BackgroundManager
) : BitcoinBaseAdapter(kit, syncMode = syncMode, backgroundManager = backgroundManager), BitcoinCashKit.Listener, ISendBitcoinAdapter {

    constructor(wallet: Wallet, syncMode: SyncMode?, bitcoinCashCoinType: BitcoinCashCoinType?, testMode: Boolean, backgroundManager: BackgroundManager) :
            this(createKit(wallet, syncMode, bitcoinCashCoinType, testMode), syncMode, backgroundManager)

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

        private fun createKit(wallet: Wallet, syncMode: SyncMode?, bitcoinCashCoinType: BitcoinCashCoinType?, testMode: Boolean): BitcoinCashKit {
            val account = wallet.account
            val accountType = account.type
            val bchCoinType = bitcoinCashCoinType ?: throw AdapterErrorWrongParameters("BitcoinCashCoinType is null")
            val kitCoinType = when(bchCoinType){
                BitcoinCashCoinType.type145 -> MainNetBitcoinCash.CoinType.Type145
                BitcoinCashCoinType.type0 -> MainNetBitcoinCash.CoinType.Type0
            }

            if (accountType is AccountType.Mnemonic && accountType.words.size == 12) {
                return BitcoinCashKit(context = App.instance,
                        words = accountType.words,
                        walletId = account.id,
                        syncMode = getSyncMode(syncMode ?: SyncMode.Slow),
                        networkType = getNetworkType(testMode, kitCoinType),
                        confirmationsThreshold = confirmationsThreshold)
            }

            throw UnsupportedAccountException()
        }

        fun clear(walletId: String, testMode: Boolean) {
            BitcoinCashKit.clear(App.instance, getNetworkType(testMode), walletId)
        }

        private fun getNetworkType(testMode: Boolean, kitCoinType: MainNetBitcoinCash.CoinType = MainNetBitcoinCash.CoinType.Type145) =
                if (testMode) NetworkType.TestNet else NetworkType.MainNet(kitCoinType)
    }
}
