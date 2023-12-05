package io.horizontalsystems.bankwallet.modules.receive.address

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.accountTypeDerivation
import io.horizontalsystems.bankwallet.core.bitcoinCashCoinType
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.receive.address.ReceiveAddressModule.AdditionalData
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import java.math.BigDecimal

class ReceiveAddressViewModel(
    private val wallet: Wallet,
    private val adapterManager: IAdapterManager
) : ViewModel() {

    private var viewState: ViewState = ViewState.Loading
    private var address = ""
    private var amount: BigDecimal? = null
    private var accountActive = true
    private var networkName = ""
    private var mainNet = true
    private var watchAccount = wallet.account.isWatchAccount
    private var alertText: ReceiveAddressModule.AlertText = getAlertText(watchAccount)

    var uiState by mutableStateOf(
        ReceiveAddressModule.UiState(
            viewState = viewState,
            address = address,
            networkName = networkName,
            watchAccount = watchAccount,
            additionalItems = getAdditionalData(),
            amount = amount,
            alertText = alertText,
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

    private fun getAlertText(watchAccount: Boolean): ReceiveAddressModule.AlertText {
        return when {
            watchAccount -> ReceiveAddressModule.AlertText.Normal(
                Translator.getString(R.string.Balance_Receive_WatchAddressAlert)
            )
            else -> ReceiveAddressModule.AlertText.Normal(
                Translator.getString(R.string.Balance_Receive_AddressAlert)
            )
        }
    }

    private fun setData() {
        val adapter = adapterManager.getReceiveAdapterForWallet(wallet)
        if (adapter != null) {
            address = adapter.receiveAddress
            accountActive = adapter.isAccountActive
            mainNet = adapter.isMainNet
            viewState = ViewState.Success
        } else {
            viewState = ViewState.Error(NullPointerException())
        }
        syncState()
    }

    private fun syncState() {
        uiState = ReceiveAddressModule.UiState(
            viewState = viewState,
            address = address,
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
        setData()
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
        syncState()
    }

}
