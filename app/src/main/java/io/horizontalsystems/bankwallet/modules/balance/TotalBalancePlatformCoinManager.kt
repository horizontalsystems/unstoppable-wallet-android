package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.marketkit.models.CoinType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class TotalBalancePlatformCoinManager(
    private val coinManager: ICoinManager,
    private val localStorage: ILocalStorage,
) {
    private val platformCoins = listOf(CoinType.Bitcoin, CoinType.Ethereum)
        .mapNotNull {
            coinManager.getPlatformCoin(it)
        }

    private var platformCoin = localStorage.balanceTotalCoinUid?.let { balanceTotalCoinUid ->
        platformCoins.find { it.coin.uid == balanceTotalCoinUid }
    } ?: platformCoins.firstOrNull()

    private val _platformCoinFlow = MutableStateFlow(platformCoin)
    val platformCoinFlow = _platformCoinFlow.asStateFlow()

    fun toggleType() {
        val indexOf = platformCoins.indexOf(platformCoin)
        platformCoin = platformCoins.getOrNull(indexOf + 1) ?: platformCoins.firstOrNull()

        localStorage.balanceTotalCoinUid = platformCoin?.coin?.uid

        _platformCoinFlow.update {
            platformCoin
        }
    }
}
