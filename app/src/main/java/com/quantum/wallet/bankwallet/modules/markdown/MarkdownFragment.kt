package com.quantum.wallet.bankwallet.modules.markdown

import android.os.Parcelable
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.BaseComposeFragment
import com.quantum.wallet.bankwallet.core.slideFromRight
import com.quantum.wallet.bankwallet.ui.compose.TranslatableString
import com.quantum.wallet.bankwallet.ui.compose.components.MenuItem
import com.quantum.wallet.bankwallet.uiv3.components.HSScaffold
import kotlinx.parcelize.Parcelize

class MarkdownFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        withInput<Input>(navController) { input ->
            MarkdownScreen(
                handleRelativeUrl = input.handleRelativeUrl,
                showAsPopup = input.showAsPopup,
                markdownUrl = input.markdownUrl,
                onCloseClick = { navController.popBackStack() },
                onUrlClick = { url ->
                    navController.slideFromRight(
                        R.id.markdownFragment, Input(url)
                    )
                }
            )
        }
    }

    @Parcelize
    data class Input(
        val markdownUrl: String,
        val handleRelativeUrl: Boolean = false,
        val showAsPopup: Boolean = false,
    ) : Parcelable
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
        title = stringResource(R.string.CoinPage_Indicators),
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
