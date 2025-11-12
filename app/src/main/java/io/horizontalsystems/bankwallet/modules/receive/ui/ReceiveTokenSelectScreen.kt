package io.horizontalsystems.bankwallet.modules.receive.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.alternativeImageUrl
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.imagePlaceholder
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.core.slideFromBottomForResult
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.market.ImageSource
import io.horizontalsystems.bankwallet.modules.receive.viewmodels.CoinForReceiveType
import io.horizontalsystems.bankwallet.modules.receive.viewmodels.ReceiveTokenSelectViewModel
import io.horizontalsystems.bankwallet.modules.restoreconfig.BirthdayHeightConfig
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.HsImageCircle
import io.horizontalsystems.bankwallet.ui.compose.components.ListEmptyView
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bankwallet.uiv3.components.bottom.BottomSearchBar
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellPrimary
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightNavigation
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import io.horizontalsystems.marketkit.models.FullCoin
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiveTokenSelectScreen(
    navController: NavController,
    activeAccount: Account,
    onMultipleAddressesClick: (String) -> Unit,
    onMultipleDerivationsClick: (String) -> Unit,
    onMultipleBlockchainsClick: (String) -> Unit,
    onMultipleZcashAddressTypeClick: (Wallet) -> Unit,
    onCoinClick: (Wallet) -> Unit,
    onBackPress: () -> Unit,
) {
    val viewModel = viewModel<ReceiveTokenSelectViewModel>(
        factory = ReceiveTokenSelectViewModel.Factory(activeAccount)
    )
    val uiState = viewModel.uiState
    val fullCoins = uiState.fullCoins
    val coroutineScope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf(uiState.searchQuery) }
    var isSearchActive by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState()
    var bottomSheetFullCoin by remember { mutableStateOf<FullCoin?>(null) }

    val lazyListState = rememberSaveable(
        fullCoins.size,
        saver = LazyListState.Saver
    ) {
        LazyListState()
    }

    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (lazyListState.isScrollInProgress) {
            if (isSearchActive) {
                isSearchActive = false
            }
        }
    }

    HSScaffold(
        title = stringResource(id = R.string.Balance_Receive),
        onBack = onBackPress,
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ComposeAppTheme.colors.lawrence)
            ) {
                if (fullCoins.all { (_, items) -> items.isEmpty() }) {
                    ListEmptyView(
                        text = stringResource(R.string.Search_NotFounded),
                        icon = R.drawable.warning_filled_24
                    )
                } else {
                    LazyColumn(
                        state = lazyListState
                    ) {
                        items(fullCoins) { fullCoin ->
                            ReceiveCoin(
                                coinName = fullCoin.coin.name,
                                coinCode = fullCoin.coin.code,
                                coinIconUrl = fullCoin.coin.imageUrl,
                                alternativeCoinIconUrl = fullCoin.coin.alternativeImageUrl,
                                coinIconPlaceholder = fullCoin.iconPlaceholder,
                                onClick = {
                                    if (viewModel.shouldShowBottomSheet(fullCoin)) {
                                        bottomSheetFullCoin = fullCoin
                                        return@ReceiveCoin
                                    }
                                    coroutineScope.launch {
                                        val type = viewModel.getCoinForReceiveType(fullCoin)

                                        processCoinClick(
                                            type,
                                            fullCoin,
                                            onMultipleAddressesClick,
                                            onMultipleDerivationsClick,
                                            onMultipleBlockchainsClick,
                                            onMultipleZcashAddressTypeClick,
                                            onCoinClick
                                        )
                                    }
                                }
                            )
                            HsDivider()
                        }
                        item {
                            VSpacer(72.dp)
                        }
                    }
                }
                BottomSearchBar(
                    searchQuery = searchQuery,
                    isSearchActive = isSearchActive,
                    onActiveChange = { active ->
                        isSearchActive = active
                    },
                    onSearchQueryChange = { query ->
                        viewModel.updateFilter(query)
                        searchQuery = query
                    }
                )
            }
        }
        bottomSheetFullCoin?.let { fullCoin ->
            ModalBottomSheet(
                onDismissRequest = {
                    bottomSheetFullCoin = null
                },
                sheetState = sheetState,
                containerColor = ComposeAppTheme.colors.transparent
            ) {
                ExistingOrNewWalletBottomSheet(
                    fullCoin = fullCoin,
                    showBirthdayConfig = {
                        bottomSheetFullCoin = null
                        navController.slideFromBottomForResult<BirthdayHeightConfig.Result>(
                            resId = R.id.zcashConfigure,
                            input = fullCoin.tokens.first()
                        ) { result ->
                            if (result.config != null) {
                                coroutineScope.launch {
                                    viewModel.getWalletForCoinWithBirthday(fullCoin, result.config)
                                        ?.let { onCoinClick(it) }
                                }
                            }
                        }
                    },
                    createNewWallet = {
                        coroutineScope.launch {
                            val type = viewModel.getCoinForReceiveType(fullCoin)
                            processCoinClick(
                                type,
                                fullCoin,
                                onMultipleAddressesClick,
                                onMultipleDerivationsClick,
                                onMultipleBlockchainsClick,
                                onMultipleZcashAddressTypeClick,
                                onCoinClick
                            )
                            bottomSheetFullCoin = null
                        }
                    },
                    onCloseClick = {
                        bottomSheetFullCoin = null
                    }
                )
            }
        }
    }
}

