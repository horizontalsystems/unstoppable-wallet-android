package com.quantum.wallet.bankwallet.modules.settings.faq

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.core.managers.FaqManager

object FaqModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val faqRepository = FaqRepository(FaqManager, App.connectivityManager, App.languageManager)

            return FaqViewModel(faqRepository) as T
        }
    }
}
