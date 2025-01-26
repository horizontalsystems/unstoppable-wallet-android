package cash.p.terminal.modules.restoreaccount.restoreblockchains

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.toLiveData
import androidx.lifecycle.viewModelScope
import cash.p.terminal.R
import cash.p.terminal.wallet.Clearable
import cash.p.terminal.core.description
import cash.p.terminal.ui_compose.components.ImageSource
import io.horizontalsystems.core.entities.Blockchain
import io.horizontalsystems.core.imageUrl
import io.reactivex.BackpressureStrategy
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow

class RestoreBlockchainsViewModel(
    private val service: RestoreBlockchainsService,
    private val clearables: List<Clearable>
) : ViewModel() {

    val viewItemsLiveData = MutableLiveData<List<CoinViewItem<Blockchain>>>()
    val disableBlockchainLiveData = MutableLiveData<String>()
    var restored by mutableStateOf(false)
        private set
    val restoreEnabledLiveData: LiveData<Boolean>
        get() = service.canRestore.toFlowable(BackpressureStrategy.DROP).toLiveData()

    init {
        viewModelScope.launch {
            service.itemsObservable.asFlow().collect {
                sync(it)
            }
        }

        viewModelScope.launch {
            service.cancelEnableBlockchainObservable.asFlow().collect {
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
