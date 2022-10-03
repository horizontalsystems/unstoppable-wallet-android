package io.horizontalsystems.bankwallet.modules.send.evm.confirmation

import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ethereum.CautionViewItemFactory
import io.horizontalsystems.bankwallet.core.ethereum.EvmCoinServiceFactory
import io.horizontalsystems.bankwallet.core.managers.EvmKitWrapper
import io.horizontalsystems.bankwallet.modules.evmfee.EvmCommonGasDataService
import io.horizontalsystems.bankwallet.modules.evmfee.EvmFeeCellViewModel
import io.horizontalsystems.bankwallet.modules.evmfee.EvmFeeService
import io.horizontalsystems.bankwallet.modules.evmfee.IEvmGasPriceService
import io.horizontalsystems.bankwallet.modules.evmfee.eip1559.Eip1559GasPriceService
import io.horizontalsystems.bankwallet.modules.evmfee.legacy.LegacyGasPriceService
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmData
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmModule
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionService
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionViewModel
import io.horizontalsystems.ethereumkit.core.LegacyGasPriceProvider
import io.horizontalsystems.ethereumkit.core.eip1559.Eip1559GasPriceProvider
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType

object SendEvmConfirmationModule {

    class Factory(
        private val evmKitWrapper: EvmKitWrapper,
        private val sendEvmData: SendEvmData
    ) : ViewModelProvider.Factory {

        private val feeToken by lazy {
            val blockchainType = when (evmKitWrapper.evmKit.chain) {
                Chain.BinanceSmartChain -> BlockchainType.BinanceSmartChain
                Chain.Polygon -> BlockchainType.Polygon
                Chain.Avalanche -> BlockchainType.Avalanche
                Chain.Optimism -> BlockchainType.Optimism
                Chain.ArbitrumOne -> BlockchainType.ArbitrumOne
                else -> BlockchainType.Ethereum
            }
            App.marketKit.token(TokenQuery(blockchainType, TokenType.Native))!!
        }
        private val gasPriceService: IEvmGasPriceService by lazy {
            val evmKit = evmKitWrapper.evmKit
            if (evmKit.chain.isEIP1559Supported) {
                val gasPriceProvider = Eip1559GasPriceProvider(evmKit)
                Eip1559GasPriceService(gasPriceProvider, evmKit)
            } else {
                val gasPriceProvider = LegacyGasPriceProvider(evmKit)
                LegacyGasPriceService(gasPriceProvider)
            }
        }
        private val feeService by lazy {
            val gasLimitSurchargePercent = if (sendEvmData.transactionData.input.isEmpty()) 0 else 20
            val gasDataService = EvmCommonGasDataService.instance(evmKitWrapper.evmKit, evmKitWrapper.blockchainType, gasLimitSurchargePercent)
            EvmFeeService(evmKitWrapper.evmKit, gasPriceService, gasDataService, sendEvmData.transactionData)
        }
        private val coinServiceFactory by lazy { EvmCoinServiceFactory(feeToken, App.marketKit, App.currencyManager) }
        private val cautionViewItemFactory by lazy { CautionViewItemFactory(coinServiceFactory.baseCoinService) }
        private val sendService by lazy {
            SendEvmTransactionService(sendEvmData, evmKitWrapper, feeService, App.evmLabelManager)
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                SendEvmTransactionViewModel::class.java -> {
                    SendEvmTransactionViewModel(sendService, coinServiceFactory, cautionViewItemFactory, App.evmLabelManager) as T
                }
                EvmFeeCellViewModel::class.java -> {
                    EvmFeeCellViewModel(feeService, gasPriceService, coinServiceFactory.baseCoinService) as T
                }
                else -> throw IllegalArgumentException()
            }
        }
    }

    fun prepareParams(sendData: SendEvmData, sendNavId: Int) = bundleOf(
        SendEvmModule.transactionDataKey to SendEvmModule.TransactionDataParcelable(sendData.transactionData),
        SendEvmModule.additionalInfoKey to sendData.additionalInfo,
        SendEvmModule.sendNavGraphIdKey to sendNavId
    )

}
