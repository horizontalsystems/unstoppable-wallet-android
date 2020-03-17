package io.horizontalsystems.bankwallet.modules.blockchainsettingslist

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.CoinType

class BlockchainSettingsListPresenter(
        val view: BlockchainSettingsListModule.IView,
        val router: BlockchainSettingsListModule.IRouter,
        private val interactor: BlockchainSettingsListModule.IInteractor,
        private val coinTypes: List<CoinType>,
        val showDoneButton: Boolean)
    : ViewModel(), BlockchainSettingsListModule.IViewDelegate {

    override fun onSelect(viewItem: BlockchainSettingListViewItem) {
        router.openBlockchainSetting(viewItem.coin.type)
    }

    override fun onDone() {
        router.closeWithResultOk()
    }

    override fun onViewResume() {
        val viewItems = getViewItems()
        view.updatedViewItems(viewItems)
    }

    private fun getViewItems(): List<BlockchainSettingListViewItem>{
        val all = interactor.coinsWithSettings
        val viewItems = all.map {
            val setting = interactor.blockchainSettings(it.type)
            BlockchainSettingListViewItem(it, setting, enabled = coinTypes.contains(it.type))
        }
        viewItems.last().showBottomShade = true
        return viewItems
    }

}
