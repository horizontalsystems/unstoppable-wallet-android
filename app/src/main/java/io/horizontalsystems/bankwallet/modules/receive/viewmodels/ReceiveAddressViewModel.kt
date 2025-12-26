package io.horizontalsystems.bankwallet.modules.receive.viewmodels

import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.IReceiveAdapter
import io.horizontalsystems.bankwallet.core.UsedAddress
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.accountTypeDerivation
import io.horizontalsystems.bankwallet.core.bitcoinCashCoinType
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.receive.ReceiveModule
import io.horizontalsystems.bankwallet.modules.receive.ReceiveModule.AdditionalData
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import java.math.BigDecimal

class ReceiveAddressViewModel(
    private val wallet: Wallet,
    private val adapterManager: IAdapterManager,
    private val isTransparentAddress: Boolean,
) : ViewModelUiState<ReceiveModule.UiState>() {

    private var viewState: ViewState = ViewState.Loading
    private var address = ""
    private var usedAddresses: List<UsedAddress> = listOf()
    private var usedChangeAddresses: List<UsedAddress> = listOf()
    private var amount: BigDecimal? = null
    private var accountActive = true
    private var blockchainName: String? = null
    private var addressFormat: String? = null
    private var addressType: String? = null
    private var mainNet = true
    private var watchAccount = wallet.account.isWatchAccount
    private val addressUriService = AddressUriService(wallet.token)

    private var addressUriState = addressUriService.stateFlow.value

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

        viewModelScope.launch {
            addressUriService.stateFlow.collect {
                handleUpdatedAddressUriState(it)
            }
        }

        setNetworkName()
    }

    private fun handleUpdatedAddressUriState(state: AddressUriService.State) {
        addressUriState = state

        emitState()
    }

    override fun createState() = ReceiveModule.UiState(
        viewState = viewState,
        address = address,
        mainNet = mainNet,
        usedAddresses = usedAddresses,
        usedChangeAddresses = usedChangeAddresses,
        showTronAlert = !accountActive,
        uri = addressUriState.uri,
        blockchainName = blockchainName,
        addressFormat = addressFormat,
        addressType = addressType,
        watchAccount = watchAccount,
        additionalItems = getAdditionalData(),
        amount = amount,
        amountString = amount?.let { App.numberFormatter.formatCoinFull(it, wallet.token.coin.code, wallet.token.decimals) },
        alertText = null,
    )

    private fun setNetworkName() {
        when (val tokenType = wallet.token.type) {
            is TokenType.Derived -> {
                addressFormat = "${tokenType.derivation.accountTypeDerivation.addressType} (${tokenType.derivation.accountTypeDerivation.rawName})"
            }

            is TokenType.AddressTyped -> {
                addressFormat = tokenType.type.bitcoinCashCoinType.title
            }

            else -> {
                if (wallet.token.blockchainType == BlockchainType.Zcash) {
                    addressType =
                        Translator.getString(if (isTransparentAddress) R.string.Balance_Zcash_Transparent else R.string.Balance_Zcash_Unified)
                } else {
                    blockchainName = wallet.token.blockchain.name
                }
            }
        }
        emitState()
    }

    private suspend fun setData() {
        val adapter = adapterManager.getReceiveAdapterForWallet(wallet)
        if (adapter != null) {
            address = getReceiveAddress(adapter, isTransparentAddress)
            addressUriService.setAddress(address)
            usedAddresses = adapter.usedAddresses(false)
            usedChangeAddresses = adapter.usedAddresses(true)
            mainNet = adapter.isMainNet
            viewState = ViewState.Success

            accountActive = try {
                adapter.isAddressActive(adapter.receiveAddress)
            } catch (e: Exception) {
                viewState = ViewState.Error(e)
                false
            }
        } else {
            viewState = ViewState.Error(NullPointerException())
        }
        emitState()
    }

    private fun getReceiveAddress(
        adapter: IReceiveAdapter,
        transparentAddress: Boolean
    ): String {
        return if (transparentAddress && adapter.receiveAddressTransparent != null) {
            adapter.receiveAddressTransparent ?: ""
        } else {
            adapter.receiveAddress
        }
    }

    private fun getAdditionalData(): List<AdditionalData> {
        val items = mutableListOf<AdditionalData>()

        if (!accountActive) {
            items.add(AdditionalData.AccountNotActive)
        }

        return items
    }

    fun onErrorClick() {
        viewModelScope.launch(Dispatchers.IO) {
            setData()
        }
    }

    fun setAmount(amount: BigDecimal?) {
        this.amount = amount

        addressUriService.setAmount(amount)

        emitState()
    }

}
