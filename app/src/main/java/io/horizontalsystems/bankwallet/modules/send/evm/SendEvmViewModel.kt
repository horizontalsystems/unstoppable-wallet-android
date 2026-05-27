package io.horizontalsystems.bankwallet.modules.send.evm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.collectWith
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.ISendEthereumAdapter
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.isNative
import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.providers.AppConfigProvider
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.amount.AmountValidator
import io.horizontalsystems.bankwallet.modules.amount.SendAmountService
import io.horizontalsystems.bankwallet.modules.send.SendUiState
import io.horizontalsystems.bankwallet.modules.xrate.XRateService
import java.math.BigDecimal
import java.math.RoundingMode

@HiltViewModel(assistedFactory = SendEvmViewModel.Factory::class)
class SendEvmViewModel @AssistedInject constructor(
    @Assisted val wallet: Wallet,
    @Assisted private val address: Address,
    @Assisted private val hideAddress: Boolean,
    adapterManager: IAdapterManager,
    marketKit: MarketKitWrapper,
    currencyManager: CurrencyManager,
    private val connectivityManager: ConnectivityManager,
    appConfigProvider: AppConfigProvider,
) : ViewModelUiState<SendUiState>() {

    val adapter: ISendEthereumAdapter
    private val xRateService: XRateService
    private val amountService: SendAmountService
    private val addressService: SendEvmAddressService

    val sendToken = wallet.token
    val coinMaxAllowedDecimals = wallet.token.decimals
    val fiatMaxAllowedDecimals: Int
    private val showAddressInput = !hideAddress

    private var amountState: SendAmountService.State
    private var addressState: SendEvmAddressService.State

    var coinRate by mutableStateOf<CurrencyValue?>(null)
        private set

    init {
        adapter = adapterManager.getAdapterForWallet<ISendEthereumAdapter>(wallet)
            ?: throw IllegalArgumentException("SendEthereumAdapter is null")
        xRateService = XRateService(marketKit, currencyManager.baseCurrency)
        fiatMaxAllowedDecimals = appConfigProvider.fiatDecimal

        amountService = SendAmountService(
            AmountValidator(),
            wallet.token.coin.code,
            adapter.balanceData.available.setScale(coinMaxAllowedDecimals, RoundingMode.DOWN),
            wallet.token.type.isNative
        )
        addressService = SendEvmAddressService()

        amountState = amountService.stateFlow.value
        addressState = addressService.stateFlow.value

        coinRate = xRateService.getRate(sendToken.coin.uid)

        amountService.stateFlow.collectWith(viewModelScope) {
            handleUpdatedAmountState(it)
        }
        addressService.stateFlow.collectWith(viewModelScope) {
            handleUpdatedAddressState(it)
        }
        xRateService.getRateFlow(sendToken.coin.uid).collectWith(viewModelScope) {
            coinRate = it
        }

        addressService.setAddress(address)
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

    @AssistedFactory
    interface Factory {
        fun create(wallet: Wallet, address: Address, hideAddress: Boolean): SendEvmViewModel
    }
}
