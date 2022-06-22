package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.marketkit.models.CoinType
import io.horizontalsystems.marketkit.models.PlatformCoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class BaseCoinManager(
    private val coinManager: ICoinManager,
    private val localStorage: ILocalStorage,
) {
    val platformCoins = listOf(CoinType.Bitcoin, CoinType.Ethereum, CoinType.BinanceSmartChain)
        .mapNotNull {
            coinManager.getPlatformCoin(it)
        }

    private var platformCoin = localStorage.balanceTotalCoinUid?.let { balanceTotalCoinUid ->
        platformCoins.find { it.coin.uid == balanceTotalCoinUid }
    } ?: platformCoins.firstOrNull()

    private val _baseCoinFlow = MutableStateFlow(platformCoin)
    val baseCoinFlow = _baseCoinFlow.asStateFlow()

    fun toggleBaseCoin() {
        val indexOfNext = platformCoins.indexOf(platformCoin) + 1
        setBaseCoin(platformCoins.getOrNull(indexOfNext) ?: platformCoins.firstOrNull())
    }

    fun setBaseCoin(platformCoin: PlatformCoin?) {
        this.platformCoin = platformCoin
        localStorage.balanceTotalCoinUid = platformCoin?.coin?.uid

        _baseCoinFlow.update {
            platformCoin
        }
    }
}
