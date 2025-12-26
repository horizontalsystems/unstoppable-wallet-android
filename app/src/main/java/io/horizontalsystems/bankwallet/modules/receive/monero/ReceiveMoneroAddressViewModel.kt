package io.horizontalsystems.bankwallet.modules.receive.monero

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.adapters.MoneroAdapter
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.receive.ReceiveModule
import io.horizontalsystems.bankwallet.modules.receive.ReceiveModule.AdditionalData
import io.horizontalsystems.bankwallet.modules.receive.viewmodels.AddressUriService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

class ReceiveMoneroAddressViewModel(
    private val wallet: Wallet,
    private val adapterManager: IAdapterManager
) : ViewModelUiState<ReceiveMoneroUiState>() {

    private var viewState: ViewState = ViewState.Loading
    private var address = ""
    private var subaddresses: List<SubaddressViewItem> = listOf()
    private val watchAccount = wallet.account.isWatchAccount
    private val blockchainName = wallet.token.blockchain.name

    private var amount: BigDecimal? = null
    private var mainNet = true
    private val addressUriService = AddressUriService(wallet.token)

    private var addressUriState = addressUriService.stateFlow.value

    init {
        viewModelScope.launch {
            addressUriService.stateFlow.collect {
                handleUpdatedAddressUriState(it)
            }
        }

        viewModelScope.launch(Dispatchers.Default) {
            fetchAddress()
            emitState()
        }
    }

    private suspend fun fetchAddress() {
        try {
            val adapter = adapterManager.getAdapterForWallet<MoneroAdapter>(wallet) ?: throw ReceiveMoneroError.NoAdapter
            mainNet = adapter.isMainNet

            while (adapter.receiveAddress.isEmpty()) {
                viewState = ViewState.Loading
                emitState()
                delay(1000)
            }

            setAddress(adapter.receiveAddress)
            subaddresses = adapter.getSubaddresses().map { SubaddressViewItem(it.addressIndex, it.address, it.txsCount.toInt()) }
            viewState = ViewState.Success
        } catch (e: Throwable) {
            viewState = ViewState.Error(e)
        }
    }

    private fun handleUpdatedAddressUriState(state: AddressUriService.State) {
        addressUriState = state

        emitState()
    }

    override fun createState() = ReceiveMoneroUiState(
        viewState = viewState,
        uri = addressUriState.uri,
        address = address,
        blockchainName = blockchainName,
        watchAccount = watchAccount,
        amount = amount,
        amountString = amount?.let { App.numberFormatter.formatCoinFull(it, wallet.token.coin.code, wallet.token.decimals) },
        subaddresses = subaddresses
    )

    fun onErrorClick() {
        viewModelScope.launch(Dispatchers.Default) {
            fetchAddress()

            emitState()
        }
    }

    fun setAmount(amount: BigDecimal?) {
        this.amount = amount

        addressUriService.setAmount(amount)

        emitState()
    }

    private fun setAddress(receiveAddress: String) {
        address = receiveAddress

        addressUriService.setAddress(address)
    }

    class Factory(private val wallet: Wallet) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReceiveMoneroAddressViewModel(wallet, App.adapterManager) as T
        }
    }
}

sealed class ReceiveMoneroError : Throwable() {
    object NoAdapter : ReceiveMoneroError()
}

data class ReceiveMoneroUiState(
    override val viewState: ViewState,
    override val uri: String,
    override val address: String,
    override val blockchainName: String,
    override val watchAccount: Boolean,
    override val amount: BigDecimal?,
    override val amountString: String?,
    val subaddresses: List<SubaddressViewItem>,
) : ReceiveModule.AbstractUiState() {
    override val additionalItems = listOf<AdditionalData>()
    override val addressFormat = null
    override val addressType = null
    override val alertText = null
    override val mainNet: Boolean = true
}

@Parcelize
data class SubaddressViewItem(
    val index: Int,
    val address: String,
    val transactions: Int
) : Parcelable
