package cash.p.terminal.modules.multiswap.sendtransaction.services

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import cash.p.terminal.core.App
import io.horizontalsystems.core.logger.AppLogger
import cash.p.terminal.core.ISendZcashAdapter
import cash.p.terminal.core.ethereum.CautionViewItem
import cash.p.terminal.core.isNative
import cash.p.terminal.core.managers.PendingTransactionRegistrar
import cash.p.terminal.core.providers.AppConfigProvider
import cash.p.terminal.entities.Address
import cash.p.terminal.entities.CoinValue
import cash.p.terminal.entities.PendingTransactionDraft
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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject

class SendTransactionServiceZCash(
    token: Token
) : ISendTransactionService<ISendZcashAdapter>(token) {

    private val xRateService = XRateService(App.marketKit, App.currencyManager.baseCurrency)
    private val amountService = SendAmountService(
        amountValidator = AmountValidator(),
        coinCode = wallet.token.coin.code,
        availableBalance = adapter.maxSpendableBalance,
        leaveSomeBalanceForFee = wallet.token.type.isNative
    )
    private val addressService = SendZCashAddressService(adapter)

    val blockchainType = wallet.token.blockchainType
    val coinMaxAllowedDecimals = wallet.token.decimals
    val fiatMaxAllowedDecimals = AppConfigProvider.fiatDecimal

    private var pendingTxId: String? = null
    private val pendingRegistrar: PendingTransactionRegistrar by inject(PendingTransactionRegistrar::class.java)

    private var amountState = amountService.stateFlow.value
    private var addressState = addressService.stateFlow.value
    private var memo: String = ""

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
        val coinValue = CoinValue(token, adapter.fee.value)
        val primaryAmountInfo = SendModule.AmountInfo.CoinValueInfo(coinValue)
        val secondaryAmountInfo = rate?.let {
            SendModule.AmountInfo.CurrencyValueInfo(
                CurrencyValue(
                    it.currency,
                    it.value * adapter.fee.value
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
        availableBalance = adapter.maxSpendableBalance,
        networkFee = feeAmountData,
        cautions = cautions,
        sendable = sendable,
        loading = loading,
        fields = fields
    )

    override suspend fun setSendTransactionData(data: SendTransactionData) {
        check(data is SendTransactionData.Btc)
        memo = data.memo

        amountService.setAmount(data.amount)
        coroutineScope.launch {
            addressService.setAddress(
                Address(
                    hex = data.address,
                    blockchainType = blockchainType
                )
            )
        }
    }

    override fun hasSettings() = false

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

    override suspend fun sendTransaction(mevProtectionEnabled: Boolean): SendTransactionResult {
        try {
            val sdkBalance = adapterManager.getBalanceAdapterForWallet(wallet)
                ?.balanceData?.available ?: amountState.availableBalance
            val draft = PendingTransactionDraft(
                wallet = wallet,
                token = wallet.token,
                amount = amountState.amount!!,
                fee = adapter.fee.firstOrNull(),
                sdkBalanceAtCreation = sdkBalance,
                fromAddress = "",  // ZCash doesn't require from address
                toAddress = addressState.address!!.hex,
                memo = memo,
                txHash = null  // ZCash doesn't return hash immediately
            )

            pendingTxId = pendingRegistrar.register(draft)

            val txId = adapter.send(
                amount = amountState.amount!!,
                address = addressState.address!!.hex,
                memo = memo,
                logger = logger
            )
            pendingTxId?.let {
                pendingRegistrar.updateTxId(it, txId.byteArray.toHexReversed())
            }

            return SendTransactionResult.ZCash(SendResult.Sent(txId.byteArray.toHexReversed()))
        } catch (e: Throwable) {
            pendingTxId?.let {
                pendingRegistrar.deleteFailed(it)
            }

            cautions = listOf(createCaution(e))
            emitState()
            throw e
        }
    }
}
