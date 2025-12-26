package io.horizontalsystems.bankwallet.modules.settings.appstatus

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ShareCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.stats.StatEntity
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.modules.settings.appstatus.AppStatusModule.BlockContent
import io.horizontalsystems.bankwallet.modules.settings.appstatus.AppStatusModule.BlockData
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.InfoText
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_leah
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.core.helpers.HudHelper


@Composable
fun AppStatusScreen(
    navController: NavController
) {
    val viewModel = viewModel<AppStatusViewModel>(factory = AppStatusModule.Factory())
    val uiState = viewModel.uiState
    val clipboardManager = LocalClipboardManager.current
    val localView = LocalView.current
    val context = LocalContext.current

    HSScaffold(
        title = stringResource(R.string.Settings_AppStatus),
        onBack = navController::popBackStack,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp)
            ) {
                ButtonPrimaryYellow(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.Button_Copy),
                    onClick = {
                        uiState.appStatusAsText?.let {
                            clipboardManager.setText(AnnotatedString(it))
                            HudHelper.showSuccessMessage(localView, R.string.Hud_Text_Copied)

                            stat(
                                page = StatPage.AppStatus,
                                event = StatEvent.Copy(StatEntity.Status)
                            )
                        }
                    }
                )
                HSpacer(8.dp)
                ButtonPrimaryDefault(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.Button_Share),
                    onClick = {
                        uiState.appStatusAsText?.let {
                            ShareCompat.IntentBuilder(context)
                                .setType("text/plain")
                                .setText(it)
                                .startChooser()

                            stat(
                                page = StatPage.AppStatus,
                                event = StatEvent.Share(StatEntity.Status)
                            )
                        }
                    }
                )
            }
            uiState.blockViewItems.forEach { blockData ->
                StatusBlock(
                    sectionTitle = blockData.title,
                    contentItems = blockData.content,
                )
            }
            VSpacer(32.dp)
        }
    }
}

@Composable
private fun StatusBlock(
    sectionTitle: String?,
    contentItems: List<BlockContent>,
) {
    VSpacer(12.dp)
    sectionTitle?.let {
        InfoText(text = it.uppercase())
    }
    CellUniversalLawrenceSection(contentItems) { item ->
        RowUniversal(
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            when (item) {
                is BlockContent.Header -> {
                    subhead2_leah(
                        text = item.title,
                    )
                }

                is BlockContent.Text -> {
                    subhead2_grey(
                        text = item.text,
                    )
                }

                is BlockContent.TitleValue -> {
                    subhead2_grey(
                        modifier = Modifier.weight(1f),
                        text = item.title,
                    )
                    subhead1_leah(
                        modifier = Modifier.padding(start = 8.dp),
                        text = item.value,
                    )
                }
            }
        }
    }
    VSpacer(12.dp)
}

@Preview
@Composable
fun StatusBlockPreview() {
    val testBlocks = listOf(
        BlockData(
            title = "Status",
            content = listOf(
                BlockContent.Header("Header"),
                BlockContent.TitleValue("Title", "Value"),
                BlockContent.TitleValue("Title 2", "Value 2"),
                BlockContent.Text("So then I thought what if I use chat GPT, save a link to my home screen on my phone, and start a new thread on it called MyFitness app. For anyone not familiar with chat GPT you can essentially give it prompts and get it to give outputs that provide information you need. Not always 100% correct but you can give it feedback to adjust as needed."),
            )
        ),
        BlockData(
            title = null,
            content = listOf(
                BlockContent.TitleValue("Title", "Value"),
                BlockContent.TitleValue("Title 2", "Value 2"),
                BlockContent.Text("So then I thought what if I use chat GPT, save a link to my home screen on my phone, and start a new thread on it called MyFitness app. For anyone not familiar with chat GPT you can essentially give it prompts and get it to give outputs that provide information you need. Not always 100% correct but you can give it feedback to adjust as needed."),
            )
        ),
    )
    ComposeAppTheme {
        testBlocks.forEach {
            StatusBlock(
                sectionTitle = it.title,
                contentItems = it.content,
            )
        }
    }
}