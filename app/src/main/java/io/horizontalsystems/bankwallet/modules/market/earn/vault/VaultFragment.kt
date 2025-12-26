package io.horizontalsystems.bankwallet.modules.market.earn.vault

import android.os.Parcelable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.chart.ChartViewModel
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.Chart
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.components.Badge
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.CellFooter
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.HsImage
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_leah
import io.horizontalsystems.bankwallet.ui.helpers.LinkHelper
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import kotlinx.parcelize.Parcelize

class VaultFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        withInput<Input>(navController) { input ->
            val factory = VaultModule.Factory(input)
            val viewModel = viewModel<VaultViewModel>(factory = factory)
            val chartViewModel = viewModel<ChartViewModel>(
                factory = factory
            )
            VaultScreen(
                viewModel,
                chartViewModel,
                navController
            )
        }
    }

    @Parcelize
    data class Input(
        val rank: Int,
        val address: String,
        val name: String,
        val tvl: String,
        val chain: String,
        val url: String?,
        val holders: String?,
        val assetSymbol: String,
        val protocolName: String,
        val assetLogo: String?,
    ) : Parcelable

}

@Composable
private fun VaultScreen(
    viewModel: VaultViewModel,
    chartViewModel: ChartViewModel,
    navController: NavController,
) {
    val uiState = viewModel.uiState
    val context = LocalContext.current

    HSScaffold(
        title = uiState.vaultViewItem.assetSymbol,
        onBack = navController::popBackStack,
    ) {
        Column(Modifier.navigationBarsPadding()) {
            HSSwipeRefresh(
                refreshing = uiState.isRefreshing,
                onRefresh = {
                    chartViewModel.refresh()
                }
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) {
                        item {
                            VaultCard(
                                uiState.vaultViewItem.name,
                                uiState.vaultViewItem.assetLogo,
                                uiState.vaultViewItem.rank
                            )
                        }
                        item {
                            Chart(chartViewModel)
                        }
                        item {
                            VSpacer(16.dp)
                            VaultDetails(uiState.vaultViewItem)
                        }
                        item {
                            VSpacer(18.dp)
                            ButtonPrimaryDefault(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp),
                                title = stringResource(R.string.Market_Vaults_OpenDapp),
                                enabled = uiState.vaultViewItem.url != null,
                                onClick = {
                                    uiState.vaultViewItem.url?.let {
                                        LinkHelper.openLinkInAppBrowser(context, it)
                                    }
                                }
                            )
                            VSpacer(32.dp)
                        }
                        item {
                            CellFooter("Powered by Vaults.fyi")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VaultDetails(item: VaultModule.VaultViewItem) {
    CellUniversalLawrenceSection(
        buildList {
            add {
                DetailCell(
                    stringResource(R.string.Market_Vaults_Vault_TVL),
                    item.tvl,
                    titleBadge = item.rank
                )
            }
            add {
                DetailCell(
                    stringResource(R.string.Market_Vaults_Vault_Network),
                    item.chain
                )
            }
            add {
                DetailCell(
                    stringResource(R.string.Market_Vaults_Vault_Protocol),
                    item.protocolName
                )
            }
            add {
                DetailCell(
                    stringResource(R.string.Market_Vaults_Vault_UnderlyingToken),
                    item.assetSymbol
                )
            }
            item.holders?.let {
                add {
                    DetailCell(
                        stringResource(R.string.Market_Vaults_Vault_Holders),
                        it
                    )
                }
            }
        }
    )
}

@Composable
fun DetailCell(
    title: String,
    value: String,
    titleBadge: String? = null
) {
    RowUniversal(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        subhead2_grey(
            text = title,
        )
        titleBadge?.let {
            HSpacer(8.dp)
            Badge(
                modifier = Modifier.padding(end = 8.dp),
                text = it
            )
        }
        Spacer(Modifier.weight(1f))

        subhead1_leah(
            text = value,
            maxLines = 1
        )
    }
}

@Composable
fun VaultCard(
    title: String,
    image: String?,
    rank: String
) {
    Column {
        HsDivider()
        Row(
            modifier = Modifier
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HsImage(
                url = image,
                alternativeUrl = null,
                placeholder = R.drawable.coin_placeholder,
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(32.dp)
                    .clip(CircleShape)
            )

            subhead2_leah(
                text = title,
                modifier = Modifier.weight(1f),
            )

            subhead1_grey(
                text = rank
            )

        }
    }
}

