package com.quantum.wallet.bankwallet.modules.multiswap

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.core.badge
import com.quantum.wallet.bankwallet.ui.compose.ComposeAppTheme
import com.quantum.wallet.bankwallet.ui.compose.components.CoinImage
import com.quantum.wallet.bankwallet.ui.compose.components.HsDivider
import com.quantum.wallet.bankwallet.ui.compose.components.SearchBar
import com.quantum.wallet.bankwallet.ui.compose.components.VSpacer
import com.quantum.wallet.bankwallet.uiv3.components.BoxBordered
import com.quantum.wallet.bankwallet.uiv3.components.cell.CellMiddleInfo
import com.quantum.wallet.bankwallet.uiv3.components.cell.CellPrimary
import com.quantum.wallet.bankwallet.uiv3.components.cell.CellRightInfo
import com.quantum.wallet.bankwallet.uiv3.components.cell.hs

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SelectSwapCoinDialogScreen(
    title: String,
    coinBalanceItems: List<CoinBalanceItem>,
    onSearchTextChanged: (String) -> Unit,
    onClose: () -> Unit,
    onClickItem: (CoinBalanceItem) -> Unit
) {
    Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
        SearchBar(
            title = title,
            searchHintText = stringResource(R.string.ManageCoins_Search),
            onClose = onClose,
            onSearchTextChanged = onSearchTextChanged
        )

        LazyColumn(modifier = Modifier.imePadding()) {
            item {
                HsDivider()
            }
            items(coinBalanceItems) { coinItem ->
                BoxBordered(
                    bottom = true
                ) {
                    CellPrimary(
                        left = {
                            CoinImage(
                                coin = coinItem.token.coin,
                                modifier = Modifier.size(32.dp)
                            )
                        },
                        middle = {
                            CellMiddleInfo(
                                title = coinItem.token.coin.code.hs,
                                badge = coinItem.token.badge?.hs,
                                subtitle = coinItem.token.coin.name.hs,
                            )
                        },
                        right = {
                            CellRightInfo(
                                title =
                                    coinItem.balance?.let {
                                        App.numberFormatter.formatCoinShort(
                                            it,
                                            coinItem.token.coin.code,
                                            8
                                        ).hs
                                    } ?: "".hs,
                                subtitle = coinItem.fiatBalanceValue?.let { fiatBalanceValue ->
                                    App.numberFormatter.formatFiatShort(
                                        fiatBalanceValue.value,
                                        fiatBalanceValue.currency.symbol,
                                        2
                                    ).hs
                                } ?: "".hs
                            )
                        },
                        onClick = { onClickItem.invoke(coinItem) },
                    )
                }
            }
            item {
                VSpacer(height = 72.dp)
            }
        }
    }
}
