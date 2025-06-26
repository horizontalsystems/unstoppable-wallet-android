package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.providers.AppConfigProvider
import io.horizontalsystems.bankwallet.core.storage.SpamAddressStorage
import io.horizontalsystems.bankwallet.entities.SpamAddress
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.math.BigDecimal
import java.math.BigInteger

abstract class BaseSpamManager(
    protected val localStorage: ILocalStorage,
    protected val coinManager: ICoinManager,
    protected val spamAddressStorage: SpamAddressStorage,
    private val marketKitWrapper: MarketKitWrapper,
    private val appConfigProvider: AppConfigProvider
) {
    protected val coroutineScope = CoroutineScope(Dispatchers.Default)

    private val coinValueLimits by lazy {
        appConfigProvider.spamCoinValueLimits
    }

    private val fullCoins by lazy {
        marketKitWrapper.fullCoinsByCoinCode(coinValueLimits.map { it.key })
    }

    fun find(address: String): SpamAddress? {
        return spamAddressStorage.findByAddress(address)
    }

    protected fun createSpamConfig(blockchainType: BlockchainType): SpamConfig {
        val tokens = fullCoins.flatMap { it.tokens.filter { it.blockchainType == blockchainType } }
        var baseCoinValue = BigInteger.ZERO
        val coinsMap = mutableMapOf<String, BigInteger>()

        for (token in tokens) {
            val minValue = coinValueLimits[token.coin.code] ?: continue
            when (val tokenType = token.type) {
                is TokenType.Eip20 -> {
                    try {
                        coinsMap[tokenType.address] = scaleUp(minValue, token.decimals)
                    } catch (_: Throwable) {
                    }
                }

                is TokenType.Native -> {
                    baseCoinValue = scaleUp(minValue, token.decimals)
                }

                else -> Unit
            }
        }

        return SpamConfig(baseCoinValue, coinsMap, blockchainType)
    }

    private fun scaleUp(value: Double, decimals: Int): BigInteger {
        return BigDecimal(value).movePointRight(decimals).toBigInteger()
    }

    abstract fun supports(blockchainType: BlockchainType): Boolean
}
