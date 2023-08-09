package io.horizontalsystems.bankwallet.core.factories

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IFeeRateProvider
import io.horizontalsystems.bankwallet.core.providers.*
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
