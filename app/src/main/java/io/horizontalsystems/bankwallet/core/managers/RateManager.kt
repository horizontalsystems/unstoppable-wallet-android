package io.horizontalsystems.bankwallet.core.managers

import android.content.Context
import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.marketkit.models.CoinType
import io.horizontalsystems.xrateskit.XRatesKit
import io.horizontalsystems.xrateskit.entities.Auditor
import io.horizontalsystems.xrateskit.entities.DefiTvl
import io.horizontalsystems.xrateskit.entities.TimePeriod
import io.reactivex.Single
import java.math.BigDecimal
import io.horizontalsystems.coinkit.models.CoinType as CoinKitCoinType

class RateManager(
        context: Context,
        private val appConfigProvider: IAppConfigProvider) : IRateManager {

    private val kit: XRatesKit by lazy {
        XRatesKit.create(
                context,
                rateExpirationInterval = 60 * 10,
                cryptoCompareApiKey = appConfigProvider.cryptoCompareApiKey,
                defiyieldProviderApiKey = appConfigProvider.defiyieldProviderApiKey,
                coinsRemoteUrl = appConfigProvider.coinsJsonUrl,
                providerCoinsRemoteUrl = appConfigProvider.providerCoinsJsonUrl,
        )
    }

    override fun historicalRateCached(coinType: CoinType, currencyCode: String, timestamp: Long): BigDecimal? {
        return kit.getHistoricalRate(coinType.toCoinKitCoinType(), currencyCode, timestamp)
    }

    override fun historicalRate(coinType: CoinType, currencyCode: String, timestamp: Long): Single<BigDecimal> {
        return kit.getHistoricalRate(coinType.toCoinKitCoinType(), currencyCode, timestamp)?.let { Single.just(it) }
                ?: kit.getHistoricalRateAsync(coinType.toCoinKitCoinType(), currencyCode, timestamp)
    }

    override fun getAuditsAsync(coinType: CoinType): Single<List<Auditor>> {
        return kit.getAuditReportsAsync(coinType.toCoinKitCoinType())
    }

    override fun getTopDefiTvlAsync(currencyCode: String, fetchDiffPeriod: TimePeriod, itemsCount: Int, chain: String?): Single<List<DefiTvl>> {
        return kit.getTopDefiTvlAsync(currencyCode, fetchDiffPeriod, itemsCount, chain)
    }
}

private fun CoinType.toCoinKitCoinType(): CoinKitCoinType = when (this) {
    CoinType.Bitcoin -> CoinKitCoinType.Bitcoin
    CoinType.BitcoinCash -> CoinKitCoinType.BitcoinCash
    CoinType.Litecoin -> CoinKitCoinType.Litecoin
    CoinType.Dash -> CoinKitCoinType.Dash
    CoinType.Zcash -> CoinKitCoinType.Zcash
    CoinType.Ethereum -> CoinKitCoinType.Ethereum
    CoinType.BinanceSmartChain -> CoinKitCoinType.BinanceSmartChain
    is CoinType.Erc20 -> CoinKitCoinType.Erc20(address)
    is CoinType.Bep20 -> CoinKitCoinType.Bep20(address)
    is CoinType.Bep2 -> CoinKitCoinType.Bep2(symbol)
    is CoinType.ArbitrumOne,
    is CoinType.Avalanche,
    is CoinType.Fantom,
    is CoinType.HarmonyShard0,
    is CoinType.HuobiToken,
    is CoinType.Iotex,
    is CoinType.Moonriver,
    is CoinType.OkexChain,
    is CoinType.PolygonPos,
    is CoinType.Solana,
    is CoinType.Sora,
    is CoinType.Tomochain,
    is CoinType.Xdai -> CoinKitCoinType.Unsupported("")
    is CoinType.Unsupported -> CoinKitCoinType.Unsupported(type)
}
