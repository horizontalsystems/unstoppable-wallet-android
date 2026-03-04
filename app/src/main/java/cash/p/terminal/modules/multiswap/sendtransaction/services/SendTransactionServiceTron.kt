package cash.p.terminal.modules.multiswap.sendtransaction.services

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.core.ISendTronAdapter
import cash.p.terminal.core.LocalizedException
import cash.p.terminal.core.ethereum.CautionViewItem
import cash.p.terminal.core.isNative
import cash.p.terminal.core.providers.AppConfigProvider
import cash.p.terminal.entities.Address
import cash.p.terminal.entities.CoinValue
import cash.p.terminal.modules.amount.AmountValidator
import cash.p.terminal.modules.amount.SendAmountService
import cash.p.terminal.modules.multiswap.sendtransaction.ISendTransactionService
import cash.p.terminal.modules.multiswap.sendtransaction.SendTransactionData
import cash.p.terminal.modules.multiswap.sendtransaction.SendTransactionResult
import cash.p.terminal.modules.multiswap.sendtransaction.SendTransactionServiceState
import cash.p.terminal.modules.multiswap.sendtransaction.SendTransactionSettings
import cash.p.terminal.modules.multiswap.ui.DataField
import cash.p.terminal.modules.send.SendModule
import cash.p.terminal.modules.send.SendResult
import cash.p.terminal.modules.send.tron.SendTronAddressService
import cash.p.terminal.modules.send.tron.SendTronFeeService
import cash.p.terminal.modules.xrate.XRateService
import cash.p.terminal.wallet.Token
import io.horizontalsystems.core.logger.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

