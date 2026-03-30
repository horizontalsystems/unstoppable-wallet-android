package cash.p.terminal.modules.receive.viewmodels

import android.os.Parcelable
import androidx.lifecycle.viewModelScope
import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.core.adapters.MoneroAdapter
import cash.p.terminal.core.managers.MoneroSubaddressInfo
import cash.p.terminal.modules.receive.ReceiveModule
import cash.p.terminal.ui_compose.entities.ViewState
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.wallet.Wallet
import io.horizontalsystems.core.DispatcherProvider
import io.horizontalsystems.core.ViewModelUiState
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.parcelize.Parcelize
import timber.log.Timber
import java.math.BigDecimal

class ReceiveMoneroViewModel(
    private val wallet: Wallet,
    private val adapterManager: IAdapterManager,
    private val localStorage: ILocalStorage,
    dispatcherProvider: DispatcherProvider,
) : ViewModelUiState<ReceiveMoneroUiState>() {

    private val addressUriService = AddressUriService(wallet.token)

    private var viewState: ViewState = ViewState.Loading
    private var address = ""
    private var currentAddressIndex = 0
    private var addressBadge = AddressBadge.UNUSED
    private var subaddresses: List<MoneroSubaddressInfo> = emptyList()
    private var amount: BigDecimal? = null
    private var isCreatingAddress = false
    private var isNewInSession = false
    private val watchAccount = wallet.account.isWatchAccount

    private var addressUriState = addressUriService.stateFlow.value

    init {
        viewModelScope.launch {
            addressUriService.stateFlow.collect {
                addressUriState = it
                emitState()
            }
        }
        viewModelScope.launch(dispatcherProvider.io) {
            adapterManager.adaptersReadyObservable.asFlow()
                .collect { fetchData() }
        }
        viewModelScope.launch(dispatcherProvider.io) {
            fetchData()
        }
    }

    private suspend fun fetchData() {
        val adapter = adapterManager.getAdapterForWallet<MoneroAdapter>(wallet)
        if (adapter == null) {
            viewState = ViewState.Loading
            emitState()
            return
        }
        val allSubaddresses = adapter.getSubaddresses()
        if (allSubaddresses.isEmpty()) {
            viewState = ViewState.Loading
            emitState()
            return
        }
        subaddresses = allSubaddresses
        val latest = allSubaddresses.last()
        currentAddressIndex = latest.index
        address = latest.address
        addressBadge = calculateBadge()
        addressUriService.setAddress(address)
        viewState = ViewState.Success
        emitState()
    }

    override fun createState() = ReceiveMoneroUiState(
        viewState = viewState,
        uri = addressUriState.uri,
        address = address,
        blockchainName = wallet.token.blockchain.name,
        watchAccount = watchAccount,
        amount = amount,
        addressBadge = addressBadge,
        hasAddressHistory = subaddresses.count { it.index != currentAddressIndex } > 0,
        isCreatingAddress = isCreatingAddress,
    )

    fun createNewAddress() {
        if (isCreatingAddress) return
        isCreatingAddress = true
        emitState()

        viewModelScope.launch {
            try {
                val adapter = adapterManager.getAdapterForWallet<MoneroAdapter>(wallet)
                    ?: return@launch
                val newAddress = adapter.createNewSubaddress()
                address = newAddress
                subaddresses = adapter.getSubaddresses()
                currentAddressIndex = subaddresses.lastOrNull()?.index ?: 0
                isNewInSession = true
                addressBadge = AddressBadge.NEW
                addressUriService.setAddress(address)
            } catch (e: Exception) {
                Timber.e(e, "Failed to create new Monero subaddress")
            } finally {
                isCreatingAddress = false
                emitState()
            }
        }
    }

    fun setAmount(amount: BigDecimal?) {
        if (amount != null && amount <= BigDecimal.ZERO) {
            this.amount = null
        } else {
            this.amount = amount
        }
        addressUriService.setAmount(this.amount)
        emitState()
    }

    fun onErrorClick() {
        viewModelScope.launch {
            fetchData()
        }
    }

    val skipNewAddressConfirm: Boolean
        get() = localStorage.moneroSkipNewAddressConfirm

    fun setSkipNewAddressConfirm(skip: Boolean) {
        localStorage.moneroSkipNewAddressConfirm = skip
    }

    fun getSubaddressesForHistory(): List<MoneroSubaddressInfo> = subaddresses

    private fun calculateBadge(): AddressBadge {
        if (isNewInSession) return AddressBadge.NEW
        val current = subaddresses.find { it.index == currentAddressIndex }
        return if (current != null && current.receivedAmount > 0) {
            AddressBadge.USED
        } else {
            AddressBadge.UNUSED
        }
    }
}

data class ReceiveMoneroUiState(
    override val viewState: ViewState,
    override val alertText: ReceiveModule.AlertText? = null,
    override val uri: String = "",
    override val address: String = "",
    override val mainNet: Boolean = true,
    override val blockchainName: String? = null,
    override val addressFormat: String? = null,
    override val additionalItems: List<ReceiveModule.AdditionalData> = emptyList(),
    override val watchAccount: Boolean = false,
    override val amount: BigDecimal? = null,
    val addressBadge: AddressBadge = AddressBadge.UNUSED,
    val hasAddressHistory: Boolean = false,
    val isCreatingAddress: Boolean = false,
) : ReceiveModule.AbstractUiState()

enum class AddressBadge { NEW, UNUSED, USED }

@Parcelize
data class MoneroUsedAddressesParams(
    val subaddresses: List<MoneroSubaddressParcelable>,
) : Parcelable

@Parcelize
data class MoneroSubaddressParcelable(
    val index: Int,
    val address: String,
    val receivedAmount: Long,
) : Parcelable
