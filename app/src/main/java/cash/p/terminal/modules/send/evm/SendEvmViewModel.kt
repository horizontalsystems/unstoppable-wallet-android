package cash.p.terminal.modules.send.evm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.collectWith
import cash.p.terminal.core.App
import cash.p.terminal.core.ISendEthereumAdapter
import io.horizontalsystems.core.ViewModelUiState
import cash.p.terminal.core.managers.ConnectivityManager
import cash.p.terminal.entities.Address
import cash.p.terminal.modules.amount.SendAmountService
import cash.p.terminal.modules.send.SendUiState
import cash.p.terminal.modules.xrate.XRateService
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.Wallet
import java.math.BigDecimal

internal class SendEvmViewModel(
    val wallet: Wallet,
    sendToken: Token,
    val adapter: ISendEthereumAdapter,
    xRateService: XRateService,
    private val amountService: SendAmountService,
    private val addressService: SendEvmAddressService,
    val coinMaxAllowedDecimals: Int,
    private val showAddressInput: Boolean,
    private val connectivityManager: ConnectivityManager,
    private val address: Address
) : ViewModelUiState<SendUiState>() {
    val fiatMaxAllowedDecimals = App.appConfigProvider.fiatDecimal

    private var amountState = amountService.stateFlow.value
    private var addressState = addressService.stateFlow.value

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
        amountCaution = amountState.amountCaution,
        canBeSend = amountState.canBeSend && addressState.canBeSend,
        showAddressInput = showAddressInput,
        address = address,
    )

    fun onEnterAmount(amount: BigDecimal?) {
        amountService.setAmount(amount)
    }

    fun onEnterAddress(address: Address?) {
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
}
