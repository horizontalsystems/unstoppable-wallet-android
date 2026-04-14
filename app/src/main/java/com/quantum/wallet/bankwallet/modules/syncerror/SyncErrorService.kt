package com.quantum.wallet.bankwallet.modules.syncerror

import com.quantum.wallet.bankwallet.core.IAdapterManager
import com.quantum.wallet.bankwallet.core.managers.BtcBlockchainManager
import com.quantum.wallet.bankwallet.core.managers.EvmBlockchainManager
import com.quantum.wallet.bankwallet.entities.Wallet
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
            BlockchainType.Tron -> SyncErrorModule.BlockchainWrapper.Evm(wallet.token.blockchain)
            else -> {
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
