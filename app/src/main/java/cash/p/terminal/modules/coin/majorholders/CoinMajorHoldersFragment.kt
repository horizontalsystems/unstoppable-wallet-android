package cash.p.terminal.modules.coin.majorholders

import android.os.Parcelable
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.ui_compose.BaseComposeFragment
import io.horizontalsystems.core.requireInput
import io.horizontalsystems.core.entities.ViewState
import cash.p.terminal.modules.coin.MajorHolderItem
import cash.p.terminal.modules.coin.overview.ui.Loading
import cash.p.terminal.strings.helpers.shorten
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui.compose.components.ButtonSecondaryDefault
import cash.p.terminal.ui_compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui_compose.components.HSpacer
import cash.p.terminal.ui.compose.components.ListErrorView
import cash.p.terminal.ui_compose.components.MenuItem
import cash.p.terminal.ui_compose.components.RowUniversal
import cash.p.terminal.ui_compose.components.SectionItemBorderedRowUniversalClear
import cash.p.terminal.ui.compose.components.SnackbarError
import cash.p.terminal.ui.compose.components.StackedBarChart
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.components.body_leah
import cash.p.terminal.ui_compose.components.captionSB_grey
import cash.p.terminal.ui_compose.components.headline1_bran
import cash.p.terminal.ui_compose.components.subhead1_grey
import cash.p.terminal.ui_compose.components.subhead2_grey
import cash.p.terminal.ui.helpers.LinkHelper
import cash.p.terminal.ui.helpers.TextHelper
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.core.entities.Blockchain
import kotlinx.parcelize.Parcelize

class CoinMajorHoldersFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.requireInput<Input>()

        CoinMajorHoldersScreen(
            input.coinUid,
            input.blockchain,
            navController,
        )
    }

    @Parcelize
    data class Input(val coinUid: String, val blockchain: Blockchain) : Parcelable
}

@Composable
private fun CoinMajorHoldersScreen(
    coinUid: String,
    blockchain: Blockchain,
    navController: NavController,
    viewModel: CoinMajorHoldersViewModel = viewModel(
        factory = CoinMajorHoldersModule.Factory(coinUid, blockchain)
    )
) {

    Surface(color = ComposeAppTheme.colors.tyler) {
        Column {
            AppBar(
                title = blockchain.name,
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Close),
                        icon = R.drawable.ic_close,
                        onClick = { navController.popBackStack() }
                    )
                )
            )

            Crossfade(viewModel.uiState.viewState) { viewState ->
                when (viewState) {
                    ViewState.Loading -> {
                        Loading()
                    }

                    is ViewState.Error -> {
                        ListErrorView(stringResource(R.string.SyncError), viewModel::onErrorClick)
                        viewModel.uiState.error?.let {
                            SnackbarError(it.getString())
                            viewModel.errorShown()
                        }
                    }

                    ViewState.Success -> {
                        CoinMajorHoldersContent(viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun CoinMajorHoldersContent(
    viewModel: CoinMajorHoldersViewModel,
) {
    val uiState = viewModel.uiState

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 30.dp)
    ) {

        item {
            HoldersGeneralInfo(uiState.top10Share, uiState.totalHoldersCount)
        }

        item {
            StackedBarChart(
                slices = uiState.chartData,
                modifier = Modifier.padding(top = 12.dp, start = 16.dp, end = 16.dp, bottom = 24.dp)
            )
        }

        items(uiState.topHolders) {
            TopWalletCell(it)
        }

        item {
            uiState.seeAllUrl?.let {
                val context = LocalContext.current
                SeeAllButton { LinkHelper.openLinkInAppBrowser(context, it) }
            }
        }
    }
}

@Composable
private fun HoldersGeneralInfo(top10Share: String, totalHoldersCount: String) {
    VSpacer(12.dp)
    subhead2_grey(
        modifier = Modifier.padding(horizontal = 16.dp),
        text = stringResource(R.string.CoinPage_MajorHolders_HoldersNumber, totalHoldersCount)
    )
    VSpacer(12.dp)
    Row(
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        headline1_bran(
            text = top10Share,
            modifier = Modifier.alignByBaseline()
        )
        HSpacer(8.dp)
        subhead1_grey(
            text = stringResource(R.string.CoinPage_MajorHolders_InTopWallets),
            modifier = Modifier.alignByBaseline()
        )
    }
}

@Composable
private fun SeeAllButton(onClick: () -> Unit) {
    VSpacer(32.dp)
    CellUniversalLawrenceSection(
        listOf {
            RowUniversal(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                onClick = onClick
            ) {
                body_leah(
                    text = stringResource(R.string.Market_SeeAll),
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_right),
                    contentDescription = null,
                    tint = ComposeAppTheme.colors.grey
                )
            }
        }
    )
    VSpacer(32.dp)
}

@Composable
private fun TopWalletCell(item: MajorHolderItem) {
    val localView = LocalView.current

    SectionItemBorderedRowUniversalClear(borderTop = true) {
        captionSB_grey(
            text = item.index.toString(),
            modifier = Modifier.width(24.dp),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            body_leah(text = item.sharePercent)
            VSpacer(1.dp)
            subhead2_grey(text = item.balance)
        }

        ButtonSecondaryDefault(
            modifier = Modifier
                .padding(start = 8.dp, end = 8.dp)
                .height(28.dp),
            title = item.address.shorten(),
            onClick = {
                TextHelper.copyText(item.address)
                HudHelper.showSuccessMessage(localView, R.string.Hud_Text_Copied)
            }
        )
    }
}
