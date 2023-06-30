package cash.p.terminal.modules.withdrawcex

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.core.App
import cash.p.terminal.core.HSCaution
import cash.p.terminal.core.providers.CexAsset
import cash.p.terminal.core.providers.CexNetwork
import cash.p.terminal.entities.Address
import cash.p.terminal.modules.amount.SendAmountService
import cash.p.terminal.modules.xrate.XRateService
import kotlinx.coroutines.launch
import java.math.BigDecimal

class WithdrawCexViewModel(
    val cexAsset: CexAsset,
    private val xRateService: XRateService,
    private val amountService: SendAmountService,
    private val addressService: SendAddressService
) : ViewModel() {
    private val coinUid = cexAsset.coin?.uid

    val fiatMaxAllowedDecimals = App.appConfigProvider.fiatDecimal
    val coinMaxAllowedDecimals = cexAsset.decimals
    val networks = cexAsset.networks.filter { it.withdrawEnabled }
    val networkSelectionEnabled = networks.size > 1

    var coinRate by mutableStateOf(coinUid?.let { xRateService.getRate(it) })
        private set

    private var amountState = amountService.stateFlow.value
    private var addressState = addressService.stateFlow.value
    private var network: CexNetwork? = networks.firstOrNull()

    var uiState by mutableStateOf(
        WithdrawCexUiState(
            networkName = network?.name,
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
                networkName = network?.name,
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

    fun onSelectNetwork(network: CexNetwork) {
        this.network = network

        emitState()
    }
}

data class WithdrawCexUiState(
    val networkName: String?,
    val availableBalance: BigDecimal?,
    val amountCaution: HSCaution?,
    val addressError: Throwable?,
    val canBeSend: Boolean
)