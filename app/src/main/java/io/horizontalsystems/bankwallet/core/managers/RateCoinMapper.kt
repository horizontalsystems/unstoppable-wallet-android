package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IBlockedChartCoins
import io.horizontalsystems.bankwallet.core.IRateCoinMapper

class RateCoinMapper : IRateCoinMapper, IBlockedChartCoins {

    private val nonExistCoin = "AI-DAI"
    override var blockedCoins = mutableListOf<String>()
    override var convertedCoinMap: MutableMap<String, String> = mutableMapOf()
    override var unconvertedCoinMap: MutableMap<String, String> = mutableMapOf()

    override fun addCoin(direction: RateDirectionMap, from: String, to: String?) {
        if (to == null) {
            blockedCoins.add(from)
        }
        when (direction) {
            RateDirectionMap.Convert -> convertedCoinMap[from] = to ?: nonExistCoin
            RateDirectionMap.Unconvert -> unconvertedCoinMap[from] = to ?: nonExistCoin
        }
    }
}

enum class RateDirectionMap {
    Convert,
    Unconvert
}
