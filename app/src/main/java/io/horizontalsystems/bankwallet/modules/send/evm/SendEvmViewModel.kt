package io.horizontalsystems.bankwallet.modules.send.evm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.collectWith
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISendEthereumAdapter
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.send.SendAmountAdvancedService
import io.horizontalsystems.bankwallet.modules.send.SendUiState
import io.horizontalsystems.bankwallet.modules.xrate.XRateService
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
    private val showAddressInput = addressService.predefinedAddress == null

    var uiState by mutableStateOf(
        SendUiState(
            availableBalance = amountState.availableBalance,
            amountCaution = amountState.amountCaution,
            addressError = addressState.addressError,
            canBeSend = amountState.canBeSend && addressState.canBeSend,
            showAddressInput = showAddressInput
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
            showAddressInput = showAddressInput,
        )
    }

    fun getSendData(): SendEvmData? {
        val tmpEvmAmount = amountState.evmAmount ?: return null
        val evmAddress = addressState.evmAddress ?: return null

        val transactionData = adapter.getTransactionData(tmpEvmAmount, evmAddress)

        return SendEvmData(transactionData)
    }
}
