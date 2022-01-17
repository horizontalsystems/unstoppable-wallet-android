package io.horizontalsystems.bankwallet.modules.swap.confirmation.oneinch

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ICustomRangedFeeProvider
import io.horizontalsystems.bankwallet.core.ethereum.CautionViewItemFactory
import io.horizontalsystems.bankwallet.core.ethereum.EvmFeeCellViewModel
import io.horizontalsystems.bankwallet.core.ethereum.EvmCoinServiceFactory
import io.horizontalsystems.bankwallet.core.factories.FeeRateProviderFactory
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionViewModel
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.swap.oneinch.OneInchKitHelper

object OneInchConfirmationModule {
    private const val oneInchSwapParametersKey = "oneInchSwapParametersKey"

    class Factory(
            private val blockchain: SwapMainModule.Blockchain,
            private val arguments: Bundle
    ) : ViewModelProvider.Factory {

        private val oneInchSwapParameters by lazy { arguments.getParcelable<OneInchSwapParameters>(oneInchSwapParametersKey)!! }
        private val evmKitWrapper by lazy { blockchain.evmKitWrapper!! }
        private val oneInchKitHelper by lazy { OneInchKitHelper(evmKitWrapper.evmKit) }
        private val coin by lazy { blockchain.coin!! }
        private val transactionService by lazy {
            val feeRateProvider = FeeRateProviderFactory.provider(coin.coinType) as ICustomRangedFeeProvider
            OneInchTransactionFeeService(oneInchKitHelper, oneInchSwapParameters, feeRateProvider)
        }
        private val coinServiceFactory by lazy { EvmCoinServiceFactory(coin, App.marketKit, App.currencyManager) }
        private val sendService by lazy { OneInchSendEvmTransactionService(evmKitWrapper, transactionService, App.activateCoinManager) }
        private val cautionViewItemFactory by lazy { CautionViewItemFactory(coinServiceFactory.baseCoinService) }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                SendEvmTransactionViewModel::class.java -> {
                    SendEvmTransactionViewModel(sendService, coinServiceFactory, cautionViewItemFactory) as T
                }
                EvmFeeCellViewModel::class.java -> {
                    EvmFeeCellViewModel(transactionService, coinServiceFactory.baseCoinService) as T
                }
                else -> throw IllegalArgumentException()
            }
        }
    }

    fun prepareParams(oneInchSwapParameters: OneInchSwapParameters) = bundleOf(oneInchSwapParametersKey to oneInchSwapParameters)

}
