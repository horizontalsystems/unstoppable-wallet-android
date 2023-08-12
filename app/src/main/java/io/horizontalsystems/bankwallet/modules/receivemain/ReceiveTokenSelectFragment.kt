package io.horizontalsystems.bankwallet.modules.receivemain

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.CoinImage
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.SearchBar
import io.horizontalsystems.bankwallet.ui.compose.components.SectionUniversalItem
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.core.findNavController

private val testItems = listOf(
    ReceiveTokenViewItem(
        coinName = "Bitcoin",
        coinCode = "BTC",
        coinIconUrl = "https://cdn.blocksdecoded.com/coin-icons/32px/bitcoin@3x.png",
        coinIconPlaceholder = R.drawable.coin_placeholder,
    ),
    ReceiveTokenViewItem(
        coinName = "Ethereum",
        coinCode = "ETH",
        coinIconUrl = "https://cdn.blocksdecoded.com/coin-icons/32px/ethereum@3x.png",
        coinIconPlaceholder = R.drawable.coin_placeholder,
    ),
)

class ReceiveTokenSelectFragment : BaseFragment() {

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
                ReceiveTokenSelectScreen(findNavController())
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ReceiveTokenSelectScreen(
    navController: NavController,
) {
    ComposeAppTheme {
        Column(
            modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)
        ) {
            SearchBar(
                title = stringResource(R.string.Balance_Receive),
                searchHintText = "",
                menuItems = listOf(),
                onClose = { navController.popBackStack() },
                onSearchTextChanged = { text ->
                    //viewModel.updateFilter(text)
                }
            )

            LazyColumn {
                item {
                    VSpacer(12.dp)
                }
                itemsIndexed(testItems) { index, item ->
                    SectionUniversalItem(borderTop = index == 0, borderBottom = true) {
                        ReceiveCoin(
                            coinName = item.coinName,
                            coinCode = item.coinCode,
                            coinIconUrl = item.coinIconUrl,
                            coinIconPlaceholder = item.coinIconPlaceholder,
                            onClick = {
                                navController.slideFromRight(
                                    R.id.receiveNetworkSelectFragment
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReceiveCoin(
    coinName: String,
    coinCode: String,
    coinIconUrl: String,
    coinIconPlaceholder: Int,
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
            }
        }
    }
}

data class ReceiveTokenViewItem(
    val coinName: String,
    val coinCode: String,
    val coinIconUrl: String,
    val coinIconPlaceholder: Int,
)

@Preview
@Composable
fun Preview_ReceiveTokenSelectScreen() {
    val navController = rememberNavController()
    ComposeAppTheme {
        ReceiveTokenSelectScreen(navController)
    }
}