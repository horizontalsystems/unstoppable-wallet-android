package io.horizontalsystems.bankwallet.modules.swap

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.ISwapProvider
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

    val providerItems: List<ISwapProvider>
        get() = service.availableProviders

    val selectedProviderItem: ISwapProvider
        get() = service.currentProvider


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
