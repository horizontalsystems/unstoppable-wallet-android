package io.horizontalsystems.bankwallet.modules.settings.about

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.settings.main.AppSetting
import io.horizontalsystems.bankwallet.modules.settings.main.SettingViewItem
import io.reactivex.disposables.Disposable

class AboutViewModel(
    private val service: AboutService,
) : ViewModel() {

    val githubLink by service::githubLink
    val appWebPageLink by service::appWebPageLink
    val reportEmail by service::reportEmail
    val appVersion by service::appVersion

    val settingItemsLiveData = MutableLiveData<List<List<SettingViewItem>>>()

    var disposable: Disposable? = null

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
                SettingViewItem(AppSetting.WhatsNew),
            )
        )

        items.add(
            listOf(
                SettingViewItem(AppSetting.AppStatus),
                SettingViewItem(AppSetting.Terms, showAlert = !service.termsAccepted),
            )
        )

        items.add(
            listOf(
                SettingViewItem(AppSetting.Github,),
                SettingViewItem(AppSetting.AppWebsite,),
            )
        )

        items.add(
            listOf(
                SettingViewItem(AppSetting.RateUs),
                SettingViewItem(AppSetting.TellFriends,),
            )
        )

        items.add(
            listOf(
                SettingViewItem(AppSetting.Contact),
            )
        )

        settingItemsLiveData.postValue(items)
    }

    override fun onCleared() {
        service.stop()
        disposable?.dispose()
    }

}
