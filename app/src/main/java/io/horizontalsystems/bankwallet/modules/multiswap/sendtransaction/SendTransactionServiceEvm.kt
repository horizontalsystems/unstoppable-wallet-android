package io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ethereum.CautionViewItem
import io.horizontalsystems.bankwallet.core.ethereum.CautionViewItemFactory
import io.horizontalsystems.bankwallet.core.ethereum.EvmCoinServiceFactory
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.evmfee.Cautions
import io.horizontalsystems.bankwallet.modules.evmfee.Eip1559FeeSettings
import io.horizontalsystems.bankwallet.modules.evmfee.EvmCommonGasDataService
import io.horizontalsystems.bankwallet.modules.evmfee.EvmFeeModule
import io.horizontalsystems.bankwallet.modules.evmfee.EvmFeeService
import io.horizontalsystems.bankwallet.modules.evmfee.EvmSettingsInput
import io.horizontalsystems.bankwallet.modules.evmfee.IEvmGasPriceService
import io.horizontalsystems.bankwallet.modules.evmfee.LegacyFeeSettings
import io.horizontalsystems.bankwallet.modules.evmfee.eip1559.Eip1559FeeSettingsViewModel
import io.horizontalsystems.bankwallet.modules.evmfee.eip1559.Eip1559GasPriceService
import io.horizontalsystems.bankwallet.modules.evmfee.legacy.LegacyFeeSettingsViewModel
import io.horizontalsystems.bankwallet.modules.evmfee.legacy.LegacyGasPriceService
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataField
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataFieldNonce
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.send.evm.settings.SendEvmNonceService
import io.horizontalsystems.bankwallet.modules.send.evm.settings.SendEvmNonceViewModel
import io.horizontalsystems.bankwallet.modules.send.evm.settings.SendEvmSettingsModule
import io.horizontalsystems.bankwallet.modules.send.evm.settings.SendEvmSettingsService
import io.horizontalsystems.bankwallet.modules.send.evm.settings.SendEvmSettingsViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.ethereumkit.core.LegacyGasPriceProvider
import io.horizontalsystems.ethereumkit.core.eip1559.Eip1559GasPriceProvider
import io.horizontalsystems.ethereumkit.decorations.TransactionDecoration
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.BlockchainType
import io.reactivex.Flowable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await
import java.math.BigDecimal

