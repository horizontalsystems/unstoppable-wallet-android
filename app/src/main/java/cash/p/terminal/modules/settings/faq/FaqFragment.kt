package cash.p.terminal.modules.settings.faq

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.lifecycle.viewmodel.compose.viewModel
import cash.p.terminal.R
import cash.p.terminal.core.BaseComposeFragment
import cash.p.terminal.core.LocalizedException
import cash.p.terminal.core.slideFromRight
import cash.p.terminal.entities.Faq
import cash.p.terminal.entities.ViewState
import cash.p.terminal.modules.coin.overview.ui.Loading
import cash.p.terminal.modules.markdown.MarkdownFragment
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui.compose.components.HsBackButton
import cash.p.terminal.ui.compose.components.RowUniversal
import cash.p.terminal.ui.compose.components.ScreenMessageWithAction
import cash.p.terminal.ui.compose.components.ScrollableTabs
import cash.p.terminal.ui.compose.components.TabItem
import cash.p.terminal.ui.compose.components.subhead1_leah
import io.horizontalsystems.core.findNavController
import java.net.UnknownHostException

class FaqListFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent() {
        ComposeAppTheme {
            FaqScreen(
                onCloseClick = { findNavController().popBackStack() },
                onItemClick = { faqItem ->
                    val arguments =
                        bundleOf(MarkdownFragment.markdownUrlKey to faqItem.markdown)
                    findNavController().slideFromRight(R.id.markdownFragment, arguments)
                }
            )
        }
    }

}

@Composable
private fun FaqScreen(
    onCloseClick: () -> Unit,
    onItemClick: (Faq) -> Unit,
    viewModel: FaqViewModel = viewModel(factory = FaqModule.Factory())
) {
    val viewState = viewModel.viewState
    Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
        AppBar(
            title = TranslatableString.ResString(R.string.Settings_Faq),
            navigationIcon = {
                HsBackButton(onClick = onCloseClick)
            }
        )
        Crossfade(viewState) { viewState ->
            when (viewState) {
                ViewState.Loading -> {
                    Loading()
                }
                is ViewState.Error -> {
                    val s = when (val error = viewState.t) {
                        is UnknownHostException -> stringResource(R.string.Hud_Text_NoInternet)
                        is LocalizedException -> stringResource(error.errorTextRes)
                        else -> stringResource(R.string.Hud_UnknownError, error)
                    }

                    ScreenMessageWithAction(s, R.drawable.ic_error_48)
                }
                ViewState.Success -> {
                    Column {
                        val tabItems =
                            viewModel.sections.map {
                                TabItem(
                                    it.section,
                                    it == viewModel.selectedSection,
                                    it
                                )
                            }
                        ScrollableTabs(tabItems) { tab ->
                            viewModel.onSelectSection(tab)
                        }

                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            Spacer(Modifier.height(12.dp))
                            CellUniversalLawrenceSection(viewModel.faqItems) { faq ->
                                RowUniversal(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    onClick =  { onItemClick(faq) }
                                ) {
                                    subhead1_leah(text = faq.title)
                                }
                            }
                            Spacer(Modifier.height(32.dp))
                        }
                    }
                }
            }
        }
    }
}
