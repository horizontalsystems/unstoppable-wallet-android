package io.horizontalsystems.bankwallet.modules.swap.providerselect

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.modules.swap.SwapMainService
import io.horizontalsystems.views.ListPosition

class SelectSwapProviderViewModel(
        val service: SwapMainService
) : ViewModel() {

    private val providers = service.availableProviders

    val viewItemsLiveData = MutableLiveData<List<SwapProviderViewItem>>()

    init {
        val currentProvider = service.currentProvider
        val providersCount = providers.size

        val viewItems = providers.mapIndexed { index, provider ->
            SwapProviderViewItem(
                    title = provider.title,
                    iconName = provider.id,
                    isSelected = provider.id == currentProvider.id,
                    listPosition = ListPosition.getListPosition(providersCount, index)
            )
        }

        viewItemsLiveData.postValue(viewItems)
    }

    fun onClick(position: Int) {
        service.setProvider(providers[position])
    }

}
