package io.horizontalsystems.bankwallet.modules.swap.confirmation.oneinch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ethereum.CautionViewItemFactory
import io.horizontalsystems.bankwallet.core.ethereum.EvmCoinServiceFactory
import io.horizontalsystems.bankwallet.modules.evmfee.EvmFeeCellViewModel
import io.horizontalsystems.bankwallet.modules.evmfee.IEvmGasPriceService
import io.horizontalsystems.bankwallet.modules.evmfee.eip1559.Eip1559GasPriceService
import io.horizontalsystems.bankwallet.modules.evmfee.legacy.LegacyGasPriceService
import io.horizontalsystems.bankwallet.modules.send.evm.settings.SendEvmNonceService
import io.horizontalsystems.bankwallet.modules.send.evm.settings.SendEvmNonceViewModel
import io.horizontalsystems.bankwallet.modules.send.evm.settings.SendEvmSettingsService
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionViewModel
import io.horizontalsystems.bankwallet.modules.swap.SwapViewItemHelper
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.OneInchSwapParameters
import io.horizontalsystems.bankwallet.modules.swap.oneinch.OneInchKitHelper
import io.horizontalsystems.ethereumkit.core.LegacyGasPriceProvider
import io.horizontalsystems.ethereumkit.core.eip1559.Eip1559GasPriceProvider
import io.horizontalsystems.marketkit.models.BlockchainType

object OneInchConfirmationModule {

    class Factory(val blockchainType: BlockchainType, private val oneInchSwapParameters: OneInchSwapParameters) : ViewModelProvider.Factory {

        private val evmKitWrapper by lazy { App.evmBlockchainManager.getEvmKitManager(blockchainType).evmKitWrapper!! }
        private val oneInchKitHelper by lazy { OneInchKitHelper(evmKitWrapper.evmKit) }
        private val token by lazy { App.evmBlockchainManager.getBaseToken(blockchainType)!! }
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
            OneInchFeeService(oneInchKitHelper, evmKitWrapper.evmKit, gasPriceService, oneInchSwapParameters)
        }
        private val coinServiceFactory by lazy {
            EvmCoinServiceFactory(
                token,
                App.marketKit,
                App.currencyManager,
                App.coinManager
            )
        }
        private val nonceService by lazy { SendEvmNonceService(evmKitWrapper.evmKit) }
        private val settingsService by lazy { SendEvmSettingsService(feeService, nonceService) }
        private val sendService by lazy {
            OneInchSendEvmTransactionService(
                evmKitWrapper,
                feeService,
                settingsService,
                SwapViewItemHelper(App.numberFormatter)
            )
        }
        private val cautionViewItemFactory by lazy { CautionViewItemFactory(coinServiceFactory.baseCoinService) }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                SendEvmTransactionViewModel::class.java -> {
                    SendEvmTransactionViewModel(
                        sendService,
                        coinServiceFactory,
                        cautionViewItemFactory,
                        blockchainType = blockchainType,
                        contactsRepo = App.contactsRepository
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

}
