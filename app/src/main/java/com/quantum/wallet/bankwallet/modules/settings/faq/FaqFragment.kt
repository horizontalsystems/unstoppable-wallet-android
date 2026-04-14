package com.quantum.wallet.bankwallet.modules.settings.faq

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.BaseComposeFragment
import com.quantum.wallet.bankwallet.core.LocalizedException
import com.quantum.wallet.bankwallet.core.slideFromRight
import com.quantum.wallet.bankwallet.core.stats.StatEvent
import com.quantum.wallet.bankwallet.core.stats.StatPage
import com.quantum.wallet.bankwallet.core.stats.stat
import com.quantum.wallet.bankwallet.entities.Faq
import com.quantum.wallet.bankwallet.entities.ViewState
import com.quantum.wallet.bankwallet.modules.coin.overview.ui.Loading
import com.quantum.wallet.bankwallet.modules.markdown.MarkdownFragment
import com.quantum.wallet.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import com.quantum.wallet.bankwallet.ui.compose.components.RowUniversal
import com.quantum.wallet.bankwallet.ui.compose.components.ScreenMessageWithAction
import com.quantum.wallet.bankwallet.ui.compose.components.subhead1_leah
import com.quantum.wallet.bankwallet.uiv3.components.HSScaffold
import com.quantum.wallet.bankwallet.uiv3.components.tabs.TabItem
import com.quantum.wallet.bankwallet.uiv3.components.tabs.TabsTop
import com.quantum.wallet.bankwallet.uiv3.components.tabs.TabsTopType
import java.net.UnknownHostException

class FaqListFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        FaqScreen(
            onCloseClick = { navController.popBackStack() },
            onItemClick = { faqItem ->
                navController.slideFromRight(
                    R.id.markdownFragment,
                    MarkdownFragment.Input(faqItem.markdown)
                )

                stat(page = StatPage.Faq, event = StatEvent.OpenArticle(faqItem.markdown))
            }
        )
    }

}

@Composable
private fun FaqScreen(
    onCloseClick: () -> Unit,
    onItemClick: (Faq) -> Unit,
    viewModel: FaqViewModel = viewModel(factory = FaqModule.Factory())
) {
    val viewState = viewModel.viewState

    HSScaffold(
        title = stringResource(R.string.Settings_Faq),
        onBack = onCloseClick,
    ) {
        Column(modifier = Modifier.navigationBarsPadding()) {
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
                            TabsTop(TabsTopType.Scrolled, tabItems) { tab ->
                                viewModel.onSelectSection(tab)
                            }

                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                Spacer(Modifier.height(12.dp))
                                CellUniversalLawrenceSection(viewModel.faqItems) { faq ->
                                    RowUniversal(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        onClick = { onItemClick(faq) }
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
}
