package io.horizontalsystems.bankwallet.modules.managecoins

import androidx.lifecycle.MutableLiveData
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType
import io.horizontalsystems.bankwallet.modules.createwallet.view.CoinManageViewItem

class ManageWalletsView: ManageWalletsModule.IView {

    val showManageKeysDialog = SingleLiveEvent<Pair<Coin, PredefinedAccountType>>()
    val coinsLiveData = MutableLiveData<List<CoinManageViewItem>>()
    val showErrorEvent = SingleLiveEvent<Exception>()
    val showSuccessEvent = SingleLiveEvent<Unit>()

    override fun setItems(coinViewItems: List<CoinManageViewItem>) {
        coinsLiveData.postValue(coinViewItems)
    }

    override fun showNoAccountDialog(coin: Coin, predefinedAccountType: PredefinedAccountType) {
        showManageKeysDialog.postValue(Pair(coin, predefinedAccountType))
    }

    override fun showSuccess() {
        showSuccessEvent.call()
    }

    override fun showError(e: Exception) {
        showErrorEvent.postValue(e)
    }
}
