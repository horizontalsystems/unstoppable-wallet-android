package io.horizontalsystems.bankwallet.modules.settings.language

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent

class LanguageSettingsViewModel: ViewModel(), LanguageSettingsModule.ILanguageSettingsView, LanguageSettingsModule.ILanguageSettingsRouter {

    lateinit var delegate: LanguageSettingsModule.ILanguageSettingsViewDelegate
    val languageItems = MutableLiveData<List<LanguageViewItem>>()
    val reloadAppLiveEvent = SingleLiveEvent<Unit>()

    fun init() {
        LanguageSettingsModule.init(this, this)
        delegate.viewDidLoad()
    }

    override fun show(items: List<LanguageViewItem>) {
        languageItems.value = items
    }

    override fun reloadAppInterface() {
        reloadAppLiveEvent.call()
    }
}
