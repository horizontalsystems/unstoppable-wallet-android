package com.quantum.wallet.bankwallet.modules.releasenotes

import android.os.Parcelable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.BaseComposeFragment
import com.quantum.wallet.bankwallet.core.getInput
import com.quantum.wallet.bankwallet.modules.markdown.MarkdownContent
import com.quantum.wallet.bankwallet.ui.compose.ComposeAppTheme
import com.quantum.wallet.bankwallet.ui.compose.TranslatableString
import com.quantum.wallet.bankwallet.ui.compose.components.HsDivider
import com.quantum.wallet.bankwallet.ui.compose.components.HsIconButton
import com.quantum.wallet.bankwallet.ui.compose.components.MenuItem
import com.quantum.wallet.bankwallet.ui.compose.components.caption_jacob
import com.quantum.wallet.bankwallet.ui.helpers.LinkHelper
import com.quantum.wallet.bankwallet.uiv3.components.HSScaffold
import kotlinx.parcelize.Parcelize

class ReleaseNotesFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        ReleaseNotesScreen(
            closeablePopup = navController.getInput<Input>()?.showAsClosablePopup ?: false,
            onCloseClick = { navController.popBackStack() },
        )
    }

    @Parcelize
    data class Input(val showAsClosablePopup: Boolean) : Parcelable
}

@Composable
fun ReleaseNotesScreen(
    closeablePopup: Boolean,
    onCloseClick: () -> Unit,
    viewModel: ReleaseNotesViewModel = viewModel(factory = ReleaseNotesModule.Factory()),
) {
    BackHandler() {
        viewModel.whatsNewShown()
        onCloseClick.invoke()
    }

    HSScaffold(
        title = "",
        onBack = if (closeablePopup) null else onCloseClick,
        menuItems = if (closeablePopup) listOf(
            MenuItem(
                title = TranslatableString.ResString(R.string.Button_Close),
                icon = R.drawable.ic_close,
                onClick = {
                    viewModel.whatsNewShown()
                    onCloseClick.invoke()
                }
            )
        ) else listOf()
    ) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            MarkdownContent(
                modifier = Modifier.weight(1f),
                viewState = viewModel.viewState,
                markdownBlocks = viewModel.markdownBlocks,
                onRetryClick = { viewModel.retry() },
                onUrlClick = {}
            )

            HsDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ComposeAppTheme.colors.tyler)
                    .height(62.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.padding(start = 16.dp))
                IconButton(
                    R.drawable.ic_twitter_filled_24,
                    viewModel.twitterUrl,
                    stringResource(R.string.CoinPage_Twitter)
                )
                IconButton(
                    R.drawable.ic_telegram_filled_24,
                    viewModel.telegramUrl,
                    stringResource(R.string.CoinPage_Telegram)
                )

                Spacer(Modifier.weight(1f))

                caption_jacob(
                    modifier = Modifier.padding(end = 24.dp),
                    text = stringResource(R.string.ReleaseNotes_JoinUnstoppables)
                )
            }
        }
    }
}

@Composable
private fun IconButton(icon: Int, url: String, description: String) {
    val context = LocalContext.current
    HsIconButton(onClick = { LinkHelper.openLinkInAppBrowser(context, url) }) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = description,
            tint = ComposeAppTheme.colors.jacob
        )
    }
}
