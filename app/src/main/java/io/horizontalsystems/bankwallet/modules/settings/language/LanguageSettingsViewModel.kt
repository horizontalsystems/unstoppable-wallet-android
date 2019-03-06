package io.horizontalsystems.bankwallet.modules.settings.language

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent

class LanguageSettingsViewModel: ViewModel(), LanguageSettingsModule.ILanguageSettingsView, LanguageSettingsModule.ILanguageSettingsRouter {

    lateinit var delegate: LanguageSettingsModule.ILanguageSettingsViewDelegate
    val languageItems = MutableLiveData<List<LanguageItem>>()
    val reloadAppLiveEvent = SingleLiveEvent<Unit>()

    fun init() {
        LanguageSettingsModule.init(this, this)
        delegate.viewDidLoad()
    }

    override fun show(items: List<LanguageItem>) {
        languageItems.value = items
    }

    override fun reloadAppInterface() {
        reloadAppLiveEvent.call()
    }
}
