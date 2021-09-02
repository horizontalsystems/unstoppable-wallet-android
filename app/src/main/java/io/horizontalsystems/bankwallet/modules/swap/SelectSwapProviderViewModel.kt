package io.horizontalsystems.bankwallet.modules.swap

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.ui.selector.ViewItemWrapper

class SelectSwapProviderViewModel(
    val service: SwapMainService
) : ViewModel() {

    val viewItems = service.availableProviders.map { provider ->
        ViewItemWrapper(provider.title, provider)
    }
    val selectedItem = viewItems.first { it.item == service.currentProvider }

    fun setProvider(provider: SwapMainModule.ISwapProvider) {
        service.setProvider(provider)
    }

}
