package io.horizontalsystems.bankwallet.modules.coin.technicalindicators

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.Loading
import io.horizontalsystems.bankwallet.modules.coin.technicalindicators.TechnicalIndicatorsDetailsModule.DetailViewItem
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderText
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.ScreenMessageWithAction
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.marketkit.models.HsPointTimePeriod

class TechnicalIndicatorsDetailsFragment : BaseFragment() {

    private val coinUid by lazy {
        requireArguments().getString(COIN_UID_KEY)
    }

    private val period by lazy {
        val value = requireArguments().getString(PERIOD_KEY)
        HsPointTimePeriod.fromString(value)
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
                                    findNavController().popBackStack()
                                }
                            )
                        }
                    } else {
                        TechnicalIndicatorsDetailsScreen(
                            coinUid = coinUid!!,
                            period = period!!,
                            onBackPress = {
                                findNavController().popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }

    companion object {
        private const val COIN_UID_KEY = "coin_uid_key"
        private const val PERIOD_KEY = "period_key"

        fun prepareParams(coinUid: String, period: HsPointTimePeriod) =
            bundleOf(
                COIN_UID_KEY to coinUid,
                PERIOD_KEY to period.value
            )
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
                title = TranslatableString.ResString(R.string.Coin_Analytics_Details),
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