package io.horizontalsystems.bankwallet.modules.settings.main

import androidx.annotation.StringRes
import androidx.lifecycle.MutableLiveData
import io.horizontalsystems.bankwallet.entities.LaunchPage

class MainSettingsView : MainSettingsModule.IMainSettingsView {

    val backedUp = MutableLiveData<Boolean>()
    val pinSet = MutableLiveData<Boolean>()
    val baseCurrency = MutableLiveData<String>()
    val walletConnectSessionCount = MutableLiveData<String?>()
    val language = MutableLiveData<String>()
    val currentThemeName = MutableLiveData<Int>()
    val appVersion = MutableLiveData<String>()
    val termsAccepted = MutableLiveData<Boolean>()
    val launchScreen = MutableLiveData<LaunchPage>()

    override fun setBackedUp(backedUp: Boolean) {
        this.backedUp.postValue(backedUp)
    }

    override fun setPinIsSet(pinSet: Boolean) {
        this.pinSet.postValue(pinSet)
    }

    override fun setBaseCurrency(currency: String) {
        this.baseCurrency.postValue(currency)
    }

    override fun setWalletConnectSessionCount(count: String?) {
        this.walletConnectSessionCount.postValue(count)
    }

    override fun setLanguage(language: String) {
        this.language.postValue(language)
    }

    override fun setThemeName(@StringRes themeName: Int) {
        currentThemeName.postValue(themeName)
    }

    override fun setAppVersion(appVersion: String) {
        this.appVersion.postValue(appVersion)
    }

    override fun setTermsAccepted(termsAccepted: Boolean) {
        this.termsAccepted.postValue(termsAccepted)
    }

    override fun setLaunchScreen(launchPage: LaunchPage) {
        launchScreen.postValue(launchPage)
    }
}
