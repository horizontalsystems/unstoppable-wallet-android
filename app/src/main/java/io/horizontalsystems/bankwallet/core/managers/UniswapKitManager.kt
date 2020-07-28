package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.EthereumKitNotCreated
import io.horizontalsystems.bankwallet.core.IEthereumKitManager
import io.horizontalsystems.bankwallet.core.IUniswapKitManager
import io.horizontalsystems.uniswapkit.UniswapKit

class UniswapKitManager(private val ethereumKitManager: IEthereumKitManager) : IUniswapKitManager {
    private var kit: UniswapKit? = null

    override fun uniswapKit(): UniswapKit {
        kit?.let { return it }

        kit = ethereumKitManager.ethereumKit?.let { UniswapKit.getInstance(it) }
                ?: throw EthereumKitNotCreated()

        return kit!!
    }

    override fun unlink() {
        kit = null
        ethereumKitManager.unlink()
    }

}
