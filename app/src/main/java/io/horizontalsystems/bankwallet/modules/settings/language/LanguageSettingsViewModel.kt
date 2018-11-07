package io.horizontalsystems.bankwallet.modules.settings.language

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent

class LanguageSettingsViewModel: ViewModel(), LanguageSettingsModule.ILanguageSettingsView, LanguageSettingsModule.ILanguageSettingsRouter {

    lateinit var delegate: LanguageSettingsModule.ILanguageSettingsViewDelegate
    val languageItems = MutableLiveData<List<LanguageItem>>()
    val titleLiveDate = MutableLiveData<Int>()
    val reloadAppLiveEvent = SingleLiveEvent<Unit>()

    fun init() {
        LanguageSettingsModule.init(this, this)
        delegate.viewDidLoad()
    }

    override fun setTitle(title: Int) {
        titleLiveDate.value = title
    }

    override fun show(items: List<LanguageItem>) {
        languageItems.value = items
    }

    override fun reloadAppInterface() {
        reloadAppLiveEvent.call()
    }
}
