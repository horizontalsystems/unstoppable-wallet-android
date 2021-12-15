package io.horizontalsystems.bankwallet.modules.settings.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.subscribeIO
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

        service.start()
    }

    private fun setItems() {
        val items = mutableListOf<List<SettingViewItem>>()
        items.add(
            listOf(
                SettingViewItem(AppSetting.ManageWallets, showAlert = !service.allBackedUp),
                SettingViewItem(AppSetting.SecurityCenter, showAlert = !service.isPinSet),
            )
        )

        items.add(listOf(SettingViewItem(AppSetting.WalletConnect, getWalletConnectValue())))

        items.add(
            listOf(
                SettingViewItem(
                    AppSetting.LaunchScreen,
                    Translator.getString(service.launchScreen.titleRes)
                ),
                SettingViewItem(AppSetting.BaseCurrency, service.baseCurrency.code),
                SettingViewItem(AppSetting.Language, service.currentLanguageDisplayName),
                SettingViewItem(AppSetting.Theme, Translator.getString(service.themeName)),
                SettingViewItem(AppSetting.Experimental),
            )
        )

        items.add(
            listOf(
                SettingViewItem(AppSetting.FAQ),
                SettingViewItem(AppSetting.Academy),
            )
        )

        items.add(
            listOf(
                SettingViewItem(AppSetting.AboutApp, showAlert = !service.termsAccepted),
            )
        )

        settingItemsLiveData.postValue(items)
    }

    private fun getWalletConnectValue() =
        if (service.walletConnectSessionCount > 0) "${service.walletConnectSessionCount}" else null


    fun onLanguageChange() {
        service.setAppRelaunchingFromSettings()
    }

    // ViewModel

    override fun onCleared() {
        service.stop()
        disposable?.dispose()
    }

}
