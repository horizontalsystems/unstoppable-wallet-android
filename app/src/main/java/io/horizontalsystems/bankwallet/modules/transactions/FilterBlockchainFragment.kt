package io.horizontalsystems.bankwallet.modules.transactions

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.navGraphViewModels
import coil.compose.rememberAsyncImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.CellMultilineClear
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.marketkit.models.Blockchain

class FilterBlockchainFragment : BaseComposeFragment() {

    private val viewModel by navGraphViewModels<TransactionsViewModel>(R.id.mainFragment)

    @Composable
    override fun GetContent(navController: NavController) {
        FilterBlockchainScreen(navController, viewModel)
    }
}


@Composable
fun FilterBlockchainScreen(navController: NavController, viewModel: TransactionsViewModel) {
    val filterBlockchains by viewModel.filterBlockchainsLiveData.observeAsState()

    Surface(color = ComposeAppTheme.colors.tyler) {
        Column {
            AppBar(
                title = stringResource(R.string.Transactions_Filter_ChooseBlockchain),
                navigationIcon = {
                    HsBackButton(onClick = navController::popBackStack)
                }
            )
            filterBlockchains?.let { blockchains ->
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(blockchains) { filterItem ->
                        BlockchainCell(viewModel, filterItem, navController)
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockchainCell(
    viewModel: TransactionsViewModel,
    filterItem: Filter<Blockchain?>,
    navController: NavController
) {
    CellMultilineClear(borderTop = true) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clickable {
                    viewModel.onEnterFilterBlockchain(filterItem)
                    navController.popBackStack()
                }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val blockchain = filterItem.item
            if (blockchain != null) {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = blockchain.type.imageUrl,
                        error = painterResource(R.drawable.ic_platform_placeholder_32)
                    ),
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(32.dp),
                    contentDescription = null
                )
                body_leah(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .weight(1f),
                    text = blockchain.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.icon_24_circle_coin),
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(24.dp),
                    contentDescription = null
                )
                body_leah(text = stringResource(R.string.Transactions_Filter_AllBlockchains))
            }
            if (filterItem.selected) {
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    painter = painterResource(R.drawable.icon_20_check_1),
                    contentDescription = null,
                    tint = ComposeAppTheme.colors.jacob
                )
            }
        }
    }
}
