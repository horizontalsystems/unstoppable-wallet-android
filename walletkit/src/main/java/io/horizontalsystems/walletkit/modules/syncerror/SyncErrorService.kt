package io.horizontalsystems.walletkit.modules.syncerror

import io.horizontalsystems.walletkit.core.IAdapterManager
import io.horizontalsystems.walletkit.core.managers.BtcBlockchainManager
import io.horizontalsystems.walletkit.core.managers.EvmBlockchainManager
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.marketkit.models.BlockchainType

class SyncErrorService(
    private val wallet: Wallet,
    private val adapterManager: IAdapterManager,
    val reportEmail: String,
    private val btcBlockchainManager: BtcBlockchainManager,
    private val evmBlockchainManager: EvmBlockchainManager
) {

    val blockchainWrapper by lazy {
        when (wallet.token.blockchainType) {
            BlockchainType.Monero -> SyncErrorModule.BlockchainWrapper.Monero
            BlockchainType.Zcash -> SyncErrorModule.BlockchainWrapper.Zcash
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

    val sourceChangeable = blockchainWrapper != null

    fun retry() {
        adapterManager.refreshByWallet(wallet)
    }
}
