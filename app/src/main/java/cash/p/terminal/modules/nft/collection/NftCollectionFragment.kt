package cash.p.terminal.modules.nft.collection

import android.os.Parcelable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.navGraphViewModels
import cash.p.terminal.R
import cash.p.terminal.ui_compose.BaseComposeFragment
import cash.p.terminal.core.getInput
import cash.p.terminal.modules.nft.collection.assets.NftCollectionAssetsScreen
import cash.p.terminal.modules.nft.collection.events.NftCollectionEventsScreen
import cash.p.terminal.modules.nft.collection.overview.NftCollectionOverviewScreen
import cash.p.terminal.modules.nft.collection.overview.NftCollectionOverviewViewModel
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.MenuItem
import cash.p.terminal.ui_compose.components.TabItem
import cash.p.terminal.ui_compose.components.Tabs
import cash.p.terminal.ui.helpers.LinkHelper
import cash.p.terminal.ui.helpers.TextHelper
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.core.entities.BlockchainType
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

class NftCollectionFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.getInput<Input>()
        val nftCollectionUid = input?.collectionUid ?: ""
        val blockchainTypeString = input?.blockchainTypeUid ?: ""
        val blockchainType = BlockchainType.fromUid(blockchainTypeString)

        val viewModel by navGraphViewModels<NftCollectionOverviewViewModel>(R.id.nftCollectionFragment) {
            NftCollectionModule.Factory(blockchainType, nftCollectionUid)
        }

        NftCollectionScreen(
            navController,
            viewModel
        )
    }

    @Parcelize
    data class Input(val collectionUid: String, val blockchainTypeUid: String) : Parcelable
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NftCollectionScreen(navController: NavController, viewModel: NftCollectionOverviewViewModel) {
    val tabs = viewModel.tabs
    val pagerState = rememberPagerState(initialPage = 0) { tabs.size }
    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current
    val context = LocalContext.current

    Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
        AppBar(
            menuItems = listOf(
                MenuItem(
                    title = TranslatableString.ResString(R.string.Button_Close),
                    icon = R.drawable.ic_close,
                    onClick = {
                        navController.popBackStack()
                    }
                )
            )
        )

        val selectedTab = tabs[pagerState.currentPage]
        val tabItems = tabs.map {
            TabItem(stringResource(id = it.titleResId), it == selectedTab, it)
        }
        Tabs(tabItems, onClick = {
            coroutineScope.launch {
                pagerState.scrollToPage(it.ordinal)
            }
        })

        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false
        ) { page ->
            when (tabs[page]) {
                NftCollectionModule.Tab.Overview -> {
                    NftCollectionOverviewScreen(
                        viewModel,
                        onCopyText = {
                            TextHelper.copyText(it)
                            HudHelper.showSuccessMessage(view, R.string.Hud_Text_Copied)
                        },
                        onOpenUrl = {
                            LinkHelper.openLinkInAppBrowser(context, it)
                        }
                    )
                }

                NftCollectionModule.Tab.Items -> {
                    NftCollectionAssetsScreen(navController, viewModel.blockchainType, viewModel.collectionUid)
                }

                NftCollectionModule.Tab.Activity -> {
                    NftCollectionEventsScreen(navController, viewModel.blockchainType, viewModel.collectionUid, viewModel.contracts)
                }
            }
        }
    }
}
