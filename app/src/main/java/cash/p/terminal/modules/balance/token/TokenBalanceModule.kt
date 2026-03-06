package cash.p.terminal.modules.balance.token

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.core.getKoinInstance
import cash.p.terminal.core.managers.AdapterManager
import cash.p.terminal.core.managers.MarketFavoritesManager
import cash.p.terminal.core.managers.StackingManager
import cash.p.terminal.core.managers.PendingBalanceCalculator
import io.horizontalsystems.core.DispatcherProvider
import cash.p.terminal.core.managers.TransactionAdapterManager
import cash.p.terminal.modules.balance.BalanceAdapterRepository
import cash.p.terminal.modules.balance.BalanceCache
import cash.p.terminal.modules.balance.BalanceViewItem
import cash.p.terminal.modules.balance.BalanceViewItemFactory
import cash.p.terminal.modules.balance.DefaultBalanceXRateRepository
import cash.p.terminal.modules.balance.TotalBalance
import cash.p.terminal.modules.balance.TotalService
import cash.p.terminal.modules.displayoptions.DisplayDiffOptionType
import cash.p.terminal.modules.displayoptions.DisplayPricePeriod
import cash.p.terminal.modules.transactions.NftMetadataService
import cash.p.terminal.modules.transactions.TransactionSyncStateRepository
import cash.p.terminal.modules.transactions.TransactionViewItem
import cash.p.terminal.modules.transactions.TransactionsRateRepository
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.network.pirate.domain.repository.PiratePlaceRepository
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.managers.IBalanceHiddenManager
import io.horizontalsystems.core.IAppNumberFormatter
import org.koin.java.KoinJavaComponent.inject

class TokenBalanceModule {

    class Factory(private val wallet: Wallet) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val adapterManager: IAdapterManager by inject(AdapterManager::class.java)
            val transactionAdapterManager: TransactionAdapterManager by inject(
                TransactionAdapterManager::class.java
            )
            val transactionSyncStateRepository: TransactionSyncStateRepository by inject(
                TransactionSyncStateRepository::class.java
            )
            val pendingBalanceCalculator: PendingBalanceCalculator by inject(
                PendingBalanceCalculator::class.java
            )
            val dispatcherProvider: DispatcherProvider by inject(DispatcherProvider::class.java)
            val marketFavoritesManager: MarketFavoritesManager by inject(MarketFavoritesManager::class.java)
            val piratePlaceRepository: PiratePlaceRepository by inject(PiratePlaceRepository::class.java)
            val stackingManager: StackingManager by inject(StackingManager::class.java)
            val balanceService = TokenBalanceService(
                wallet = wallet,
                xRateRepository = DefaultBalanceXRateRepository(
                    "wallet",
                    App.currencyManager,
                    App.marketKit
                ),
                balanceAdapterRepository = BalanceAdapterRepository(
                    adapterManager,
                    BalanceCache(App.appDatabase.enabledWalletsCacheDao()),
                    pendingBalanceCalculator,
                    dispatcherProvider
                ),
            )

            val tokenTransactionsService = TokenTransactionsService(
                wallet = wallet,
                rateRepository = TransactionsRateRepository(App.currencyManager, App.marketKit),
                transactionSyncStateRepository = transactionSyncStateRepository,
                transactionAdapterManager = transactionAdapterManager,
                nftMetadataService = NftMetadataService(App.nftMetadataManager),
                spamManager = getKoinInstance()
            )

            val totalService = TotalService(
                currencyManager = App.currencyManager,
                marketKit = App.marketKit,
                baseTokenManager = App.baseTokenManager,
                balanceHiddenManager = App.balanceHiddenManager
            )

            val numberFormatter: IAppNumberFormatter by inject(IAppNumberFormatter::class.java)

            return TokenBalanceViewModel(
                totalBalance = TotalBalance(
                    totalService = totalService,
                    balanceHiddenManager = App.balanceHiddenManager
                ),
                wallet = wallet,
                balanceService = balanceService,
                balanceViewItemFactory = BalanceViewItemFactory(),
                transactionsService = tokenTransactionsService,
                transactionViewItem2Factory = getKoinInstance(),
                connectivityManager = App.connectivityManager,
                accountManager = App.accountManager,
                transactionHiddenManager = getKoinInstance(),
                getChangeNowAssociatedCoinTickerUseCase = getKoinInstance(),
                balanceHiddenManager = getKoinInstance<IBalanceHiddenManager>(),
                premiumSettings = getKoinInstance(),
                amlStatusManager = getKoinInstance(),
                marketFavoritesManager = marketFavoritesManager,
                piratePlaceRepository = piratePlaceRepository,
                stackingManager = stackingManager,
                priceManager = App.priceManager,
                localStorage = App.localStorage,
                numberFormatter = numberFormatter,
                contactsRepository = getKoinInstance(),
            ) as T
        }
    }

    enum class StakingStatus { ACTIVE, INACTIVE }

    data class TokenBalanceUiState(
        val title: String,
        val coinCode: String = "",
        val badge: String? = null,
        val balanceViewItem: BalanceViewItem?,
        val transactions: Map<String, List<TransactionViewItem>>?,
        val hasHiddenTransactions: Boolean,
        val showAmlPromo: Boolean = false,
        val amlCheckEnabled: Boolean = false,
        val isFavorite: Boolean = false,
        val stakingStatus: StakingStatus? = null,
        val stakingUnpaid: String? = null,
        val isCustomToken: Boolean = false,
        val displayDiffPricePeriod: DisplayPricePeriod = DisplayPricePeriod.ONE_DAY,
        val displayDiffOptionType: DisplayDiffOptionType = DisplayDiffOptionType.BOTH,
        val isRoundingAmount: Boolean = false,
        val isShowShieldFunds: Boolean = false
    )
}
