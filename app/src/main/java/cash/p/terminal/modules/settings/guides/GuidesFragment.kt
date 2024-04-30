package cash.p.terminal.modules.settings.guides

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import cash.p.terminal.R
import cash.p.terminal.core.BaseComposeFragment
import cash.p.terminal.core.LocalizedException
import cash.p.terminal.core.slideFromRight
import cash.p.terminal.entities.Guide
import cash.p.terminal.entities.ViewState
import cash.p.terminal.modules.coin.overview.ui.Loading
import cash.p.terminal.modules.markdown.MarkdownFragment
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.HsBackButton
import cash.p.terminal.ui.compose.components.ScreenMessageWithAction
import cash.p.terminal.ui.compose.components.ScrollableTabs
import cash.p.terminal.ui.compose.components.TabItem
import cash.p.terminal.ui.compose.components.caption_grey
import cash.p.terminal.ui.compose.components.title3_leah
import io.horizontalsystems.core.helpers.DateHelper
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

    val viewState = viewModel.viewState
    val categories = viewModel.categories
    val selectedCategory = viewModel.selectedCategory
    val guides = viewModel.guides

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
                                contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
                            ) {
                                items(guides) { guide ->
                                    CardsPreviewCardsGuide(guide) {
                                        navController.slideFromRight(
                                            R.id.markdownFragment,
                                            MarkdownFragment.Input(guide.fileUrl, true)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CardsPreviewCardsGuide(guide: Guide, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ComposeAppTheme.colors.lawrence)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(ComposeAppTheme.colors.raina)
        ) {
            Image(
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                painter = rememberAsyncImagePainter(model = guide.imageUrl),
                contentDescription = null
            )
        }

        caption_grey(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
            text = DateHelper.shortDate(guide.updatedAt)
        )

        title3_leah(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            text = guide.title
        )
    }
}
