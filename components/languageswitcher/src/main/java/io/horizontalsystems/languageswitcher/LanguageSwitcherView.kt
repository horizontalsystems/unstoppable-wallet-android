package io.horizontalsystems.languageswitcher

import androidx.lifecycle.MutableLiveData

class LanguageSwitcherView : LanguageSwitcherModule.IView {
    val languageItems = MutableLiveData<List<LanguageViewItem>>()

    override fun show(items: List<LanguageViewItem>) {
        languageItems.value = items
    }
}
