package io.horizontalsystems.bankwallet.modules.send.evm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.collectWith
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.core.ISendEthereumAdapter
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.xrate.XRateService
import io.horizontalsystems.marketkit.models.PlatformCoin
import java.math.BigDecimal

class SendEvmViewModel(
    val wallet: Wallet,
    val sendCoin: PlatformCoin,
    val adapter: ISendEthereumAdapter,
    private val xRateService: XRateService,
    private val amountService: SendEvmAmountService,
    private val addressService: SendEvmAddressService,
    val coinMaxAllowedDecimals: Int
) : ViewModel() {
    val fiatMaxAllowedDecimals = App.appConfigProvider.fiatDecimal

    private var amountState = amountService.stateFlow.value
    private var addressState = addressService.stateFlow.value

    var uiState by mutableStateOf(
        SendEvmUiState(
            availableBalance = amountState.availableBalance,
            amountCaution = amountState.amountCaution,
            addressError = addressState.addressError,
            canBeSend = amountState.canBeSend && addressState.canBeSend,
        )
    )
        private set

    var coinRate by mutableStateOf(xRateService.getRate(sendCoin.coin.uid))
        private set

    init {
        amountService.stateFlow.collectWith(viewModelScope) {
            handleUpdatedAmountState(it)
        }
        addressService.stateFlow.collectWith(viewModelScope) {
            handleUpdatedAddressState(it)
        }
        xRateService.getRateFlow(sendCoin.coin.uid).collectWith(viewModelScope) {
            coinRate = it
        }
    }

    fun onEnterAmount(amount: BigDecimal?) {
        amountService.setAmount(amount)
    }

    fun onEnterAddress(address: Address?) {
        addressService.setAddress(address)
    }

    private fun handleUpdatedAmountState(amountState: SendEvmAmountService.State) {
        this.amountState = amountState

        emitState()
    }

    private fun handleUpdatedAddressState(addressState: SendEvmAddressService.State) {
        this.addressState = addressState

        emitState()
    }

    private fun emitState() {
        uiState = SendEvmUiState(
            availableBalance = amountState.availableBalance,
            amountCaution = amountState.amountCaution,
            addressError = addressState.addressError,
            canBeSend = amountState.canBeSend && addressState.canBeSend,
        )
    }

    fun getSendData(): SendEvmData? {
        val tmpEvmAmount = amountState.evmAmount ?: return null
        val evmAddress = addressState.evmAddress ?: return null
        val domain = addressState.address?.domain

        val transactionData = adapter.getTransactionData(tmpEvmAmount, evmAddress)
        val additionalInfo = SendEvmData.AdditionalInfo.Send(SendEvmData.SendInfo(domain))

        return SendEvmData(transactionData, additionalInfo)
    }
}

data class SendEvmUiState(
    val availableBalance: BigDecimal,
    val amountCaution: HSCaution?,
    val addressError: Throwable?,
    val canBeSend: Boolean
)
