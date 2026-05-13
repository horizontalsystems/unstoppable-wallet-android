package io.horizontalsystems.bankwallet.modules.markdown

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import kotlinx.serialization.Serializable

@Serializable
data class MarkdownFragment(val input: Input) : HSScreen() {

    @Composable
    override fun GetContent(navController: HSNavigation) {
        MarkdownScreen(
            handleRelativeUrl = input.handleRelativeUrl,
            showAsPopup = input.showAsPopup,
            markdownUrl = input.markdownUrl,
            onCloseClick = { navController.removeLastOrNull() },
            onUrlClick = { url ->
                navController.slideFromRight(
                    MarkdownFragment(Input(url))
                )
            }
        )
    }

    @Serializable
    data class Input(
        val markdownUrl: String,
        val handleRelativeUrl: Boolean = false,
        val showAsPopup: Boolean = false,
    )
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
    HSScaffold(
        title = "",
        onBack = if (showAsPopup) null else onCloseClick,
        menuItems = if (showAsPopup) listOf(
            MenuItem(
                title = TranslatableString.ResString(R.string.Button_Close),
                icon = R.drawable.ic_close,
                onClick = onCloseClick
            )
        ) else listOf()
    ) {
        MarkdownContent(
            modifier = Modifier
                .statusBarsPadding()
                .navigationBarsPadding(),
            viewState = viewModel.viewState,
            markdownBlocks = viewModel.markdownBlocks,
            handleRelativeUrl = handleRelativeUrl,
            onRetryClick = { viewModel.retry() },
            onUrlClick = onUrlClick
        )
    }
}
