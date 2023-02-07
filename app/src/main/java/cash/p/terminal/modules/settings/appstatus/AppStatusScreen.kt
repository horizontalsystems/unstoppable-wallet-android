package cash.p.terminal.modules.settings.appstatus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.HsBackButton
import cash.p.terminal.ui.compose.components.MenuItem
import cash.p.terminal.ui.compose.components.subhead2_grey
import io.horizontalsystems.core.helpers.HudHelper

@Composable
fun AppStatusScreen(navController: NavController) {
    val viewModel = viewModel<AppStatusViewModel>(factory = AppStatusModule.Factory())
    val appStatusText = viewModel.appStatus
    val clipboardManager = LocalClipboardManager.current
    val localView = LocalView.current

    ComposeAppTheme {
        Column {
            AppBar(
                TranslatableString.ResString(R.string.Settings_AppStatus),
                navigationIcon = {
                    HsBackButton(onClick = { navController.popBackStack() })
                },
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Alert_Copy),
                        onClick = {
                            appStatusText?.let { clipboardManager.setText(AnnotatedString(it)) }
                            HudHelper.showSuccessMessage(localView, R.string.Hud_Text_Copied)
                        },
                    )
                )
            )
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .background(color = ComposeAppTheme.colors.tyler)
                    .fillMaxSize()
            ) {
                appStatusText?.let {
                    subhead2_grey(
                        text = it,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }
}
