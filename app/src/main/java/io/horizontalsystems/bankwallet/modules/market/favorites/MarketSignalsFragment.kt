package io.horizontalsystems.bankwallet.modules.market.favorites

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.InfoText
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.SectionItemPosition
import io.horizontalsystems.bankwallet.ui.compose.components.SectionUniversalItem
import io.horizontalsystems.bankwallet.ui.compose.components.SignalBadge
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantWarning
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.sectionItemBorder
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_leah
import io.horizontalsystems.marketkit.models.Analytics.TechnicalAdvice.Advice

class MarketSignalsFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        MarketSignalsScreen(navController)
    }
}

@Composable
fun MarketSignalsScreen(navController: NavController) {
    val previousBackStackEntry = remember { navController.previousBackStackEntry }
    val marketFavoritesViewModel = viewModel<MarketFavoritesViewModel>(viewModelStoreOwner = previousBackStackEntry!!)

    Scaffold(
        topBar = {
            AppBar(
                title = stringResource(R.string.Market_Signals),
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Close),
                        icon = R.drawable.ic_close,
                        onClick = navController::popBackStack
                    )
                ),
            )
        },
        backgroundColor = ComposeAppTheme.colors.tyler
    ) {
        Column(modifier = Modifier.padding(it)) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {

                item {
                    InfoText(
                        text = stringResource(R.string.Market_Signal_Description),
                        paddingStart = 16.dp,
                        paddingEnd = 16.dp
                    )
                    VSpacer(height = 24.dp)
                }

                val signals = listOf(Advice.StrongBuy, Advice.Buy, Advice.Neutral, Advice.Sell, Advice.StrongSell, Advice.Overbought)
                signals.forEachIndexed { index, signal ->
                    val position: SectionItemPosition = when (index) {
                        0 -> SectionItemPosition.First
                        signals.size - 1 -> SectionItemPosition.Last
                        else -> SectionItemPosition.Middle
                    }

                    item {
                        SectionUniversalItem(borderTop = index != 0) {
                            RowUniversal(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .then(Modifier.sectionItemBorder(1.dp, ComposeAppTheme.colors.steel20, 12.dp, position)),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp)
                                        .height(40.dp)
                                        .width(77.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    SignalBadge(signal)
                                }
                                subhead2_leah(
                                    modifier = Modifier
                                        .padding(end = 16.dp),
                                    text = stringResource(id = signal.descriptionResId)
                                )
                            }
                        }
                    }
                }

                item {
                    VSpacer(height = 16.dp)
                    TextImportantWarning(text = stringResource(id = R.string.Market_Signal_Warning))
                    VSpacer(height = 50.dp)
                }
            }

            ButtonsGroupWithShade {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp),
                    title = stringResource(R.string.Market_Signal_TurnOn),
                    onClick = {
                        navController.popBackStack()

                        marketFavoritesViewModel.showSignals()
                    }
                )
            }
        }
    }
}

val Advice.descriptionResId: Int
    get() = when (this) {
        Advice.StrongBuy -> R.string.Market_Signal_StrongBuy_Description
        Advice.Buy -> R.string.Market_Signal_Buy_Description
        Advice.Neutral -> R.string.Market_Signal_Neutral_Descripion
        Advice.Sell -> R.string.Market_Signal_Sell_Description
        Advice.StrongSell -> R.string.Market_Signal_StrongSell_Description
        Advice.Oversold,
        Advice.Overbought -> R.string.Market_Signal_Risky_Description
    }
