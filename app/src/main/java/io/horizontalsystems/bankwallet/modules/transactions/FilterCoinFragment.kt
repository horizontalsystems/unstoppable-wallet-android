package io.horizontalsystems.bankwallet.modules.transactions

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.navGraphViewModels
import coil.compose.rememberAsyncImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.badge
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.B2
import io.horizontalsystems.bankwallet.ui.compose.components.Badge
import io.horizontalsystems.bankwallet.ui.compose.components.CellMultilineClear
import io.horizontalsystems.bankwallet.ui.compose.components.D1
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold

class FilterCoinFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        val viewModel: TransactionsViewModel? = try {
            navGraphViewModels<TransactionsViewModel>(R.id.mainFragment) { TransactionsModule.Factory() }.value
        } catch (e: IllegalStateException) {
            Toast.makeText(App.instance, "ViewModel is Null", Toast.LENGTH_SHORT).show()
            null
        }

        if (viewModel == null) {
            navController.popBackStack(R.id.filterCoinFragment, true)
            return
        }

        FilterCoinScreen(navController, viewModel)
    }

}


@Composable
fun FilterCoinScreen(navController: NavController, viewModel: TransactionsViewModel) {
    val filterCoins by viewModel.filterTokensLiveData.observeAsState()

    HSScaffold(
        title = stringResource(R.string.Transactions_Filter_ChooseCoin),
        onBack = navController::popBackStack,
    ) {
        Column {
            filterCoins?.let { filterCoins ->
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(filterCoins) {
                        CellMultilineClear(borderTop = true) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable {
                                        viewModel.setFilterToken(it.item)
                                        navController.popBackStack()
                                    }
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val token = it.item?.token
                                if (token != null) {
                                    Image(
                                        painter = rememberAsyncImagePainter(
                                            model = token.coin.imageUrl,
                                            error = painterResource(token.iconPlaceholder)
                                        ),
                                        modifier = Modifier
                                            .padding(end = 16.dp)
                                            .size(24.dp),
                                        contentDescription = null
                                    )
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            B2(text = token.coin.code)
                                            it.item.token.badge?.let { badge ->
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Badge(text = badge)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(1.dp))
                                        D1(text = token.coin.name)
                                    }
                                } else {
                                    Image(
                                        painter = painterResource(R.drawable.icon_24_circle_coin),
                                        modifier = Modifier
                                            .padding(end = 16.dp)
                                            .size(24.dp),
                                        contentDescription = null
                                    )
                                    B2(text = stringResource(R.string.Transactions_Filter_AllCoins))
                                }
                                if (it.selected) {
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
                }
            }
        }
    }
}
