package com.quantum.wallet.bankwallet.modules.multiswap.sendtransaction

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
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.core.HSCaution
import com.quantum.wallet.bankwallet.core.ISendBitcoinAdapter
import com.quantum.wallet.bankwallet.core.adapters.BitcoinFeeInfo
import com.quantum.wallet.bankwallet.core.factories.FeeRateProviderFactory
import com.quantum.wallet.bankwallet.entities.Address
import com.quantum.wallet.bankwallet.entities.CoinValue
import com.quantum.wallet.bankwallet.modules.amount.AmountInputType
import com.quantum.wallet.bankwallet.modules.amount.AmountValidator
import com.quantum.wallet.bankwallet.modules.evmfee.EvmSettingsInput
import com.quantum.wallet.bankwallet.modules.fee.HSFee
import com.quantum.wallet.bankwallet.modules.multiswap.ui.DataField
import com.quantum.wallet.bankwallet.modules.send.SendModule
import com.quantum.wallet.bankwallet.modules.send.bitcoin.SendBitcoinAddressService
import com.quantum.wallet.bankwallet.modules.send.bitcoin.SendBitcoinAmountService
import com.quantum.wallet.bankwallet.modules.send.bitcoin.SendBitcoinFeeRateService
import com.quantum.wallet.bankwallet.modules.send.bitcoin.SendBitcoinFeeService
import com.quantum.wallet.bankwallet.modules.send.bitcoin.advanced.FeeRateCaution
import com.quantum.wallet.bankwallet.modules.send.bitcoin.settings.SendBtcSettingsViewModel
import com.quantum.wallet.bankwallet.ui.compose.ComposeAppTheme
import com.quantum.wallet.bankwallet.ui.compose.TranslatableString
import com.quantum.wallet.bankwallet.ui.compose.components.InfoText
import com.quantum.wallet.bankwallet.ui.compose.components.MenuItem
import com.quantum.wallet.bankwallet.ui.compose.components.VSpacer
import com.quantum.wallet.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bitcoincore.storage.UtxoFilters
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

class SendTransactionServiceBtc(private val token: Token) : AbstractSendTransactionService(true, false) {
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

    override fun createState(): SendTransactionServiceState {
        val sendable = amountState.canBeSend && feeRateState.canBeSend && addressState.canBeSend

        val hasError = amountState.amountCaution?.isError() == true ||
                feeRateState.feeRateCaution?.isError() == true &&
                addressState.addressError != null

        val loading = !sendable && !hasError

        return SendTransactionServiceState(
            uuid = uuid,
            networkFee = networkFee,
            cautions = listOfNotNull(amountState.amountCaution, feeRateState.feeRateCaution).map(HSCaution::toCautionViewItem),
            sendable = sendable,
            loading = loading,
            fields = fields,
        )
    }

    override suspend fun setSendTransactionData(data: SendTransactionData) {
        check(data is SendTransactionData.Btc)

        memo = data.memo
        changeToFirstInput = data.changeToFirstInput
        utxoFilters = data.utxoFilters

        data.recommendedGasRate?.let {
            feeRateService.setRecommendedAndMin(it, it)
        }

        feeService.setMemo(memo)
        feeService.setChangeToFirstInput(changeToFirstInput)
        feeService.setUtxoFilters(utxoFilters)

        amountService.setMemo(memo)
        amountService.setUserMinimumSendAmount(data.minimumSendAmount)
        amountService.setChangeToFirstInput(changeToFirstInput)
        amountService.setUtxoFilters(utxoFilters)
        amountService.setAmount(data.amount)

        addressService.setAddress(Address(data.address))
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
            HSFee(
                coinCode = viewModel.token.coin.code,
                coinDecimal = viewModel.coinMaxAllowedDecimals,
                fee = uiState.fee,
                amountInputType = AmountInputType.COIN,
                rate = uiState.rate,
                navController = navController
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