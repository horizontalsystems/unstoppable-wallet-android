package cash.p.terminal.core.adapters

import android.util.Log
import cash.p.terminal.core.App
import cash.p.terminal.core.ISendBitcoinAdapter
import cash.p.terminal.core.UnsupportedAccountException
import cash.p.terminal.core.splitToAddresses
import cash.p.terminal.core.utils.Utils.getIpByUrl
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.network.pirate.domain.repository.MasterNodesRepository
import cash.p.terminal.wallet.AccountType
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.entities.UsedAddress
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.models.BalanceInfo
import io.horizontalsystems.bitcoincore.models.BlockInfo
import io.horizontalsystems.bitcoincore.models.TransactionInfo
import io.horizontalsystems.bitcoincore.storage.UnspentOutputInfo
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.dashkit.DashKit
import io.horizontalsystems.dashkit.DashKit.NetworkType
import io.horizontalsystems.dashkit.MainNetDash
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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
        customPeers: String,
        masterNodesRepository: MasterNodesRepository
    ) : this(
        kit = createKit(wallet, syncMode, customPeers, masterNodesRepository),
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
        private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        private fun createKit(
            wallet: Wallet,
            syncMode: BitcoinCore.SyncMode,
            userPeers: String,
            masterNodesRepository: MasterNodesRepository,
        ): DashKit {
            val account = wallet.account

            return when (val accountType = account.type) {
                is AccountType.HdExtendedKey -> {
                    DashKit(
                        context = App.instance,
                        extendedKey = accountType.hdExtendedKey,
                        walletId = account.id,
                        syncMode = syncMode,
                        networkType = NetworkType.MainNet,
                        confirmationsThreshold = confirmationsThreshold,
                        initWithEmptySeeds = true
                    )
                }

                is AccountType.Mnemonic -> {
                    DashKit(
                        context = App.instance,
                        words = accountType.words,
                        passphrase = accountType.passphrase,
                        walletId = account.id,
                        syncMode = syncMode,
                        networkType = NetworkType.MainNet,
                        confirmationsThreshold = confirmationsThreshold,
                        initWithEmptySeeds = true
                    )
                }

                is AccountType.BitcoinAddress -> {
                    DashKit(
                        context = App.instance,
                        watchAddress = accountType.address,
                        walletId = account.id,
                        syncMode = syncMode,
                        networkType = NetworkType.MainNet,
                        confirmationsThreshold = confirmationsThreshold,
                        initWithEmptySeeds = true
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
                    return DashKit(
                        context = App.instance,
                        extendedKey = wallet.getHDExtendedKey()!!,
                        walletId = account.id,
                        syncMode = syncMode,
                        networkType = NetworkType.MainNet,
                        confirmationsThreshold = confirmationsThreshold,
                        initWithEmptySeeds = true,
                        iInputSigner = hardwareWalletEcdaBitcoinSigner,
                        iSchnorrInputSigner = hardwareWalletSchnorrSigner
                    )
                }

                else -> throw UnsupportedAccountException()
            }.apply {
                setupPeers(masterNodesRepository, userPeers)
            }
        }

        fun clear(walletId: String) {
            DashKit.clear(App.instance, NetworkType.MainNet, walletId)
        }

        private fun DashKit.setupPeers(
            masterNodesRepository: MasterNodesRepository,
            userPeers: String
        ) {
            coroutineScope.launch(CoroutineExceptionHandler { _, throwable ->
                throwable.printStackTrace()
                Log.d("DashAdapter", "Failed to set peers", throwable)
            }) {
                trySetPeers(userPeers.splitToAddresses()) ||
                        trySetPeers(MainNetDash.defaultSeeds) ||
                        trySetPeers(masterNodesRepository.getMasterNodes().ips)
            }
        }

        private fun DashKit.trySetPeers(peers: List<String>) =
            peers.mapNotNull(::getIpByUrl).flatten().run {
                if (isNotEmpty()) {
                    addPeers(this)
                    true
                } else {
                    false
                }
            }
    }
}
