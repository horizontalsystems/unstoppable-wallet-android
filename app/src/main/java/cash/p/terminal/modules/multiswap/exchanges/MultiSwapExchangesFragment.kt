package cash.p.terminal.modules.multiswap.exchanges

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navGraphViewModels
import androidx.navigation.toRoute
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.core.composablePage
import cash.p.terminal.core.composablePopup
import cash.p.terminal.modules.multiswap.MultiSwapLegInfo
import cash.p.terminal.modules.multiswap.SwapConfirmScreen
import cash.p.terminal.modules.multiswap.SwapSelectProviderScreen
import cash.p.terminal.modules.multiswap.SwapSelectProviderViewModel
import cash.p.terminal.modules.multiswap.exchange.MultiSwapExchangeScreen
import cash.p.terminal.modules.multiswap.exchange.MultiSwapExchangeViewModel
import cash.p.terminal.modules.multiswap.settings.SwapTransactionSettingsScreen
import cash.p.terminal.modules.transactions.TransactionsModule
import cash.p.terminal.modules.transactions.TransactionsViewModel
import cash.p.terminal.navigation.slideFromBottom
import cash.p.terminal.ui_compose.BaseComposeFragment
import cash.p.terminal.ui_compose.components.HudHelper
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

class MultiSwapExchangesFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        val transactionsViewModel: TransactionsViewModel? = try {
            navGraphViewModels<TransactionsViewModel>(R.id.mainFragment) { TransactionsModule.Factory() }.value
        } catch (_: IllegalArgumentException) {
            null
        }

        val directSwapId = arguments?.getString(ARG_PENDING_MULTI_SWAP_ID)
        val startDestination: Any = if (directSwapId != null) {
            DetailRoute(directSwapId)
        } else {
            ListRoute
        }

        val innerNavController = rememberNavController()

        NavHost(
            navController = innerNavController,
            startDestination = startDestination,
        ) {
            composable<ListRoute> {
                ExchangesListContent(
                    fragmentNavController = navController,
                    innerNavController = innerNavController,
                )
            }
            composable<DetailRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<DetailRoute>()
                ExchangeDetailContent(
                    swapId = route.swapId,
                    fragmentNavController = navController,
                    transactionsViewModel = transactionsViewModel,
                    onBack = {
                        if (!innerNavController.navigateUp()) {
                            navController.navigateUp()
                        }
                    },
                )
            }
        }
    }

    companion object {
        const val ARG_PENDING_MULTI_SWAP_ID = "pendingMultiSwapId"
    }
}

@Composable
private fun ExchangesListContent(
    fragmentNavController: NavController,
    innerNavController: NavController,
) {
    val viewModel = koinViewModel<MultiSwapExchangesViewModel>()

    LaunchedEffect(Unit) {
        snapshotFlow { viewModel.uiState.items }
            .drop(1)
            .filter { it.isEmpty() }
            .collect { fragmentNavController.navigateUp() }
    }

    MultiSwapExchangesScreen(
        uiState = viewModel.uiState,
        onSelect = { id -> innerNavController.navigate(DetailRoute(id)) },
        onDelete = viewModel::onDelete,
        onBack = fragmentNavController::navigateUp,
    )
}

