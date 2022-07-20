package io.horizontalsystems.bankwallet.modules.settings.guides

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.LocalizedException
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.entities.Guide
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.overview.Loading
import io.horizontalsystems.bankwallet.modules.markdown.MarkdownFragment
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.DateHelper
import java.net.UnknownHostException

class GuidesFragment : BaseFragment() {

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
                    GuidesScreen(findNavController())
                }
            }
        }
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
            title = TranslatableString.ResString(R.string.Guides_Title),
            navigationIcon = {
                HsIconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_back),
                        contentDescription = "back",
                        tint = ComposeAppTheme.colors.jacob
                    )
                }
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
                                        val arguments = bundleOf(
                                            MarkdownFragment.markdownUrlKey to guide.fileUrl,
                                            MarkdownFragment.handleRelativeUrlKey to true
                                        )
                                        navController.slideFromRight(
                                            R.id.markdownFragment,
                                            arguments
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
