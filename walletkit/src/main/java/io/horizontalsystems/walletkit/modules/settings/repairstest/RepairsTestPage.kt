// TEMP: repairs test, remove after testing
package io.horizontalsystems.walletkit.modules.settings.repairstest

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.designkit.components.HSButton
import io.horizontalsystems.designkit.theme.ComposeAppTheme
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import io.horizontalsystems.walletkit.uiv3.components.HSScaffold
import io.horizontalsystems.walletkit.uiv3.components.Section
import io.horizontalsystems.walletkit.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.walletkit.uiv3.components.cell.CellPrimary
import io.horizontalsystems.walletkit.uiv3.components.cell.CellRightControlsSwitcher
import io.horizontalsystems.walletkit.uiv3.components.cell.hs
import kotlinx.serialization.Serializable

// TEMP: repairs test, remove after testing
@Serializable
data object RepairsTestPage : HSPage() {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        RepairsTestScreen(onBack = { navigation.removeLastOrNull() })
    }
}

// TEMP: repairs test, remove after testing
@Composable
private fun RepairsTestScreen(onBack: () -> Unit) {
    var testSwitch by remember { mutableStateOf(false) }
    var resultShown by remember { mutableStateOf(false) }

    HSScaffold(
        title = stringResource(R.string.RepairsTest_Title),
        onBack = onBack,
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            VSpacer(12.dp)
            Text(
                modifier = Modifier.padding(horizontal = 32.dp),
                text = stringResource(R.string.RepairsTest_Description),
                style = ComposeAppTheme.typography.subhead,
                color = ComposeAppTheme.colors.grey,
            )
            VSpacer(12.dp)
            Section {
                CellPrimary(
                    middle = {
                        CellMiddleInfo(title = stringResource(R.string.RepairsTest_Switch).hs)
                    },
                    right = {
                        CellRightControlsSwitcher(
                            checked = testSwitch,
                            onCheckedChange = { testSwitch = it },
                        )
                    },
                    onClick = { testSwitch = !testSwitch },
                )
            }
            VSpacer(24.dp)
            HSButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                title = stringResource(R.string.RepairsTest_ShowResult),
                onClick = { resultShown = true },
            )
            if (resultShown) {
                VSpacer(12.dp)
                Text(
                    modifier = Modifier.padding(horizontal = 32.dp),
                    text = stringResource(R.string.RepairsTest_Result),
                    style = ComposeAppTheme.typography.body,
                    color = ComposeAppTheme.colors.leah,
                )
            }
            VSpacer(36.dp)
        }
    }
}
