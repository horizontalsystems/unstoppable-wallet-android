package io.horizontalsystems.bankwallet.modules.nft.collection

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class NftCollectionViewModel(

) : ViewModel() {

    var selectedTabLiveData = MutableLiveData(NftCollectionModule.Tab.Overview)
    val tabs = NftCollectionModule.Tab.values()

    fun onSelect(tab: NftCollectionModule.Tab) {
        selectedTabLiveData.postValue(tab)
    }

}
