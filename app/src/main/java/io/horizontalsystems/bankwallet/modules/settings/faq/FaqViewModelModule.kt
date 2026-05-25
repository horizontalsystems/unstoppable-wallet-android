package io.horizontalsystems.bankwallet.modules.settings.faq

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import io.horizontalsystems.bankwallet.core.managers.FaqManager
import io.horizontalsystems.bankwallet.core.managers.LanguageManager

@Module
@InstallIn(ViewModelComponent::class)
object FaqViewModelModule {

    @Provides
    @ViewModelScoped
    fun provideFaqRepository(
        connectivityManager: ConnectivityManager,
        languageManager: LanguageManager,
    ): FaqRepository = FaqRepository(FaqManager, connectivityManager, languageManager)
}
