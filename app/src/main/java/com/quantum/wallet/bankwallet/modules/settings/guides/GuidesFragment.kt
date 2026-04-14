package com.quantum.wallet.bankwallet.modules.settings.guides

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
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
import com.quantum.wallet.bankwallet.entities.ViewState
import com.quantum.wallet.bankwallet.modules.coin.overview.ui.Loading
import com.quantum.wallet.bankwallet.modules.markdown.MarkdownFragment
import com.quantum.wallet.bankwallet.ui.compose.ComposeAppTheme
import com.quantum.wallet.bankwallet.ui.compose.components.HFillSpacer
import com.quantum.wallet.bankwallet.ui.compose.components.HsDivider
import com.quantum.wallet.bankwallet.ui.compose.components.ScreenMessageWithAction
import com.quantum.wallet.bankwallet.ui.compose.components.body_leah
import com.quantum.wallet.bankwallet.ui.compose.components.cell.CellUniversal
import com.quantum.wallet.bankwallet.ui.compose.components.headline2_leah
import com.quantum.wallet.bankwallet.uiv3.components.HSScaffold
import com.quantum.wallet.bankwallet.uiv3.components.tabs.TabItem
import com.quantum.wallet.bankwallet.uiv3.components.tabs.TabsTop
import com.quantum.wallet.bankwallet.uiv3.components.tabs.TabsTopType
import java.net.UnknownHostException

class GuidesFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        GuidesScreen(navController)
    }

}

@Composable
fun GuidesScreen(navController: NavController) {
    val viewModel = viewModel<GuidesViewModel>(factory = GuidesModule.Factory())

    val uiState = viewModel.uiState

    val viewState = uiState.viewState
    val categories = uiState.categories
    val selectedCategory = uiState.selectedCategory
    val expandedSections = uiState.expandedSections

    HSScaffold(
        title = stringResource(R.string.Guides_Title),
        onBack = navController::popBackStack,
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
                        if (selectedCategory != null) {
                            Column {
                                val tabItems = categories.map {
                                    TabItem(
                                        it.category,
                                        it == selectedCategory,
                                        it
                                    )
                                }
                                TabsTop(TabsTopType.Scrolled, tabItems) { tab ->
                                    viewModel.onSelectCategory(tab)
                                }
                                val listState = rememberSaveable(
                                    selectedCategory,
                                    saver = LazyListState.Saver
                                ) {
                                    LazyListState()
                                }
                                LazyColumn(
                                    state = listState,
                                    contentPadding = PaddingValues(bottom = 32.dp)
                                ) {
                                    val sections = selectedCategory.sections
                                    val sectionsSize = sections.size

                                    sections.forEachIndexed { i, section ->
                                        val lastSection = i == sectionsSize - 1
                                        val sectionTitle = section.title
                                        val expanded = expandedSections.contains(sectionTitle)
                                        item {
                                            CellUniversal(
                                                borderTop = i != 0,
                                                color = ComposeAppTheme.colors.lawrence,
                                                onClick = {
                                                    viewModel.toggleSection(sectionTitle, expanded)
                                                }
                                            ) {
                                                headline2_leah(sectionTitle)
                                                HFillSpacer(8.dp)
                                                val iconId = if (expanded) {
                                                    R.drawable.ic_arrow_big_up_20
                                                } else {
                                                    R.drawable.ic_arrow_big_down_20
                                                }
                                                Icon(
                                                    painter = painterResource(iconId),
                                                    contentDescription = null,
                                                    tint = ComposeAppTheme.colors.grey
                                                )
                                            }
                                        }
                                        if (expanded) {
                                            itemsIndexed(section.items) { j, guide ->
                                                CellUniversal(
                                                    borderTop = j != 0,
                                                    onClick = {
                                                        navController.slideFromRight(
                                                            R.id.markdownFragment,
                                                            MarkdownFragment.Input(
                                                                guide.markdown,
                                                                true
                                                            )
                                                        )

                                                        stat(
                                                            page = StatPage.Academy,
                                                            event = StatEvent.OpenArticle(guide.markdown)
                                                        )
                                                    }
                                                ) {
                                                    body_leah(guide.title)
                                                }
                                            }
                                            if (lastSection) {
                                                item {
                                                    HsDivider()
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
