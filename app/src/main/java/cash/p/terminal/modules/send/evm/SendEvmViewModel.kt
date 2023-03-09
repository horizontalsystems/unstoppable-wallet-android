package cash.p.terminal.modules.send.evm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.collectWith
import cash.p.terminal.core.App
import cash.p.terminal.core.ISendEthereumAdapter
import cash.p.terminal.entities.Address
import cash.p.terminal.entities.Wallet
import cash.p.terminal.modules.send.SendAmountAdvancedService
import cash.p.terminal.modules.send.SendUiState
import cash.p.terminal.modules.xrate.XRateService
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

class SendEvmViewModel(
        val wallet: Wallet,
        val sendToken: Token,
        val adapter: ISendEthereumAdapter,
        private val xRateService: XRateService,
        private val amountService: SendAmountAdvancedService,
        private val addressService: SendEvmAddressService,
        val coinMaxAllowedDecimals: Int
) : ViewModel() {
    val fiatMaxAllowedDecimals = App.appConfigProvider.fiatDecimal

    private var amountState = amountService.stateFlow.value
    private var addressState = addressService.stateFlow.value

    var uiState by mutableStateOf(
        SendUiState(
            availableBalance = amountState.availableBalance,
            amountCaution = amountState.amountCaution,
            addressError = addressState.addressError,
            canBeSend = amountState.canBeSend && addressState.canBeSend,
        )
    )
        private set

    var coinRate by mutableStateOf(xRateService.getRate(sendToken.coin.uid))
        private set

    init {
        amountService.stateFlow.collectWith(viewModelScope) {
            handleUpdatedAmountState(it)
        }
        addressService.stateFlow.collectWith(viewModelScope) {
            handleUpdatedAddressState(it)
        }
        xRateService.getRateFlow(sendToken.coin.uid).collectWith(viewModelScope) {
            coinRate = it
        }
    }

    fun onEnterAmount(amount: BigDecimal?) {
        amountService.setAmount(amount)
    }

    fun onEnterAddress(address: Address?) {
        addressService.setAddress(address)
    }

    private fun handleUpdatedAmountState(amountState: SendAmountAdvancedService.State) {
        this.amountState = amountState

        emitState()
    }

    private fun handleUpdatedAddressState(addressState: SendEvmAddressService.State) {
        this.addressState = addressState

        emitState()
    }

    private fun emitState() {
        uiState = SendUiState(
            availableBalance = amountState.availableBalance,
            amountCaution = amountState.amountCaution,
            addressError = addressState.addressError,
            canBeSend = amountState.canBeSend && addressState.canBeSend,
        )
    }

    fun getSendData(): SendEvmData? {
        val tmpEvmAmount = amountState.evmAmount ?: return null
        val evmAddress = addressState.evmAddress ?: return null

        val transactionData = adapter.getTransactionData(tmpEvmAmount, evmAddress)

        return SendEvmData(transactionData)
    }
}
