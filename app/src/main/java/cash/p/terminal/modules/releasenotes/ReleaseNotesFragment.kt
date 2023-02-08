package cash.p.terminal.modules.releasenotes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cash.p.terminal.R
import cash.p.terminal.core.BaseFragment
import cash.p.terminal.modules.markdown.MarkdownContent
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.*
import cash.p.terminal.ui.helpers.LinkHelper
import io.horizontalsystems.core.findNavController

class ReleaseNotesFragment : BaseFragment() {

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
                    ReleaseNotesScreen(
                        closeablePopup = arguments?.getBoolean(showAsClosablePopupKey) ?: false,
                        onCloseClick = { findNavController().popBackStack() },
                    )
                }
            }
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
                    viewModel.twitterUrl
                )
                IconButton(
                    R.drawable.ic_telegram_filled_24,
                    viewModel.telegramUrl
                )
                IconButton(
                    R.drawable.ic_reddit_filled_24,
                    viewModel.redditUrl
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
private fun IconButton(icon: Int, twitterUrl: String) {
    val context = LocalContext.current
    HsIconButton(onClick = { LinkHelper.openLinkInAppBrowser(context, twitterUrl) }) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            tint = ComposeAppTheme.colors.grey
        )
    }
}
