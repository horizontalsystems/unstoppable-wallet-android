package cash.p.terminal.modules.multiswap.sendtransaction.services

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import cash.p.terminal.core.App
import cash.p.terminal.core.AppLogger
import cash.p.terminal.core.adapters.BinanceAdapter
import cash.p.terminal.core.ethereum.CautionViewItem
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
import cash.p.terminal.modules.send.binance.SendBinanceAddressService
import cash.p.terminal.modules.send.binance.SendBinanceFeeService
import cash.p.terminal.modules.xrate.XRateService
import cash.p.terminal.wallet.Token
import cash.z.ecc.android.sdk.ext.collectWith
import io.horizontalsystems.core.entities.CurrencyValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BinanceChainSendTransactionService(
    token: Token
) : ISendTransactionService<BinanceAdapter>(token) {

    private val amountService =
        SendAmountService(AmountValidator(), wallet.coin.code, adapter.availableBalance)
    private val addressService = SendBinanceAddressService(adapter, null)
    private val feeService = SendBinanceFeeService(adapter, wallet.token, App.feeCoinProvider)
    private val xRateService = XRateService(App.marketKit, App.currencyManager.baseCurrency)

    val blockchainType = wallet.token.blockchainType
    val feeToken by feeService::feeToken
    val feeTokenMaxAllowedDecimals = feeToken.decimals

    val coinMaxAllowedDecimals = wallet.token.decimals
    val fiatMaxAllowedDecimals = App.appConfigProvider.fiatDecimal

    private var amountState = amountService.stateFlow.value
    private var addressState = addressService.stateFlow.value
    private var feeState = feeService.stateFlow.value

    var coinRate by mutableStateOf(xRateService.getRate(wallet.coin.uid))
        private set
    var feeCoinRate by mutableStateOf(xRateService.getRate(feeToken.coin.uid))
        private set

    private val logger = AppLogger("Send-${wallet.coin.code}")

    private val _sendTransactionSettingsFlow = MutableStateFlow(
        SendTransactionSettings.Common
    )
    override val sendTransactionSettingsFlow: StateFlow<SendTransactionSettings> =
        _sendTransactionSettingsFlow.asStateFlow()

    private var feeAmountData: SendModule.AmountData? = null
    private var cautions: List<CautionViewItem> = listOf()
    private var sendable = false
    private var loading = true
    private var fields = listOf<DataField>()

    override fun createState() = SendTransactionServiceState(
        networkFee = feeAmountData,
        cautions = cautions,
        sendable = sendable,
        loading = loading,
        fields = fields
    )

    override fun start(coroutineScope: CoroutineScope) {
        amountService.stateFlow.collectWith(coroutineScope) {
            handleUpdatedAmountState(it)
        }
        addressService.stateFlow.collectWith(coroutineScope) {
            handleUpdatedAddressState(it)
        }
        feeService.stateFlow.collectWith(coroutineScope) {
            handleUpdatedFeeState(it)
        }
        xRateService.getRateFlow(wallet.coin.uid).collectWith(coroutineScope) {
            coinRate = it
        }
        xRateService.getRateFlow(feeToken.coin.uid).collectWith(coroutineScope) {
            feeAmountData = SendModule.AmountData(
                SendModule.AmountInfo.CoinValueInfo(
                    CoinValue(
                        feeToken,
                        feeService.stateFlow.value.fee
                    )
                ),
                rate?.let {
                    SendModule.AmountInfo.CurrencyValueInfo(
                        CurrencyValue(
                            it.currency,
                            it.value * feeService.stateFlow.value.fee
                        )
                    )
                }
            )
            emitState()
        }

        feeService.start()
    }

    fun onEnterAddress(address: Address?) {
        addressService.setAddress(address)
    }

    private fun handleUpdatedAddressState(addressState: SendBinanceAddressService.State) {
        this.addressState = addressState

        emitState()
    }

    private fun handleUpdatedAmountState(amountState: SendAmountService.State) {
        this.amountState = amountState
        sendable = addressState.canBeSend

        emitState()
    }

    private fun handleUpdatedFeeState(feeState: SendBinanceFeeService.State) {
        val fee = feeService.stateFlow.value.fee
        val coinValue = CoinValue(token, fee)
        val primaryAmountInfo = SendModule.AmountInfo.CoinValueInfo(coinValue)
        val secondaryAmountInfo = rate?.let {
            SendModule.AmountInfo.CurrencyValueInfo(CurrencyValue(it.currency, it.value * fee))
        }

        SendModule.AmountData(primaryAmountInfo, secondaryAmountInfo)
        this.feeState = feeState
        loading = false
        emitState()
    }

    override fun setSendTransactionData(data: SendTransactionData) {
        check(data is SendTransactionData.Common)

        amountService.setAmount(data.amount)
        addressService.setAddress(
            Address(
                hex = data.address,
                blockchainType = data.token.blockchainType
            )
        )
    }

    @SuppressLint("CheckResult")
    override suspend fun sendTransaction(): SendTransactionResult {
        val logger = logger.getScopedUnique()
        try {
            val transactionId = adapter.send(
                amount = amountState.amount!!,
                address = addressState.address!!.hex,
                memo = null,
                logger = logger
            ).blockingGet()
            return SendTransactionResult.Common(SendResult.Sent(transactionId))
        } catch (e: Throwable) {
            cautions = listOf(createCaution(e))
            emitState()
            throw e
        }
    }

    @Composable
    override fun GetSettingsContent(navController: NavController) = Unit
}