package cash.p.terminal.core.adapters

import cash.p.terminal.core.App
import cash.p.terminal.core.ISendBitcoinAdapter
import cash.p.terminal.core.UnsupportedAccountException
import cash.p.terminal.core.derivation
import cash.p.terminal.wallet.entities.UsedAddress
import cash.p.terminal.core.purpose
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.wallet.AccountType
import cash.p.terminal.wallet.Wallet
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.models.BalanceInfo
import io.horizontalsystems.bitcoincore.models.BlockInfo
import io.horizontalsystems.bitcoincore.models.TransactionInfo
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.litecoinkit.LitecoinKit
import io.horizontalsystems.litecoinkit.LitecoinKit.NetworkType
import io.horizontalsystems.core.entities.BlockchainType
import cash.p.terminal.wallet.entities.TokenType
import java.math.BigDecimal
import kotlin.math.pow

class LitecoinAdapter(
    override val kit: LitecoinKit,
    syncMode: BitcoinCore.SyncMode,
    backgroundManager: BackgroundManager,
    wallet: Wallet,
) : BitcoinBaseAdapter(kit, syncMode, backgroundManager, wallet, confirmationsThreshold), LitecoinKit.Listener, ISendBitcoinAdapter {

    constructor(
        wallet: Wallet,
        syncMode: BitcoinCore.SyncMode,
        backgroundManager: BackgroundManager,
        derivation: TokenType.Derivation
    ) : this(createKit(wallet, syncMode, derivation), syncMode, backgroundManager, wallet)

    init {
        kit.listener = this
    }

    //
    // BitcoinBaseAdapter
    //

    override val satoshisInBitcoin: BigDecimal = BigDecimal.valueOf(10.0.pow(decimal.toDouble()))

    //
    // LitecoinKit Listener
    //

    override val explorerTitle: String
        get() = "blockchair.com"


    override fun getTransactionUrl(transactionHash: String): String =
        "https://blockchair.com/litecoin/transaction/$transactionHash"

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

    override val blockchainType = BlockchainType.Litecoin

    override fun usedAddresses(change: Boolean): List<UsedAddress> =
        kit.usedAddresses(change).map { UsedAddress(it.index, it.address, "https://blockchair.com/litecoin/address/${it.address}") }


    companion object {
        private const val confirmationsThreshold = 3

        private fun createKit(
            wallet: Wallet,
            syncMode: BitcoinCore.SyncMode,
            derivation: TokenType.Derivation,
        ): LitecoinKit {
            val account = wallet.account

            when (val accountType = account.type) {
                is AccountType.HdExtendedKey -> {
                    return LitecoinKit(
                        context = App.instance,
                        extendedKey = accountType.hdExtendedKey,
                        purpose = derivation.purpose,
                        walletId = account.id,
                        syncMode = syncMode,
                        networkType = NetworkType.MainNet,
                        confirmationsThreshold = confirmationsThreshold,
                    )
                }
                is AccountType.Mnemonic -> {
                    return LitecoinKit(
                        context = App.instance,
                        words = accountType.words,
                        passphrase = accountType.passphrase,
                        walletId = account.id,
                        syncMode = syncMode,
                        networkType = NetworkType.MainNet,
                        confirmationsThreshold = confirmationsThreshold,
                        purpose = derivation.purpose
                    )
                }
                is AccountType.BitcoinAddress -> {
                    return LitecoinKit(
                        context = App.instance,
                        watchAddress =  accountType.address,
                        walletId = account.id,
                        syncMode = syncMode,
                        networkType = NetworkType.MainNet,
                        confirmationsThreshold = confirmationsThreshold
                    )
                }
                is AccountType.HardwareCard -> {
                    val hardwareWalletEcdaBitcoinSigner = buildHardwareWalletEcdaBitcoinSigner(
                        accountId = account.id,
                        cardId = accountType.cardId,
                        blockchainType = wallet.token.blockchainType,
                        tokenType = wallet.token.type,
                    )
                    val hardwareWalletSchnorrSigner = buildHardwareWalletSchnorrBitcoinSigner(
                        accountId = account.id,
                        cardId = accountType.cardId,
                        blockchainType = wallet.token.blockchainType,
                        tokenType = wallet.token.type,
                    )
                    return LitecoinKit(
                        context = App.instance,
                        extendedKey = wallet.getHDExtendedKey()!!,
                        purpose = derivation.purpose,
                        walletId = account.id,
                        syncMode = syncMode,
                        networkType = NetworkType.MainNet,
                        confirmationsThreshold = confirmationsThreshold,
                        iInputSigner = hardwareWalletEcdaBitcoinSigner,
                        iSchnorrInputSigner = hardwareWalletSchnorrSigner
                    )
                }
                else -> throw UnsupportedAccountException()
            }
        }

        fun clear(walletId: String) {
            LitecoinKit.clear(App.instance, NetworkType.MainNet, walletId)
        }

        fun firstAddress(accountType: AccountType, tokenType: TokenType) : String {
            when (accountType) {
                is AccountType.Mnemonic -> {
                    val seed = accountType.seed
                    val derivation = tokenType.derivation ?: throw IllegalArgumentException()

                    val address = LitecoinKit.firstAddress(
                        seed,
                        derivation.purpose,
                        NetworkType.MainNet
                    )

                    return address.stringValue
                }
                is AccountType.HdExtendedKey -> {
                    val key = accountType.hdExtendedKey
                    val derivation = tokenType.derivation ?: throw IllegalArgumentException()
                    val address = LitecoinKit.firstAddress(
                        key,
                        derivation.purpose,
                        NetworkType.MainNet
                    )

                    return address.stringValue
                }
                is AccountType.BitcoinAddress -> {
                    return accountType.address
                }
                is AccountType.Cex,
                is AccountType.EvmAddress,
                is AccountType.EvmPrivateKey,
                is AccountType.HardwareCard,
                is AccountType.MnemonicMonero,
                is AccountType.SolanaAddress,
                is AccountType.TonAddress,
                is AccountType.TronAddress,
                is AccountType.ZCashUfvKey -> throw UnsupportedAccountException()
            }
        }

    }
}
