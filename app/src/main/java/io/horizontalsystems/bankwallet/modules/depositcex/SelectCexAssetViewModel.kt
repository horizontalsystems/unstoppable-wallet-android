package io.horizontalsystems.bankwallet.modules.depositcex

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.core.managers.CexAssetManager

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
                    cexAsset = cexAsset,
                    depositEnabled = cexAsset.depositEnabled,
                    withdrawEnabled = cexAsset.withdrawEnabled,
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
