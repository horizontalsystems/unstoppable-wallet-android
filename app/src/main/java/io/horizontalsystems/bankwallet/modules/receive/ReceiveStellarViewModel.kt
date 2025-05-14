package io.horizontalsystems.bankwallet.modules.receive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IReceiveStellarAdapter
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.receive.ReceiveModule.AdditionalData
import io.horizontalsystems.bankwallet.modules.receive.ReceiveModule.AlertText
import io.horizontalsystems.bankwallet.modules.receive.viewmodels.AddressUriService
import kotlinx.coroutines.launch
import java.math.BigDecimal

class ReceiveStellarViewModel(private val wallet: Wallet) : ViewModelUiState<ReceiveStellarUiState>() {
    private val watchAccount = wallet.account.isWatchAccount
    private val networkName = Translator.getString(R.string.Balance_Network) + ": " + wallet.token.blockchain.name
    private var address: String = ""
    private var amount: BigDecimal? = null
    private var viewState: ViewState = ViewState.Loading
    private val alertText: AlertText? = null

    private val addressUriService = AddressUriService(wallet.token)

    private var addressUriState = addressUriService.stateFlow.value

    init {
        val adapter = App.adapterManager.getReceiveAdapterForWalletT<IReceiveStellarAdapter>(wallet)

        if (adapter == null) {
            viewState = ViewState.Error(ReceiveStellarError.NoAdapter)
        } else {
            viewState = ViewState.Success
            address = adapter.receiveAddress
        }

        viewModelScope.launch {
            addressUriService.stateFlow.collect {
                handleUpdatedAddressUriState(it)
            }
        }

        addressUriService.setAddress(address)

        emitState()
    }

    private fun handleUpdatedAddressUriState(state: AddressUriService.State) {
        addressUriState = state

        emitState()
    }

    override fun createState() = ReceiveStellarUiState(
        viewState = viewState,
        alertText = alertText,
        uri = addressUriState.uri,
        address = address,
        networkName = networkName,
        additionalItems = listOf(),
        watchAccount = watchAccount,
        amount = amount,
    )

    fun setAmount(amount: BigDecimal?) {
        this.amount = amount

        addressUriService.setAmount(amount)

        emitState()
    }

    fun onErrorClick() {
        TODO("Not yet implemented")
    }

    class Factory(private val wallet: Wallet) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReceiveStellarViewModel(wallet) as T
        }
    }
}

sealed class ReceiveStellarError : Throwable() {
    object NoAdapter : ReceiveStellarError()
}

data class ReceiveStellarUiState(
    override val viewState: ViewState,
    override val alertText: AlertText?,
    override val uri: String,
    override val address: String,
    override val networkName: String,
    override val additionalItems: List<AdditionalData>,
    override val watchAccount: Boolean,
    override val amount: BigDecimal?,
) : ReceiveModule.AbstractUiState()
