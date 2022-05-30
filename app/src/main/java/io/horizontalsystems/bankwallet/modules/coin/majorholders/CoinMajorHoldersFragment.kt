package io.horizontalsystems.bankwallet.modules.coin.majorholders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.MajorHolderItem
import io.horizontalsystems.bankwallet.modules.coin.overview.Loading
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.bankwallet.ui.helpers.LinkHelper
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper

class CoinMajorHoldersFragment : BaseFragment() {

    private val coinUid by lazy {
        requireArguments().getString(COIN_UID_KEY)!!
    }

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
                ComposeAppTheme {
                    CoinMajorHoldersScreen(
                        coinUid,
                        findNavController(),
                    )
                }
            }
        }
    }


    companion object {
        private const val COIN_UID_KEY = "coin_uid_key"

        fun prepareParams(coinUid: String) = bundleOf(COIN_UID_KEY to coinUid)
    }
}

@Composable
private fun CoinMajorHoldersScreen(
    coinUid: String,
    navController: NavController,
    viewModel: CoinMajorHoldersViewModel = viewModel(
        factory = CoinMajorHoldersModule.Factory(coinUid)
    )
) {

    val viewState = viewModel.viewState
    val errorMessage = viewModel.errorMessage

    Surface(color = ComposeAppTheme.colors.tyler) {
        Column {
            AppBar(
                TranslatableString.ResString(R.string.CoinPage_MajorHolders),
                navigationIcon = {
                    HsIconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "back button",
                            tint = ComposeAppTheme.colors.jacob
                        )
                    }
                },
            )

            Crossfade(viewState) { viewState ->
                when (viewState) {
                    is ViewState.Loading -> {
                        Loading()
                    }
                    is ViewState.Error -> {
                        ListErrorView(stringResource(R.string.SyncError), viewModel::onErrorClick)
                    }
                    ViewState.Success -> {
                        CoinMajorHoldersContent(viewModel)
                    }
                }
            }

        }

        errorMessage?.let {
            SnackbarError(it.getString())
            viewModel.errorShown()
        }
    }
}

@Composable
private fun CoinMajorHoldersContent(viewModel: CoinMajorHoldersViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(top = 24.dp, bottom=30.dp)
    ) {

        item {
            SemiCircleChartBlock(viewModel.semiPieChartValue)
        }

        item {
            ListHeader()
        }

        items(viewModel.topWallets) {
            TopWalletCell(it)
        }

    }
}

@Composable
private fun ListHeader() {
    CellSingleLineClear(
        borderTop = true
    ) {
        Text(
            text = stringResource(R.string.CoinPage_MajorHolders_TopWallets),
            style = ComposeAppTheme.typography.body,
            color = ComposeAppTheme.colors.leah,
        )
    }
}

@Composable
private fun TopWalletCell(item: MajorHolderItem) {
    val context = LocalContext.current
    val localView = LocalView.current

    CellSingleLineClear(
        borderTop = true
    ) {
        Text(
            text = item.index.toString(),
            style = ComposeAppTheme.typography.captionSB,
            color = ComposeAppTheme.colors.grey,
            modifier = Modifier.width(24.dp),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = item.sharePercent,
            style = ComposeAppTheme.typography.body,
            color = ComposeAppTheme.colors.jacob,
        )
        Spacer(Modifier.weight(1f))
        ButtonSecondaryDefault(
            modifier = Modifier.padding(start = 8.dp, end = 8.dp),
            title = item.address,
            onClick = {
                TextHelper.copyText(item.address)
                HudHelper.showSuccessMessage(localView, R.string.Hud_Text_Copied)
            },
            ellipsis = Ellipsis.Middle(8)
        )
        ButtonSecondaryCircle(
            icon = R.drawable.ic_globe_20,
            onClick = {
                LinkHelper.openLinkInAppBrowser(
                    context = context,
                    link = "https://etherscan.io/address/${item.address}"
                )
            }
        )

    }
}

@Composable
private fun SemiCircleChartBlock(share: Float) {
    val portionRest = 100 - share

    SemiCircleChart(
        modifier = Modifier.padding(horizontal = 32.dp),
        percentValues = listOf(share, portionRest),
        title = App.numberFormatter.format(share, 0, 2, suffix = "%")
    )

    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 32.dp, end = 32.dp, top = 12.dp),
        textAlign = TextAlign.Center,
        overflow = TextOverflow.Ellipsis,
        text = stringResource(R.string.CoinPage_MajorHolders_InTopWallets),
        color = ComposeAppTheme.colors.grey,
        style = ComposeAppTheme.typography.subhead1
    )

    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 38.dp, bottom = 7.dp),
        textAlign = TextAlign.Start,
        overflow = TextOverflow.Ellipsis,
        text = stringResource(R.string.CoinPage_MajorHolders_Description),
        color = ComposeAppTheme.colors.grey,
        style = ComposeAppTheme.typography.subhead2
    )
}
