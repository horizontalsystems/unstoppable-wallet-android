package io.horizontalsystems.bankwallet.modules.managecoins

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.core.IPredefinedAccountType
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.viewHelpers.HudHelper

class ManageWalletsViewModel : ViewModel(), ManageWalletsModule.IView, ManageWalletsModule.IRouter {

    val coinsLoadedLiveEvent = SingleLiveEvent<Void>()
    val showManageKeysDialog = SingleLiveEvent<Pair<Coin, IPredefinedAccountType>>()
    val openRestoreWordsModule = SingleLiveEvent<Pair<Int,Int>>()
    val openRestoreEosModule = SingleLiveEvent<Int>()
    val showErrorEvent = SingleLiveEvent<Exception>()
    val closeLiveDate = SingleLiveEvent<Void>()

    lateinit var delegate: ManageWalletsModule.IViewDelegate


    fun init() {
        ManageWalletsModule.init(this, this)
        delegate.viewDidLoad()
    }

    // View

    override fun updateCoins() {
        coinsLoadedLiveEvent.call()
    }

    override fun showNoAccountDialog(coin: Coin, predefinedAccountType: IPredefinedAccountType) {
        showManageKeysDialog.postValue(Pair(coin, predefinedAccountType))
    }

    override fun showSuccess() {
        HudHelper.showSuccessMessage(R.string.Hud_Text_Done, 500)
    }

    override fun showError(e: Exception) {
        showErrorEvent.postValue(e)
    }

    // Router

    override fun openRestoreWordsModule(wordsCount: Int, titleRes: Int) {
        openRestoreWordsModule.postValue(Pair(wordsCount, titleRes))
    }

    override fun openRestoreEosModule(titleRes: Int) {
        openRestoreEosModule.postValue(titleRes)
    }

    override fun close() {
        closeLiveDate.call()
    }

    // View model

    override fun onCleared() {
        delegate.onClear()
    }

}
