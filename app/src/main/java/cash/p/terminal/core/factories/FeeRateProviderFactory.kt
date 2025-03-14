package cash.p.terminal.core.factories

import cash.p.terminal.core.App
import cash.p.terminal.core.IFeeRateProvider
import cash.p.terminal.core.providers.BitcoinCashFeeRateProvider
import cash.p.terminal.core.providers.BitcoinFeeRateProvider
import cash.p.terminal.core.providers.DashFeeRateProvider
import cash.p.terminal.core.providers.DogecoinFeeRateProvider
import cash.p.terminal.core.providers.ECashFeeRateProvider
import cash.p.terminal.core.providers.LitecoinFeeRateProvider
import io.horizontalsystems.core.entities.BlockchainType

object FeeRateProviderFactory {
    fun provider(blockchainType: BlockchainType): IFeeRateProvider? {
        val feeRateProvider = App.feeRateProvider

        return when (blockchainType) {
            is BlockchainType.Bitcoin -> BitcoinFeeRateProvider(feeRateProvider)
            is BlockchainType.Litecoin -> LitecoinFeeRateProvider(feeRateProvider)
            is BlockchainType.Dogecoin -> DogecoinFeeRateProvider(feeRateProvider)
            is BlockchainType.BitcoinCash -> BitcoinCashFeeRateProvider(feeRateProvider)
            is BlockchainType.ECash -> ECashFeeRateProvider()
            is BlockchainType.Dash -> DashFeeRateProvider(feeRateProvider)
            else -> null
        }
    }

}
