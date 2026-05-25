package io.horizontalsystems.bankwallet.modules.settings.guides

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import io.horizontalsystems.bankwallet.core.managers.GuidesManager
import io.horizontalsystems.bankwallet.core.managers.LanguageManager

@Module
@InstallIn(ViewModelComponent::class)
object GuidesViewModelModule {

    @Provides
    @ViewModelScoped
    fun provideGuidesRepository(
        connectivityManager: ConnectivityManager,
        languageManager: LanguageManager,
    ): GuidesRepository = GuidesRepository(GuidesManager, connectivityManager, languageManager)
}
