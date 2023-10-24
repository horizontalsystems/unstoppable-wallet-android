package io.horizontalsystems.bankwallet.modules.markdown

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.os.bundleOf
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem

class MarkdownFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        MarkdownScreen(
            handleRelativeUrl = arguments?.getBoolean(handleRelativeUrlKey) ?: false,
            showAsPopup = arguments?.getBoolean(showAsPopupKey) ?: false,
            markdownUrl = arguments?.getString(markdownUrlKey) ?: "",
            onCloseClick = { navController.popBackStack() },
            onUrlClick = { url ->
                navController.slideFromRight(
                    R.id.markdownFragment, bundleOf(markdownUrlKey to url)
                )
            }
        )
    }

    companion object {
        const val markdownUrlKey = "urlKey"
        const val handleRelativeUrlKey = "handleRelativeUrlKey"
        const val showAsPopupKey = "showAsPopupKey"
    }
}

@Composable
private fun MarkdownScreen(
    handleRelativeUrl: Boolean,
    showAsPopup: Boolean,
    markdownUrl: String,
    onCloseClick: () -> Unit,
    onUrlClick: (String) -> Unit,
    viewModel: MarkdownViewModel = viewModel(factory = MarkdownModule.Factory(markdownUrl))
) {

    Scaffold(
        backgroundColor = ComposeAppTheme.colors.tyler,
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
            handleRelativeUrl = handleRelativeUrl,
            onRetryClick = { viewModel.retry() },
            onUrlClick = onUrlClick
        )
    }
}
