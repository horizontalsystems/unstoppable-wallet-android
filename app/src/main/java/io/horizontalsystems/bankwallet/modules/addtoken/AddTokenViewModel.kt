package io.horizontalsystems.bankwallet.modules.addtoken

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.CustomToken
import io.horizontalsystems.bankwallet.entities.blockchainType
import io.horizontalsystems.bankwallet.modules.swap.settings.Caution
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.marketkit.models.PlatformCoin
import io.reactivex.disposables.CompositeDisposable

class AddTokenViewModel(private val addTokenService: AddTokenService) : ViewModel() {

    val loadingLiveData = MutableLiveData<Boolean>()
    val cautionLiveData = MutableLiveData<Caution?>()
    val viewItemLiveData = MutableLiveData<AddTokenModule.ViewItem?>()
    val buttonEnabledLiveData = MutableLiveData<Boolean>()
    val showSuccess = SingleLiveEvent<Unit>()

    private var disposables = CompositeDisposable()

    init {
        observeState()
    }

    private fun observeState() {
        addTokenService.stateObservable
            .subscribe {
                sync(it)
            }.let {
                disposables.add(it)
            }
    }

    fun onTextChange(text: CharSequence?) {
        addTokenService.set(text.toString().trim())
    }


    override fun onCleared() {
        addTokenService.onCleared()
        disposables.clear()
        super.onCleared()
    }

    fun onAddClick() {
        addTokenService.save()
        showSuccess.call()
    }

    private fun sync(state: AddTokenModule.State) {
        loadingLiveData.postValue(state == AddTokenModule.State.Loading)

        viewItemLiveData.postValue(getViewItemByState(state))

        buttonEnabledLiveData.postValue(state is AddTokenModule.State.Fetched)

        val caution = when (state) {
            is AddTokenModule.State.Failed -> {
                Caution(getErrorText(state.error), Caution.Type.Error)
            }
            is AddTokenModule.State.AlreadyExists -> {
                Caution(Translator.getString(R.string.AddToken_CoinAlreadyInListWarning), Caution.Type.Warning)
            }
            else -> null
        }

        cautionLiveData.postValue(caution)
    }

    private fun getViewItemByState(state: AddTokenModule.State): AddTokenModule.ViewItem? {
        return when (state) {
            is AddTokenModule.State.AlreadyExists -> getPlatformCoinsViewItem(state.platformCoins)
            is AddTokenModule.State.Fetched -> getCustomTokensViewItem(state.customTokens)
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

    private fun getCustomTokensViewItem(customTokens: List<CustomToken>): AddTokenModule.ViewItem {
        return AddTokenModule.ViewItem(
            coinType = customTokens.mapNotNull { it.coinType.blockchainType }.joinToString(separator = " / "),
            coinName = customTokens.firstOrNull()?.coinName,
            symbol = customTokens.firstOrNull()?.coinCode,
            decimals = customTokens.firstOrNull()?.decimal
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
