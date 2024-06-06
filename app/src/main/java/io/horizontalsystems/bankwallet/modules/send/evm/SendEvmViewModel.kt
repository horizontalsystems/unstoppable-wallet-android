package io.horizontalsystems.bankwallet.modules.send.evm

import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.ISendEthereumAdapter
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.amount.SendAmountService
import io.horizontalsystems.bankwallet.modules.multiswap.FiatService
import io.horizontalsystems.bankwallet.modules.send.SendUiState
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

class SendEvmViewModel(
    val wallet: Wallet,
    val sendToken: Token,
    val adapter: ISendEthereumAdapter,
    private val amountService: SendAmountService,
    private val addressService: SendEvmAddressService,
    val coinMaxAllowedDecimals: Int,
    private val showAddressInput: Boolean,
    private val connectivityManager: ConnectivityManager,
    private val currencyManager: CurrencyManager,
    private val fiatService: FiatService
) : ViewModelUiState<SendUiState>() {
    private var amountState = amountService.stateFlow.value
    private var fiatAmountState = fiatService.stateFlow.value
    private var addressState = addressService.stateFlow.value
    private val currency = currencyManager.baseCurrency

    init {
        fiatService.setCurrency(currency)
        fiatService.setToken(sendToken)

        viewModelScope.launch {
            fiatService.stateFlow.collect {
                handleUpdatedFiatAmountState(it)
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

    override fun createState() = SendUiState(
        availableBalance = amountState.availableBalance,
        amountCaution = amountState.amountCaution,
        addressError = addressState.addressError,
        canBeSend = amountState.canBeSend && addressState.canBeSend,
        showAddressInput = showAddressInput,
        currency = currency,
        amount = amountState.amount,
        fiatAmountInputEnabled = fiatAmountState.coinPrice != null,
        fiatAmount = fiatAmountState.fiatAmount
    )

    fun onEnterAmount(amount: BigDecimal?) = amountService.setAmount(amount)
    fun onEnterAmountPercentage(percentage: Int) {
        val availableBalance = amountState.availableBalance ?: return

        val amount = availableBalance
            .times(BigDecimal(percentage / 100.0))
            .setScale(sendToken.decimals, RoundingMode.DOWN)
            .stripTrailingZeros()

        amountService.setAmount(amount)
    }

    fun onEnterFiatAmount(v: BigDecimal?) = fiatService.setFiatAmount(v)
    fun onEnterAddress(address: Address?) = addressService.setAddress(address)

    private fun handleUpdatedAmountState(amountState: SendAmountService.State) {
        this.amountState = amountState
        fiatService.setAmount(amountState.amount)

        emitState()
    }

    private fun handleUpdatedFiatAmountState(fiatAmountState: FiatService.State) {
        this.fiatAmountState = fiatAmountState
        amountService.setAmount(fiatAmountState.amount)

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
}
