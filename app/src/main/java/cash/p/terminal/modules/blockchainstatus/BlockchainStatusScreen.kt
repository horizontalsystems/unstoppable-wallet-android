package cash.p.terminal.modules.blockchainstatus

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cash.p.terminal.R
import androidx.compose.material3.Icon
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.ButtonPrimaryDefault
import cash.p.terminal.ui_compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui_compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui_compose.components.HSpacer
import cash.p.terminal.ui_compose.components.HsBackButton
import cash.p.terminal.ui_compose.components.HudHelper
import cash.p.terminal.ui_compose.components.InfoText
import cash.p.terminal.ui_compose.components.RowUniversal
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.components.body_leah
import cash.p.terminal.ui_compose.components.subhead1_leah
import cash.p.terminal.ui_compose.components.subhead1_lucian
import cash.p.terminal.ui_compose.components.subhead1_remus
import cash.p.terminal.ui_compose.components.subhead2_grey
import cash.p.terminal.ui_compose.components.subhead2_leah
import cash.p.terminal.ui_compose.theme.ComposeAppTheme

@Composable
internal fun BlockchainStatusScreen(
    viewModel: BlockchainStatusViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current
    val localView = LocalView.current
    val context = LocalContext.current

    Scaffold(
        containerColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = "${uiState.blockchainName} ${stringResource(R.string.blockchain_status)}",
                navigationIcon = {
                    HsBackButton(onClick = onBack)
                },
            )
        }
    ) { paddingValues ->
        if (uiState.loading) {
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = ComposeAppTheme.colors.grey,
                    strokeWidth = 4.dp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(paddingValues)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 12.dp)
                    ) {
                        ButtonPrimaryYellow(
                            modifier = Modifier.weight(1f),
                            title = stringResource(R.string.Button_Copy),
                            enabled = !uiState.statusLoading,
                            onClick = {
                                uiState.statusAsText?.let {
                                    clipboardManager.setText(AnnotatedString(it))
                                    HudHelper.showSuccessMessage(
                                        localView,
                                        R.string.Hud_Text_Copied
                                    )
                                }
                            }
                        )
                        HSpacer(8.dp)
                        ButtonPrimaryDefault(
                            modifier = Modifier.weight(1f),
                            title = stringResource(R.string.Button_Share),
                            enabled = !uiState.statusLoading,
                            onClick = {
                                try {
                                    val uri = viewModel.getShareFileUri(context)
                                    if (uri != null) {
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                context.getString(R.string.blockchain_status)
                                            )
                                            addFlags(
                                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                    or Intent.FLAG_ACTIVITY_NEW_TASK
                                            )
                                        }
                                        context.startActivity(Intent.createChooser(intent, null))
                                    } else {
                                        HudHelper.showErrorMessage(
                                            localView,
                                            R.string.error_cannot_create_log_file
                                        )
                                    }
                                } catch (e: Exception) {
                                    HudHelper.showErrorMessage(
                                        localView,
                                        e.message ?: context.getString(R.string.Error)
                                    )
                                }
                            }
                        )
                    }
                }

                // Kit Info
                item {
                    KitInfoBlock(uiState.kitVersion, uiState.kitStarted)
                }

                // Status sections (with loading indicator if still loading)
                if (uiState.statusLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = ComposeAppTheme.colors.grey,
                                strokeWidth = 3.dp
                            )
                        }
                    }
                } else {
                    items(uiState.statusSections) { section ->
                        StatusSectionBlock(section)
                    }

                    uiState.sharedSection?.let { section ->
                        item {
                            StatusSectionBlock(section)
                        }
                    }
                }

                // App logs
                if (uiState.logBlocks.isNotEmpty()) {
                    item {
                        InfoText(text = "APP LOG")
                    }
                    items(uiState.logBlocks) { logBlock ->
                        CellUniversalLawrenceSection(
                            listOf(logBlock)
                        ) { block ->
                            RowUniversal(
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    subhead2_leah(text = block.title)
                                    subhead2_grey(text = block.content.trimEnd())
                                }
                            }
                        }
                        VSpacer(12.dp)
                    }
                }

                item {
                    VSpacer(32.dp)
                }
            }
        }
    }
}

@Composable
private fun KitInfoBlock(kitVersion: String, kitStarted: Boolean) {
    VSpacer(12.dp)
    InfoText(text = "KIT INFO")
    CellUniversalLawrenceSection(
        listOf("version" to kitVersion, "started" to kitStarted)
    ) { (key, _) ->
        RowUniversal(
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            if (key == "version") {
                subhead2_grey(
                    modifier = Modifier.weight(1f),
                    text = "Version"
                )
                subhead1_leah(text = kitVersion)
            } else {
                subhead2_grey(
                    modifier = Modifier.weight(1f),
                    text = "Kit Started"
                )
                if (kitStarted) {
                    subhead1_remus(text = "Yes")
                } else {
                    subhead1_lucian(text = "No")
                }
            }
        }
    }
}

@Composable
internal fun BlockchainStatusButton(onClick: () -> Unit) {
    CellUniversalLawrenceSection(listOf(Unit)) {
        RowUniversal(
            modifier = Modifier.padding(horizontal = 16.dp),
            onClick = onClick
        ) {
            body_leah(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.blockchain_status)
            )
            Icon(
                painter = painterResource(R.drawable.ic_arrow_right),
                tint = ComposeAppTheme.colors.grey,
                contentDescription = null,
            )
        }
    }
}

@Composable
internal fun StatusSectionBlock(section: StatusSection) {
    VSpacer(12.dp)
    InfoText(text = section.title.uppercase())
    CellUniversalLawrenceSection(section.items) { item ->
        RowUniversal(
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            when (item) {
                is StatusItem.KeyValue -> {
                    subhead2_grey(text = item.key)
                    subhead1_leah(
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                        text = item.value,
                        textAlign = TextAlign.End
                    )
                }
                is StatusItem.Nested -> {
                    Column(modifier = Modifier.weight(1f)) {
                        body_leah(text = item.title)
                        item.items.forEach { kv ->
                            Row {
                                subhead2_grey(text = "  ${kv.key}: ")
                                subhead2_grey(text = kv.value)
                            }
                        }
                    }
                }
            }
        }
    }
}
