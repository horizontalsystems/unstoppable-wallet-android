package io.horizontalsystems.bankwallet.modules.balance.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.CoinImage
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.ListEmptyView
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.SearchBar
import io.horizontalsystems.bankwallet.ui.compose.components.SectionUniversalItem
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.core.findNavController

private val testItems = listOf(
    SendTokenViewItem(
        coinName = "Bitcoin",
        coinCode = "BTC",
        coinIconUrl = "https://cdn.blocksdecoded.com/coin-icons/32px/bitcoin@3x.png",
        coinIconPlaceholder = R.drawable.coin_placeholder,
        primaryValue = "0.00000001",
        secondaryValue = "$29000",
    ),
    SendTokenViewItem(
        coinName = "Ethereum",
        coinCode = "ETH",
        coinIconUrl = "https://cdn.blocksdecoded.com/coin-icons/32px/ethereum@3x.png",
        coinIconPlaceholder = R.drawable.coin_placeholder,
        primaryValue = "2.12",
        secondaryValue = "$1900",
    ),
)

class SendTokenSelectFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )

            setContent {
                SendTokenSelectScreen(findNavController())
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SendTokenSelectScreen(
    navController: NavController,
) {
    val noItems = testItems.isEmpty()
    ComposeAppTheme {
        Column(
            modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)
        ) {
            if (noItems) {
                AppBar(
                    title = TranslatableString.ResString(R.string.Balance_Send),
                    navigationIcon = {
                        HsBackButton(onClick = { navController.popBackStack() })
                    }
                )
            } else {
                SearchBar(
                    title = stringResource(R.string.Balance_Send),
                    searchHintText = "",
                    menuItems = listOf(),
                    onClose = { navController.popBackStack() },
                    onSearchTextChanged = { text ->
                        //viewModel.updateFilter(text)
                    }
                )
            }

            if (noItems) {
                ListEmptyView(
                    text = stringResource(R.string.Balance_NoAssetsToSend),
                    icon = R.drawable.ic_empty_wallet
                )
            } else {
                LazyColumn {
                    item {
                        VSpacer(12.dp)
                    }
                    itemsIndexed(testItems) { index, item ->
                        SectionUniversalItem(borderTop = index == 0, borderBottom = true) {
                            SendCoin(
                                coinName = item.coinName,
                                coinCode = item.coinCode,
                                coinIconUrl = item.coinIconUrl,
                                coinIconPlaceholder = item.coinIconPlaceholder,
                                primaryValue = item.primaryValue,
                                secondaryValue = item.secondaryValue,
                                onClick = { }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SendCoin(
    coinName: String,
    coinCode: String,
    coinIconUrl: String,
    coinIconPlaceholder: Int,
    primaryValue: String? = null,
    secondaryValue: String? = null,
    onClick: (() -> Unit)? = null
) {
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp),
        onClick = onClick
    ) {
        CoinImage(
            iconUrl = coinIconUrl,
            placeholder = coinIconPlaceholder,
            modifier = Modifier
                .padding(end = 16.dp)
                .size(32.dp)
        )
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                body_leah(
                    modifier = Modifier.weight(1f).padding(end = 16.dp),
                    text = coinCode,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                primaryValue?.let { value ->
                    body_leah(
                        text = value,
                        maxLines = 1,
                    )
                }
            }
            VSpacer(3.dp)
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                subhead2_grey(
                    text = coinName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                secondaryValue?.let { value ->
                    Spacer(Modifier.width(8.dp))
                    subhead2_grey(
                        text = value,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

data class SendTokenViewItem(
    val coinName: String,
    val coinCode: String,
    val coinIconUrl: String,
    val coinIconPlaceholder: Int,
    val primaryValue: String? = null,
    val secondaryValue: String? = null,
)

@Preview
@Composable
fun Preview_SendTokenSelectScreen() {
    val navController = rememberNavController()
    ComposeAppTheme {
        SendTokenSelectScreen(navController)
    }
}