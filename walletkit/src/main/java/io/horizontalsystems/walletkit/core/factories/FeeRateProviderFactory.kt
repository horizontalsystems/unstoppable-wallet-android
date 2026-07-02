package io.horizontalsystems.walletkit.core.factories

import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.IFeeRateProvider
import io.horizontalsystems.walletkit.core.providers.BitcoinCashFeeRateProvider
import io.horizontalsystems.walletkit.core.providers.BitcoinFeeRateProvider
import io.horizontalsystems.walletkit.core.providers.DashFeeRateProvider
import io.horizontalsystems.walletkit.core.providers.ECashFeeRateProvider
import io.horizontalsystems.walletkit.core.providers.LitecoinFeeRateProvider
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
