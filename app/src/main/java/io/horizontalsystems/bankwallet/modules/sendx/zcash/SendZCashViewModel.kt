package io.horizontalsystems.bankwallet.modules.sendx.zcash

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.core.ISendZcashAdapter
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.xrate.XRateService
import kotlinx.coroutines.launch
import java.math.BigDecimal
import kotlin.math.min

class SendZCashViewModel(
    private val adapter: ISendZcashAdapter,
    val wallet: Wallet,
    private val xRateService: XRateService,
    private val amountService: SendZCashAmountService,
    private val addressService: SendZCashAddressService,
    private val memoService: SendZCashMemoService
) : ViewModel() {

    val coinMaxAllowedDecimals = min(wallet.platformCoin.decimals, App.appConfigProvider.maxDecimal)
    val fiatMaxAllowedDecimals = App.appConfigProvider.fiatDecimal
    val memoMaxLength by memoService::memoMaxLength

    private val fee = adapter.fee
    private var amountState = amountService.stateFlow.value
    private var addressState = addressService.stateFlow.value
    private var memoState = memoService.stateFlow.value

    var uiState by mutableStateOf(
        SendZCashUiState(
            fee = fee,
            availableBalance = amountState.availableBalance,
            addressError = addressState.addressError,
            amountCaution = amountState.amountCaution,
            memoIsAllowed = memoState.memoIsAllowed,
            canBeSend = amountState.canBeSend && addressState.canBeSend,
        )
    )
        private set

    var coinRate by mutableStateOf(xRateService.getRate(wallet.coin.uid))
        private set

    init {
        viewModelScope.launch {
            xRateService.getRateFlow(wallet.coin.uid)
                .collect {
                    coinRate = it
                }
        }
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
            memoService.stateFlow
                .collect {
                    handleUpdatedMemoState(it)
                }
        }

        amountService.start()

        viewModelScope.launch {
            addressService.start()
        }
    }

    fun onEnterAmount(amount: BigDecimal?) {
        amountService.setAmount(amount)
    }

    fun onEnterAddress(address: Address?) {
        viewModelScope.launch {
            addressService.setAddress(address)
        }
    }

    fun onEnterMemo(memo: String) {
        memoService.setMemo(memo)
    }

    private fun handleUpdatedAmountState(amountState: SendZCashAmountService.State) {
        this.amountState = amountState

        emitState()
    }

    private fun handleUpdatedAddressState(addressState: SendZCashAddressService.State) {
        this.addressState = addressState

        memoService.setAddressType(addressState.addressType)

        emitState()
    }

    private fun handleUpdatedMemoState(memoState: SendZCashMemoService.State) {
        this.memoState = memoState

        emitState()
    }


    private fun emitState() {
        uiState = SendZCashUiState(
            availableBalance = amountState.availableBalance,
            fee = fee,
            addressError = addressState.addressError,
            amountCaution = amountState.amountCaution,
            memoIsAllowed = memoState.memoIsAllowed,
            canBeSend = amountState.canBeSend && addressState.canBeSend,
        )
    }
}

data class SendZCashUiState(
    val fee: BigDecimal,
    val availableBalance: BigDecimal,
    val addressError: Throwable?,
    val amountCaution: HSCaution?,
    val memoIsAllowed: Boolean,
    val canBeSend: Boolean
)
