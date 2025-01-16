package io.horizontalsystems.bankwallet.modules.send.evm

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.collectWith
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISendEthereumAdapter
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.amount.SendAmountService
import io.horizontalsystems.bankwallet.modules.send.SendUiState
import io.horizontalsystems.bankwallet.modules.xrate.XRateService
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal
import java.math.RoundingMode

class SendEvmViewModel(
    val wallet: Wallet,
    val sendToken: Token,
    val adapter: ISendEthereumAdapter,
    private val xRateService: XRateService,
    private val amountService: SendAmountService,
    private val addressService: SendEvmAddressService,
    val coinMaxAllowedDecimals: Int,
    private val showAddressInput: Boolean,
    private val connectivityManager: ConnectivityManager,
    private val currency: Currency
) : ViewModelUiState<SendUiState>() {
    val fiatMaxAllowedDecimals = App.appConfigProvider.fiatDecimal

    private var amountState = amountService.stateFlow.value
    private var addressState = addressService.stateFlow.value
    private var coinAmount: BigDecimal? = null
    private var fiatAmount: BigDecimal? = null

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

    override fun createState() = SendUiState(
        availableBalance = amountState.availableBalance,
        coinAmount = coinAmount,
        fiatAmount = fiatAmount,
        amountCaution = amountState.amountCaution,
        addressError = addressState.addressError,
        canBeSend = amountState.canBeSend && addressState.canBeSend,
        showAddressInput = showAddressInput,
        canBeSendToAddress = addressState.canBeSend,
        address = addressState.address,
        currency = currency
    )

    fun onEnterAmount(amount: BigDecimal?) {
        this.coinAmount = amount
        refreshFiatAmount()
        amountService.setAmount(amount)
    }

    fun onEnterFiatAmount(fiatAmount: BigDecimal?) {
        this.fiatAmount = fiatAmount
        refreshCoinAmount()
        amountService.setAmount(coinAmount)
    }

    fun onEnterAmountPercentage(percentage: Int) {
        val amount = amountState.availableBalance
            .times(BigDecimal(percentage / 100.0))
            .setScale(coinMaxAllowedDecimals, RoundingMode.DOWN)
            .stripTrailingZeros()

        onEnterAmount(amount)
    }

    fun onEnterAddress(address: Address?) {
        Log.e("AAA", "onEnterAddress: $address")
        addressService.setAddress(address)
    }

    private fun handleUpdatedAmountState(amountState: SendAmountService.State) {
        this.amountState = amountState

        emitState()
    }

    private fun handleUpdatedAddressState(addressState: SendEvmAddressService.State) {
        this.addressState = addressState

        emitState()
    }

    fun getSendData(): SendEvmData? {
        val tmpAmount = amountState.amount ?: return null
        val evmAddress = addressState.evmAddress ?: return null

        val transactionData = adapter.getTransactionData(tmpAmount, evmAddress)

        return SendEvmData(transactionData)
    }

    fun hasConnection(): Boolean {
        return connectivityManager.isConnected
    }

    private fun refreshFiatAmount() {
        fiatAmount = coinAmount?.let { amount ->
            coinRate?.let { coinPrice ->
                (amount * coinPrice.value).setScale(currency.decimal, RoundingMode.DOWN).stripTrailingZeros()
            }
        }
    }

    private fun refreshCoinAmount() {
        coinAmount = fiatAmount?.let { fiatAmount ->
            coinRate?.let { coinPrice ->
                fiatAmount.divide(coinPrice.value, sendToken.decimals, RoundingMode.DOWN).stripTrailingZeros()
            }
        }
    }
}
