package cash.p.terminal.modules.coin.technicalindicators

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.fragment.findNavController
import cash.p.terminal.R
import cash.p.terminal.core.BaseFragment
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui.compose.components.HeaderText
import cash.p.terminal.ui.compose.components.HsBackButton
import cash.p.terminal.ui.compose.components.RowUniversal
import cash.p.terminal.ui.compose.components.VSpacer
import cash.p.terminal.ui.compose.components.subhead2_grey

class TechnicalIndicatorsDetailsFragment : BaseFragment() {

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
                    TechnicalIndicatorsDetailsScreen {
                        findNavController().popBackStack()
                    }
                }
            }
        }
    }
}

@Composable
private fun TechnicalIndicatorsDetailsScreen(onBackPress: () -> Unit) {
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = ComposeAppTheme.colors.tyler)
            ) {
                VSpacer(12.dp)
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
                                text = detail.advice.title,
                                style = ComposeAppTheme.typography.subhead1,
                                color = detail.advice.color,
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

private val sections = listOf(
    TechnicalIndicatorsDetailsModule.SectionViewItem(
        "Moving Averages",
        listOf(
            TechnicalIndicatorsDetailsModule.DetailViewItem("EMA 9", AdviceViewType.SELL),
            TechnicalIndicatorsDetailsModule.DetailViewItem("EMA 25", AdviceViewType.SELL),
            TechnicalIndicatorsDetailsModule.DetailViewItem("EMA 50", AdviceViewType.NEUTRAL),
            TechnicalIndicatorsDetailsModule.DetailViewItem("EMA 100", AdviceViewType.BUY),
            TechnicalIndicatorsDetailsModule.DetailViewItem("EMA 200", AdviceViewType.BUY),
        )
    ),
    TechnicalIndicatorsDetailsModule.SectionViewItem(
        "Moving Averages",
        listOf(
            TechnicalIndicatorsDetailsModule.DetailViewItem("RSI", AdviceViewType.SELL),
            TechnicalIndicatorsDetailsModule.DetailViewItem("MACD", AdviceViewType.SELL),
            TechnicalIndicatorsDetailsModule.DetailViewItem("Momentum", AdviceViewType.NEUTRAL),
        )
    ),
)

@Preview
@Composable
private fun TechnicalIndicatorsDetails_Preview() {
    ComposeAppTheme {
        TechnicalIndicatorsDetailsScreen {}
    }
}