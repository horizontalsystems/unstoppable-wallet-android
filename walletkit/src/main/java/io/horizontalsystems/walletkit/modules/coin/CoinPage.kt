package io.horizontalsystems.walletkit.modules.coin

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.stats.StatEvent
import io.horizontalsystems.walletkit.core.stats.StatPage
import io.horizontalsystems.walletkit.core.stats.stat
import io.horizontalsystems.walletkit.helpers.HudHelper
import io.horizontalsystems.walletkit.modules.coin.overview.ui.CoinOverviewScreen
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.TranslatableString
import io.horizontalsystems.walletkit.ui.compose.components.ListEmptyView
import io.horizontalsystems.walletkit.ui.compose.components.MenuItem
import io.horizontalsystems.walletkit.uiv3.components.HSScaffold
import kotlinx.serialization.Serializable

@Serializable
data class CoinPage(val input: Input) : HSPage(accessibleWhileLocked = true) {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        CoinScreen(
            input.coinUid,
            viewModel(factory = CoinModule.Factory(input.coinUid)),
            navigation
        )
    }

    @Serializable
    data class Input(val coinUid: String)
}

@Composable
fun CoinScreen(
    coinUid: String,
    coinViewModel: CoinViewModel?,
    navigation: HSNavigation
) {
    if (coinViewModel != null) {
        CoinContent(coinViewModel, navigation)
    } else {
        CoinNotFound(coinUid, navigation)
    }
}

@Composable
fun CoinContent(
    viewModel: CoinViewModel,
    navigation: HSNavigation
) {
    val view = LocalView.current

    HSScaffold(
        title = viewModel.fullCoin.coin.code,
        onBack = navigation::removeLastOrNull,
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
                coinToken = viewModel.coinToken,
                popularToken = viewModel.popularToken,
                navigation = navigation
            )

            viewModel.successMessage?.let {
                HudHelper.showSuccessMessage(view, it)

                viewModel.onSuccessMessageShown()
            }
        }
    }
}

@Composable
fun CoinNotFound(coinUid: String, navigation: HSNavigation) {
    HSScaffold(
        title = coinUid,
        onBack = navigation::removeLastOrNull,
    ) {
        ListEmptyView(
            text = stringResource(R.string.CoinPage_CoinNotFound, coinUid),
            icon = R.drawable.ic_not_available
        )
    }
}
