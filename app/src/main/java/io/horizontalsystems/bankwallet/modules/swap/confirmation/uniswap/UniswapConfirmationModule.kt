package io.horizontalsystems.bankwallet.modules.swap.confirmation.uniswap

import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavOptions
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ICustomRangedFeeProvider
import io.horizontalsystems.bankwallet.core.ethereum.EthereumFeeViewModel
import io.horizontalsystems.bankwallet.core.ethereum.EvmCoinServiceFactory
import io.horizontalsystems.bankwallet.core.ethereum.EvmTransactionFeeService
import io.horizontalsystems.bankwallet.core.factories.FeeRateProviderFactory
import io.horizontalsystems.bankwallet.modules.sendevm.SendEvmData
import io.horizontalsystems.bankwallet.modules.sendevm.SendEvmModule
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionService
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionViewModel
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.core.findNavController

object UniswapConfirmationModule {

    class Factory(
        private val blockchain: SwapMainModule.Blockchain,
        private val sendEvmData: SendEvmData
    ) : ViewModelProvider.Factory {

        private val evmKit by lazy { blockchain.evmKit!! }
        private val coin by lazy { blockchain.coin!! }
        private val transactionFeeService by lazy {
            val feeRateProvider = FeeRateProviderFactory.provider(coin.coinType) as ICustomRangedFeeProvider
            EvmTransactionFeeService(evmKit, feeRateProvider, 20)
        }
        private val coinServiceFactory by lazy {
            EvmCoinServiceFactory(
                coin,
                App.marketKit,
                App.currencyManager,
            )
        }
        private val sendService by lazy {
            SendEvmTransactionService(
                sendEvmData,
                evmKit,
                transactionFeeService,
                App.activateCoinManager
            )
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return when (modelClass) {
                SendEvmTransactionViewModel::class.java -> {
                    SendEvmTransactionViewModel(sendService, coinServiceFactory) as T
                }
                EthereumFeeViewModel::class.java -> {
                    EthereumFeeViewModel(
                        transactionFeeService,
                        coinServiceFactory.baseCoinService
                    ) as T
                }
                else -> throw IllegalArgumentException()
            }
        }
    }

    fun start(
        fragment: Fragment,
        navigateTo: Int,
        navOptions: NavOptions,
        sendEvmData: SendEvmData
    ) {
        val arguments = bundleOf(
            SendEvmModule.transactionDataKey to SendEvmModule.TransactionDataParcelable(sendEvmData.transactionData),
            SendEvmModule.additionalInfoKey to sendEvmData.additionalInfo
        )
        fragment.findNavController().navigate(navigateTo, arguments, navOptions)
    }

}