package cash.p.terminal.modules.swap

import cash.z.ecc.android.sdk.ext.collectWith
import cash.p.terminal.core.fiat.AmountTypeSwitchService
import cash.p.terminal.core.fiat.FiatService
import cash.p.terminal.entities.CoinValue
import cash.p.terminal.entities.CurrencyValue
import cash.p.terminal.modules.send.SendModule
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID
import java.util.concurrent.Executors

class SwapTokenService(
    private val switchService: AmountTypeSwitchService,
    private val fiatService: FiatService,
    private val resetAmountOnCoinSelect: Boolean,
    initialToken: Token?,
) {
    private val singleDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val coroutineScope = CoroutineScope(singleDispatcher)
    private val uuid = UUID.randomUUID().leastSignificantBits

    var token: Token? = initialToken
        private set
    private var primaryPrefix = if (switchService.amountType == AmountTypeSwitchService.AmountType.Currency) fiatService.currency.symbol else null

    private var amount = ""
    private var secondaryInfo: String = ""
    private var isEstimated = false
    private var isLoading = false
    private var amountEnabled = true

    val state: SwapXMainModule.SwapXCoinCardViewState
        get() = SwapXMainModule.SwapXCoinCardViewState(
            token = token,
            uuid = uuid,
            inputState = SwapXMainModule.SwapXAmountInputState(
                amount = amount,
                secondaryInfo = secondaryInfo,
                primaryPrefix = primaryPrefix,
                validDecimals = validDecimals,
                amountEnabled = amountEnabled,
                dimAmount = isLoading && isEstimated,
            ),
        )

    private val _stateFlow = MutableStateFlow(state)
    val stateFlow: StateFlow<SwapXMainModule.SwapXCoinCardViewState>
        get() = _stateFlow

    fun start() {
        fiatService.set(token)
        syncState()

        fiatService.fullAmountInfoFlow
            .collectWith(coroutineScope) {
                syncFullAmountInfo(it)
            }
    }

    private fun syncState() {
        _stateFlow.update { state }
    }

    private val validDecimals: Int
        get() {
            val decimals = when (switchService.amountType) {
                AmountTypeSwitchService.AmountType.Coin -> token?.decimals ?: maxValidDecimals
                AmountTypeSwitchService.AmountType.Currency -> fiatService.currency.decimal
            }
            return decimals
        }

    fun setToken(token: Token?) {
        this.token = token
        fiatService.set(token)
        syncState()
    }

    fun onSelectCoin(token: Token) {
        this.token = token
        fiatService.set(token)
        if (resetAmountOnCoinSelect) {
            onChangeAmount("")
        }
        syncState()
    }

    fun onChangeAmount(amount: String?, estimated: Boolean = false) {
        isEstimated = estimated
        val validAmount = amount?.toBigDecimalOrNull()
        val fullAmountInfo = if (estimated) {
            fiatService.buildForCoin(validAmount)
        } else {
            fiatService.buildAmountInfo(validAmount)
        }

        syncFullAmountInfo(fullAmountInfo)
    }

    fun getCoinAmount(amount: String?): BigDecimal? {
        val validAmount = amount?.toBigDecimalOrNull()
        val fullAmountInfo = fiatService.buildAmountInfo(validAmount)
        return fullAmountInfo?.coinValue?.value ?: validAmount
    }

    private fun syncFullAmountInfo(
        fullAmountInfo: FiatService.FullAmountInfo?,
    ) {
        updateInputFields()

        if (fullAmountInfo == null) {
            amount = ""
            secondaryInfo = secondaryInfoPlaceHolder() ?: ""
        } else {
            val decimals = fullAmountInfo.primaryDecimal
            val amountString = fullAmountInfo.primaryValue.setScale(decimals, RoundingMode.FLOOR)?.stripTrailingZeros()?.toPlainString()

            amount = amountString ?: ""
            secondaryInfo = fullAmountInfo.secondaryInfo?.getFormatted() ?: ""
        }
        syncState()
    }

    private fun updateInputFields() {
        primaryPrefix = if (switchService.amountType == AmountTypeSwitchService.AmountType.Currency) fiatService.currency.symbol else null
        syncState()
    }

    private fun secondaryInfoPlaceHolder(): String? = when (switchService.amountType) {
        AmountTypeSwitchService.AmountType.Coin -> {
            val amountInfo = SendModule.AmountInfo.CurrencyValueInfo(CurrencyValue(fiatService.currency, BigDecimal.ZERO))
            amountInfo.getFormatted()
        }

        AmountTypeSwitchService.AmountType.Currency -> {
            val amountInfo = token?.let {
                SendModule.AmountInfo.CoinValueInfo(
                    CoinValue(it, BigDecimal.ZERO)
                )
            }
            amountInfo?.getFormatted()
        }
    }

    fun setAmountEnabled(enabled: Boolean) {
        amountEnabled = enabled
        syncState()
    }

    fun getCoinAmount(coinAmount: BigDecimal): BigDecimal {
        return fiatService.buildForCoin(coinAmount)?.primaryValue ?: coinAmount
    }

    fun setLoading(loading: Boolean) {
        isLoading = loading
        syncState()
    }

    fun stop() {
        coroutineScope.cancel()
    }

    companion object {
        private const val maxValidDecimals = 8
    }
}
