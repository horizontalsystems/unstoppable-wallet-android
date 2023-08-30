package cash.p.terminal.modules.receive.address

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.R
import cash.p.terminal.core.IAdapterManager
import cash.p.terminal.core.IReceiveAdapter
import cash.p.terminal.core.accountTypeDerivation
import cash.p.terminal.core.bitcoinCashCoinType
import cash.p.terminal.core.providers.Translator
import cash.p.terminal.entities.ViewState
import cash.p.terminal.entities.Wallet
import cash.p.terminal.modules.receive.address.ReceiveAddressModule.DescriptionItem
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow

class ReceiveAddressViewModel(
    private val wallet: Wallet,
    private val adapterManager: IAdapterManager
) : ViewModel() {

    private var viewState: ViewState = ViewState.Loading
    private var address = ""
    private val coinCode = wallet.coin.code
    private var qrDescription = getQrDescription(wallet.account.isWatchAccount)
    private var descriptionItems: List<DescriptionItem> = listOf()
    private var warning: String? = null
    private var popupWarningItem: ReceiveAddressModule.PopupWarningItem? = null

    var uiState by mutableStateOf(
        ReceiveAddressModule.UiState(
            viewState = viewState,
            coinCode = coinCode,
            address = address,
            qrDescription = qrDescription,
            descriptionItems = descriptionItems,
            warning = warning,
            popupWarningItem = popupWarningItem
        )
    )
        private set

    init {
        viewModelScope.launch {
            adapterManager.adaptersReadyObservable.asFlow()
                .collect {
                    setData()
                }
        }
        setData()
    }

    private fun setData() {
        val adapter = adapterManager.getReceiveAdapterForWallet(wallet)
        if (adapter != null) {
            prepare(adapter)
        } else {
            viewState = ViewState.Error(NullPointerException())
        }
        syncState()
    }

    fun popupShown() {
        popupWarningItem = null
        syncState()
    }

    private fun prepare(receiveAdapter: IReceiveAdapter) {
        syncViewState(receiveAdapter.receiveAddress, receiveAdapter.isAccountActive, receiveAdapter.isMainNet)
    }

    private fun syncViewState(receiveAddress: String, accountActive: Boolean, isMainNet: Boolean) {
        viewState = ViewState.Success
        if (!accountActive) {
            popupWarningItem = ReceiveAddressModule.PopupWarningItem(
                title = Translator.getString(R.string.Tron_AddressNotActive_Title),
                description = Translator.getString(R.string.Tron_AddressNotActive_Info),
            )
        }
        address = receiveAddress
        descriptionItems = getDescriptionItems(receiveAddress, accountActive, isMainNet)
    }

    private fun syncState() {
        uiState = ReceiveAddressModule.UiState(
            viewState = viewState,
            coinCode = coinCode,
            address = address,
            qrDescription = qrDescription,
            descriptionItems = descriptionItems,
            warning = warning,
            popupWarningItem = popupWarningItem
        )
    }

    private fun getDescriptionItems(address: String? = null, accountActive: Boolean, isMainNet: Boolean): List<DescriptionItem> {
        val items = mutableListOf<DescriptionItem>()

        address?.let {
            items.add(
                DescriptionItem.Value(
                    title = Translator.getString(R.string.Balance_Address),
                    value = it
                )
            )
        }

        var value = when (val tokenType = wallet.token.type) {
            is TokenType.Derived -> tokenType.derivation.accountTypeDerivation.rawName
            is TokenType.AddressTyped -> Translator.getString(tokenType.type.bitcoinCashCoinType.title)
            else -> wallet.token.blockchain.name
        }

        if (!isMainNet) {
            value += " (TestNet)"
        }

        items.add(
            DescriptionItem.Value(
                title = Translator.getString(R.string.Balance_Network),
                value = value
            )
        )

        if (!accountActive) {
            items.add(
                DescriptionItem.ValueInfo(
                    title = Translator.getString(R.string.Balance_Receive_Account),
                    value = Translator.getString(R.string.Balance_Receive_NotActive),
                    infoTitle = Translator.getString(R.string.Tron_AddressNotActive_Title),
                    infoText = Translator.getString(R.string.Tron_AddressNotActive_Info)
                )
            )
        }

        return items
    }

    private fun getQrDescription(watchAccount: Boolean) = if (watchAccount) {
        Translator.getString(R.string.Balance_ReceiveWatchAddressHint, wallet.coin.code)
    } else {
        Translator.getString(R.string.Balance_ReceiveAddressHint, wallet.coin.code)
    }

    fun onErrorClick() {
        setData()
    }

}
