package io.horizontalsystems.bankwallet.modules.swap

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.ISwapProvider
import io.horizontalsystems.bankwallet.ui.selector.ViewItemWrapper
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

    val providerItems: List<ViewItemWrapper<ISwapProvider>>
        get() = service.availableProviders.map { provider ->
            ViewItemWrapper(provider.title, provider)
        }

    val selectedProviderItem: ViewItemWrapper<ISwapProvider>
        get() = service.currentProvider.let { ViewItemWrapper(it.title, it) }


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
