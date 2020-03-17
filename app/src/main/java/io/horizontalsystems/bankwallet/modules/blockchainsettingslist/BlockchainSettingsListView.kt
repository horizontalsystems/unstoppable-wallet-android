package io.horizontalsystems.bankwallet.modules.blockchainsettingslist

import io.horizontalsystems.core.SingleLiveEvent

class BlockchainSettingsListView : BlockchainSettingsListModule.IView {

    val updateViewItems = SingleLiveEvent<List<BlockchainSettingListViewItem>>()

    override fun updatedViewItems(viewItems: List<BlockchainSettingListViewItem>) {
        updateViewItems.postValue(viewItems)
    }
}
