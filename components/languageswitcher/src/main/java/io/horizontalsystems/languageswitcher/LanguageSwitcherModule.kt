package io.horizontalsystems.languageswitcher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.core.CoreApp
import io.horizontalsystems.views.ListPosition

object LanguageSwitcherModule {

    interface IView {
        fun show(items: List<LanguageViewItem>)
    }

    interface IViewDelegate {
        fun viewDidLoad()
        fun didSelect(position: Int)
    }

    interface IInteractor {
        var currentLanguage: String
        val availableLanguages: List<String>
        fun getName(language: String): String
        fun getNativeName(language: String): String
    }

    interface IRouter {
        fun reloadAppInterface()
        fun close()
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val view = LanguageSwitcherView()
            val router = LanguageSwitcherRouter()
            val interactor = LanguageSwitcherInteractor(CoreApp.languageManager, CoreApp.languageConfigProvider)
            val presenter = LanguageSwitcherPresenter(view, router, interactor)

            return presenter as T
        }
    }
}

data class LanguageViewItem(val language: String, val name: String, val nativeName: String, var current: Boolean, val listPosition: ListPosition)
