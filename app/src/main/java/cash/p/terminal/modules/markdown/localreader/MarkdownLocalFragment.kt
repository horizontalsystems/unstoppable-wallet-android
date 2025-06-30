package cash.p.terminal.modules.markdown.localreader

import android.os.Parcelable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.modules.markdown.MarkdownContent
import cash.p.terminal.modules.markdown.MarkdownFragment
import cash.p.terminal.navigation.slideFromRight
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.ui_compose.BaseComposeFragment
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.HsBackButton
import cash.p.terminal.ui_compose.components.MenuItem
import cash.p.terminal.ui_compose.getInput
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import kotlinx.parcelize.Parcelize

class MarkdownLocalFragment : BaseComposeFragment() {
    private val viewModel: MarkdownLocalViewModel by viewModels<MarkdownLocalViewModel>()

    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.getInput<Input>()
        LaunchedEffect(Unit) {
            input?.let {
                viewModel.parseContent(getString(input.resId))
            }
        }

        MarkdownLocalScreen(
            showAsPopup = input?.showAsPopup ?: false,
            viewModel = viewModel,
            onCloseClick = navController::popBackStack,
            onUrlClick = { url ->
                navController.slideFromRight(
                    R.id.markdownFragment, MarkdownFragment.Input(url)
                )
            }
        )
    }

    @Parcelize
    data class Input(
        val resId: Int,
        val showAsPopup: Boolean = false,
    ) : Parcelable
}

@Composable
private fun MarkdownLocalScreen(
    showAsPopup: Boolean,
    viewModel: MarkdownLocalViewModel,
    onCloseClick: () -> Unit,
    onUrlClick: (String) -> Unit,
) {

    Scaffold(
        containerColor = ComposeAppTheme.colors.tyler,
        topBar = {
            if (showAsPopup) {
                AppBar(
                    menuItems = listOf(
                        MenuItem(
                            title = TranslatableString.ResString(R.string.Button_Close),
                            icon = R.drawable.ic_close,
                            onClick = onCloseClick
                        )
                    )
                )
            } else {
                AppBar(navigationIcon = { HsBackButton(onClick = onCloseClick) })
            }
        }
    ) {
        MarkdownContent(
            modifier = Modifier.padding(it),
            viewState = viewModel.viewState,
            markdownBlocks = viewModel.markdownBlocks,
            handleRelativeUrl = true,
            onRetryClick = {},
            onUrlClick = onUrlClick
        )
    }
}
