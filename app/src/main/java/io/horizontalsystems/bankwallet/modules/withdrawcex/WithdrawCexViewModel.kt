package io.horizontalsystems.bankwallet.modules.withdrawcex

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.core.providers.CexAsset
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.amount.SendAmountService
import io.horizontalsystems.bankwallet.modules.xrate.XRateService
import kotlinx.coroutines.launch
import java.math.BigDecimal

class WithdrawCexViewModel(
    val cexAsset: CexAsset,
    private val xRateService: XRateService,
    private val amountService: SendAmountService,
    private val addressService: SendAddressService
) : ViewModel() {
    private var address: String? = null

    private val coinUid = cexAsset.coin?.uid

    val fiatMaxAllowedDecimals = App.appConfigProvider.fiatDecimal
    val coinMaxAllowedDecimals = cexAsset.decimals

    private var networkName = cexAsset.networks.firstOrNull()?.name

    var coinRate by mutableStateOf(coinUid?.let { xRateService.getRate(it) })
        private set

    private var amountState = amountService.stateFlow.value
    private var addressState = addressService.stateFlow.value

    var uiState by mutableStateOf(
        WithdrawCexUiState(
            networkName = networkName,
            availableBalance = amountState.availableBalance,
            amountCaution = amountState.amountCaution,
            addressError = addressState.addressError,
            canBeSend = amountState.canBeSend && addressState.canBeSend
        )
    )
        private set

    init {
        coinUid?.let {
            viewModelScope.launch {
                xRateService.getRateFlow(it).collect {
                    coinRate = it
                }
            }
        }

        viewModelScope.launch {
            amountService.stateFlow.collect {
                handleUpdatedAmountState(it)
            }
        }

        viewModelScope.launch {
            addressService.stateFlow.collect {
                handleUpdatedAddressState(it)
            }
        }
    }

    private fun handleUpdatedAmountState(amountState: SendAmountService.State) {
        this.amountState = amountState

        emitState()
    }

    private fun handleUpdatedAddressState(addressState: SendAddressService.State) {
        this.addressState = addressState

        emitState()
    }

    private fun emitState() {
        viewModelScope.launch {
            uiState = WithdrawCexUiState(
                networkName = networkName,
                availableBalance = amountState.availableBalance,
                amountCaution = amountState.amountCaution,
                addressError = addressState.addressError,
                canBeSend = amountState.canBeSend && addressState.canBeSend
            )
        }
    }

    fun onEnterAmount(v: BigDecimal?) {
        amountService.setAmount(v)
    }

    fun onEnterAddress(v: String) {
        addressService.setAddress(Address(v))
    }
}

data class WithdrawCexUiState(
    val networkName: String?,
    val availableBalance: BigDecimal?,
    val amountCaution: HSCaution?,
    val addressError: Throwable?,
    val canBeSend: Boolean
)