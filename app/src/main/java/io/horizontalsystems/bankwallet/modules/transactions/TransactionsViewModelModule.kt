package io.horizontalsystems.bankwallet.modules.transactions

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.managers.NftMetadataManager
import io.horizontalsystems.bankwallet.core.managers.SpamManager
import io.horizontalsystems.bankwallet.core.managers.TransactionAdapterManager
import io.horizontalsystems.bankwallet.modules.contacts.ContactsRepository

/**
 * ViewModelComponent module: bindings scoped to a single ViewModel instance.
 * TransactionsService is not a @Singleton — it is created fresh per ViewModel
 * and owns three internal repositories that are implementation details.
 */
@Module
@InstallIn(ViewModelComponent::class)
object TransactionsViewModelModule {

    @Provides
    @ViewModelScoped
    fun provideTransactionsService(
        transactionAdapterManager: TransactionAdapterManager,
        currencyManager: CurrencyManager,
        marketKit: MarketKitWrapper,
        contactsRepository: ContactsRepository,
        nftMetadataManager: NftMetadataManager,
        spamManager: SpamManager,
    ): TransactionsService = TransactionsService(
        TransactionRecordRepository(transactionAdapterManager),
        TransactionsRateRepository(currencyManager, marketKit),
        TransactionSyncStateRepository(transactionAdapterManager),
        contactsRepository,
        NftMetadataService(nftMetadataManager),
        spamManager,
    )
}
