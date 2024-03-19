package cash.p.terminal.modules.swapxxx.sendtransaction

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.core.App
import cash.p.terminal.core.ethereum.CautionViewItem
import cash.p.terminal.core.ethereum.CautionViewItemFactory
import cash.p.terminal.core.ethereum.EvmCoinServiceFactory
import cash.p.terminal.entities.DataState
import cash.p.terminal.modules.evmfee.EvmCommonGasDataService
import cash.p.terminal.modules.evmfee.EvmFeeModule
import cash.p.terminal.modules.evmfee.EvmFeeService
import cash.p.terminal.modules.evmfee.IEvmGasPriceService
import cash.p.terminal.modules.evmfee.eip1559.Eip1559GasPriceService
import cash.p.terminal.modules.evmfee.legacy.LegacyGasPriceService
import cash.p.terminal.modules.send.SendModule
import cash.p.terminal.modules.send.evm.settings.SendEvmFeeSettingsScreen
import cash.p.terminal.modules.send.evm.settings.SendEvmNonceService
import cash.p.terminal.modules.send.evm.settings.SendEvmNonceViewModel
import cash.p.terminal.modules.send.evm.settings.SendEvmSettingsModule
import cash.p.terminal.modules.send.evm.settings.SendEvmSettingsService
import cash.p.terminal.modules.send.evm.settings.SendEvmSettingsViewModel
import io.horizontalsystems.ethereumkit.core.LegacyGasPriceProvider
import io.horizontalsystems.ethereumkit.core.eip1559.Eip1559GasPriceProvider
import io.horizontalsystems.marketkit.models.BlockchainType
import io.reactivex.Flowable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SendTransactionServiceEvm(blockchainType: BlockchainType) : ISendTransactionService() {
    private val token by lazy { App.evmBlockchainManager.getBaseToken(blockchainType)!! }
    private val evmKitWrapper by lazy { App.evmBlockchainManager.getEvmKitManager(blockchainType).evmKitWrapper!! }
    private val gasPriceService: IEvmGasPriceService by lazy {
        val evmKit = evmKitWrapper.evmKit
        if (evmKit.chain.isEIP1559Supported) {
            val gasPriceProvider = Eip1559GasPriceProvider(evmKit)
            Eip1559GasPriceService(gasPriceProvider, Flowable.empty())
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
        EvmFeeService(evmKitWrapper.evmKit, gasPriceService, gasDataService)
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
//    private val sendService by lazy {
//        SendEvmTransactionService(
//            sendEvmData,
//            evmKitWrapper,
//            settingsService,
//            App.evmLabelManager
//        )
//    }

    private val baseCoinService = coinServiceFactory.baseCoinService
    private val cautionViewItemFactory by lazy { CautionViewItemFactory(baseCoinService) }

    private val _sendTransactionSettingsFlow = MutableStateFlow(
        SendTransactionSettings.Evm(null)
    )
    override val sendTransactionSettingsFlow = _sendTransactionSettingsFlow.asStateFlow()

    private var transaction: SendEvmSettingsService.Transaction? = null
    private var feeAmountData: SendModule.AmountData? = null
    private var cautions: List<CautionViewItem> = listOf()
    private var sendable = false
    private var loading = true

    override fun createState() = SendTransactionServiceState(
        networkFee = feeAmountData,
        cautions = cautions,
        sendable = sendable,
        loading = loading
    )

    override fun start(coroutineScope: CoroutineScope) {
        coroutineScope.launch {
            gasPriceService.stateFlow.collect { gasPriceState ->
                _sendTransactionSettingsFlow.update {
                    SendTransactionSettings.Evm(gasPriceState.dataOrNull)
                }
            }
        }
        coroutineScope.launch {
            settingsService.start()
        }
        coroutineScope.launch {
            settingsService.stateFlow.collect { transactionState ->
                handleTransactionState(transactionState)
            }
        }
    }

    private fun handleTransactionState(transactionState: DataState<SendEvmSettingsService.Transaction>) {
        loading = transactionState.loading
        transaction = transactionState.dataOrNull
        feeAmountData = transaction?.let {
            baseCoinService.amountData(
                it.gasData.estimatedFee,
                it.gasData.isSurcharged
            )
        }
        cautions = listOf()
        sendable = false

        when (transactionState) {
            is DataState.Error -> {
                cautions = cautionViewItemFactory.cautionViewItems(
                    listOf(),
                    listOf(transactionState.error)
                )
            }

            is DataState.Success -> {
                cautions = cautionViewItemFactory.cautionViewItems(
                    transactionState.data.warnings,
                    transactionState.data.errors
                )
                sendable = transactionState.data.errors.isEmpty()
            }

            DataState.Loading -> {
            }
        }

        emitState()
    }

    override fun setSendTransactionData(data: SendTransactionData) {
        check(data is SendTransactionData.Evm)

        feeService.setGasLimit(data.gasLimit)
        feeService.setTransactionData(data.transactionData)
    }

    override suspend fun sendTransaction() {
        val transaction = transaction ?: throw Exception()
        if (transaction.errors.isNotEmpty()) throw Exception()

        val transactionData = transaction.transactionData
        val gasPrice = transaction.gasData.gasPrice
        val gasLimit = transaction.gasData.gasLimit
        val nonce = transaction.nonce

        delay(300)
//        evmKitWrapper.sendSingle(transactionData, gasPrice, gasLimit, nonce).await()
    }

    @Composable
    override fun GetContent(navController: NavController) {
        val nonceViewModel = viewModel<SendEvmNonceViewModel>(initializer = {
            SendEvmNonceViewModel(nonceService)
        })

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
