package io.horizontalsystems.bankwallet.modules.settings.main

import androidx.lifecycle.MutableLiveData

class MainSettingsView : MainSettingsModule.IMainSettingsView {

    val backedUp = MutableLiveData<Boolean>()
    val pinSet = MutableLiveData<Boolean>()
    val baseCurrency = MutableLiveData<String>()
    val walletConnectPeer = MutableLiveData<String?>()
    val language = MutableLiveData<String>()
    val lightMode = MutableLiveData<Boolean>()
    val appVersion = MutableLiveData<String>()
    val termsAccepted = MutableLiveData<Boolean>()

    override fun setBackedUp(backedUp: Boolean) {
        this.backedUp.postValue(backedUp)
    }

    override fun setPinIsSet(pinSet: Boolean) {
        this.pinSet.postValue(pinSet)
    }

    override fun setBaseCurrency(currency: String) {
        this.baseCurrency.postValue(currency)
    }

    override fun setCurrentWalletConnectPeer(peer: String?) {
        this.walletConnectPeer.postValue(peer)
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

    override fun setTermsAccepted(termsAccepted: Boolean) {
        this.termsAccepted.postValue(termsAccepted)
    }
}
