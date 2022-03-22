package io.horizontalsystems.bankwallet.core.factories

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IFeeRateProvider
import io.horizontalsystems.bankwallet.core.providers.BitcoinCashFeeRateProvider
import io.horizontalsystems.bankwallet.core.providers.BitcoinFeeRateProvider
import io.horizontalsystems.bankwallet.core.providers.DashFeeRateProvider
import io.horizontalsystems.bankwallet.core.providers.LitecoinFeeRateProvider
import io.horizontalsystems.marketkit.models.CoinType

object FeeRateProviderFactory {
    fun provider(coinType: CoinType): IFeeRateProvider? {
        val feeRateProvider = App.feeRateProvider

        return when (coinType) {
            is CoinType.Bitcoin -> BitcoinFeeRateProvider(feeRateProvider)
            is CoinType.Litecoin -> LitecoinFeeRateProvider(feeRateProvider)
            is CoinType.BitcoinCash -> BitcoinCashFeeRateProvider(feeRateProvider)
            is CoinType.Dash -> DashFeeRateProvider(feeRateProvider)
            else -> null
        }
    }

}
