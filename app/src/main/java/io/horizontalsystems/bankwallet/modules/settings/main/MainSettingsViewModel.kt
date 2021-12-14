package io.horizontalsystems.bankwallet.modules.settings.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.settings.main.MainSettingsModule.Setting
import io.horizontalsystems.bankwallet.modules.settings.main.MainSettingsModule.SettingViewItem
import io.reactivex.disposables.Disposable

class MainSettingsViewModel(
    private val service: MainSettingsService,
    val companyWebPage: String,
) : ViewModel() {

    private var disposable: Disposable? = null

    val settingItemsLiveData = MutableLiveData<List<List<SettingViewItem>>>()
    val appVersion by service::appVersion

    init {
        setItems()

        service.stateUpdatedObservable
            .subscribeIO { setItems() }
            .let { disposable = it }
    }

    private fun setItems() {
        val items = mutableListOf<List<SettingViewItem>>()
        items.add(
            listOf(
                SettingViewItem(Setting.ManageWallets, showAlert = !service.allBackedUp),
                SettingViewItem(Setting.SecurityCenter, showAlert = !service.isPinSet),
            )
        )

        items.add(listOf(SettingViewItem(Setting.WalletConnect, getWalletConnectValue())))

        items.add(
            listOf(
                SettingViewItem(
                    Setting.LaunchScreen,
                    Translator.getString(service.launchScreen.titleRes)
                ),
                SettingViewItem(Setting.BaseCurrency, service.baseCurrency.code),
                SettingViewItem(Setting.Language, service.currentLanguageDisplayName),
                SettingViewItem(Setting.Theme, Translator.getString(service.themeName)),
                SettingViewItem(Setting.Experimental),
            )
        )

        items.add(
            listOf(
                SettingViewItem(Setting.FAQ),
                SettingViewItem(Setting.Academy),
            )
        )

        items.add(
            listOf(
                SettingViewItem(Setting.AboutApp, showAlert = !service.termsAccepted),
            )
        )

        settingItemsLiveData.postValue(items)
    }

    private fun getWalletConnectValue() =
        if (service.walletConnectSessionCount > 0) "${service.walletConnectSessionCount}" else null


    fun setAppRelaunchingFromSettings() {
        service.setAppRelaunchingFromSettings()
    }

    // ViewModel

    override fun onCleared() {
        service.stop()
        disposable?.dispose()
    }

}
