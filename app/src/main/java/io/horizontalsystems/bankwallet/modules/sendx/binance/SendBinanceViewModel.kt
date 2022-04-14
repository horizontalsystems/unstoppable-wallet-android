package io.horizontalsystems.bankwallet.modules.sendx.binance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.core.ISendBinanceAdapter
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.xrate.XRateService
import kotlinx.coroutines.launch
import java.math.BigDecimal
import kotlin.math.min

class SendBinanceViewModel(
    val wallet: Wallet,
    private val adapter: ISendBinanceAdapter,
    private val amountService: SendBinanceAmountService,
    private val addressService: SendBinanceAddressService,
    private val feeService: SendBinanceFeeService,
    private val xRateService: XRateService,
) : ViewModel() {
    val feeCoin by feeService::feeCoin
    val feeCoinMaxAllowedDecimals = min(feeCoin.decimals, App.appConfigProvider.maxDecimal)

    val coinMaxAllowedDecimals = min(wallet.platformCoin.decimals, App.appConfigProvider.maxDecimal)
    val fiatMaxAllowedDecimals = App.appConfigProvider.fiatDecimal

    private var amountState = amountService.stateFlow.value
    private var addressState = addressService.stateFlow.value
    private var feeState = feeService.stateFlow.value

    var uiState by mutableStateOf(
        SendBinanceUiState(
            availableBalance = amountState.availableBalance,
            fee = feeState.fee,
            feeCaution = feeState.feeCaution,
            amountCaution = amountState.amountCaution,
            addressError = addressState.addressError,
            canBeSend = amountState.canBeSend && addressState.canBeSend && feeState.canBeSend,
        )
    )
        private set

    var coinRate by mutableStateOf(xRateService.getRate(wallet.coin.uid))
    var feeCoinRate by mutableStateOf(xRateService.getRate(feeCoin.coin.uid))

    init {
        viewModelScope.launch {
            amountService.stateFlow
                .collect {
                    handleUpdatedAmountState(it)
                }
        }
        viewModelScope.launch {
            addressService.stateFlow
                .collect {
                    handleUpdatedAddressState(it)
                }
        }
        viewModelScope.launch {
            feeService.stateFlow
                .collect {
                    handleUpdatedFeeState(it)
                }
        }
        viewModelScope.launch {
            xRateService.getRateFlow(wallet.coin.uid)
                .collect {
                    coinRate = it
                }
        }
        viewModelScope.launch {
            xRateService.getRateFlow(feeCoin.coin.uid)
                .collect {
                    feeCoinRate = it
                }
        }

        amountService.start()
        addressService.start()
        feeService.start()
    }

    fun onEnterAmount(amount: BigDecimal?) {
        amountService.setAmount(amount)
    }

    fun onEnterAddress(address: Address?) {
        addressService.setAddress(address)
    }

    private fun handleUpdatedAddressState(addressState: SendBinanceAddressService.State) {
        this.addressState = addressState

        emitState()
    }

    private fun handleUpdatedAmountState(amountState: SendBinanceAmountService.State) {
        this.amountState = amountState

        emitState()
    }

    private fun handleUpdatedFeeState(feeState: SendBinanceFeeService.State) {
        this.feeState = feeState

        emitState()
    }

    private fun emitState() {
        uiState = SendBinanceUiState(
            availableBalance = amountState.availableBalance,
            fee = adapter.fee,
            feeCaution = feeState.feeCaution,
            amountCaution = amountState.amountCaution,
            addressError = addressState.addressError,
            canBeSend = amountState.canBeSend && addressState.canBeSend && feeState.canBeSend,
        )
    }
}

data class SendBinanceUiState(
    val availableBalance: BigDecimal,
    val fee: BigDecimal?,
    val feeCaution: HSCaution?,
    val addressError: Throwable?,
    val amountCaution: HSCaution?,
    val canBeSend: Boolean,
)