@Composable
private fun ExchangeDetailContent(
    swapId: String,
    fragmentNavController: NavController,
    transactionsViewModel: TransactionsViewModel?,
    onBack: () -> Unit,
) {
    val viewModel = koinViewModel<MultiSwapExchangeViewModel> {
        parametersOf(swapId)
    }

    LaunchedEffect(viewModel.closeScreen) {
        if (viewModel.closeScreen) {
            onBack()
        }
    }

    val detailNavController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()

    NavHost(
        navController = detailNavController,
        startDestination = ExchangeMainRoute,
    ) {
        composable<ExchangeMainRoute> {
            MultiSwapExchangeScreen(
                uiState = viewModel.uiState,
                timeRemainingProgress = { viewModel.timeRemainingProgress },
                onSwap = {
                    val action = viewModel.uiState?.actionCreate
                    if (action != null && !action.inProgress) {
                        viewModel.createMissingWallets(action.tokensToAdd)
                    } else if (action == null && viewModel.selectedLeg2Quote != null) {
                        detailNavController.navigate(ConfirmSwapRoute)
                    }
                },
                swapButtonTitle = when {
                    viewModel.uiState?.actionCreate?.inProgress == true ->
                        stringResource(R.string.swap_creating_wallets)
                    viewModel.uiState?.actionCreate != null ->
                        stringResource(R.string.swap_create_wallets)
                    else -> stringResource(R.string.Swap)
                },
                onRefresh = viewModel::refreshQuotes,
                onContinueLater = viewModel::onContinueLater,
                onDeleteAndClose = viewModel::onDeleteAndClose,
                onBack = onBack,
                onClickProvider = {
                    if (viewModel.leg2Quotes.isNotEmpty()) {
                        detailNavController.navigate(SelectProviderRoute)
                    }
                },
                onClickLeg1 = {
                    coroutineScope.launch {
                        val recordUid = viewModel.leg1NavigationRecordUid ?: return@launch
                        val transactionItem = transactionsViewModel?.getTransactionItem(recordUid) ?: return@launch
                        transactionsViewModel.tmpItemToShow = transactionItem
                        fragmentNavController.slideFromBottom(R.id.transactionInfoFragment)
                    }
                },
            )
        }
        composablePopup<SelectProviderRoute> { backStackEntry ->
            val quotes = viewModel.leg2Quotes
            if (quotes.isEmpty()) {
                LaunchedEffect(Unit) {
                    detailNavController.navigateUp()
                }
                return@composablePopup
            }
            val selectProviderViewModel = viewModel<SwapSelectProviderViewModel>(
                viewModelStoreOwner = backStackEntry,
                factory = SwapSelectProviderViewModel.Factory(quotes)
            )
            SwapSelectProviderScreen(
                onClickClose = detailNavController::navigateUp,
                quotes = selectProviderViewModel.uiState.quoteViewItems,
                currentQuote = viewModel.selectedLeg2Quote,
                swapRates = {
                    HudHelper.vibrate(App.instance)
                    selectProviderViewModel.swapRates()
                },
                onSelectQuote = { quote ->
                    viewModel.onSelectLeg2Quote(quote)
                    detailNavController.navigateUp()
                }
            )
        }
        composablePage<ConfirmSwapRoute> {
            val quote = viewModel.selectedLeg2Quote ?: run {
                LaunchedEffect(Unit) { detailNavController.navigateUp() }
                return@composablePage
            }
            val balanceState by viewModel.leg2BalanceStateFlow.collectAsStateWithLifecycle(viewModel.leg2BalanceStateFlow.value)
            SwapConfirmScreen(
                fragmentNavController = fragmentNavController,
                swapNavController = detailNavController,
                quote = quote,
                settings = emptyMap(),
                provider = quote.provider,
                displayBalance = balanceState.displayBalance,
                balanceHidden = viewModel.leg2BalanceHidden,
                feeToken = balanceState.feeToken,
                feeCoinBalance = balanceState.feeCoinBalance,
                onToggleHideBalance = viewModel::toggleLeg2BalanceHidden,
                onOpenSettings = { detailNavController.navigate(Leg2TransactionSettingsRoute) },
                multiSwapLegInfo = MultiSwapLegInfo.Leg2(swapId),
            )
        }
        composablePage<Leg2TransactionSettingsRoute> {
            SwapTransactionSettingsScreen(navController = detailNavController)
        }
    }
}

@Serializable
private object ListRoute

@Serializable
private data class DetailRoute(val swapId: String)

@Serializable
private object ExchangeMainRoute

@Serializable
private object SelectProviderRoute

@Serializable
private object ConfirmSwapRoute

@Serializable
private object Leg2TransactionSettingsRoute
