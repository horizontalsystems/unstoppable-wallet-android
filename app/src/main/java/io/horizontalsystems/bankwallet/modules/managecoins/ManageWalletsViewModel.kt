package io.horizontalsystems.bankwallet.modules.managecoins

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.entities.AccountOrigin
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinSettings
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType
import io.horizontalsystems.bankwallet.modules.createwallet.view.CoinManageViewItem
import io.horizontalsystems.bankwallet.viewHelpers.HudHelper

class ManageWalletsViewModel : ViewModel(), ManageWalletsModule.IView, ManageWalletsModule.IRouter {

    val showManageKeysDialog = SingleLiveEvent<Pair<Coin, PredefinedAccountType>>()
    val openRestoreModule = SingleLiveEvent<PredefinedAccountType>()
    val coinsLiveData = MutableLiveData<List<CoinManageViewItem>>()
    val showErrorEvent = SingleLiveEvent<Exception>()
    val closeLiveDate = SingleLiveEvent<Void>()
    val showCoinSettings = SingleLiveEvent<Triple<Coin, CoinSettings, AccountOrigin>>()

    lateinit var delegate: ManageWalletsModule.IViewDelegate


    fun init() {
        ManageWalletsModule.init(this, this)
        delegate.viewDidLoad()
    }

    // View

    override fun setItems(coinViewItems: List<CoinManageViewItem>) {
        coinsLiveData.postValue(coinViewItems)
    }

    override fun showNoAccountDialog(coin: Coin, predefinedAccountType: PredefinedAccountType) {
        showManageKeysDialog.postValue(Pair(coin, predefinedAccountType))
    }

    override fun showSuccess() {
        HudHelper.showSuccessMessage(R.string.Hud_Text_Done, 500)
    }

    override fun showError(e: Exception) {
        showErrorEvent.postValue(e)
    }

    // Router

    override fun showCoinSettings(coin: Coin, coinSettingsToRequest: CoinSettings, origin: AccountOrigin) {
        showCoinSettings.postValue(Triple(coin, coinSettingsToRequest, origin))
    }

    override fun openRestore(predefinedAccountType: PredefinedAccountType) {
        openRestoreModule.postValue(predefinedAccountType)
    }

    override fun close() {
        closeLiveDate.call()
    }

}
