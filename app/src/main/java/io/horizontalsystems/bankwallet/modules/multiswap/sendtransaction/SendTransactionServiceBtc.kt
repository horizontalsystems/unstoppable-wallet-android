package io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.core.ISendBitcoinAdapter
import io.horizontalsystems.bankwallet.core.adapters.BitcoinFeeInfo
import io.horizontalsystems.bankwallet.core.factories.FeeRateProviderFactory
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.modules.amount.AmountInputType
import io.horizontalsystems.bankwallet.modules.amount.AmountValidator
import io.horizontalsystems.bankwallet.modules.evmfee.EvmSettingsInput
import io.horizontalsystems.bankwallet.modules.fee.HSFeeRaw
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataField
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.send.bitcoin.SendBitcoinAddressService
import io.horizontalsystems.bankwallet.modules.send.bitcoin.SendBitcoinAmountService
import io.horizontalsystems.bankwallet.modules.send.bitcoin.SendBitcoinFeeRateService
import io.horizontalsystems.bankwallet.modules.send.bitcoin.SendBitcoinFeeService
import io.horizontalsystems.bankwallet.modules.send.bitcoin.advanced.FeeRateCaution
import io.horizontalsystems.bankwallet.modules.send.bitcoin.settings.SendBtcSettingsViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.InfoText
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bitcoincore.storage.UtxoFilters
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

class SendTransactionServiceBtc(private val token: Token) : AbstractSendTransactionService(true) {
    private val adapter = App.adapterManager.getAdapterForToken<ISendBitcoinAdapter>(token)!!
    private val provider = FeeRateProviderFactory.provider(token.blockchainType)!!
    private val feeService = SendBitcoinFeeService(adapter)
    private val feeRateService = SendBitcoinFeeRateService(provider)
    private val amountService = SendBitcoinAmountService(adapter, token.coin.code, AmountValidator())
    private val addressService = SendBitcoinAddressService(adapter)

    private var feeRateState = feeRateService.stateFlow.value
    private var bitcoinFeeInfo = feeService.bitcoinFeeInfoFlow.value
    private var amountState = amountService.stateFlow.value
    private var addressState = addressService.stateFlow.value

    private var memo: String? = null
    private var changeToFirstInput: Boolean = false
    private var utxoFilters: UtxoFilters = UtxoFilters()
    private var networkFee: SendModule.AmountData? = null

    private var fields = listOf<DataField>()

    override val sendTransactionSettingsFlow = MutableStateFlow(SendTransactionSettings.Btc())

    override fun start(coroutineScope: CoroutineScope) {
        coroutineScope.launch {
            feeRateService.stateFlow.collect {
                handleFeeRateState(it)
            }
        }
        coroutineScope.launch {
            feeService.bitcoinFeeInfoFlow.collect {
                handleBitcoinFeeInfo(it)
            }
        }
        coroutineScope.launch {
            amountService.stateFlow.collect {
                handleAmountState(it)
            }
        }
        coroutineScope.launch {
            addressService.stateFlow.collect {
                handleAddressState(it)
            }
        }

        coroutineScope.launch {
            feeRateService.start()
        }
    }

    private fun handleAddressState(state: SendBitcoinAddressService.State) {
        addressState = state

        amountService.setValidAddress(addressState.validAddress)
        feeService.setValidAddress(addressState.validAddress)

        emitState()
    }

    private fun handleAmountState(state: SendBitcoinAmountService.State) {
        amountState = state

        feeService.setAmount(amountState.amount)

        emitState()
    }

    private fun handleBitcoinFeeInfo(info: BitcoinFeeInfo?) {
        bitcoinFeeInfo = info

        refreshNetworkFee()

        emitState()
    }

    private fun refreshNetworkFee() {
        networkFee = bitcoinFeeInfo?.fee?.let { fee ->
            getAmountData(CoinValue(token, fee))
        }
    }

    private fun handleFeeRateState(state: SendBitcoinFeeRateService.State) {
        feeRateState = state

        feeService.setFeeRate(feeRateState.feeRate)
        amountService.setFeeRate(feeRateState.feeRate)

        emitState()
    }

