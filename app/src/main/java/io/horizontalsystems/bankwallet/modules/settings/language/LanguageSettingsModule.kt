package io.horizontalsystems.bankwallet.modules.settings.language

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App

object LanguageSettingsModule {

    interface ILanguageSettingsView {
        fun show(items: List<LanguageViewItem>)
    }

    interface ILanguageSettingsViewDelegate {
        fun viewDidLoad()
        fun didSelect(position: Int)
    }

    interface ILanguageSettingsInteractor {
        var currentLanguage: String
        val availableLanguages: List<String>
        fun getName(language: String): String
        fun getNativeName(language: String): String
    }

    interface ILanguageSettingsRouter{
        fun reloadAppInterface()
        fun close()
    }

    fun start(context: Context) {
        val intent = Intent(context, LanguageSettingsActivity::class.java)
        context.startActivity(intent)
    }

    class Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val view = LanguageSettingsView()
            val router = LanguageSettingsRouter()
            val interactor = LanguageSettingsInteractor(App.languageManager, App.appConfigProvider)
            val presenter = LanguageSettingsPresenter(view, router, interactor)

            return presenter as T
        }
    }
}

data class LanguageViewItem(val language: String, val name: String, val nativeName: String, var current: Boolean)
