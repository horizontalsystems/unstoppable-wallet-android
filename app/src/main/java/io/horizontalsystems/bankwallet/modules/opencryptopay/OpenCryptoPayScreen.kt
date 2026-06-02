package io.horizontalsystems.bankwallet.modules.opencryptopay

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.balance.ui.BalanceCardInner2
import io.horizontalsystems.bankwallet.modules.balance.ui.BalanceCardSubtitleType
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.Loading
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.tokenselect.TokenSelectViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.ListEmptyView
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_grey
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bankwallet.uiv3.components.bottom.BottomSearchBar
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun OpenCryptoPayScreen(
    navController: HSNavigation,
    lnurl: String,
) {
    val viewModel = viewModel<OpenCryptoPayViewModel>(factory = OpenCryptoPayViewModel.Factory(lnurl))
    val uiState = viewModel.uiState
    val view = LocalView.current

    uiState.error?.let { msg ->
        HudHelper.showErrorMessage(view, msg)
        viewModel.onErrorShown()
    }

    uiState.navigateToEvmConfirm?.let { data ->
        navController.slideFromRight(
            OpenCryptoPayEvmConfirmationPage(
                OpenCryptoPayEvmConfirmationPage.Input(
                    wallet = data.wallet,
                    callbackUrl = data.callbackUrl,
                    quoteId = data.quoteId,
                    paymentId = data.paymentId,
                    method = data.method,
                    asset = data.asset,
                    assetAmount = data.assetAmount,
                    blockchainType = data.blockchainType,
                    merchant = data.merchant,
                    expirationIso = data.expirationIso,
                    minFee = data.minFee,
                    sendEntryPointDestId = OpenCryptoPayPage::class,
                )
            ),
        )
        viewModel.onNavigatedToEvmConfirm()
    }

    uiState.navigateToConfirm?.let { data ->
        navController.slideFromRight(
            OpenCryptoPayConfirmationPage(
                OpenCryptoPayConfirmationPage.Input(
                    wallet = data.wallet,
                    callbackUrl = data.callbackUrl,
                    quoteId = data.quoteId,
                    paymentId = data.paymentId,
                    method = data.method,
                    asset = data.asset,
                    assetAmount = data.assetAmount,
                    merchant = data.merchant,
                    expirationIso = data.expirationIso,
                    minFee = data.minFee,
                    sendEntryPointDestId = OpenCryptoPayPage::class,
                )
            ),
        )
        viewModel.onNavigatedToConfirm()
    }

    val blockchainTypes = remember(uiState.methods) {
        uiState.methods.map { it.blockchainType }.distinct()
    }
    val tokenTypes = remember(uiState.methods) {
        uiState.methods.map { it.wallet.token.type }.distinct()
    }
    val vmKey = remember(tokenTypes) { tokenTypes.joinToString { it.id } }
    val tokenSelectViewModel = viewModel<TokenSelectViewModel>(
        key = vmKey,
        factory = TokenSelectViewModel.FactoryForSend(blockchainTypes, tokenTypes),
    )
    val tokenUiState = tokenSelectViewModel.uiState

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberSaveable(tokenUiState.items.size, saver = LazyListState.Saver) {
        LazyListState()
    }

    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (lazyListState.isScrollInProgress && isSearchActive) {
            isSearchActive = false
        }
    }

    HSScaffold(
        title = stringResource(R.string.Balance_Send),
        onBack = navController::removeLastOrNull,
    ) {
        Crossfade(uiState.loading) { isLoading ->
            if (isLoading) {
                Loading()
            } else {
                Column {
                    subhead1_grey(
                        modifier = Modifier.padding(start = 32.dp, top = 12.dp, end = 32.dp, bottom = 32.dp),
                        text = stringResource(R.string.OpenCryptoPay_ChooseTokenDescription),
                    )

                    Box(modifier = Modifier.fillMaxSize()) {
                        if (tokenUiState.noItems) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(ComposeAppTheme.colors.lawrence),
                            ) {
                                ListEmptyView(
                                    text = stringResource(
                                        if (tokenUiState.hasAssets) R.string.Search_NotFounded
                                        else R.string.Balance_NoAssetsToSend
                                    ),
                                    icon = R.drawable.warning_filled_24
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .imePadding()
                                    .background(ComposeAppTheme.colors.lawrence),
                                state = lazyListState
                            ) {
                                items(tokenUiState.items) { item ->
                                    BalanceCardInner2(
                                        viewItem = item,
                                        balanceHidden = tokenUiState.balanceHidden,
                                        type = BalanceCardSubtitleType.CoinName,
                                        onClick = {
                                            isSearchActive = false
                                            coroutineScope.launch {
                                                delay(200)
                                                val method = uiState.methods.find { it.wallet == item.wallet }
                                                method?.let { viewModel.onPayClick(it) }
                                            }
                                        }
                                    )
                                    HsDivider()
                                }
                                item { VSpacer(88.dp) }
                            }
                        }

                        BottomSearchBar(
                            searchQuery = searchQuery,
                            isSearchActive = isSearchActive,
                            onActiveChange = { isSearchActive = it },
                            onSearchQueryChange = { query ->
                                searchQuery = query
                                tokenSelectViewModel.updateFilter(query)
                            },
                        )
                    }
                }
            }
        }
    }
}
