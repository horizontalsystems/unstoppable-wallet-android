package com.quantum.wallet.bankwallet.modules.coin.investments

import android.os.Parcelable
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.BaseComposeFragment
import com.quantum.wallet.bankwallet.entities.ViewState
import com.quantum.wallet.bankwallet.modules.coin.investments.CoinInvestmentsModule.FundViewItem
import com.quantum.wallet.bankwallet.modules.coin.overview.ui.Loading
import com.quantum.wallet.bankwallet.ui.compose.HSSwipeRefresh
import com.quantum.wallet.bankwallet.ui.compose.components.CellSingleLineClear
import com.quantum.wallet.bankwallet.ui.compose.components.CellSingleLineLawrenceSection
import com.quantum.wallet.bankwallet.ui.compose.components.HsImage
import com.quantum.wallet.bankwallet.ui.compose.components.ListErrorView
import com.quantum.wallet.bankwallet.ui.compose.components.body_jacob
import com.quantum.wallet.bankwallet.ui.compose.components.body_leah
import com.quantum.wallet.bankwallet.ui.compose.components.subhead1_grey
import com.quantum.wallet.bankwallet.ui.compose.components.subhead2_remus
import com.quantum.wallet.bankwallet.ui.helpers.LinkHelper
import com.quantum.wallet.bankwallet.uiv3.components.HSScaffold
import kotlinx.parcelize.Parcelize

class CoinInvestmentsFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        withInput<Input>(navController) { input ->
            CoinInvestmentsScreen(
                viewModel = viewModel(factory = CoinInvestmentsModule.Factory(input.coinUid)),
                onClickNavigation = {
                    navController.popBackStack()
                },
                onClickFundUrl = {
                    LinkHelper.openLinkInAppBrowser(requireContext(), it)
                }
            )
        }
    }

    @Parcelize
    data class Input(val coinUid: String) : Parcelable
}

@Composable
private fun CoinInvestmentsScreen(
    viewModel: CoinInvestmentsViewModel,
    onClickNavigation: () -> Unit,
    onClickFundUrl: (url: String) -> Unit
) {
    val viewState by viewModel.viewStateLiveData.observeAsState()
    val isRefreshing by viewModel.isRefreshingLiveData.observeAsState(false)
    val viewItems by viewModel.viewItemsLiveData.observeAsState()

    HSScaffold(
        title = stringResource(R.string.CoinPage_FundsInvested),
        onBack = onClickNavigation,
    ) {
        HSSwipeRefresh(
            refreshing = isRefreshing,
            modifier = Modifier.fillMaxSize(),
            onRefresh = viewModel::refresh,
            content = {
                Crossfade(viewState, label = "") { viewState ->
                    when (viewState) {
                        ViewState.Loading -> {
                            Loading()
                        }

                        is ViewState.Error -> {
                            ListErrorView(
                                stringResource(R.string.SyncError),
                                viewModel::onErrorClick
                            )
                        }

                        ViewState.Success -> {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                viewItems?.forEach { viewItem ->
                                    item {
                                        CoinInvestmentHeader(viewItem.amount, viewItem.info)

                                        Spacer(modifier = Modifier.height(12.dp))
                                    }
                                    item {
                                        CellSingleLineLawrenceSection(viewItem.fundViewItems) { fundViewItem ->
                                            CoinInvestmentFund(fundViewItem) {
                                                onClickFundUrl(
                                                    fundViewItem.url
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(24.dp))
                                    }
                                }
                            }
                        }

                        null -> {}
                    }
                }
            }
        )
    }
}

@Composable
fun CoinInvestmentHeader(amount: String, info: String) {
    CellSingleLineClear(borderTop = true) {
        body_jacob(
            modifier = Modifier.weight(1f),
            text = amount,
        )

        subhead1_grey(text = info)
    }
}

@Composable
fun CoinInvestmentFund(fundViewItem: FundViewItem, onClick: () -> Unit) {
    val hasWebsiteUrl = fundViewItem.url.isNotBlank()
    Row(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onClick, enabled = hasWebsiteUrl),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HsImage(
            url = fundViewItem.logoUrl,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .size(24.dp)
        )
        body_leah(
            modifier = Modifier.weight(1f),
            text = fundViewItem.name,
            overflow = TextOverflow.Ellipsis
        )
        if (fundViewItem.isLead) {
            subhead2_remus(
                modifier = Modifier.padding(horizontal = 6.dp),
                text = stringResource(R.string.CoinPage_CoinInvestments_Lead),
            )
        }
        if (hasWebsiteUrl) {
            Image(
                painter = painterResource(id = R.drawable.ic_arrow_right),
                contentDescription = "arrow icon"
            )
        }
        Spacer(Modifier.width(16.dp))
    }
}
