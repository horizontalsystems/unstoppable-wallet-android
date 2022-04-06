package io.horizontalsystems.bankwallet.modules.syncerror

import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.managers.BtcBlockchainManager
import io.horizontalsystems.bankwallet.core.managers.EvmBlockchainManager
import io.horizontalsystems.bankwallet.entities.Wallet

class SyncErrorService(
    private val wallet: Wallet,
    private val adapterManager: IAdapterManager,
    val reportEmail: String,
    private val btcBlockchainManager: BtcBlockchainManager,
    private val evmBlockchainManager: EvmBlockchainManager
) {

    val blockchain by lazy {
        btcBlockchainManager.blockchain(wallet.coinType)?.let {
            SyncErrorModule.Blockchain.Btc(it)
        } ?: run {
            evmBlockchainManager.getBlockchain(wallet.coinType)?.let {
                SyncErrorModule.Blockchain.Evm(it)
            }
        }
    }

    val coinName: String = wallet.coin.name

    val sourceChangeable = blockchain != null

    fun retry() {
        adapterManager.refreshByWallet(wallet)
    }
}
