package io.horizontalsystems.bankwallet.modules.coin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.CoinOverviewScreen
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.multiswap.SwapPage
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSPage
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.ListEmptyView
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.serialization.Serializable

@Serializable
data class CoinPage(val input: Input) : HSPage() {

    @Composable
    override fun GetContent(navController: HSNavigation) {
        CoinScreen(
            input.coinUid,
            viewModel(factory = CoinModule.Factory(input.coinUid)),
            navController
        )
    }

    @Serializable
    data class Input(val coinUid: String)
}

@Composable
fun CoinScreen(
    coinUid: String,
    coinViewModel: CoinViewModel?,
    navController: HSNavigation
) {
    if (coinViewModel != null) {
        CoinContent(coinViewModel, navController)
    } else {
        CoinNotFound(coinUid, navController)
    }
}

@Composable
fun CoinContent(
    viewModel: CoinViewModel,
    navController: HSNavigation
) {
    val view = LocalView.current

    HSScaffold(
        title = viewModel.fullCoin.coin.code,
        onBack = navController::removeLastOrNull,
        bottomBar = {
            CoinBottomButtons(viewModel, navController)
        },
        menuItems = buildList {
            if (viewModel.isWatchlistEnabled) {
                if (viewModel.isFavorite) {
                    add(
                        MenuItem(
                            title = TranslatableString.ResString(R.string.CoinPage_Unfavorite),
                            icon = R.drawable.ic_heart_filled_24,
                            tint = ComposeAppTheme.colors.jacob,
                            onClick = {
                                viewModel.onUnfavoriteClick()

                                stat(
                                    page = StatPage.CoinPage,
                                    event = StatEvent.RemoveFromWatchlist(viewModel.fullCoin.coin.uid)
                                )
                            }
                        )
                    )
                } else {
                    add(
                        MenuItem(
                            title = TranslatableString.ResString(R.string.CoinPage_Favorite),
                            icon = R.drawable.ic_heart_24,
                            tint = ComposeAppTheme.colors.grey,
                            onClick = {
                                viewModel.onFavoriteClick()

                                stat(
                                    page = StatPage.CoinPage,
                                    event = StatEvent.AddToWatchlist(viewModel.fullCoin.coin.uid)
                                )
                            }
                        )
                    )
                }
            }
        }
    ) {
        Column {
            CoinOverviewScreen(
                fullCoin = viewModel.fullCoin,
                navController = navController
            )

            viewModel.successMessage?.let {
                HudHelper.showSuccessMessage(view, it)

                viewModel.onSuccessMessageShown()
            }
        }
    }
}

@Composable
private fun CoinBottomButtons(
    viewModel: CoinViewModel,
    navController: HSNavigation
) {
    val coinToken = viewModel.coinToken ?: return
    val popularToken = viewModel.popularToken

    ButtonsGroupWithShade {
        Row(
            modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 16.dp)
        ) {
            ButtonPrimaryYellow(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.CoinPage_Buy),
                onClick = {
                    navController.slideFromRight(
                        SwapPage(
                            SwapPage.Input(
                                tokenIn = popularToken,
                                tokenOut = coinToken
                            )
                        )
                    )
                }
            )

            HSpacer(8.dp)

            ButtonPrimaryDefault(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.CoinPage_Sell),
                onClick = {
                    navController.slideFromRight(
                        SwapPage(
                            SwapPage.Input(
                                tokenIn = coinToken,
                                tokenOut = popularToken
                            )
                        )
                    )
                }
            )
        }
    }
}

@Composable
fun CoinNotFound(coinUid: String, navController: HSNavigation) {
    HSScaffold(
        title = coinUid,
        onBack = navController::removeLastOrNull,
    ) {
        ListEmptyView(
            text = stringResource(R.string.CoinPage_CoinNotFound, coinUid),
            icon = R.drawable.ic_not_available
        )
    }
}
