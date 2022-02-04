package io.horizontalsystems.bankwallet.modules.swap.confirmation.uniswap

import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ICustomRangedFeeProvider
import io.horizontalsystems.bankwallet.core.ethereum.*
import io.horizontalsystems.bankwallet.core.factories.FeeRateProviderFactory
import io.horizontalsystems.bankwallet.modules.sendevm.SendEvmData
import io.horizontalsystems.bankwallet.modules.sendevm.SendEvmModule
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionService
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionViewModel
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.feesettings.LegacyGasPriceService
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.ethereumkit.core.EthereumKit

object UniswapConfirmationModule {

    class Factory(
        private val blockchain: SwapMainModule.Blockchain,
        private val sendEvmData: SendEvmData
    ) : ViewModelProvider.Factory {

        private val evmKitWrapper by lazy { blockchain.evmKitWrapper!! }
        private val coin by lazy { blockchain.coin!! }
        private val gasPriceService: IEvmGasPriceService by lazy {
            val feeRateProvider = FeeRateProviderFactory.provider(coin.coinType) as ICustomRangedFeeProvider
            when (evmKitWrapper.evmKit.networkType) {
                EthereumKit.NetworkType.EthMainNet,
                EthereumKit.NetworkType.EthRopsten,
                EthereumKit.NetworkType.EthKovan,
                EthereumKit.NetworkType.EthGoerli,
                EthereumKit.NetworkType.EthRinkeby -> LegacyGasPriceService(feeRateProvider) // TODO switch to EIP1559 GasPrice service
                EthereumKit.NetworkType.BscMainNet -> LegacyGasPriceService(feeRateProvider)
            }
        }
        private val feeService by lazy {
            EvmTransactionFeeServiceNew(evmKitWrapper.evmKit, gasPriceService, sendEvmData.transactionData,20)
        }
        private val coinServiceFactory by lazy { EvmCoinServiceFactory(coin, App.marketKit, App.currencyManager) }
        private val cautionViewItemFactory by lazy { CautionViewItemFactory(coinServiceFactory.baseCoinService) }
        private val sendService by lazy {
            SendEvmTransactionService(sendEvmData, evmKitWrapper, feeService, App.activateCoinManager)
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                SendEvmTransactionViewModel::class.java -> {
                    SendEvmTransactionViewModel(sendService, coinServiceFactory, cautionViewItemFactory) as T
                }
                EvmFeeCellViewModel::class.java -> {
                    EvmFeeCellViewModel(
                        feeService,
                        coinServiceFactory.baseCoinService
                    ) as T
                }
                else -> throw IllegalArgumentException()
            }
        }
    }

    fun prepareParams(sendEvmData: SendEvmData) = bundleOf(
        SendEvmModule.transactionDataKey to SendEvmModule.TransactionDataParcelable(sendEvmData.transactionData),
        SendEvmModule.additionalInfoKey to sendEvmData.additionalInfo
    )

}