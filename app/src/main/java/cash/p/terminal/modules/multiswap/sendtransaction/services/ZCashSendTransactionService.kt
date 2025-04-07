package cash.p.terminal.modules.multiswap.sendtransaction.services

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import cash.p.terminal.core.App
import cash.p.terminal.core.AppLogger
import cash.p.terminal.core.ISendZcashAdapter
import cash.p.terminal.core.ethereum.CautionViewItem
import cash.p.terminal.core.isNative
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
import cash.p.terminal.modules.send.zcash.SendZCashAddressService
import cash.p.terminal.modules.xrate.XRateService
import cash.p.terminal.wallet.Token
import cash.z.ecc.android.sdk.ext.collectWith
import io.horizontalsystems.core.entities.CurrencyValue
import io.horizontalsystems.core.toHexReversed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ZCashSendTransactionService(
    token: Token
) : ISendTransactionService<ISendZcashAdapter>(token) {

    private val xRateService = XRateService(App.marketKit, App.currencyManager.baseCurrency)
    private val amountService = SendAmountService(
        amountValidator = AmountValidator(),
        coinCode = wallet.token.coin.code,
        availableBalance = adapter.availableBalance,
        leaveSomeBalanceForFee = wallet.token.type.isNative
    )
    private val addressService = SendZCashAddressService(adapter, null)

    val blockchainType = wallet.token.blockchainType
    val coinMaxAllowedDecimals = wallet.token.decimals
    val fiatMaxAllowedDecimals = App.appConfigProvider.fiatDecimal

    private var amountState = amountService.stateFlow.value
    private var addressState = addressService.stateFlow.value

    var coinRate by mutableStateOf(xRateService.getRate(wallet.coin.uid))
        private set

    private val logger = AppLogger("Send-${wallet.coin.code}")

    override fun start(coroutineScope: CoroutineScope) {
        xRateService.getRateFlow(wallet.coin.uid).collectWith(coroutineScope) {
            coinRate = it
        }
        amountService.stateFlow.collectWith(coroutineScope) {
            handleUpdatedAmountState(it)
        }
        addressService.stateFlow.collectWith(coroutineScope) {
            handleUpdatedAddressState(it)
        }
    }

    private val _sendTransactionSettingsFlow = MutableStateFlow(
        SendTransactionSettings.Common
    )
    override val sendTransactionSettingsFlow: StateFlow<SendTransactionSettings> =
        _sendTransactionSettingsFlow.asStateFlow()

    private val feeAmountData: SendModule.AmountData by lazy {
        val coinValue = CoinValue(token, adapter.fee)
        val primaryAmountInfo = SendModule.AmountInfo.CoinValueInfo(coinValue)
        val secondaryAmountInfo = rate?.let {
            SendModule.AmountInfo.CurrencyValueInfo(
                CurrencyValue(
                    it.currency,
                    it.value * adapter.fee
                )
            )
        }

        SendModule.AmountData(primaryAmountInfo, secondaryAmountInfo)
    }

    private var cautions: List<CautionViewItem> = listOf()
    private var sendable = false
    private var loading = true
    private var fields = listOf<DataField>()

    override fun createState() = SendTransactionServiceState(
        availableBalance = adapter.availableBalance,
        networkFee = feeAmountData,
        cautions = cautions,
        sendable = sendable,
        loading = loading,
        fields = fields
    )

    override fun setSendTransactionData(data: SendTransactionData) {
        check(data is SendTransactionData.Common)

        amountService.setAmount(data.amount)
        coroutineScope.launch {
            addressService.setAddress(
                Address(
                    hex = data.address,
                    blockchainType = data.token.blockchainType
                )
            )
        }
    }

    @Composable
    override fun GetSettingsContent(navController: NavController) = Unit

    private fun handleUpdatedAmountState(amountState: SendAmountService.State) {
        this.amountState = amountState
        sendable = amountState.canBeSend

        emitState()
    }

    private fun handleUpdatedAddressState(addressState: SendZCashAddressService.State) {
        this.addressState = addressState
        loading = false
        emitState()
    }

    override suspend fun sendTransaction(): SendTransactionResult {
        try {
            val txId = adapter.send(
                amount = amountState.amount!!,
                address = addressState.address!!.hex,
                memo = "",
                logger = logger
            )

            return SendTransactionResult.Common(SendResult.Sent(txId.byteArray.toHexReversed()))
        } catch (e: Throwable) {
            cautions = listOf(createCaution(e))
            emitState()
            throw e
        }
    }
}