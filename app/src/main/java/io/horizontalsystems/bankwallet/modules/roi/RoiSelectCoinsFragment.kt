package io.horizontalsystems.bankwallet.modules.roi

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.paidAction
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.bankwallet.ui.compose.components.AlertGroup
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryWithIcon
import io.horizontalsystems.bankwallet.ui.compose.components.HFillSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HsCheckbox
import io.horizontalsystems.bankwallet.ui.compose.components.HsImage
import io.horizontalsystems.bankwallet.ui.compose.components.SearchBar
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.cell.CellUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.cell.SectionUniversalLawrence
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_grey
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.subscriptions.core.TokenInsights

class RoiSelectCoinsFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        RoiSelectCoinsScreen(navController)
    }

}

@OptIn(ExperimentalMaterialApi::class, ExperimentalAnimationApi::class)
@Composable
fun RoiSelectCoinsScreen(navController: NavController) {
    val viewModel = viewModel<RoiSelectCoinsViewModel>(factory = RoiSelectCoinsViewModel.Factory())

    val uiState = viewModel.uiState

    Scaffold(
        topBar = {
            SearchBar(
                title = stringResource(R.string.ROI_SelectCoin_Title),
                searchHintText = stringResource(R.string.Market_Search),
                menuItems = listOf(),
                onClose = { navController.popBackStack() },
                onSearchTextChanged = { text ->
                    viewModel.onFilter(text)
                }
            )
        },
        bottomBar = {
            ButtonsGroupWithShade {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp),
                    title = stringResource(R.string.Button_Apply),
                    enabled = uiState.isSaveable,
                    onClick = {
                        viewModel.onApply()
                        navController.popBackStack()
                    }
                )
            }
        },
        backgroundColor = ComposeAppTheme.colors.tyler,
    ) {
        var dialog: PeriodSelectorDialog? by rememberSaveable { mutableStateOf(null) }

        Column(
            modifier = Modifier.padding(it),
        ) {
            VSpacer(12.dp)
            SectionUniversalLawrence {
                uiState.selectedPeriods.forEachIndexed { i, period ->
                    val text = stringResource(R.string.ROI_Period_N, i + 1)
                    CellUniversal(borderTop = i != 0) {
                        body_leah(text = text)
                        HFillSpacer(16.dp)
                        CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                            ButtonSecondaryWithIcon(
                                iconRight = painterResource(R.drawable.ic_down_arrow_20),
                                title = period.title.getString(),
                                onClick = {
                                    navController.paidAction(TokenInsights) {
                                        dialog = PeriodSelectorDialog(text, period, i)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            VSpacer(24.dp)
            CellUniversal {
                body_leah(text = stringResource(R.string.ROI_SelectCoin_SelectAssets))
                HFillSpacer(minWidth = 8.dp)
                body_leah(text = "${uiState.selectedCoins.size}/3")
            }

            LazyColumn {
                items(uiState.coinItems) { item ->
                    val checked = uiState.selectedCoins.contains(item.performanceCoin)
                    val view = LocalView.current
                    val onClick = {
                        navController.paidAction(TokenInsights) {
                            try {
                                viewModel.onToggle(item, !checked)
                            } catch (e: Throwable) {
                                HudHelper.showWarningMessage(
                                    view,
                                    text = e.message ?: e.javaClass.simpleName
                                )
                            }
                        }
                    }
                    CellUniversal(
                        onClick = { onClick.invoke() }
                    ) {
                        HsImage(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape),
                            url = item.imageUrl,
                            alternativeUrl = item.alternativeImageUrl,
                            placeholder = item.localImage,
                        )
                        HSpacer(16.dp)

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            body_leah(text = item.code)
                            VSpacer(1.dp)
                            subhead1_grey(text = item.name)
                        }
                        HsCheckbox(
                            checked = checked,
                            onCheckedChange = { onClick.invoke() }
                        )
                    }
                }
            }
        }

        dialog?.let {
            AlertGroup(
                title = it.title,
                select = Select(it.period, uiState.periods),
                onSelect = { selected ->
                    viewModel.onSelectPeriod(it.index, selected)
                    dialog = null
                },
                onDismiss = {
                    dialog = null
                }
            )
        }

    }
}

data class PeriodSelectorDialog(
    val title: String,
    val period: HsTimePeriodTranslatable,
    val index: Int,
)