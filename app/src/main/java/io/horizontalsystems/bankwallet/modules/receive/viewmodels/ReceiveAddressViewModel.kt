package cash.p.terminal.modules.receive.viewmodels
>>>>>>>> 11b2c0855 (Refactor Receive Address module navigation):app/src/main/java/cash.p.terminal/modules/receive/viewmodels/ReceiveAddressViewModel.kt

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.R
import cash.p.terminal.core.IAdapterManager
import cash.p.terminal.core.UsedAddress
import cash.p.terminal.core.accountTypeDerivation
import cash.p.terminal.core.bitcoinCashCoinType
import cash.p.terminal.core.factories.uriScheme
import cash.p.terminal.core.providers.Translator
import cash.p.terminal.core.utils.AddressUriParser
import cash.p.terminal.entities.AddressUri
import cash.p.terminal.entities.ViewState
import cash.p.terminal.entities.Wallet
import cash.p.terminal.modules.receive.ReceiveModule
import cash.p.terminal.modules.receive.ReceiveModule.AdditionalData
>>>>>>>> 11b2c0855 (Refactor Receive Address module navigation):app/src/main/java/cash.p.terminal/modules/receive/viewmodels/ReceiveAddressViewModel.kt
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class ReceiveAddressViewModel(
    private val wallet: Wallet,
    private val adapterManager: IAdapterManager
) : ViewModel() {

    private var viewState: ViewState = ViewState.Loading
    private var address = ""
    private var usedAddresses: List<UsedAddress> = listOf()
    private var usedChangeAddresses: List<UsedAddress> = listOf()
    private var uri = ""
    private var amount: BigDecimal? = null
    private var accountActive = true
    private var networkName = ""
    private var mainNet = true
    private var watchAccount = wallet.account.isWatchAccount
    private var alertText: ReceiveModule.AlertText? = getAlertText(watchAccount)

    var uiState by mutableStateOf(
        ReceiveModule.UiState(
            viewState = viewState,
            address = address,
            usedAddresses = usedAddresses,
            usedChangeAddresses = usedChangeAddresses,
            uri = uri,
            networkName = networkName,
            watchAccount = watchAccount,
            additionalItems = getAdditionalData(),
            amount = amount,
            alertText = alertText,
        )
    )
        private set

    init {
        viewModelScope.launch(Dispatchers.IO) {
            adapterManager.adaptersReadyObservable.asFlow()
                .collect {
                    setData()
                }
        }
        viewModelScope.launch(Dispatchers.IO) {
            setData()
        }
        setNetworkName()
    }

    private fun setNetworkName() {
        when (val tokenType = wallet.token.type) {
            is TokenType.Derived -> {
                networkName = Translator.getString(R.string.Balance_Format) + ": "
                networkName += "${tokenType.derivation.accountTypeDerivation.addressType} (${tokenType.derivation.accountTypeDerivation.rawName})"
            }

            is TokenType.AddressTyped -> {
                networkName = Translator.getString(R.string.Balance_Format) + ": "
                networkName += tokenType.type.bitcoinCashCoinType.title
            }

            else -> {
                networkName = Translator.getString(R.string.Balance_Network) + ": "
                networkName += wallet.token.blockchain.name
            }
        }
        if (!mainNet) {
            networkName += " (TestNet)"
        }
        syncState()
    }

    private fun getAlertText(watchAccount: Boolean): ReceiveModule.AlertText? {
        return if (watchAccount) ReceiveModule.AlertText.Normal(
            Translator.getString(R.string.Balance_Receive_WatchAddressAlert)
        )
        else null
    }

    private suspend fun setData() {
        val adapter = adapterManager.getReceiveAdapterForWallet(wallet)
        if (adapter != null) {
            address = adapter.receiveAddress
            usedAddresses = adapter.usedAddresses(false)
            usedChangeAddresses = adapter.usedAddresses(true)
            uri = getUri()
            accountActive = adapter.isAddressActive(adapter.receiveAddress)
            mainNet = adapter.isMainNet
            viewState = ViewState.Success
        } else {
            viewState = ViewState.Error(NullPointerException())
        }
        withContext(Dispatchers.Main) {
            syncState()
        }
    }

    private fun getUri(): String {
        var newUri = address
        amount?.let {
            val parser = AddressUriParser(wallet.token.blockchainType, wallet.token.type)
            val addressUri = AddressUri(wallet.token.blockchainType.uriScheme ?: "")
            addressUri.address = newUri
            addressUri.parameters[AddressUri.Field.amountField(wallet.token.blockchainType)] = it.toString()
            addressUri.parameters[AddressUri.Field.BlockchainUid] = wallet.token.blockchainType.uid
            if (wallet.token.type !is TokenType.Derived && wallet.token.type !is TokenType.AddressTyped) {
                addressUri.parameters[AddressUri.Field.TokenUid] = wallet.token.type.id
            }
            newUri = parser.uri(addressUri)
        }

        return newUri
    }

    private fun syncState() {
        uiState = ReceiveModule.UiState(
            viewState = viewState,
            address = address,
            usedAddresses = usedAddresses,
            usedChangeAddresses = usedChangeAddresses,
            uri = uri,
            networkName = networkName,
            watchAccount = watchAccount,
            additionalItems = getAdditionalData(),
            amount = amount,
            alertText = alertText,
        )
    }

    private fun getAdditionalData(): List<AdditionalData> {
        val items = mutableListOf<AdditionalData>()

        if (!accountActive) {
            items.add(AdditionalData.AccountNotActive)
        }

        amount?.let {
            items.add(
                AdditionalData.Amount(
                    value = it.toString()
                )
            )
        }

        return items
    }

    fun onErrorClick() {
        viewModelScope.launch(Dispatchers.IO) {
            setData()
        }
    }

    fun setAmount(amount: BigDecimal?) {
        amount?.let {
            if (it <= BigDecimal.ZERO) {
                this.amount = null
                syncState()
                return
            }
        }
        this.amount = amount
        uri = getUri()
        syncState()
    }

}
