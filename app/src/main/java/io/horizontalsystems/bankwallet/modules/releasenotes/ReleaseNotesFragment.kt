package io.horizontalsystems.bankwallet.modules.releasenotes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.markdown.MarkdownContent
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.HsIconButton
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.caption_grey
import io.horizontalsystems.bankwallet.ui.helpers.LinkHelper
import io.horizontalsystems.core.findNavController

class ReleaseNotesFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent() {
        ComposeAppTheme {
            ReleaseNotesScreen(
                closeablePopup = arguments?.getBoolean(showAsClosablePopupKey) ?: false,
                onCloseClick = { findNavController().popBackStack() },
            )
        }
    }

    companion object {
        const val showAsClosablePopupKey = "showAsClosablePopup"
    }

}

@Composable
fun ReleaseNotesScreen(
    closeablePopup: Boolean,
    onCloseClick: () -> Unit,
    viewModel: ReleaseNotesViewModel = viewModel(factory = ReleaseNotesModule.Factory())
) {

    Scaffold(
        backgroundColor = ComposeAppTheme.colors.tyler,
        topBar = {
            if (closeablePopup) {
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
                AppBar(
                    navigationIcon = {
                        HsBackButton(onClick = onCloseClick)
                    }
                )
            }
        }
    ) {
        Column(Modifier.padding(it)) {
            MarkdownContent(
                modifier = Modifier.weight(1f),
                viewState = viewModel.viewState,
                markdownBlocks = viewModel.markdownBlocks,
                onRetryClick = { viewModel.retry() },
                onUrlClick = {}
            )

            Divider(
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10,
            )
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
                IconButton(
                    R.drawable.ic_reddit_filled_24,
                    viewModel.redditUrl,
                    stringResource(R.string.CoinPage_Reddit)
                )

                Spacer(Modifier.weight(1f))

                caption_grey(
                    modifier = Modifier.padding(end = 24.dp),
                    text = stringResource(R.string.ReleaseNotes_FollowUs)
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
            tint = ComposeAppTheme.colors.grey
        )
    }
}
