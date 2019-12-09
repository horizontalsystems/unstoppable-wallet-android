package io.horizontalsystems.bankwallet.modules.coinsettings

import androidx.lifecycle.MutableLiveData

class CoinSettingsView : CoinSettingsModule.IView {

    val titleData = MutableLiveData<String>()
    val viewItems = MutableLiveData<List<SettingSection>>()

    override fun setTitle(title: String) {
        titleData.postValue(title)
    }

    override fun setItems(items: List<SettingSection>) {
        viewItems.postValue(items)
    }
}
