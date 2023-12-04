package io.horizontalsystems.bankwallet.modules.coin.technicalindicators

import android.os.Parcelable
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.Loading
import io.horizontalsystems.bankwallet.modules.coin.technicalindicators.TechnicalIndicatorsDetailsModule.DetailViewItem
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderText
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.ScreenMessageWithAction
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.marketkit.models.HsPointTimePeriod
import kotlinx.parcelize.Parcelize

class TechnicalIndicatorsDetailsFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.getInput<Input>()
        val coinUid = input?.coinUid
        val period = HsPointTimePeriod.fromString(input?.periodValue)

        if (coinUid == null || period == null) {
            ScreenMessageWithAction(
                text = stringResource(R.string.Error),
                icon = R.drawable.ic_error_48
            ) {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .padding(horizontal = 48.dp)
                        .fillMaxWidth(),
                    title = stringResource(R.string.Button_Close),
                    onClick = {
                        navController.popBackStack()
                    }
                )
            }
        } else {
            TechnicalIndicatorsDetailsScreen(
                coinUid = coinUid,
                period = period,
                onBackPress = {
                    navController.popBackStack()
                }
            )
        }
    }

    @Parcelize
    data class Input(val coinUid: String, val periodValue: String) : Parcelable {
        constructor(coinUid: String, period: HsPointTimePeriod) : this(coinUid, period.value)
    }
}

@Composable
private fun TechnicalIndicatorsDetailsScreen(
    coinUid: String,
    period: HsPointTimePeriod,
    onBackPress: () -> Unit,
    viewModel: TechnicalIndicatorsDetailsViewModel = viewModel(
        factory = TechnicalIndicatorsDetailsModule.Factory(coinUid, period)
    )
) {
    val uiState = viewModel.uiState
    Scaffold(
        backgroundColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = stringResource(R.string.Coin_Analytics_Details),
                navigationIcon = {
                    HsBackButton(onClick = onBackPress)
                }
            )
        }
    ) {
        Column(modifier = Modifier.padding(it)) {
            Crossfade(uiState.viewState, label = "") { viewState ->
                when (viewState) {
                    ViewState.Success -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(color = ComposeAppTheme.colors.tyler)
                        ) {
                            VSpacer(12.dp)
                            DetailsView(uiState.sections)
                        }
                    }

                    is ViewState.Error -> {
                        ScreenMessageWithAction(
                            text = stringResource(R.string.Error),
                            icon = R.drawable.ic_error_48
                        ) {
                            ButtonPrimaryYellow(
                                modifier = Modifier
                                    .padding(horizontal = 48.dp)
                                    .fillMaxWidth(),
                                title = stringResource(R.string.Button_TryAgain),
                                onClick = { viewModel.refresh() }
                            )
                        }
                    }

                    ViewState.Loading -> {
                        Loading()
                    }

                    null -> {
                    }
                }

            }
        }
    }
}

@Composable
private fun DetailsView(sections: List<TechnicalIndicatorsDetailsModule.SectionViewItem>) {
    sections.forEach { section ->
        HeaderText(section.title.uppercase())
        CellUniversalLawrenceSection(section.details) { detail ->
            RowUniversal(modifier = Modifier.padding(horizontal = 16.dp)) {
                subhead2_grey(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    text = detail.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(detail.advice.title),
                    style = ComposeAppTheme.typography.subhead1,
                    color = detail.advice.color,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Preview
@Composable
private fun TechnicalIndicatorsDetails_Preview() {
    val sections = listOf(
        TechnicalIndicatorsDetailsModule.SectionViewItem(
            "Moving Averages",
            listOf(
                DetailViewItem("EMA 9", Advice.SELL),
                DetailViewItem("EMA 25", Advice.SELL),
                DetailViewItem("EMA 50", Advice.NEUTRAL),
                DetailViewItem("EMA 100", Advice.BUY),
                DetailViewItem("EMA 200", Advice.BUY),
            )
        ),
        TechnicalIndicatorsDetailsModule.SectionViewItem(
            "Moving Averages",
            listOf(
                DetailViewItem("RSI", Advice.SELL),
                DetailViewItem("MACD", Advice.SELL),
                DetailViewItem("Momentum", Advice.NEUTRAL),
            )
        ),
    )
    ComposeAppTheme {
        DetailsView(sections)
    }
}