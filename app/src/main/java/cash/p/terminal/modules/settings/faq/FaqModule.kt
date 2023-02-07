package cash.p.terminal.modules.settings.faq

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.core.managers.FaqManager

object FaqModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val faqRepository = FaqRepository(FaqManager, App.connectivityManager, App.languageManager)

            return FaqViewModel(faqRepository) as T
        }
    }
}
