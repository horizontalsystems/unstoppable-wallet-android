package cash.p.terminal.modules.settings.guides

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.ui_compose.BaseComposeFragment
import cash.p.terminal.core.LocalizedException
import cash.p.terminal.navigation.slideFromRight

import io.horizontalsystems.core.entities.ViewState
import cash.p.terminal.modules.coin.overview.ui.Loading
import cash.p.terminal.modules.markdown.MarkdownFragment
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.HFillSpacer
import cash.p.terminal.ui_compose.components.HsBackButton
import cash.p.terminal.ui.compose.components.ScreenMessageWithAction
import cash.p.terminal.ui_compose.components.ScrollableTabs
import cash.p.terminal.ui_compose.components.TabItem
import cash.p.terminal.ui_compose.components.body_leah
import io.horizontalsystems.chartview.cell.CellUniversal
import cash.p.terminal.ui_compose.components.headline2_leah
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
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

    Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
        AppBar(
            title = stringResource(R.string.Guides_Title),
            navigationIcon = {
                HsBackButton(onClick = { navController.popBackStack() })
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
                    if (selectedCategory != null) {
                        Column {
                            val tabItems = categories.map { TabItem(it.category, it == selectedCategory, it) }
                            ScrollableTabs(tabItems) { tab ->
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
                                                        MarkdownFragment.Input(guide.markdown, true)
                                                    )
                                                }
                                            ) {
                                                body_leah(guide.title)
                                            }
                                        }
                                        if (lastSection) {
                                            item {
                                                Divider(
                                                    thickness = 1.dp,
                                                    color = ComposeAppTheme.colors.steel10
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
        }
    }
}
