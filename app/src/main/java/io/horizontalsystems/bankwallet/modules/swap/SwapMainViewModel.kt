package io.horizontalsystems.bankwallet.modules.swap

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.ISwapProvider
import io.horizontalsystems.bankwallet.ui.selector.ViewItemWithIconWrapper
import io.reactivex.disposables.Disposable

class SwapMainViewModel(
    val service: SwapMainService
) : ViewModel() {

    private val disposable: Disposable

    val dex: SwapMainModule.Dex
        get() = service.dex

    val provider: ISwapProvider
        get() = service.dex.provider

    val providerLiveData = MutableLiveData<ISwapProvider>()

    var providerState by service::providerState

    val providerItems: List<ViewItemWithIconWrapper<ISwapProvider>>
        get() = service.availableProviders.map { provider ->
            ViewItemWithIconWrapper(provider.title, provider, provider.id)
        }

    val selectedProviderItem: ViewItemWithIconWrapper<ISwapProvider>
        get() = service.currentProvider.let { ViewItemWithIconWrapper(it.title, it, it.id) }


    init {
        disposable = service.providerObservable
            .subscribeIO {
                providerLiveData.postValue(it)
            }
    }

    fun setProvider(provider: ISwapProvider) {
        service.setProvider(provider)
    }

    override fun onCleared() {
        disposable.dispose()
    }

}
