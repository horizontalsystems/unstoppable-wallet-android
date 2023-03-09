package cash.p.terminal.modules.swap.approve.confirmation

import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.core.ethereum.CautionViewItemFactory
import cash.p.terminal.core.ethereum.EvmCoinServiceFactory
import cash.p.terminal.modules.evmfee.EvmCommonGasDataService
import cash.p.terminal.modules.evmfee.EvmFeeCellViewModel
import cash.p.terminal.modules.evmfee.EvmFeeService
import cash.p.terminal.modules.evmfee.IEvmGasPriceService
import cash.p.terminal.modules.evmfee.eip1559.Eip1559GasPriceService
import cash.p.terminal.modules.evmfee.legacy.LegacyGasPriceService
import cash.p.terminal.modules.send.evm.SendEvmData
import cash.p.terminal.modules.send.evm.SendEvmModule
import cash.p.terminal.modules.send.evm.settings.SendEvmNonceService
import cash.p.terminal.modules.send.evm.settings.SendEvmNonceViewModel
import cash.p.terminal.modules.send.evm.settings.SendEvmSettingsService
import cash.p.terminal.modules.sendevmtransaction.SendEvmTransactionService
import cash.p.terminal.modules.sendevmtransaction.SendEvmTransactionViewModel
import io.horizontalsystems.ethereumkit.core.LegacyGasPriceProvider
import io.horizontalsystems.ethereumkit.core.eip1559.Eip1559GasPriceProvider
import io.horizontalsystems.marketkit.models.BlockchainType

object SwapApproveConfirmationModule {

    class Factory(
        private val sendEvmData: SendEvmData,
        private val blockchainType: BlockchainType
    ) : ViewModelProvider.Factory {

        private val token by lazy { App.evmBlockchainManager.getBaseToken(blockchainType)!! }
        private val evmKitWrapper by lazy { App.evmBlockchainManager.getEvmKitManager(blockchainType).evmKitWrapper!! }
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
            val gasDataService = EvmCommonGasDataService.instance(evmKitWrapper.evmKit, evmKitWrapper.blockchainType, 20)
            EvmFeeService(evmKitWrapper.evmKit, gasPriceService, gasDataService, sendEvmData.transactionData)
        }
        private val coinServiceFactory by lazy {
            EvmCoinServiceFactory(
                token,
                App.marketKit,
                App.currencyManager,
                App.coinManager
            )
        }
        private val cautionViewItemFactory by lazy { CautionViewItemFactory(coinServiceFactory.baseCoinService) }
        private val nonceService by lazy { SendEvmNonceService(evmKitWrapper.evmKit) }
        private val settingsService by lazy { SendEvmSettingsService(feeService, nonceService) }
        private val sendService by lazy {
            SendEvmTransactionService(
                sendEvmData,
                evmKitWrapper,
                settingsService,
                App.evmLabelManager
            )
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                SendEvmTransactionViewModel::class.java -> {
                    SendEvmTransactionViewModel(
                        sendService,
                        coinServiceFactory,
                        cautionViewItemFactory,
                        blockchainType = blockchainType
                    ) as T
                }
                EvmFeeCellViewModel::class.java -> {
                    EvmFeeCellViewModel(feeService, gasPriceService, coinServiceFactory.baseCoinService) as T
                }
                SendEvmNonceViewModel::class.java -> {
                    SendEvmNonceViewModel(nonceService) as T
                }
                else -> throw IllegalArgumentException()
            }
        }
    }

    fun prepareParams(sendEvmData: SendEvmData, blockchainType: BlockchainType, backButton: Boolean = true) = bundleOf(
        SendEvmModule.transactionDataKey to SendEvmModule.TransactionDataParcelable(sendEvmData.transactionData),
        SendEvmModule.additionalInfoKey to sendEvmData.additionalInfo,
        SendEvmModule.blockchainTypeKey to blockchainType,
        SendEvmModule.backButtonKey to backButton,
    )

}
