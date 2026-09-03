package io.horizontalsystems.walletkit.modules.restoreaccount.restoreblockchains

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.Clearable
import io.horizontalsystems.walletkit.core.description
import io.horizontalsystems.walletkit.core.imageUrl
import io.horizontalsystems.walletkit.modules.market.ImageSource
import io.horizontalsystems.marketkit.models.Blockchain
import kotlinx.coroutines.launch

class RestoreBlockchainsViewModel(
    private val service: RestoreBlockchainsService,
    private val clearables: List<Clearable>
) : ViewModel() {

    val viewItemsLiveData = MutableLiveData<List<CoinViewItem<Blockchain>>>()
    val disableBlockchainLiveData = MutableLiveData<String>()
    var restored by mutableStateOf(false)
        private set
    val restoreEnabledLiveData: LiveData<Boolean>
        get() = service.canRestore.asLiveData()

    init {
        viewModelScope.launch {
            service.itemsFlow.collect {
                sync(it)
            }
        }

        viewModelScope.launch {
            service.cancelEnableBlockchainFlow.collect {
                disableBlockchainLiveData.postValue(it.uid)
            }
        }

        sync(service.items)
    }

    private fun sync(items: List<RestoreBlockchainsService.Item>) {
        val viewItems = items.map { viewItem(it) }
        viewItemsLiveData.postValue(viewItems)
    }

    private fun viewItem(
        item: RestoreBlockchainsService.Item,
    ) = CoinViewItem(
        item = item.blockchain,
        imageSource = ImageSource.Remote(item.blockchain.type.imageUrl, R.drawable.ic_platform_placeholder_32),
        title = item.blockchain.name,
        subtitle = item.blockchain.description,
        enabled = item.enabled,
        hasSettings = item.hasSettings,
        hasInfo = false
    )

    fun enable(blockchain: Blockchain) {
        service.enable(blockchain)
    }

    fun disable(blockchain: Blockchain) {
        service.disable(blockchain)
    }

    fun onClickSettings(blockchain: Blockchain) {
        service.configure(blockchain)
    }

    fun onRestore() {
        service.restore()
        restored = true
    }

    override fun onCleared() {
        clearables.forEach(Clearable::clear)
    }
}
