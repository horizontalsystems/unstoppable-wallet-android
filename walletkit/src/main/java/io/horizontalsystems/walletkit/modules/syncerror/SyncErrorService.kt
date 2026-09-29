package io.horizontalsystems.walletkit.modules.syncerror

import io.horizontalsystems.walletkit.core.AdapterState
import io.horizontalsystems.walletkit.core.IAdapterManager
import io.horizontalsystems.walletkit.core.ITorManager
import io.horizontalsystems.walletkit.core.TorUnsupportedException
import io.horizontalsystems.walletkit.core.managers.BtcBlockchainManager
import io.horizontalsystems.walletkit.core.managers.EvmBlockchainManager
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SyncErrorService(
    private val wallet: Wallet,
    private val adapterManager: IAdapterManager,
    val reportEmail: String,
    private val btcBlockchainManager: BtcBlockchainManager,
    private val evmBlockchainManager: EvmBlockchainManager,
    private val torManager: ITorManager,
) {

    val blockchainWrapper by lazy {
        when (wallet.token.blockchainType) {
            BlockchainType.Tron -> SyncErrorModule.BlockchainWrapper.Evm(wallet.token.blockchain)
            else -> {
                ChainRegistry[wallet.token.blockchainType]?.networkSettingsPage()?.let {
                    return@lazy SyncErrorModule.BlockchainWrapper.ChainPage(it)
                }
                btcBlockchainManager.blockchain(wallet.token.blockchainType)?.let {
                    SyncErrorModule.BlockchainWrapper.Bitcoin(it)
                } ?: run {
                    evmBlockchainManager.getBlockchain(wallet.token)?.let {
                        SyncErrorModule.BlockchainWrapper.Evm(it)
                    }
                }
            }
        }
    }

    val coinName: String = wallet.coin.name

    val torUnsupported: Boolean
        get() = (adapterManager.getBalanceAdapterForWallet(wallet)?.balanceState as? AdapterState.NotSynced)
            ?.error is TorUnsupportedException

    val sourceChangeable = blockchainWrapper != null

    // The sheet closes as soon as retry is tapped, so the retry must not be tied to its lifecycle
    fun retry() {
        CoroutineScope(Dispatchers.Default).launch {
            if (torManager.isTorEnabled) {
                torManager.newCircuits()
            }
            adapterManager.refreshByWallet(wallet)
        }
    }
}
