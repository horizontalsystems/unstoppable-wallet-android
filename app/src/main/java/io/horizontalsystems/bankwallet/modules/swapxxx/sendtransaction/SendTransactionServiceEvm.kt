package cash.p.terminal.modules.swapxxx.sendtransaction

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.core.App
import cash.p.terminal.core.ethereum.EvmCoinServiceFactory
import cash.p.terminal.modules.evmfee.EvmCommonGasDataService
import cash.p.terminal.modules.evmfee.EvmFeeModule
import cash.p.terminal.modules.evmfee.EvmFeeService
import cash.p.terminal.modules.evmfee.GasPriceInfo
import cash.p.terminal.modules.evmfee.IEvmGasPriceService
import cash.p.terminal.modules.evmfee.eip1559.Eip1559GasPriceService
import cash.p.terminal.modules.evmfee.legacy.LegacyGasPriceService
import cash.p.terminal.modules.send.evm.settings.SendEvmFeeSettingsScreen
import cash.p.terminal.modules.send.evm.settings.SendEvmNonceService
import cash.p.terminal.modules.send.evm.settings.SendEvmNonceViewModel
import cash.p.terminal.modules.send.evm.settings.SendEvmSettingsModule
import cash.p.terminal.modules.send.evm.settings.SendEvmSettingsService
import cash.p.terminal.modules.send.evm.settings.SendEvmSettingsViewModel
import io.horizontalsystems.ethereumkit.core.LegacyGasPriceProvider
import io.horizontalsystems.ethereumkit.core.eip1559.Eip1559GasPriceProvider
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class SendTransactionServiceEvm(blockchainType: BlockchainType) : ISendTransactionService() {
    private lateinit var transactionData: TransactionData

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
        val gasDataService = EvmCommonGasDataService.instance(
            evmKitWrapper.evmKit,
            evmKitWrapper.blockchainType
        )
        EvmFeeService(evmKitWrapper.evmKit, gasPriceService, gasDataService, transactionData)
    }
    private val coinServiceFactory by lazy {
        EvmCoinServiceFactory(
            token,
            App.marketKit,
            App.currencyManager,
            App.coinManager
        )
    }
//    private val cautionViewItemFactory by lazy { CautionViewItemFactory(coinServiceFactory.baseCoinService) }
    private val nonceService by lazy { SendEvmNonceService(evmKitWrapper.evmKit) }
    private val settingsService by lazy { SendEvmSettingsService(feeService, nonceService) }
//    private val sendService by lazy {
//        SendEvmTransactionService(
//            sendEvmData,
//            evmKitWrapper,
//            settingsService,
//            App.evmLabelManager
//        )
//    }

    private var gasPriceInfo: GasPriceInfo? = null

    override fun createState() = SendTransactionSettings.Evm(
        gasPriceInfo
    )

    override fun start(coroutineScope: CoroutineScope) {
        coroutineScope.launch {
            gasPriceService.stateFlow.collect {
                gasPriceInfo = it.dataOrNull

                emitState()
            }
        }
    }

    override fun setSendTransactionData(data: SendTransactionData) {
        check(data is SendTransactionData.Evm)

        transactionData = data.transactionData
    }

    @Composable
    override fun GetContent(navController: NavController) {
        val nonceViewModel = viewModel<SendEvmNonceViewModel>(initializer = {
            SendEvmNonceViewModel(nonceService)
        })

        val baseCoinService = coinServiceFactory.baseCoinService
        val feeSettingsViewModel = viewModel<ViewModel>(
            factory = EvmFeeModule.Factory(
                feeService,
                gasPriceService,
                baseCoinService
            )
        )
        val sendSettingsViewModel = viewModel<SendEvmSettingsViewModel>(
            factory = SendEvmSettingsModule.Factory(settingsService, baseCoinService)
        )
        SendEvmFeeSettingsScreen(
            viewModel = sendSettingsViewModel,
            feeSettingsViewModel = feeSettingsViewModel,
            nonceViewModel = nonceViewModel,
            navController = navController
        )
    }
}
