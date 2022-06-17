package io.horizontalsystems.bankwallet.modules.addtoken

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.collectWith
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.blockchainType
import io.horizontalsystems.bankwallet.modules.addtoken.AddTokenModule.CustomCoin
import io.horizontalsystems.bankwallet.modules.swap.settings.Caution
import io.horizontalsystems.marketkit.models.PlatformCoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AddTokenViewModel(private val addTokenService: AddTokenService) : ViewModel() {

    var buttonEnabled by mutableStateOf(false)
        private set

    var loading by mutableStateOf(false)
        private set

    var closeScreen by mutableStateOf(false)
        private set

    var showSuccessMessage by mutableStateOf(false)
        private set

    var viewItem by mutableStateOf<AddTokenModule.ViewItem?>(null)
        private set

    var caution by mutableStateOf<Caution?>(null)
        private set

    init {
        observeState()
    }

    private fun observeState() {
        addTokenService.stateFlow
            .collectWith(viewModelScope) { result ->
                result.getOrNull()?.let{
                    sync(it)
                }
            }
    }

    fun onTextChange(text: CharSequence?) {
        viewModelScope.launch {
            addTokenService.set(text.toString().trim())
        }
    }


    override fun onCleared() {
        addTokenService.onCleared()
        super.onCleared()
    }

    fun onAddClick() {
        viewModelScope.launch {
            addTokenService.save()
            showSuccessMessage = true
            delay(1500)
            closeScreen = true
        }
    }

    private fun sync(state: AddTokenModule.State) {
        loading = state == AddTokenModule.State.Loading

        viewItem = getViewItemByState(state)

        buttonEnabled = state is AddTokenModule.State.Fetched

        caution = getCaution(state)
    }

    private fun getCaution(state: AddTokenModule.State): Caution? {
        val caution = when (state) {
            is AddTokenModule.State.Failed -> {
                Caution(getErrorText(state.error), Caution.Type.Error)
            }
            is AddTokenModule.State.AlreadyExists -> {
                Caution(
                    Translator.getString(R.string.AddToken_CoinAlreadyInListWarning),
                    Caution.Type.Warning
                )
            }
            else -> null
        }
        return caution
    }

    private fun getViewItemByState(state: AddTokenModule.State): AddTokenModule.ViewItem? {
        return when (state) {
            is AddTokenModule.State.AlreadyExists -> getPlatformCoinsViewItem(state.platformCoins)
            is AddTokenModule.State.Fetched -> getCustomCoinsViewItem(state.customCoins)
            else -> null
        }
    }

    private fun getPlatformCoinsViewItem(platformCoins: List<PlatformCoin>): AddTokenModule.ViewItem {
        return AddTokenModule.ViewItem(
            coinType = platformCoins.mapNotNull { it.coinType.blockchainType }.joinToString(separator = " / "),
            coinName = platformCoins.firstOrNull()?.name,
            symbol = platformCoins.firstOrNull()?.code,
            decimals = platformCoins.firstOrNull()?.decimals
        )
    }

    private fun getCustomCoinsViewItem(customCoins: List<CustomCoin>): AddTokenModule.ViewItem {
        return AddTokenModule.ViewItem(
            coinType = customCoins.mapNotNull { it.type.blockchainType }.joinToString(separator = " / "),
            coinName = customCoins.firstOrNull()?.name,
            symbol = customCoins.firstOrNull()?.code,
            decimals = customCoins.firstOrNull()?.decimals
        )
    }

    private fun getErrorText(error: Throwable): String {
        val errorKey = when (error) {
            is AddTokenService.TokenError.NotFound -> R.string.AddEvmToken_TokenNotFound
            is AddTokenService.TokenError.InvalidReference -> R.string.AddToken_InvalidAddressError
            else -> R.string.Error
        }

        return Translator.getString(errorKey)
    }
}
