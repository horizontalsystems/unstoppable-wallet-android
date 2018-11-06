package bitcoin.wallet.modules.settings.language

import android.content.Context
import android.content.Intent
import bitcoin.wallet.core.App
import java.util.*

object LanguageSettingsModule {

    interface ILanguageSettingsView {
        fun setTitle(title: Int)
        fun show(items: List<LanguageItem>)
    }

    interface ILanguageSettingsViewDelegate {
        fun viewDidLoad()
        fun didSelect(item: LanguageItem)
    }

    interface ILanguageSettingsInteractor {
        var items: List<LanguageItem>
        fun setCurrentLanguage(item: LanguageItem)
    }

    interface ILanguageSettingsInteractorDelegate {
        fun didSetCurrentLanguage()
    }

    interface ILanguageSettingsRouter{
        fun reloadAppInterface()
    }

    fun start(context: Context) {
        val intent = Intent(context, LanguageSettingsActivity::class.java)
        context.startActivity(intent)
    }

    fun init(view: LanguageSettingsViewModel, router: ILanguageSettingsRouter) {
        val interactor = LanguageSettingsInteractor(languageManager = App.languageManager)
        val presenter = LanguageSettingsPresenter(router, interactor)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }
}

data class LanguageItem(val locale: Locale, var current: Boolean)
