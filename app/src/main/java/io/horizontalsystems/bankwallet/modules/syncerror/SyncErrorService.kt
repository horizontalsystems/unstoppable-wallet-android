package io.horizontalsystems.bankwallet.modules.syncerror

import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.managers.BtcBlockchainManager
import io.horizontalsystems.bankwallet.core.managers.EvmBlockchainManager
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.marketkit.models.BlockchainType

class SyncErrorService(
    private val wallet: Wallet,
    private val adapterManager: IAdapterManager,
    val reportEmail: String,
    private val btcBlockchainManager: BtcBlockchainManager,
    private val evmBlockchainManager: EvmBlockchainManager
) {

    val blockchainWrapper by lazy {
        if (wallet.token.blockchainType == BlockchainType.Monero) {
            SyncErrorModule.BlockchainWrapper.Monero
        } else {
            btcBlockchainManager.blockchain(wallet.token.blockchainType)?.let {
                SyncErrorModule.BlockchainWrapper.Bitcoin(it)
            } ?: run {
                evmBlockchainManager.getBlockchain(wallet.token)?.let {
                    SyncErrorModule.BlockchainWrapper.Evm(it)
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
