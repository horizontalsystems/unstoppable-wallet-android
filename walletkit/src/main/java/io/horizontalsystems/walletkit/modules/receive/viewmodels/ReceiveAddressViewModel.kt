package io.horizontalsystems.walletkit.modules.receive.viewmodels

import androidx.lifecycle.viewModelScope
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.IAdapterManager
import io.horizontalsystems.walletkit.core.IReceiveAdapter
import io.horizontalsystems.walletkit.core.UsedAddress
import io.horizontalsystems.walletkit.core.ViewModelUiState
import io.horizontalsystems.walletkit.core.accountTypeDerivation
import io.horizontalsystems.walletkit.core.bitcoinCashCoinType
import io.horizontalsystems.walletkit.core.providers.Translator
import io.horizontalsystems.walletkit.entities.ViewState
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.receive.ReceiveModule
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    private var blockchainName: String? = null
    private var addressFormat: String? = null
    private var addressType: String? = null
    private var mainNet = true
    private var watchAccount = wallet.account.isWatchAccount
    private val addressUriService = AddressUriService(wallet.token)

    private var addressUriState = addressUriService.stateFlow.value

    init {
        viewModelScope.launch(Dispatchers.IO) {
            adapterManager.adaptersReadyFlow
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
        uri = addressUriState.uri,
        blockchainName = blockchainName,
        addressFormat = addressFormat,
        addressType = addressType,
        watchAccount = watchAccount,
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
                        Translator.getString(if (isTransparentAddress) R.string.Balance_Zcash_Transparent else R.string.Balance_Zcash_Shielded)
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
            address = getFreshReceiveAddress(adapter, isTransparentAddress)
            addressUriService.setAddress(address)
            usedAddresses = adapter.usedAddresses(false)
            usedChangeAddresses = adapter.usedAddresses(true)
            mainNet = adapter.isMainNet
            viewState = ViewState.Success
        } else {
            viewState = ViewState.Error(NullPointerException())
        }
        emitState()
    }

    private suspend fun getFreshReceiveAddress(
        adapter: IReceiveAdapter,
        transparentAddress: Boolean
    ): String {
        return if (transparentAddress) {
            adapter.getFreshReceiveAddressTransparent() ?: adapter.receiveAddressTransparent ?: ""
        } else {
            adapter.getFreshReceiveAddress()
        }
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
