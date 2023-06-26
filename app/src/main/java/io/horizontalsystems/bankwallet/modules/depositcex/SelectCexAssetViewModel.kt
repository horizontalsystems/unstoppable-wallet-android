package cash.p.terminal.modules.depositcex

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.core.IAccountManager
import cash.p.terminal.core.imageUrl
import cash.p.terminal.core.managers.CexAssetManager

class SelectCexAssetViewModel(
    private val cexAssetManager: CexAssetManager,
    private val accountManager: IAccountManager
) : ViewModel() {
    val items = accountManager.activeAccount?.let {
        cexAssetManager.getAll(it)
            .map { cexAsset ->
                DepositCexModule.CexCoinViewItem(
                    title = cexAsset.id,
                    subtitle = cexAsset.name,
                    coinIconUrl = cexAsset.coin?.imageUrl,
                    coinIconPlaceholder = R.drawable.coin_placeholder,
                    assetId = cexAsset.id,
                )
            }
    } ?: listOf()

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SelectCexAssetViewModel(App.cexAssetManager, App.accountManager) as T
        }
    }
}
