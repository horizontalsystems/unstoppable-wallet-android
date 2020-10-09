package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISendBitcoinAdapter
import io.horizontalsystems.bankwallet.core.UnsupportedAccountException
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.models.BalanceInfo
import io.horizontalsystems.bitcoincore.models.BlockInfo
import io.horizontalsystems.bitcoincore.models.TransactionInfo
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.litecoinkit.LitecoinKit
import io.horizontalsystems.litecoinkit.LitecoinKit.NetworkType
import java.math.BigDecimal

class LitecoinAdapter(
        override val kit: LitecoinKit,
        derivation: AccountType.Derivation?,
        syncMode: SyncMode?,
        backgroundManager: BackgroundManager
) : BitcoinBaseAdapter(kit, derivation, syncMode, backgroundManager), LitecoinKit.Listener, ISendBitcoinAdapter {

    constructor(wallet: Wallet, derivation: AccountType.Derivation?, syncMode: SyncMode?, testMode: Boolean, backgroundManager: BackgroundManager) : this(createKit(wallet, derivation, syncMode, testMode), derivation, syncMode, backgroundManager)

    init {
        kit.listener = this
    }

    //
    // BitcoinBaseAdapter
    //

    override val satoshisInBitcoin: BigDecimal = BigDecimal.valueOf(Math.pow(10.0, decimal.toDouble()))

    override fun getReceiveAddressType(wallet: Wallet): String? {
        return derivation?.addressType()
    }

    //
    // LitecoinKit Listener
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

        private fun getNetworkType(testMode: Boolean) =
                if (testMode) NetworkType.TestNet else NetworkType.MainNet

        private fun createKit(wallet: Wallet, derivation: AccountType.Derivation?, syncMode: SyncMode?, testMode: Boolean): LitecoinKit {
            val account = wallet.account
            val accountType = account.type
            if (accountType is AccountType.Mnemonic && accountType.words.size == 12) {
                return LitecoinKit(context = App.instance,
                        words = accountType.words,
                        walletId = account.id,
                        syncMode = getSyncMode(syncMode),
                        networkType = getNetworkType(testMode),
                        confirmationsThreshold = confirmationsThreshold,
                        bip = getBip(derivation))
            }

            throw UnsupportedAccountException()
        }

        fun clear(walletId: String, testMode: Boolean) {
            LitecoinKit.clear(App.instance, getNetworkType(testMode), walletId)
        }
    }
}
