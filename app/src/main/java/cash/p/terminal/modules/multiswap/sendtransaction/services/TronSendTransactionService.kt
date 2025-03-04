package cash.p.terminal.modules.multiswap.sendtransaction.services

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import cash.p.terminal.core.App
import cash.p.terminal.core.AppLogger
import cash.p.terminal.core.ISendTronAdapter
import cash.p.terminal.core.ethereum.CautionViewItem
import cash.p.terminal.core.isNative
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
import cash.p.terminal.modules.send.tron.FeeState
import cash.p.terminal.modules.send.tron.SendTronAddressService
import cash.p.terminal.modules.send.tron.SendTronConfirmationData
import cash.p.terminal.modules.xrate.XRateService
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.entities.TokenQuery
import cash.p.terminal.wallet.entities.TokenType
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.core.entities.CurrencyValue
import io.horizontalsystems.tronkit.models.Address
import io.horizontalsystems.tronkit.transaction.Fee
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

class TronSendTransactionService(
    token: Token
) : ISendTransactionService<ISendTronAdapter>(token) {

    val amountValidator = AmountValidator()
    val coinMaxAllowedDecimals = wallet.token.decimals

    val amountService = SendAmountService(
        amountValidator = amountValidator,
        coinCode = wallet.token.coin.code,
        availableBalance = adapter.balanceData.available.setScale(coinMaxAllowedDecimals, RoundingMode.DOWN),
        leaveSomeBalanceForFee = wallet.token.type.isNative
    )
    val addressService = SendTronAddressService(adapter, wallet.token, null)
    val xRateService = XRateService(App.marketKit, App.currencyManager.baseCurrency)
    val feeToken = App.coinManager.getToken(TokenQuery(BlockchainType.Tron, TokenType.Native))
        ?: throw IllegalArgumentException()

    val logger: AppLogger = AppLogger("send-tron")

    val blockchainType = wallet.token.blockchainType
    val feeTokenMaxAllowedDecimals = feeToken.decimals
    val fiatMaxAllowedDecimals = App.appConfigProvider.fiatDecimal
    private var feeState: FeeState = FeeState.Loading

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
    private var confirmationData by mutableStateOf<SendTronConfirmationData?>(null)

    private var feeAmountData: SendModule.AmountData? = null
    private var cautions: List<CautionViewItem> = listOf()
    private var sendable = false
    private var loading = true
    private var fields = listOf<DataField>()

    override fun createState() = SendTransactionServiceState(
        availableBalance = adapter.balanceData.available,
        networkFee = feeAmountData,
        cautions = cautions,
        sendable = sendable,
        loading = loading,
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
    }

    override fun setSendTransactionData(data: SendTransactionData) {
        check(data is SendTransactionData.Common)

        amountService.setAmount(data.amount)
        coroutineScope.launch {
            addressService.setAddress(
                cash.p.terminal.entities.Address(
                    hex = data.address,
                    blockchainType = token.blockchainType
                )
            )
            estimateFee()
        }
    }

    private suspend fun estimateFee() {
        try {
            val amount = amountState.amount!!
            val tronAddress = Address.fromBase58(addressState.address!!.hex)
            val fees = adapter.estimateFee(amount, tronAddress)

            var activationFee: BigDecimal? = null
            var bandwidth: String? = null
            var energy: String? = null

            fees.forEach { fee ->
                when (fee) {
                    is Fee.AccountActivation -> {
                        activationFee =
                            fee.feeInSuns.toBigDecimal().movePointLeft(feeToken.decimals)
                    }

                    is Fee.Bandwidth -> {
                        bandwidth = "${fee.points} Bandwidth"
                    }

                    is Fee.Energy -> {
                        val formattedEnergy =
                            App.numberFormatter.formatNumberShort(fee.required.toBigDecimal(), 0)
                        energy = "$formattedEnergy Energy"
                    }
                }
            }

            feeState = FeeState.Success(fees)
            val resourcesConsumed = if (bandwidth != null) {
                bandwidth + (energy?.let { " \n + $it" } ?: "")
            } else {
                energy
            }

            val totalFee = fees.sumOf { it.feeInSuns }.toBigInteger()
            val fee = totalFee.toBigDecimal().movePointLeft(feeToken.decimals)
            val isMaxAmount = amountState.availableBalance == amountState.amount!!
            val adjustedAmount = if (token == feeToken && isMaxAmount) amount - fee else amount

            val coinValue = CoinValue(feeToken, fee)
            val primaryAmountInfo = SendModule.AmountInfo.CoinValueInfo(coinValue)
            val secondaryAmountInfo = feeCoinRate?.let {
                SendModule.AmountInfo.CurrencyValueInfo(CurrencyValue(it.currency, it.value * fee))
            }
            feeAmountData = SendModule.AmountData(primaryAmountInfo, secondaryAmountInfo)

            confirmationData = SendTronConfirmationData(
                amount = adjustedAmount,
                fee = fee,
                activationFee = activationFee,
                resourcesConsumed = resourcesConsumed,
                address = addressState.address!!,
                contact = null,
                coin = wallet.coin,
                feeCoin = feeToken.coin,
                isInactiveAddress = addressState.isInactiveAddress
            )

            loading = false
            cautions = emptyList()

            emitState()
        } catch (error: Throwable) {
            logger.warning("estimate error", error)

            feeAmountData = null
            emitState()

            confirmationData =
                confirmationData?.copy(fee = null, activationFee = null, resourcesConsumed = null)
        }
    }

    override suspend fun sendTransaction(): SendTransactionResult {
        try {
            val confirmationData = confirmationData
            requireNotNull(confirmationData) { "confirmationData is null" }

            val amount = confirmationData.amount
            val transactionId = adapter.send(amount, addressState.tronAddress!!, feeState.feeLimit)

            return SendTransactionResult.Common(SendResult.Sent(transactionId))
        } catch (e: Throwable) {
            cautions = listOf(createCaution(e))
            emitState()
            throw e
        }
    }

    private fun handleUpdatedAmountState(amountState: SendAmountService.State) {
        this.amountState = amountState
        sendable = amountState.canBeSend

        emitState()
    }

    private fun handleUpdatedAddressState(addressState: SendTronAddressService.State) {
        this.addressState = addressState
        emitState()
    }

    @Composable
    override fun GetSettingsContent(navController: NavController) = Unit
}