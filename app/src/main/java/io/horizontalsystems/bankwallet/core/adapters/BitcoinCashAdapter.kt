package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.AdapterErrorWrongParameters
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISendBitcoinAdapter
import io.horizontalsystems.bankwallet.core.UnsupportedAccountException
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.BitcoinCashCoinType
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
import io.horizontalsystems.marketkit.models.BlockchainType
import java.math.BigDecimal

class BitcoinCashAdapter(
        override val kit: BitcoinCashKit,
        syncMode: BitcoinCore.SyncMode,
        backgroundManager: BackgroundManager,
        wallet: Wallet,
) : BitcoinBaseAdapter(kit, syncMode, backgroundManager, wallet, confirmationsThreshold), BitcoinCashKit.Listener, ISendBitcoinAdapter {

    constructor(
        wallet: Wallet,
        syncMode: BitcoinCore.SyncMode,
        backgroundManager: BackgroundManager
    ) : this(createKit(wallet, syncMode), syncMode, backgroundManager, wallet)

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

    override val explorerTitle: String
        get() = "btc.com"

    override fun getTransactionUrl(transactionHash: String): String =
        "https://bch.btc.com/$transactionHash"

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

    override val blockchainType = BlockchainType.BitcoinCash


    companion object {
        private const val confirmationsThreshold = 3

        private fun createKit(wallet: Wallet, syncMode: BitcoinCore.SyncMode): BitcoinCashKit {
            val account = wallet.account
            val bchCoinType = wallet.coinSettings.bitcoinCashCoinType ?: throw AdapterErrorWrongParameters("BitcoinCashCoinType is null")
            val kitCoinType = when(bchCoinType){
                BitcoinCashCoinType.type145 -> MainNetBitcoinCash.CoinType.Type145
                BitcoinCashCoinType.type0 -> MainNetBitcoinCash.CoinType.Type0
            }
            val networkType = getNetworkType(kitCoinType)
            when (val accountType = account.type) {
                is AccountType.HdExtendedKey -> {
                    return BitcoinCashKit(
                        context = App.instance,
                        extendedKey = accountType.hdExtendedKey,
                        walletId = account.id,
                        syncMode = syncMode,
                        networkType = networkType,
                        confirmationsThreshold = confirmationsThreshold
                    )
                }
                is AccountType.Mnemonic -> {
                    return BitcoinCashKit(
                        context = App.instance,
                        words = accountType.words,
                        passphrase = accountType.passphrase,
                        walletId = account.id,
                        syncMode = syncMode,
                        networkType = networkType,
                        confirmationsThreshold = confirmationsThreshold
                    )
                }
                else -> throw UnsupportedAccountException()
            }

        }

        fun clear(walletId: String) {
            BitcoinCashKit.clear(App.instance, getNetworkType(), walletId)
        }

        private fun getNetworkType(kitCoinType: MainNetBitcoinCash.CoinType = MainNetBitcoinCash.CoinType.Type145) =
            NetworkType.MainNet(kitCoinType)
    }
}
