package io.horizontalsystems.bankwallet.modules.receivemain

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.imagePlaceholder
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.modules.receive.address.ReceiveAddressFragment
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.CoinImage
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.SearchBar
import io.horizontalsystems.bankwallet.ui.compose.components.SectionUniversalItem
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.launch

class ReceiveTokenSelectFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent() {
        val viewModel = viewModel<ReceiveTokenSelectInitViewModel>()

        val activeAccount = viewModel.getActiveAccount()

        if (activeAccount == null) {
            HudHelper.showErrorMessage(LocalView.current, "No active account")
            findNavController().popBackStack()
        } else {
            ReceiveTokenSelectScreen(findNavController(), activeAccount)
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ReceiveTokenSelectScreen(navController: NavController, activeAccount: Account) {
    val viewModel = viewModel<ReceiveTokenSelectViewModel>(
        factory = ReceiveTokenSelectViewModel.Factory(activeAccount)
    )
    val fullCoins = viewModel.uiState.fullCoins
    val coroutineScope = rememberCoroutineScope()

    ComposeAppTheme {
        Scaffold(
            backgroundColor = ComposeAppTheme.colors.tyler,
            topBar = {
                SearchBar(
                    title = stringResource(R.string.Balance_Receive),
                    searchHintText = "",
                    menuItems = listOf(),
                    onClose = { navController.popBackStack() },
                    onSearchTextChanged = { text ->
                        viewModel.updateFilter(text)
                    }
                )
            }
        ) { paddingValues ->
            LazyColumn(contentPadding = paddingValues) {
                item {
                    VSpacer(12.dp)
                }
                itemsIndexed(fullCoins) { index, fullCoin ->
                    val coin = fullCoin.coin
                    val lastItem = index == fullCoins.size - 1
                    SectionUniversalItem(borderTop = true, borderBottom = lastItem) {
                        ReceiveCoin(
                            coinName = coin.name,
                            coinCode = coin.code,
                            coinIconUrl = coin.imageUrl,
                            coinIconPlaceholder = coin.imagePlaceholder,
                            onClick = {
                                coroutineScope.launch {
                                    val popupDestinationId = navController.currentDestination?.id

                                    when (val coinActiveWalletsType = viewModel.getCoinForReceiveType(fullCoin)) {
                                        CoinForReceiveType.MultipleAddressTypes -> {
                                            navController.slideFromRight(
                                                R.id.receiveBchAddressTypeSelectFragment,
                                                BchAddressTypeSelectFragment.prepareParams(coin.uid, popupDestinationId)
                                            )
                                        }
                                        CoinForReceiveType.MultipleDerivations -> {
                                            navController.slideFromRight(
                                                R.id.receiveDerivationSelectFragment,
                                                DerivationSelectFragment.prepareParams(coin.uid, popupDestinationId)
                                            )
                                        }
                                        CoinForReceiveType.MultipleBlockchains -> {
                                            navController.slideFromRight(
                                                R.id.receiveNetworkSelectFragment,
                                                NetworkSelectFragment.prepareParams(coin.uid, popupDestinationId)
                                            )
                                        }
                                        is CoinForReceiveType.Single -> {
                                            navController.slideFromRight(
                                                R.id.receiveFragment,
                                                bundleOf(
                                                    ReceiveAddressFragment.WALLET_KEY to coinActiveWalletsType.wallet,
                                                    ReceiveAddressFragment.POPUP_DESTINATION_ID_KEY to popupDestinationId,
                                                )
                                            )
                                        }

                                        null -> Unit
                                    }
                                }
                            }
                        )
                    }
                }
                item {
                    VSpacer(32.dp)
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
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 16.dp),
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
