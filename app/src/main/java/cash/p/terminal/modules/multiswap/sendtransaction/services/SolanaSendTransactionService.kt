package cash.p.terminal.modules.multiswap.sendtransaction.services

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import cash.p.terminal.core.App
import cash.p.terminal.core.EvmError
import cash.p.terminal.core.ISendSolanaAdapter
import cash.p.terminal.core.adapters.SolanaAdapter
import cash.p.terminal.core.ethereum.CautionViewItem
import cash.p.terminal.core.ethereum.toCautionViewItem
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
import cash.p.terminal.modules.send.solana.SendSolanaAddressService
import cash.p.terminal.modules.xrate.XRateService
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.entities.TokenQuery
import cash.p.terminal.wallet.entities.TokenType
import cash.z.ecc.android.sdk.ext.collectWith
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.core.entities.CurrencyValue
import io.horizontalsystems.solanakit.SolanaKit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.math.BigDecimal
import java.math.RoundingMode

class SolanaSendTransactionService(
    token: Token
) : ISendTransactionService<ISendSolanaAdapter>(token) {

    private val coinMaxAllowedDecimals = wallet.token.decimals

    private val amountService = SendAmountService(
        amountValidator = AmountValidator(),
        coinCode = wallet.token.coin.code,
        availableBalance = adapter.availableBalance.setScale(
            coinMaxAllowedDecimals,
            RoundingMode.DOWN
        ),
        leaveSomeBalanceForFee = wallet.token.type.isNative,
    )
    private val solToken =
        App.coinManager.getToken(TokenQuery(BlockchainType.Solana, TokenType.Native))
            ?: throw IllegalArgumentException()
    private val balance = App.solanaKitManager.solanaKitWrapper?.solanaKit?.balance ?: 0L
    private val solBalance =
        SolanaAdapter.balanceInBigDecimal(balance, solToken.decimals) - SolanaKit.accountRentAmount
    private val addressService = SendSolanaAddressService(null)
    private val xRateService = XRateService(App.marketKit, App.currencyManager.baseCurrency)


    val blockchainType = wallet.token.blockchainType
    val feeTokenMaxAllowedDecimals = token.decimals
    val fiatMaxAllowedDecimals = App.appConfigProvider.fiatDecimal

    private var amountState = amountService.stateFlow.value
    private var addressState = addressService.stateFlow.value

    var coinRate by mutableStateOf(xRateService.getRate(token.coin.uid))
        private set
    private val decimalAmount: BigDecimal
        get() = amountState.amount!!

    private var cautions: List<CautionViewItem> = listOf()
    private var sendable = false
    private var loading = true
    private var fields = listOf<DataField>()

    private val _sendTransactionSettingsFlow = MutableStateFlow(
        SendTransactionSettings.Common
    )
    override val sendTransactionSettingsFlow: StateFlow<SendTransactionSettings> =
        _sendTransactionSettingsFlow.asStateFlow()

    private val feeAmountData: SendModule.AmountData by lazy {
        val coinValue = CoinValue(token, SolanaKit.fee)
        val primaryAmountInfo = SendModule.AmountInfo.CoinValueInfo(coinValue)
        val secondaryAmountInfo = rate?.let {
            SendModule.AmountInfo.CurrencyValueInfo(CurrencyValue(it.currency, it.value * SolanaKit.fee))
        }

        SendModule.AmountData(primaryAmountInfo, secondaryAmountInfo)
    }

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
        xRateService.getRateFlow(token.coin.uid).collectWith(coroutineScope) {
            coinRate = it
        }
    }

    @Composable
    override fun GetSettingsContent(navController: NavController) = Unit

    override fun setSendTransactionData(data: SendTransactionData) {
        check(data is SendTransactionData.Common)

        addressService.setAddress(
            Address(
                hex = data.address,
                blockchainType = data.token.blockchainType
            )
        )
        amountService.setAmount(data.amount)
    }

    private fun handleUpdatedAmountState(amountState: SendAmountService.State) {
        this.amountState = amountState
        sendable = amountState.canBeSend
        cautions = amountState.amountCaution?.toCautionViewItem()?.let { listOf(it) } ?: listOf()
        loading = false
        emitState()
    }

    private fun handleUpdatedAddressState(addressState: SendSolanaAddressService.State) {
        this.addressState = addressState

        emitState()
    }

    override suspend fun sendTransaction(): SendTransactionResult {
        try {
            val totalSolAmount =
                (if (token.type == TokenType.Native) decimalAmount else BigDecimal.ZERO) + SolanaKit.fee

            if (totalSolAmount > solBalance)
                throw EvmError.InsufficientBalanceWithFee

            val transaction = adapter.send(decimalAmount, addressState.evmAddress!!)

            return SendTransactionResult.Common(SendResult.Sent(transaction.transaction.hash))
        } catch (e: Throwable) {
            cautions = listOf(createCaution(e))
            emitState()
            throw e
        }
    }
}