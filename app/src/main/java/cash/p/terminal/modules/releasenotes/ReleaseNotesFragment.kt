package cash.p.terminal.modules.releasenotes

import android.os.Parcelable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.modules.markdown.MarkdownContent
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.ui_compose.components.HsSwitch
import cash.p.terminal.ui.helpers.LinkHelper
import cash.p.terminal.ui_compose.BaseComposeFragment
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.ConnectionStatusView
import cash.p.terminal.ui_compose.components.HsBackButton
import cash.p.terminal.ui_compose.components.HsIconButton
import cash.p.terminal.ui_compose.components.MenuItem
import cash.p.terminal.ui_compose.components.RowUniversal
import cash.p.terminal.ui_compose.components.body_leah
import cash.p.terminal.ui_compose.components.caption_jacob
import cash.p.terminal.ui_compose.entities.ViewState
import cash.p.terminal.ui_compose.getInput
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import kotlinx.parcelize.Parcelize
import org.koin.androidx.viewmodel.ext.android.viewModel

class ReleaseNotesFragment : BaseComposeFragment() {
    private val viewModel: ReleaseNotesViewModel by viewModel()

    override val showConnectionPanel = false

    @Composable
    override fun GetContent(navController: NavController) {
        ReleaseNotesScreen(
            closeablePopup = navController.getInput<Input>()?.showAsClosablePopup ?: false,
            uiState = viewModel.uiState,
            onCloseClick = navController::popBackStack,
            onRetryClick = { viewModel.retry() },
            onWhatsNewShown = { viewModel.whatsNewShown() },
            onShowChangelogToggle = viewModel::setShowChangeLogAfterUpdate
        )
    }

    @Parcelize
    data class Input(val showAsClosablePopup: Boolean) : Parcelable
}

@Composable
fun ReleaseNotesScreen(
    closeablePopup: Boolean,
    uiState: ReleaseNotesUiState,
    onCloseClick: () -> Unit,
    onRetryClick: () -> Unit,
    onWhatsNewShown: () -> Unit,
    onShowChangelogToggle: () -> Unit,
    onUrlClick: (String) -> Unit = {}
) {
    BackHandler {
        onWhatsNewShown()
        onCloseClick()
    }

    Scaffold(
        containerColor = ComposeAppTheme.colors.tyler,
        topBar = {
            if (closeablePopup) {
                AppBar(
                    menuItems = listOf(
                        MenuItem(
                            title = TranslatableString.ResString(R.string.Button_Close),
                            icon = R.drawable.ic_close_24,
                            onClick = {
                                onWhatsNewShown()
                                onCloseClick()
                            }
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
    ) { paddingValues ->
        Column(Modifier.padding(top = paddingValues.calculateTopPadding())) {
            MarkdownContent(
                modifier = Modifier.weight(1f),
                viewState = uiState.viewState,
                markdownContent = uiState.markdownContent,
                addFooter = true,
                onRetryClick = onRetryClick,
                onUrlClick = onUrlClick
            )

            HorizontalDivider(
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10,
            )

            ConnectionStatusView()
            RowUniversal(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ComposeAppTheme.colors.tyler)
                    .padding(horizontal = 16.dp),
                onClick = onShowChangelogToggle,
            ) {
                body_leah(
                    text = stringResource(R.string.show_changelog_after_update),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp, end = 8.dp)
                )
                HsSwitch(
                    checked = uiState.showChangelogAfterUpdate,
                    onCheckedChange = { onShowChangelogToggle() }
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ComposeAppTheme.colors.tyler)
                    .padding(bottom = paddingValues.calculateBottomPadding())
                    .height(40.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.padding(start = 16.dp))

                IconButton(
                    R.drawable.ic_twitter_filled_24,
                    uiState.twitterUrl,
                    stringResource(R.string.CoinPage_Twitter)
                )

                IconButton(
                    R.drawable.ic_telegram_filled_24,
                    uiState.telegramUrl,
                    stringResource(R.string.CoinPage_Telegram)
                )

                IconButton(
                    R.drawable.ic_reddit_filled_24,
                    uiState.redditUrl,
                    stringResource(R.string.CoinPage_Reddit)
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


@Preview(showBackground = true)
@Composable
fun ReleaseNotesScreenPreview() {
    ComposeAppTheme {
        ReleaseNotesScreen(
            closeablePopup = true,
            uiState = ReleaseNotesUiState(
                viewState = ViewState.Success,
                markdownContent = null,
                twitterUrl = "https://twitter.com/example",
                telegramUrl = "https://t.me/example",
                redditUrl = "https://reddit.com/r/example",
                showChangelogAfterUpdate = true
            ),
            onCloseClick = {},
            onRetryClick = {},
            onWhatsNewShown = {},
            onShowChangelogToggle = {}
        )
    }
}