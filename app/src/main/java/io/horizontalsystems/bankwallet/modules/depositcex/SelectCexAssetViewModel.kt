package io.horizontalsystems.bankwallet.modules.depositcex

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.core.managers.CexAssetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

class SelectCexAssetViewModel(
    private val cexAssetManager: CexAssetManager,
    private val accountManager: IAccountManager,
    private val withBalance: Boolean
) : ViewModel() {
    private var allItems: List<DepositCexModule.CexCoinViewItem> = listOf()
    private var loading = true
    private var items: List<DepositCexModule.CexCoinViewItem>? = null

    var uiState by mutableStateOf(
        SelectCexAssetUiState(
            loading = loading,
            items = items
        )
    )
        private set

    private var searchJob: Job? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            allItems = accountManager.activeAccount?.let {
                val assets = if (withBalance)
                    cexAssetManager.getWithBalance(it)
                else
                    cexAssetManager.getAllForAccount(it)

                assets.map { cexAsset ->
                    DepositCexModule.CexCoinViewItem(
                        title = cexAsset.id,
                        subtitle = cexAsset.name,
                        coinIconUrl = cexAsset.coin?.imageUrl,
                        coinIconPlaceholder = R.drawable.coin_placeholder,
                        cexAsset = cexAsset,
                        depositEnabled = cexAsset.depositEnabled,
                        withdrawEnabled = cexAsset.withdrawEnabled,
                    )
                }
                    .sortedBy { it.title }
            } ?: listOf()

            items = allItems
            loading = false
            emitState()
        }
    }

    private fun emitState() {
        viewModelScope.launch {
            uiState = SelectCexAssetUiState(
                loading = loading,
                items = items
            )
        }
    }

    fun onEnterQuery(q: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch(Dispatchers.IO) {
            items = allItems.filter {
                it.title.contains(q, true) || it.subtitle.contains(q, true)
            }
            ensureActive()
            emitState()
        }
    }

    class Factory(private val withBalance: Boolean) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SelectCexAssetViewModel(App.cexAssetManager, App.accountManager, withBalance) as T
        }
    }
}

data class SelectCexAssetUiState(
    val loading: Boolean,
    val items: List<DepositCexModule.CexCoinViewItem>?
)