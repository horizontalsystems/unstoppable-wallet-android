package io.horizontalsystems.bankwallet.modules.receive.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.alternativeImageUrl
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.receive.viewmodels.CoinForReceiveType
import io.horizontalsystems.bankwallet.modules.receive.viewmodels.ReceiveTokenSelectViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.HsImageCircle
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.SearchCell
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellPrimary
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightNavigation
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import kotlinx.coroutines.launch

@Composable
fun ReceiveTokenSelectScreen(
    activeAccount: Account,
    onMultipleAddressesClick: (String) -> Unit,
    onMultipleDerivationsClick: (String) -> Unit,
    onMultipleBlockchainsClick: (String) -> Unit,
    onCoinClick: (Wallet) -> Unit,
    onBackPress: () -> Unit,
) {
    val viewModel = viewModel<ReceiveTokenSelectViewModel>(
        factory = ReceiveTokenSelectViewModel.Factory(activeAccount)
    )
    val fullCoins = viewModel.uiState.fullCoins
    val coroutineScope = rememberCoroutineScope()

    HSScaffold(
        title = stringResource(id = R.string.Balance_Receive),
        menuItems = listOf(
            MenuItem(
                title = TranslatableString.ResString(R.string.Button_Close),
                icon = R.drawable.ic_close,
                onClick = onBackPress
            )
        )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(ComposeAppTheme.colors.lawrence)
        ) {
            stickyHeader {
                SearchCell(
                    modifier = Modifier
                        .background(ComposeAppTheme.colors.tyler)
                        .padding(top = 8.dp, start = 8.dp, end = 8.dp, bottom = 16.dp),
                    onSearchQueryChange = { text ->
                        viewModel.updateFilter(text)
                    }
                )
            }
            items(fullCoins) { fullCoin ->
                ReceiveCoin(
                    coinName = fullCoin.coin.name,
                    coinCode = fullCoin.coin.code,
                    coinIconUrl = fullCoin.coin.imageUrl,
                    alternativeCoinIconUrl = fullCoin.coin.alternativeImageUrl,
                    coinIconPlaceholder = fullCoin.iconPlaceholder,
                    onClick = {
                        coroutineScope.launch {
                            when (val coinActiveWalletsType =
                                viewModel.getCoinForReceiveType(fullCoin)) {
                                CoinForReceiveType.MultipleAddressTypes -> {
                                    onMultipleAddressesClick.invoke(fullCoin.coin.uid)
                                }

                                CoinForReceiveType.MultipleDerivations -> {
                                    onMultipleDerivationsClick.invoke(fullCoin.coin.uid)
                                }

                                CoinForReceiveType.MultipleBlockchains -> {
                                    onMultipleBlockchainsClick.invoke(fullCoin.coin.uid)
                                }

                                is CoinForReceiveType.Single -> {
                                    onCoinClick.invoke(coinActiveWalletsType.wallet)
                                }

                                null -> Unit
                            }
                        }
                    }
                )
                HsDivider()
            }
            item {
                VSpacer(32.dp)
            }
        }
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
