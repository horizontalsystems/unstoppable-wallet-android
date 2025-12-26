package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BackgroundManager
import io.horizontalsystems.bankwallet.core.ISendBitcoinAdapter
import io.horizontalsystems.bankwallet.core.UnsupportedAccountException
import io.horizontalsystems.bankwallet.core.UsedAddress
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.models.BalanceInfo
import io.horizontalsystems.bitcoincore.models.BlockInfo
import io.horizontalsystems.dashkit.DashKit
import io.horizontalsystems.dashkit.DashKit.NetworkType
import io.horizontalsystems.dashkit.models.DashTransactionInfo
import io.horizontalsystems.marketkit.models.BlockchainType

class DashAdapter(
        override val kit: DashKit,
        syncMode: BitcoinCore.SyncMode,
        backgroundManager: BackgroundManager,
        wallet: Wallet,
) : BitcoinBaseAdapter(kit, syncMode, backgroundManager, wallet), DashKit.Listener, ISendBitcoinAdapter {

    constructor(wallet: Wallet, syncMode: BitcoinCore.SyncMode, backgroundManager: BackgroundManager) :
            this(createKit(wallet, syncMode), syncMode, backgroundManager, wallet)

    init {
        kit.listener = this
    }

    //
    // DashKit Listener
    //

    override val explorerTitle: String
        get() = "blockchair.com"

    override fun getTransactionUrl(transactionHash: String): String =
        "https://blockchair.com/dash/transaction/$transactionHash"

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

    override fun usedAddresses(change: Boolean): List<UsedAddress> =
        kit.usedAddresses(change).map { UsedAddress(it.index, it.address, "https://blockchair.com/dash/address/${it.address}") }

    companion object {
        private const val confirmationsThreshold = 1

        fun firstAddress(accountType: AccountType): String {
            when (accountType) {
                is AccountType.Mnemonic -> {
                    val address = DashKit.firstAddress(
                        accountType.seed,
                        NetworkType.MainNet
                    )

                    return address.stringValue
                }
                is AccountType.HdExtendedKey -> {
                    val address = DashKit.firstAddress(
                        accountType.hdExtendedKey,
                        NetworkType.MainNet
                    )

                    return address.stringValue
                }
                is AccountType.BitcoinAddress -> {
                    return accountType.address
                }
                else -> throw UnsupportedAccountException()
            }
        }

        private fun createKit(wallet: Wallet, syncMode: BitcoinCore.SyncMode): DashKit {
            val account = wallet.account

            when (val accountType = account.type) {
                is AccountType.HdExtendedKey -> {
                    return DashKit(
                        context = App.instance,
                        extendedKey = accountType.hdExtendedKey,
                        walletId = account.id,
                        syncMode = syncMode,
                        networkType = NetworkType.MainNet,
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
                        networkType = NetworkType.MainNet,
                        confirmationsThreshold = confirmationsThreshold
                    )
                }
                is AccountType.BitcoinAddress -> {
                    return DashKit(
                        context = App.instance,
                        watchAddress = accountType.address,
                        walletId = account.id,
                        syncMode = syncMode,
                        networkType = NetworkType.MainNet,
                        confirmationsThreshold = confirmationsThreshold
                    )
                }
                else -> throw UnsupportedAccountException()
            }
        }

        fun clear(walletId: String) {
            DashKit.clear(App.instance, NetworkType.MainNet, walletId)
        }
    }
}
