package io.horizontalsystems.bankwallet.modules.managecoins

import androidx.lifecycle.MutableLiveData
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType
import io.horizontalsystems.bankwallet.modules.createwallet.view.CoinManageViewItem

class ManageWalletsView: ManageWalletsModule.IView {

    val showManageKeysDialog = SingleLiveEvent<Pair<Coin, PredefinedAccountType>>()
    val coinsLiveData = MutableLiveData<List<CoinManageViewItem>>()
    val showErrorEvent = SingleLiveEvent<Exception>()
    val showSuccessEvent = SingleLiveEvent<Unit>()
    val showDerivationSelectorDialog = SingleLiveEvent<Triple<List<AccountType.Derivation>, AccountType.Derivation, Coin>>()

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

    override fun showDerivationSelectorDialog(derivationOptions: List<AccountType.Derivation>, selected: AccountType.Derivation, coin: Coin) {
        showDerivationSelectorDialog.postValue(Triple(derivationOptions, selected, coin))
    }

}
