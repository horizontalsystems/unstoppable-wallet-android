package io.horizontalsystems.bankwallet.modules.settings.language

import androidx.lifecycle.MutableLiveData

class LanguageSettingsView: LanguageSettingsModule.ILanguageSettingsView {
    val languageItems = MutableLiveData<List<LanguageViewItem>>()

    override fun show(items: List<LanguageViewItem>) {
        languageItems.value = items
    }
}
