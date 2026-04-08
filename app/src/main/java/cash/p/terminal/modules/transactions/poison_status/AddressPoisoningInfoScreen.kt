@file:Suppress("PackageNaming")

package cash.p.terminal.modules.transactions.poison_status

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cash.p.terminal.R
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.ui_compose.annotatedStringResource
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.MenuItem
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.components.body_grey
import cash.p.terminal.ui_compose.components.body_leah
import cash.p.terminal.ui_compose.components.caption_grey
import cash.p.terminal.ui_compose.components.subhead1_leah
import cash.p.terminal.ui_compose.components.title2_leah
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import java.util.Calendar

@Suppress("ModifierMissing")
@Composable
fun AddressPoisoningInfoScreen(
    onClose: () -> Unit,
) {
    Scaffold(
        containerColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Close),
                        icon = R.drawable.ic_close_24,
                        onClick = onClose,
                    )
                )
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp),
        ) {
            VSpacer(12.dp)
            title2_leah(stringResource(R.string.address_poisoning_info_title))
            VSpacer(24.dp)
            HorizontalDivider(color = ComposeAppTheme.colors.steel20)
            VSpacer(24.dp)
            Text(
                text = annotatedStringResource(R.string.address_poisoning_info_description),
                style = ComposeAppTheme.typography.body,
                color = ComposeAppTheme.colors.leah,
            )
            VSpacer(16.dp)
            body_leah(text = stringResource(R.string.address_poisoning_info_goal))
            VSpacer(16.dp)
            body_leah(text = stringResource(R.string.address_poisoning_info_addresses_intro))
            VSpacer(4.dp)
            BulletPoint(stringResource(R.string.address_poisoning_info_bullet_1))
            BulletPoint(stringResource(R.string.address_poisoning_info_bullet_2))
            BulletPoint(stringResource(R.string.address_poisoning_info_bullet_3))
            VSpacer(16.dp)
            body_leah(text = stringResource(R.string.transaction_statuses_description))
            VSpacer(24.dp)

            PoisonStatus.entries.forEach { status ->
                StatusInfoEntry(status)
                VSpacer(16.dp)
            }

            body_leah(text = stringResource(R.string.transaction_statuses_footer))
            VSpacer(24.dp)
            caption_grey(
                text = stringResource(
                    R.string.footer_text,
                    Calendar.getInstance().get(Calendar.YEAR)
                ),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            VSpacer(32.dp)
        }
    }
}

@Composable
private fun BulletPoint(text: String) {
    Row(modifier = Modifier.padding(start = 8.dp, top = 4.dp)) {
        body_leah(text = "•")
        body_leah(
            text = text,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun StatusInfoEntry(status: PoisonStatus) {
    PoisonStatusInfoEntry(
        status = status,
        titleContent = { subhead1_leah(text = it) },
        descriptionContent = { body_grey(text = it) },
    )
}

@Composable
fun AddressPoisoningInfoDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        AddressPoisoningInfoScreen(onClose = onDismiss)
    }
}

@Suppress("UnusedPrivateMember")
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AddressPoisoningInfoScreenPreview() {
    ComposeAppTheme {
        AddressPoisoningInfoScreen(onClose = {})
    }
}