    override fun createState() = SendTransactionServiceState(
        uuid = uuid,
        networkFee = networkFee,
        cautions = listOfNotNull(amountState.amountCaution, feeRateState.feeRateCaution).map(HSCaution::toCautionViewItem),
        sendable = amountState.canBeSend && feeRateState.canBeSend && addressState.canBeSend,
        loading = false,
        fields = fields,
        extraFees = extraFees
    )

    override suspend fun setSendTransactionData(data: SendTransactionData) {
        check(data is SendTransactionData.Btc)

        memo = data.memo
        changeToFirstInput = data.changeToFirstInput
        utxoFilters = data.utxoFilters

        feeRateService.setRecommendedAndMin(data.recommendedGasRate, data.recommendedGasRate)

        feeService.setMemo(memo)
        feeService.setChangeToFirstInput(changeToFirstInput)
        feeService.setUtxoFilters(utxoFilters)

        amountService.setMemo(memo)
        amountService.setUserMinimumSendAmount(data.minimumSendAmount)
        amountService.setChangeToFirstInput(changeToFirstInput)
        amountService.setUtxoFilters(utxoFilters)
        amountService.setAmount(data.amount)

        addressService.setAddress(Address(data.address))

        setExtraFeesMap(data.feesMap)
    }

    @Composable
    override fun GetSettingsContent(navController: NavController) {
        val sendSettingsViewModel = viewModel<SendBtcSettingsViewModel>(
            factory = SendBtcSettingsViewModel.Factory(feeRateService, feeService, token)
        )

        SendBtcFeeSettingsScreen(navController, sendSettingsViewModel)
    }

    override suspend fun sendTransaction(mevProtectionEnabled: Boolean): SendTransactionResult.Btc {
        val transactionRecord = adapter.send(
            amount = amountState.amount!!,
            address = addressState.validAddress?.hex!!,
            memo = memo,
            feeRate = feeRateState.feeRate!!,
            unspentOutputs = null,
            pluginData = null,
            transactionSorting = null,
            rbfEnabled = false,
            changeToFirstInput = changeToFirstInput,
            utxoFilters = utxoFilters
        )

        return SendTransactionResult.Btc(transactionRecord)
    }
}

@Composable
fun SendBtcFeeSettingsScreen(
    navController: NavController,
    viewModel: SendBtcSettingsViewModel
) {
    val uiState = viewModel.uiState

    HSScaffold(
        title = stringResource(R.string.SendEvmSettings_Title),
        onBack = navController::popBackStack,
        menuItems = listOf(
            MenuItem(
                title = TranslatableString.ResString(R.string.Button_Reset),
                enabled = uiState.resetEnabled,
                tint = ComposeAppTheme.colors.jacob,
                onClick = {
                    viewModel.reset()
                }
            )
        )
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
        ) {
            VSpacer(12.dp)
            CellUniversalLawrenceSection(
                listOf {
                    HSFeeRaw(
                        coinCode = viewModel.token.coin.code,
                        coinDecimal = viewModel.coinMaxAllowedDecimals,
                        fee = uiState.fee,
                        amountInputType = AmountInputType.COIN,
                        rate = uiState.rate,
                        navController = navController
                    )
                }
            )

            if (viewModel.feeRateChangeable) {
                VSpacer(24.dp)
                EvmSettingsInput(
                    title = stringResource(R.string.FeeSettings_FeeRate),
                    info = stringResource(R.string.FeeSettings_FeeRate_Info),
                    value = uiState.feeRate?.toBigDecimal() ?: BigDecimal.ZERO,
                    decimals = 0,
                    caution = uiState.feeRateCaution,
                    navController = navController,
                    onValueChange = {
                        viewModel.updateFeeRate(it.toInt())
                    },
                    onClickIncrement = {
                        viewModel.incrementFeeRate()
                    },
                    onClickDecrement = {
                        viewModel.decrementFeeRate()
                    }
                )
                InfoText(
                    text = stringResource(R.string.FeeSettings_FeeRate_RecommendedInfo),
                )
            }

            uiState.feeRateCaution?.let {
                FeeRateCaution(
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp
                    ),
                    feeRateCaution = it
                )
            }

            VSpacer(32.dp)
        }
    }
}