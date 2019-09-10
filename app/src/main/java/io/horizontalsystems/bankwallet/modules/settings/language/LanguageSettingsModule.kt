package io.horizontalsystems.bankwallet.modules.settings.language

import android.content.Context
import android.content.Intent
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
        val currentLanguage: String
        val availableLanguages: List<String>
        fun setCurrentLanguage(language: String)
        fun getName(language: String): String
        fun getNativeName(language: String): String
    }

    interface ILanguageSettingsRouter{
        fun reloadAppInterface()
    }

    fun start(context: Context) {
        val intent = Intent(context, LanguageSettingsActivity::class.java)
        context.startActivity(intent)
    }

    fun init(view: LanguageSettingsViewModel, router: ILanguageSettingsRouter) {
        val interactor = LanguageSettingsInteractor(App.languageManager, App.appConfigProvider)
        val presenter = LanguageSettingsPresenter(router, interactor)

        view.delegate = presenter
        presenter.view = view
    }
}

data class LanguageViewItem(val code: String, val displayName: String, val nativeDisplayName: String, var current: Boolean)
