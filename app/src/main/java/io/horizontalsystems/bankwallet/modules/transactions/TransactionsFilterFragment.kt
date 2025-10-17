package io.horizontalsystems.bankwallet.modules.transactions

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.badge
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.core.slideFromRightForResult
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.CellSingleLineLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.HsSwitch
import io.horizontalsystems.bankwallet.ui.compose.components.InfoText
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold

class TransactionsFilterFragment : BaseComposeFragment() {

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

        FilterScreen(
            navController,
            viewModel
        )
    }

}


@Composable
fun FilterScreen(
    navController: NavController,
    viewModel: TransactionsViewModel,
) {
    val filterResetEnabled by viewModel.filterResetEnabled.observeAsState(false)
    val filterCoins by viewModel.filterTokensLiveData.observeAsState()
    val filterBlockchains by viewModel.filterBlockchainsLiveData.observeAsState()
    val filterHideUnknownTokens = viewModel.filterHideSuspiciousTx.observeAsState(true)
    val filterContact by viewModel.filterContactLiveData.observeAsState()

    val filterCoin = filterCoins?.find { it.selected }?.item
    val coinCode = filterCoin?.token?.coin?.code
    val badge = filterCoin?.token?.badge
    val selectedCoinFilterTitle = when {
        badge != null -> "$coinCode ($badge)"
        else -> coinCode
    }

    val filterBlockchain = filterBlockchains?.firstOrNull { it.selected }?.item

    HSScaffold(
        title = stringResource(R.string.Transactions_Filter),
        onBack = navController::popBackStack,
        menuItems = listOf(
            MenuItem(
                title = TranslatableString.ResString(R.string.Button_Reset),
                enabled = filterResetEnabled,
                tint = ComposeAppTheme.colors.jacob,
                onClick = {
                    viewModel.resetFilters()
                }
            )
        )
    ) {
        Column {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                VSpacer(12.dp)
                CellSingleLineLawrenceSection(
                    listOf {
                        FilterDropdownCell(
                            title = stringResource(R.string.Transactions_Filter_Blockchain),
                            value = filterBlockchain?.name
                                ?: stringResource(id = R.string.Transactions_Filter_AllBlockchains),
                            valueColor = if (filterBlockchain != null) ComposeAppTheme.colors.leah else ComposeAppTheme.colors.grey,
                            onClick = {
                                navController.slideFromRight(R.id.filterBlockchainFragment)
                            }
                        )
                    }
                )
                VSpacer(32.dp)
                CellSingleLineLawrenceSection(
                    listOf {
                        FilterDropdownCell(
                            title = stringResource(R.string.Transactions_Filter_Coin),
                            value = selectedCoinFilterTitle
                                ?: stringResource(id = R.string.Transactions_Filter_AllCoins),
                            valueColor = if (filterBlockchain != null) ComposeAppTheme.colors.leah else ComposeAppTheme.colors.grey,
                            onClick = {
                                navController.slideFromRight(R.id.filterCoinFragment)
                            }
                        )
                    }
                )
                VSpacer(32.dp)
                CellSingleLineLawrenceSection(
                    listOf {
                        FilterDropdownCell(
                            title = stringResource(R.string.Transactions_Filter_Contacts),
                            value = filterContact?.name
                                ?: stringResource(id = R.string.Transactions_Filter_AllContacts),
                            valueColor = if (filterContact != null) ComposeAppTheme.colors.leah else ComposeAppTheme.colors.grey,
                            onClick = {
                                navController.slideFromRightForResult<SelectContactFragment.Result>(
                                    R.id.selectContact,
                                    SelectContactFragment.Input(
                                        filterContact,
                                        filterBlockchain?.type
                                    )
                                ) {
                                    viewModel.onEnterContact(it.contact)
                                }
                            }
                        )
                    }
                )
                VSpacer(32.dp)
                CellSingleLineLawrenceSection(
                    listOf {
                        FilterSwitch(
                            title = stringResource(R.string.Transactions_Filter_HideSuspiciousTx),
                            enabled = filterHideUnknownTokens.value,
                            onChecked = { checked ->
                                viewModel.updateFilterHideSuspiciousTx(checked)
                            }
                        )
                    }
                )
                InfoText(
                    text = stringResource(R.string.Transactions_Filter_StablecoinDustAmount_Description),
                )
                VSpacer(24.dp)
            }

            ButtonsGroupWithShade {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    title = stringResource(R.string.Button_Apply),
                    onClick = {
                        navController.popBackStack()
                    },
                )
            }
        }
    }
}

@Composable
private fun FilterDropdownCell(
    title: String,
    value: String,
    valueColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxHeight()
            .clickable {
                onClick.invoke()
            }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        body_leah(
            text = title,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            Modifier.fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                maxLines = 1,
                style = ComposeAppTheme.typography.body,
                color = valueColor
            )
            Icon(
                modifier = Modifier.padding(start = 4.dp),
                painter = painterResource(id = R.drawable.ic_down_arrow_20),
                contentDescription = null,
                tint = ComposeAppTheme.colors.grey
            )
        }
    }
}

@Composable
private fun FilterSwitch(
    title: String,
    enabled: Boolean,
    onChecked: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxHeight()
            .clickable { onChecked(!enabled) }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        body_leah(
            text = title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.weight(1f))
        HsSwitch(
            checked = enabled,
            onCheckedChange = onChecked,
        )
    }
}