class SendTransactionServiceEvm(
    blockchainType: BlockchainType,
    initialGasPrice: GasPrice? = null,
    initialNonce: Long? = null
) : AbstractSendTransactionService(true) {
    private val token by lazy { App.evmBlockchainManager.getBaseToken(blockchainType)!! }
    private val evmKitWrapper by lazy {
        val account =
            App.accountManager.activeAccount ?: throw IllegalArgumentException("No active account")
        App.evmBlockchainManager.getEvmKitManager(blockchainType)
            .getEvmKitWrapper(account, blockchainType)
    }
    private val gasPriceService: IEvmGasPriceService by lazy {
        val evmKit = evmKitWrapper.evmKit
        if (evmKit.chain.isEIP1559Supported) {
            val gasPriceProvider = Eip1559GasPriceProvider(evmKit)
            Eip1559GasPriceService(
                gasProvider = gasPriceProvider,
                refreshSignalFlowable = Flowable.empty(),
                initialGasPrice = initialGasPrice as? GasPrice.Eip1559
            )
        } else {
            val gasPriceProvider = LegacyGasPriceProvider(evmKit)
            LegacyGasPriceService(
                gasPriceProvider = gasPriceProvider,
                initialGasPrice = (initialGasPrice as? GasPrice.Legacy)?.legacyGasPrice
            )
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
    private val nonceService by lazy {
        SendEvmNonceService(evmKitWrapper.evmKit, initialNonce)
    }
    private val settingsService by lazy { SendEvmSettingsService(feeService, nonceService) }

    private val baseCoinService = coinServiceFactory.baseCoinService
    private val cautionViewItemFactory by lazy { CautionViewItemFactory(baseCoinService) }

    private val _sendTransactionSettingsFlow = MutableStateFlow(
        SendTransactionSettings.Evm(null, evmKitWrapper.evmKit.receiveAddress)
    )
    override val sendTransactionSettingsFlow = _sendTransactionSettingsFlow.asStateFlow()
    override val mevProtectionAvailable = evmKitWrapper.merkleTransactionAdapter != null

    private var transaction: SendEvmSettingsService.Transaction? = null
    private var feeAmountData: SendModule.AmountData? = null
    private var cautions: List<CautionViewItem> = listOf()
    private var sendable = false
    private var loading = true
    private var fields = listOf<DataField>()

    override fun createState() = SendTransactionServiceState(
        uuid = uuid,
        networkFee = feeAmountData,
        cautions = cautions,
        sendable = sendable,
        loading = loading,
        fields = fields,
        extraFees = extraFees
    )

    override fun start(coroutineScope: CoroutineScope) {
        gasPriceService.start()
        feeService.start()

        coroutineScope.launch {
            gasPriceService.stateFlow.collect { gasPriceState ->
                _sendTransactionSettingsFlow.update {
                    SendTransactionSettings.Evm(
                        gasPriceState.dataOrNull,
                        evmKitWrapper.evmKit.receiveAddress
                    )
                }
            }
        }
        coroutineScope.launch {
            settingsService.start()
        }
        coroutineScope.launch(Dispatchers.Default) {
            nonceService.start()
        }
        coroutineScope.launch {
            settingsService.stateFlow.collect { transactionState ->
                handleTransactionState(transactionState)
            }
        }
        coroutineScope.launch {
            nonceService.stateFlow.collect { nonceState ->
                handleNonceState(nonceState)
            }
        }
    }

    private fun handleNonceState(nonceState: DataState<SendEvmNonceService.State>) {
        fields = emptyList()

        nonceState.dataOrNull?.let {
            if (!it.default || it.fixed) {
                fields = listOf(DataFieldNonce(it.nonce))
            }
        }

        emitState()
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

    override suspend fun setSendTransactionData(data: SendTransactionData) {
        check(data is SendTransactionData.Evm)

        feeService.setGasLimit(data.gasLimit)
        feeService.setTransactionData(data.transactionData)

        setExtraFeesMap(data.feesMap)
    }

    override suspend fun sendTransaction(mevProtectionEnabled: Boolean): SendTransactionResult.Evm {
        val transaction = transaction ?: throw Exception()
        if (transaction.errors.isNotEmpty()) throw Exception()

        val transactionData = transaction.transactionData
        val gasPrice = transaction.gasData.gasPrice
        val gasLimit = transaction.gasData.gasLimit
        val nonce = transaction.nonce

        val fullTransaction = evmKitWrapper
            .sendSingle(transactionData, gasPrice, gasLimit, nonce, mevProtectionEnabled).await()
        return SendTransactionResult.Evm(fullTransaction)
    }

    fun decorate(transactionData: TransactionData): TransactionDecoration? {
        return evmKitWrapper.evmKit.decorate(transactionData)
    }

    fun fixNonce(nonce: Long) {
        nonceService.fixNonce(nonce)
    }

    @Composable
    override fun GetSettingsContent(navController: NavController) {
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

@Composable
fun SendEvmFeeSettingsScreen(
    viewModel: SendEvmSettingsViewModel,
    feeSettingsViewModel: ViewModel,
    nonceViewModel: SendEvmNonceViewModel,
    navController: NavController
) {
    HSScaffold(
        title = stringResource(R.string.SendEvmSettings_Title),
        onBack = navController::popBackStack,
        menuItems = listOf(
            MenuItem(
                title = TranslatableString.ResString(R.string.Button_Reset),
                enabled = !viewModel.isRecommendedSettingsSelected,
                onClick = { viewModel.onClickReset() },
                tint = ComposeAppTheme.colors.jacob
            )
        )
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
        ) {
            when (feeSettingsViewModel) {
                is LegacyFeeSettingsViewModel -> {
                    LegacyFeeSettings(feeSettingsViewModel, navController)
                }

                is Eip1559FeeSettingsViewModel -> {
                    Eip1559FeeSettings(feeSettingsViewModel, navController)
                }
            }

            val nonceUiState = nonceViewModel.uiState
            if (nonceUiState.showInSettings) {
                VSpacer(24.dp)
                EvmSettingsInput(
                    title = stringResource(id = R.string.SendEvmSettings_Nonce),
                    info = stringResource(id = R.string.SendEvmSettings_Nonce_Info),
                    value = nonceUiState.nonce?.toBigDecimal() ?: BigDecimal.ZERO,
                    decimals = 0,
                    navController = navController,
                    warnings = nonceUiState.warnings,
                    errors = nonceUiState.errors,
                    onValueChange = {
                        nonceViewModel.onEnterNonce(it.toLong())
                    },
                    onClickIncrement = nonceViewModel::onIncrementNonce,
                    onClickDecrement = nonceViewModel::onDecrementNonce
                )
            }

            Cautions(viewModel.cautions)

            VSpacer(32.dp)
        }
    }
}
