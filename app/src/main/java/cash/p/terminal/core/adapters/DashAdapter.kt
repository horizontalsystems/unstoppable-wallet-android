package cash.p.terminal.core.adapters

import cash.p.terminal.core.App
import cash.p.terminal.core.ISendBitcoinAdapter
import cash.p.terminal.core.UnsupportedAccountException
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.wallet.AccountType
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.entities.UsedAddress
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.models.BalanceInfo
import io.horizontalsystems.bitcoincore.models.BlockInfo
import io.horizontalsystems.bitcoincore.storage.UnspentOutputInfo
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.entities.BlockchainType
import cash.p.terminal.core.adapters.dash.DashKit
import io.horizontalsystems.dashkit.DashKit.NetworkType
import io.horizontalsystems.dashkit.models.DashTransactionInfo
import java.math.BigDecimal

class DashAdapter(
    override val kit: DashKit,
    syncMode: BitcoinCore.SyncMode,
    backgroundManager: BackgroundManager,
    wallet: Wallet
) : BitcoinBaseAdapter(kit, syncMode, backgroundManager, wallet, confirmationsThreshold),
    DashKit.Listener, ISendBitcoinAdapter {

    constructor(
        wallet: Wallet,
        syncMode: BitcoinCore.SyncMode,
        backgroundManager: BackgroundManager,
        customPeers: String
    ) : this(createKit(wallet, syncMode, customPeers), syncMode, backgroundManager, wallet)

    init {
        kit.listener = this
    }

    //
    // BitcoinBaseAdapter
    //

    override val satoshisInBitcoin: BigDecimal =
        BigDecimal.valueOf(Math.pow(10.0, decimal.toDouble()))

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

    override fun onTransactionsUpdate(
        inserted: List<DashTransactionInfo>,
        updated: List<DashTransactionInfo>
    ) {
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

    override val unspentOutputs: List<UnspentOutputInfo>
        get() = kit.unspentOutputs

    override val blockchainType = BlockchainType.Dash

    override fun usedAddresses(change: Boolean): List<UsedAddress> =
        kit.usedAddresses(change).map {
            UsedAddress(
                index = it.index,
                address = it.address,
                explorerUrl = "https://blockchair.com/dash/address/${it.address}"
            )
        }

    companion object {
        private const val confirmationsThreshold = 3

        private fun createKit(wallet: Wallet, syncMode: BitcoinCore.SyncMode, customPeers: String): DashKit {
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
                    ).apply {
                        addPeers(prepareCustomPeers(customPeers))
                    }
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
                    ).apply {
                        addPeers(prepareCustomPeers(customPeers))
                    }
                }

                is AccountType.BitcoinAddress -> {
                    return DashKit(
                        context = App.instance,
                        watchAddress = accountType.address,
                        walletId = account.id,
                        syncMode = syncMode,
                        networkType = NetworkType.MainNet,
                        confirmationsThreshold = confirmationsThreshold
                    ).apply {
                        addPeers(prepareCustomPeers(customPeers))
                    }
                }

                else -> throw UnsupportedAccountException()
            }
        }

        private fun prepareCustomPeers(customPeers: String): List<String> {
            return customPeers.split(",").map { it.trim() }
        }

        fun clear(walletId: String) {
            DashKit.clear(App.instance, NetworkType.MainNet, walletId)
        }
    }
}
