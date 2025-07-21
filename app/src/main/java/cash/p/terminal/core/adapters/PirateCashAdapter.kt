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
import io.horizontalsystems.bitcoincore.models.TransactionInfo
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.piratecashkit.PirateCashKit
import io.horizontalsystems.piratecashkit.PirateCashKit.NetworkType
import java.math.BigDecimal

class PirateCashAdapter(
    override val kit: PirateCashKit,
    syncMode: BitcoinCore.SyncMode,
    backgroundManager: BackgroundManager,
    wallet: Wallet
) : BitcoinBaseAdapter(kit, syncMode, backgroundManager, wallet, confirmationsThreshold),
    PirateCashKit.Listener, ISendBitcoinAdapter {

    constructor(
        wallet: Wallet,
        syncMode: BitcoinCore.SyncMode,
        backgroundManager: BackgroundManager
    ) : this(
        kit = createKit(wallet, syncMode),
        syncMode = syncMode,
        backgroundManager = backgroundManager,
        wallet = wallet
    )

    init {
        kit.listener = this
    }

    //
    // BitcoinBaseAdapter
    //

    override val satoshisInBitcoin: BigDecimal =
        BigDecimal.valueOf(Math.pow(10.0, decimal.toDouble()))

    //
    // io.horizontalsystems.piratecashkit.PirateCashKit Listener
    //

    override val explorerTitle: String
        get() = "piratecash.info"

    override fun getTransactionUrl(transactionHash: String): String =
        "https://piratecash.info/tx/$transactionHash"

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
        inserted: List<TransactionInfo>,
        updated: List<TransactionInfo>
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

    override val blockchainType = BlockchainType.PirateCash

    override fun usedAddresses(change: Boolean): List<UsedAddress> =
        kit.usedAddresses(change).map {
            UsedAddress(
                index = it.index,
                address = it.address,
                explorerUrl = "https://piratecash.info/address/${it.address}"
            )
        }

    companion object {
        private const val confirmationsThreshold = 3

        private fun createKit(
            wallet: Wallet,
            syncMode: BitcoinCore.SyncMode,
        ): PirateCashKit {
            val account = wallet.account

            when (val accountType = account.type) {
                is AccountType.HdExtendedKey -> {
                    return PirateCashKit(
                        context = App.instance,
                        extendedKey = accountType.hdExtendedKey,
                        walletId = account.id,
                        syncMode = syncMode,
                        networkType = NetworkType.MainNet,
                        confirmationsThreshold = confirmationsThreshold
                    )
                }

                is AccountType.Mnemonic -> {
                    return PirateCashKit(
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
                    return PirateCashKit(
                        context = App.instance,
                        watchAddress = accountType.address,
                        walletId = account.id,
                        syncMode = syncMode,
                        networkType = NetworkType.MainNet,
                        confirmationsThreshold = confirmationsThreshold
                    )
                }

                is AccountType.HardwareCard -> {
                    val hardwareWalletEcdaBitcoinSigner = buildHardwareWalletEcdaBitcoinSigner(
                        accountId = account.id,
                        blockchainType = wallet.token.blockchainType,
                        tokenType = wallet.token.type,
                    )
                    val hardwareWalletSchnorrSigner = buildHardwareWalletSchnorrBitcoinSigner(
                        accountId = account.id,
                        blockchainType = wallet.token.blockchainType,
                        tokenType = wallet.token.type,
                    )
                    return PirateCashKit(
                        context = App.instance,
                        extendedKey = wallet.getHDExtendedKey()!!,
                        walletId = account.id,
                        syncMode = syncMode,
                        iInputSigner = hardwareWalletEcdaBitcoinSigner,
                        iSchnorrInputSigner = hardwareWalletSchnorrSigner,
                    )
                }


                else -> throw UnsupportedAccountException()
            }
        }

        fun clear(walletId: String) {
            PirateCashKit.clear(App.instance, NetworkType.MainNet, walletId)
        }
    }
}
