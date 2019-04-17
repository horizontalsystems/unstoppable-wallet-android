package io.horizontalsystems.bankwallet.modules.settings.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent

class MainSettingsViewModel : ViewModel(), MainSettingsModule.IMainSettingsView, MainSettingsModule.IMainSettingsRouter {

    lateinit var delegate: MainSettingsModule.IMainSettingsViewDelegate

    val titleLiveDate = MutableLiveData<Int>()
    val backedUpLiveDate = MutableLiveData<Boolean>()
    val baseCurrencyLiveDate = MutableLiveData<String>()
    val languageLiveDate = MutableLiveData<String>()
    val lightModeLiveDate = MutableLiveData<Boolean>()
    val appVersionLiveDate = MutableLiveData<String>()
    val tabItemBadgeLiveDate = MutableLiveData<Int>()

    val showSecuritySettingsLiveEvent = SingleLiveEvent<Unit>()
    val showBaseCurrencySettingsLiveEvent = SingleLiveEvent<Unit>()
    val showLanguageSettingsLiveEvent = SingleLiveEvent<Unit>()
    val showAboutLiveEvent = SingleLiveEvent<Unit>()
    val showAppLinkLiveEvent = SingleLiveEvent<Unit>()
    val reloadAppLiveEvent = SingleLiveEvent<Unit>()


    fun init() {
        MainSettingsModule.init(this, this)
        delegate.viewDidLoad()
    }

    override fun setTitle(title: Int) {
        titleLiveDate.value = title
    }

    override fun setBackedUp(backedUp: Boolean) {
        backedUpLiveDate.value = backedUp
    }

    override fun setBaseCurrency(currency: String) {
        baseCurrencyLiveDate.value = currency
    }

    override fun setLanguage(language: String) {
        languageLiveDate.value = language
    }

    override fun setLightMode(lightMode: Boolean) {
        lightModeLiveDate.value = lightMode
    }

    override fun setAppVersion(appVersion: String) {
        appVersionLiveDate.value = appVersion
    }

    override fun setTabItemBadge(count: Int) {
        tabItemBadgeLiveDate.value = count
    }

    override fun showSecuritySettings() {
        showSecuritySettingsLiveEvent.call()
    }

    override fun showBaseCurrencySettings() {
        showBaseCurrencySettingsLiveEvent.call()
    }

    override fun showLanguageSettings() {
        showLanguageSettingsLiveEvent.call()
    }

    override fun showAbout() {
        showAboutLiveEvent.call()
    }

    override fun openAppLink() {
        showAppLinkLiveEvent.call()
    }

    override fun reloadAppInterface() {
        reloadAppLiveEvent.call()
    }

    override fun onCleared() {
        delegate.onClear()
    }

}
