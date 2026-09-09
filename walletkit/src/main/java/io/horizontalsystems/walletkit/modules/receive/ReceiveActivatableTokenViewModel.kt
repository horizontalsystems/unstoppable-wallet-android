package io.horizontalsystems.walletkit.modules.receive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.IActivatableTokenAdapter
import io.horizontalsystems.walletkit.core.IAdapterManager
import io.horizontalsystems.walletkit.core.ViewModelUiState
import io.horizontalsystems.walletkit.entities.ViewState
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.receive.viewmodels.AddressUriService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigDecimal

/** Receive state for a token that needs activation (a trust line) before it can be received. */
class ReceiveActivatableTokenViewModel(
    private val wallet: Wallet,
    private val adapterManager: IAdapterManager,
) : ViewModelUiState<ReceiveActivatableTokenUiState>() {
    private val watchAccount = wallet.account.isWatchAccount
    private val blockchainName = wallet.token.blockchain.name
    private var address: String = ""
    private var mainNet = true
    private var amount: BigDecimal? = null
    private var viewState: ViewState = ViewState.Loading

    private val addressUriService = AddressUriService(wallet.token)
    private var activated: Boolean? = null
    private var addressUriState = addressUriService.stateFlow.value

    init {
        viewModelScope.launch {
            addressUriService.stateFlow.collect {
                addressUriState = it
                emitState()
            }
        }
        viewModelScope.launch(Dispatchers.Default) {
            fetchAddress()
            emitState()
        }
    }

    private suspend fun fetchAddress() {
        try {
            val adapter = adapterManager.getAdapterForWallet<IActivatableTokenAdapter>(wallet) ?: throw NoAdapter()
            mainNet = adapter.isMainNet
            activated = adapter.isActivated()
            viewState = ViewState.Success
            address = adapter.receiveAddress
            addressUriService.setAddress(address)
        } catch (e: Throwable) {
            viewState = ViewState.Error(e)
        }
    }

    override fun createState() = ReceiveActivatableTokenUiState(
        viewState = viewState,
        uri = addressUriState.uri,
        address = address,
        mainNet = mainNet,
        blockchainName = blockchainName,
        watchAccount = watchAccount,
        amount = amount,
        amountString = amount?.let { App.numberFormatter.formatCoinFull(it, wallet.token.coin.code, wallet.token.decimals) },
        activationRequired = activated == false,
        coinCode = wallet.coin.code,
        activated = activated,
    )

    fun setAmount(amount: BigDecimal?) {
        this.amount = amount
        addressUriService.setAmount(amount)
        emitState()
    }

    fun onErrorClick() {
        viewModelScope.launch(Dispatchers.Default) {
            fetchAddress()
            emitState()
        }
    }

    fun onActivationResult(activated: Boolean) {
        if (!activated) return
        viewModelScope.launch(Dispatchers.Default) {
            fetchAddress()
            emitState()
        }
    }

    class NoAdapter : Exception()

    class Factory(private val wallet: Wallet) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReceiveActivatableTokenViewModel(wallet, App.adapterManager) as T
        }
    }
}

data class ReceiveActivatableTokenUiState(
    override val viewState: ViewState,
    override val uri: String,
    override val address: String,
    override val mainNet: Boolean,
    override val blockchainName: String,
    override val watchAccount: Boolean,
    override val amount: BigDecimal?,
    override val amountString: String?,
    val activationRequired: Boolean,
    val coinCode: String,
    /** Null until the adapter has answered. */
    val activated: Boolean?,
) : ReceiveModule.AbstractUiState() {
    override val addressFormat = null
    override val addressType = null
    override val alertText = null
}
