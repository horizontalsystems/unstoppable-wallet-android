package io.horizontalsystems.core.core.factories

import io.horizontalsystems.core.core.App
import io.horizontalsystems.core.core.IFeeRateProvider
import io.horizontalsystems.core.core.providers.BitcoinCashFeeRateProvider
import io.horizontalsystems.core.core.providers.BitcoinFeeRateProvider
import io.horizontalsystems.core.core.providers.DashFeeRateProvider
import io.horizontalsystems.core.core.providers.ECashFeeRateProvider
import io.horizontalsystems.core.core.providers.LitecoinFeeRateProvider
import io.horizontalsystems.marketkit.models.BlockchainType

object FeeRateProviderFactory {
    fun provider(blockchainType: BlockchainType): IFeeRateProvider? {
        val feeRateProvider = App.feeRateProvider

        return when (blockchainType) {
            is BlockchainType.Bitcoin -> BitcoinFeeRateProvider(feeRateProvider)
            is BlockchainType.Litecoin -> LitecoinFeeRateProvider(feeRateProvider)
            is BlockchainType.BitcoinCash -> BitcoinCashFeeRateProvider(feeRateProvider)
            is BlockchainType.ECash -> ECashFeeRateProvider()
            is BlockchainType.Dash -> DashFeeRateProvider(feeRateProvider)
            else -> null
        }
    }

}