class SendTransactionServiceTron(
    token: Token
) : ISendTransactionService<ISendTronAdapter>(token) {

    val amountValidator = AmountValidator()
    val coinMaxAllowedDecimals = wallet.token.decimals

    val amountService by lazy {
        val adjustedBalance = adapterManager.getAdjustedBalanceData(wallet)?.available
            ?: adapter.balanceData.available
        SendAmountService(
            amountValidator = amountValidator,
            coinCode = wallet.token.coin.code,
            availableBalance = adjustedBalance.setScale(
                coinMaxAllowedDecimals,
                RoundingMode.DOWN
            ),
            leaveSomeBalanceForFee = wallet.token.type.isNative
        )
    }
    val addressService = SendTronAddressService(adapter, wallet.token)
    val xRateService = XRateService(App.marketKit, App.currencyManager.baseCurrency)
    private val feeService = SendTronFeeService(adapter, feeToken)

    val logger: AppLogger = AppLogger("send-tron")

    val blockchainType = wallet.token.blockchainType
    val feeTokenMaxAllowedDecimals = feeToken.decimals
    val fiatMaxAllowedDecimals = AppConfigProvider.fiatDecimal
    private var feeState = feeService.stateFlow.value

    private var networkFee: SendModule.AmountData? = null
    private var sendTransactionData: SendTransactionData.Tron? = null

    private var amountState = amountService.stateFlow.value
    private var addressState = addressService.stateFlow.value

    private val _sendTransactionSettingsFlow = MutableStateFlow(
        SendTransactionSettings.Common
    )
    override val sendTransactionSettingsFlow: StateFlow<SendTransactionSettings> =
        _sendTransactionSettingsFlow.asStateFlow()


    var coinRate by mutableStateOf(xRateService.getRate(token.coin.uid))
        private set
    var feeCoinRate by mutableStateOf(xRateService.getRate(feeToken.coin.uid))
        private set

    private var cautions: List<CautionViewItem> = listOf()
    private var fields = listOf<DataField>()
    private var hasEnoughFeeBalance: Boolean = true
    private var feeCaution: CautionViewItem? = null

    override fun createState() = SendTransactionServiceState(
        availableBalance = adapterManager.getAdjustedBalanceData(wallet)?.available
            ?: adapter.balanceData.available,
        networkFee = networkFee,
        cautions = cautions,
        feeCaution = feeCaution,
        sendable = hasEnoughFeeBalance && (sendTransactionData != null || (amountState.canBeSend && feeState.canBeSend && addressState.canBeSend)),
        loading = false,
        fields = fields
    )

    override fun start(coroutineScope: CoroutineScope) {
        coroutineScope.launch {
            amountService.stateFlow.collect {
                handleUpdatedAmountState(it)
            }
        }
        coroutineScope.launch {
            addressService.stateFlow.collect {
                handleUpdatedAddressState(it)
            }
        }
        coroutineScope.launch {
            xRateService.getRateFlow(token.coin.uid).collect {
                coinRate = it
            }
        }
        coroutineScope.launch {
            xRateService.getRateFlow(feeToken.coin.uid).collect {
                feeCoinRate = it
            }
        }

        coroutineScope.launch {
            feeService.stateFlow.collect {
                handleUpdatedFeeState(it)
            }
        }
    }

    override suspend fun setSendTransactionData(data: SendTransactionData) {
        check(data is SendTransactionData.Tron)

        sendTransactionData = data

        when (data) {
            is SendTransactionData.Tron.WithContract -> {
                feeService.setContract(data.contract)
            }

            is SendTransactionData.Tron.WithCreateTransaction -> {
                feeService.setCreatedTransaction(data.transaction)
            }

            is SendTransactionData.Tron.Regular -> {
                amountService.setAmount(data.amount)
                addressService.setAddress(Address(data.address))
            }
        }
        emitState()
    }

    override suspend fun sendTransaction(mevProtectionEnabled: Boolean): SendTransactionResult {
        val transactionId = try {
            when (val tmpSendTransactionData = sendTransactionData) {
                is SendTransactionData.Tron.WithContract -> {
                    adapter.send(tmpSendTransactionData.contract, feeState.feeLimit)
                }

                is SendTransactionData.Tron.WithCreateTransaction -> {
                    adapter.send(tmpSendTransactionData.transaction)
                }

                is SendTransactionData.Tron.Regular ->
                    adapter.send(
                        amount = amountState.amount!!,
                        to = addressState.tronAddress!!,
                        feeLimit = feeState.feeLimit
                    )

                null -> {
                    adapter.send(
                        amountState.amount!!,
                        addressState.tronAddress!!,
                        feeState.feeLimit
                    )
                }
            }
        } catch (e: Throwable) {
            cautions = listOf(createCaution(e))
            emitState()
            throw e
        }

        return SendTransactionResult.Tron(SendResult.Sent(transactionId))
    }

    private fun handleUpdatedFeeState(state: SendTronFeeService.State) {
        feeState = state

        networkFee = feeState.fee?.let {
            getAmountData(CoinValue(feeToken, it))
        }

        checkFeeBalance(feeState.fee)
        emitState()
    }

    private suspend fun handleUpdatedAmountState(amountState: SendAmountService.State) {
        this.amountState = amountState
        feeService.setAmount(amountState.amount)
        emitState()
    }

    private suspend fun handleUpdatedAddressState(addressState: SendTronAddressService.State) {
        this.addressState = addressState
        feeService.setTronAddress(addressState.tronAddress)
        emitState()
    }

    private fun checkFeeBalance(fee: BigDecimal?) {
        if (fee == null) {
            hasEnoughFeeBalance = true
            feeCaution = null
            return
        }

        val trxBalance = adapter.trxBalanceData.available
        val requiredTrx = if (token.type.isNative) {
            // If sending TRX, need balance for amount + fee
            (amountState.amount ?: BigDecimal.ZERO) + fee
        } else {
            // If sending TRC20 token, need TRX only for fee
            fee
        }

        hasEnoughFeeBalance = trxBalance >= requiredTrx
        feeCaution = if (!hasEnoughFeeBalance) {
            val formattedBalance = numberFormatter.formatCoinFull(
                trxBalance,
                null,
                feeToken.decimals
            )
            createCaution(
                LocalizedException(R.string.Error_InsufficientBalanceForFee, feeToken.coin.code, formattedBalance)
            )
        } else {
            null
        }
    }

    override fun hasSettings() = false

    @Composable
    override fun GetSettingsContent(navController: NavController) = Unit
}