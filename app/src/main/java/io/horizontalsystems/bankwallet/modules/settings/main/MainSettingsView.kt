package io.horizontalsystems.bankwallet.modules.settings.main

import androidx.lifecycle.MutableLiveData

class MainSettingsView : MainSettingsModule.IMainSettingsView {

    val backedUp = MutableLiveData<Boolean>()
    val baseCurrency = MutableLiveData<String>()
    val language = MutableLiveData<String>()
    val lightMode = MutableLiveData<Boolean>()
    val appVersion = MutableLiveData<String>()

    override fun setBackedUp(backedUp: Boolean) {
        this.backedUp.postValue(backedUp)
    }

    override fun setBaseCurrency(currency: String) {
        this.baseCurrency.postValue(currency)
    }

    override fun setLanguage(language: String) {
        this.language.postValue(language)
    }

    override fun setLightMode(lightMode: Boolean) {
        this.lightMode.postValue(lightMode)
    }

    override fun setAppVersion(appVersion: String) {
        this.appVersion.postValue(appVersion)
    }
}
