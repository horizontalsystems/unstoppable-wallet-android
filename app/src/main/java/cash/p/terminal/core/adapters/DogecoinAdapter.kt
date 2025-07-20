package cash.p.terminal.core.adapters

import cash.p.dogecoinkit.DogecoinKit
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
import java.math.BigDecimal
import kotlin.math.pow

class DogecoinAdapter(
    override val kit: DogecoinKit,
    syncMode: BitcoinCore.SyncMode,
    backgroundManager: BackgroundManager,
    wallet: Wallet,
) : BitcoinBaseAdapter(kit, syncMode, backgroundManager, wallet, confirmationsThreshold),
    DogecoinKit.Listener, ISendBitcoinAdapter {

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

    override val satoshisInBitcoin: BigDecimal = BigDecimal.valueOf(10.0.pow(decimal.toDouble()))

    //
    // DogecoinKit Listener
    //

    override val explorerTitle: String
        get() = "blockchair.com"


    override fun getTransactionUrl(transactionHash: String): String =
        "https://blockchair.com/dogecoin/transaction/$transactionHash"

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

    override val blockchainType = BlockchainType.Dogecoin

    override fun usedAddresses(change: Boolean): List<UsedAddress> =
        kit.usedAddresses(change).map {
            UsedAddress(
                it.index,
                it.address,
                "https://blockchair.com/dogecoin/address/${it.address}"
            )
        }


    companion object {
        private const val confirmationsThreshold = 3

        private fun createKit(
            wallet: Wallet,
            syncMode: BitcoinCore.SyncMode,
        ): DogecoinKit {
            val account = wallet.account

            when (val accountType = account.type) {
                is AccountType.Mnemonic -> {
                    return DogecoinKit(
                        context = App.instance,
                        words = accountType.words,
                        passphrase = accountType.passphrase,
                        walletId = account.id,
                        syncMode = syncMode,
                        networkType = DogecoinKit.NetworkType.MainNet,
                        confirmationsThreshold = confirmationsThreshold,
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
                    return DogecoinKit(
                        context = App.instance,
                        extendedKey = wallet.getHDExtendedKey()!!,
                        walletId = account.id,
                        syncMode = syncMode,
                        networkType = DogecoinKit.NetworkType.MainNet,
                        confirmationsThreshold = confirmationsThreshold,
                        iInputSigner = hardwareWalletEcdaBitcoinSigner,
                        iSchnorrInputSigner = hardwareWalletSchnorrSigner
                    )
                }

                else -> throw UnsupportedAccountException()
            }
        }

        fun clear(walletId: String) {
            DogecoinKit.clear(App.instance, DogecoinKit.NetworkType.MainNet, walletId)
        }
    }
}
