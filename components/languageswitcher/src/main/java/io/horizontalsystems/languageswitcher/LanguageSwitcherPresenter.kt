package io.horizontalsystems.languageswitcher

import androidx.lifecycle.ViewModel
import io.horizontalsystems.views.ListPosition

class LanguageSwitcherPresenter(
        val view: LanguageSwitcherModule.IView,
        val router: LanguageSwitcherModule.IRouter,
        private val interactor: LanguageSwitcherModule.IInteractor)
    : ViewModel(), LanguageSwitcherModule.IViewDelegate {

    private val languages = interactor.availableLanguages

    override fun viewDidLoad() {
        val currentLanguage = interactor.currentLanguage
        val items = languages.mapIndexed { index, language ->
            LanguageViewItem(
                    language,
                    interactor.getName(language), interactor.getNativeName(language),
                    currentLanguage == language,
                    listPosition = ListPosition.getListPosition(languages.size, index))
        }

        view.show(items)
    }

    override fun didSelect(position: Int) {
        val selected = languages[position]

        if (selected == interactor.currentLanguage) {
            router.close()
        } else {
            interactor.currentLanguage = selected
            router.reloadAppInterface()
        }
    }
}
