package io.horizontalsystems.bankwallet.modules.send.bitcoin.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.adapters.BitcoinFeeInfo
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.send.bitcoin.SendBitcoinFeeRateService
import io.horizontalsystems.bankwallet.modules.send.bitcoin.SendBitcoinFeeService
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.launch
import java.math.BigDecimal

class SendBtcSettingsViewModel(
    private val feeRateService: SendBitcoinFeeRateService,
    private val feeService: SendBitcoinFeeService,
    val token: Token
) : ViewModelUiState<SendBtcSettingsUiState>() {
    val coinMaxAllowedDecimals = token.decimals
    val fiatMaxAllowedDecimals = App.appConfigProvider.fiatDecimal

    private var feeRateState = feeRateService.stateFlow.value
    private var bitcoinFeeInfo = feeService.bitcoinFeeInfoFlow.value
    val feeRateChangeable by feeRateService::feeRateChangeable

    private val baseCurrency = App.currencyManager.baseCurrency
    private val rate = App.marketKit.coinPrice(token.coin.uid, baseCurrency.code)?.let {
        CurrencyValue(baseCurrency, it.value)
    }

    init {
        viewModelScope.launch {
            feeRateService.stateFlow.collect {
                handleFeeRateState(it)
            }
        }

        viewModelScope.launch {
            feeService.bitcoinFeeInfoFlow.collect {
                handleBitcoinFeeInfo(it)
            }
        }
    }

    private fun handleFeeRateState(state: SendBitcoinFeeRateService.State) {
        feeRateState = state

        feeService.setFeeRate(feeRateState.feeRate)

        emitState()
    }

    private fun handleBitcoinFeeInfo(info: BitcoinFeeInfo?) {
        bitcoinFeeInfo = info

        emitState()
    }

    override fun createState() = SendBtcSettingsUiState(
        resetEnabled = !feeRateState.isRecommended,
        feeRate = feeRateState.feeRate,
        feeRateCaution = feeRateState.feeRateCaution,
        fee = bitcoinFeeInfo?.fee,
        rate = rate
    )

    fun reset() {
        feeRateService.reset()
    }

    fun updateFeeRate(v: Int) {
        feeRateService.setFeeRate(v)
    }

    fun incrementFeeRate() {
        val incremented = (feeRateState.feeRate ?: 0) + 1
        updateFeeRate(incremented)
    }

    fun decrementFeeRate() {
        var incremented = (feeRateState.feeRate ?: 0) - 1
        if (incremented < 0) {
            incremented = 0
        }
        updateFeeRate(incremented)
    }

    class Factory(
        private val feeRateService: SendBitcoinFeeRateService,
        private val feeService: SendBitcoinFeeService,
        private val token: Token
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SendBtcSettingsViewModel(feeRateService, feeService, token) as T
        }
    }
}

data class SendBtcSettingsUiState(
    val resetEnabled: Boolean,
    val feeRate: Int?,
    val feeRateCaution: HSCaution?,
    val fee: BigDecimal?,
    val rate: CurrencyValue?,
)