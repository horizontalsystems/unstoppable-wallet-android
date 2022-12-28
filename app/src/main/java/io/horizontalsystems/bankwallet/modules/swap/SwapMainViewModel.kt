package io.horizontalsystems.bankwallet.modules.swap

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.fiat.AmountTypeSwitchService
import io.horizontalsystems.bankwallet.core.fiat.AmountTypeSwitchService.AmountType
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.ISwapProvider
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.WithTranslatableTitle
import io.horizontalsystems.core.ICurrencyManager
import io.reactivex.disposables.CompositeDisposable

class SwapMainViewModel(
    val service: SwapMainService,
    val switchService: AmountTypeSwitchService,
    private val currencyManager: ICurrencyManager,
) : ViewModel() {

    private val disposable = CompositeDisposable()

    val dex: SwapMainModule.Dex
        get() = service.dex

    val provider: ISwapProvider
        get() = service.dex.provider

    val providerLiveData = MutableLiveData<ISwapProvider>()

    var providerState by service::providerState

    val providerItems: List<ISwapProvider>
        get() = service.availableProviders

    val selectedProviderItem: ISwapProvider
        get() = service.currentProvider

    var amountTypeSelect by mutableStateOf(buildAmountTypeSelect())
        private set

    var amountTypeSelectEnabled by mutableStateOf(switchService.toggleAvailable)
        private  set

    init {
        service.providerObservable
            .subscribeIO {
                providerLiveData.postValue(it)
            }.let {
                disposable.add(it)
            }

        switchService.amountTypeObservable
            .subscribeIO {
                amountTypeSelect = buildAmountTypeSelect()
            }.let {
                disposable.add(it)
            }

        switchService.toggleAvailableObservable
            .subscribeIO {
                amountTypeSelectEnabled = it
            }.let {
                disposable.add(it)
            }
    }

    fun setProvider(provider: ISwapProvider) {
        service.setProvider(provider)
    }

    private fun buildAmountTypeSelect() = Select(
        selected = switchService.amountType.item,
        options = listOf(AmountTypeItem.Coin, AmountTypeItem.Currency(currencyManager.baseCurrency.code))
    )

    fun onToggleAmountType() {
        switchService.toggle()
    }

    override fun onCleared() {
        disposable.dispose()
    }

    private val AmountType.item: AmountTypeItem
        get() = when (this) {
            AmountType.Coin -> AmountTypeItem.Coin
            AmountType.Currency -> AmountTypeItem.Currency(currencyManager.baseCurrency.code)
        }

    sealed class AmountTypeItem : WithTranslatableTitle {
        object Coin : AmountTypeItem()
        class Currency(val name: String) : AmountTypeItem()

        override val title: TranslatableString
            get() = when (this) {
                Coin -> TranslatableString.ResString(R.string.Swap_AmountTypeCoin)
                is Currency -> TranslatableString.PlainString(name)
            }

        override fun equals(other: Any?): Boolean {
            return other is Coin && this is Coin || other is Currency && this is Currency && other.name == this.name
        }

        override fun hashCode() = when (this) {
            Coin -> javaClass.hashCode()
            is Currency -> name.hashCode()
        }
    }
}