private fun processCoinClick(
    type: CoinForReceiveType?,
    fullCoin: FullCoin,
    onMultipleAddressesClick: (String) -> Unit,
    onMultipleDerivationsClick: (String) -> Unit,
    onMultipleBlockchainsClick: (String) -> Unit,
    onMultipleZcashAddressTypeClick: (Wallet) -> Unit,
    onCoinClick: (Wallet) -> Unit
) {
    when (type) {
        CoinForReceiveType.MultipleAddressTypes -> onMultipleAddressesClick(
            fullCoin.coin.uid
        )

        CoinForReceiveType.MultipleDerivations -> onMultipleDerivationsClick(
            fullCoin.coin.uid
        )

        CoinForReceiveType.MultipleBlockchains -> onMultipleBlockchainsClick(
            fullCoin.coin.uid
        )

        is CoinForReceiveType.MultipleZcashAddressTypes -> onMultipleZcashAddressTypeClick(
            type.wallet
        )

        is CoinForReceiveType.Single -> {
            onCoinClick(type.wallet)
        }

        null -> Unit
    }
}

@Composable
fun ReceiveCoin(
    coinName: String,
    coinCode: String,
    coinIconUrl: String,
    alternativeCoinIconUrl: String?,
    coinIconPlaceholder: Int,
    onClick: (() -> Unit)? = null
) {
    CellPrimary(
        left = {
            HsImageCircle(
                Modifier.size(32.dp),
                coinIconUrl,
                alternativeCoinIconUrl,
                coinIconPlaceholder,
            )
        },
        middle = {
            CellMiddleInfo(
                title = coinCode.hs,
                subtitle = coinName.hs,
            )
        },
        right = {
            CellRightNavigation()
        },
        onClick = onClick
    )
}

@Composable
fun ExistingOrNewWalletBottomSheet(
    fullCoin: FullCoin,
    showBirthdayConfig: (FullCoin) -> Unit,
    createNewWallet: (FullCoin) -> Unit,
    onCloseClick: () -> Unit
) {
    val coin = fullCoin.coin

    BottomSheetHeader(
        iconPainter = ImageSource.Remote(
            coin.imageUrl,
            coin.imagePlaceholder,
            coin.alternativeImageUrl
        ).painter(),
        title = coin.name,
        onCloseClick = onCloseClick
    ) {
        VSpacer(12.dp)
        body_leah(
            modifier = Modifier.padding(horizontal = 32.dp),
            text = stringResource(
                R.string.Balance_Receive_HaveYouOwnedCoins,
                coin.code.uppercase()
            )
        )
        VSpacer(24.dp)
        VSpacer(32.dp)

        ButtonPrimaryYellow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            title = stringResource(R.string.Balance_Receive_YesAlreadyOwn),
            onClick = {
                showBirthdayConfig(fullCoin)
            }
        )

        VSpacer(12.dp)
        ButtonPrimaryDefault(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            title = stringResource(R.string.Balance_Receive_IDontHave),
            onClick = { createNewWallet(fullCoin) }
        )
        VSpacer(32.dp)
    }
}