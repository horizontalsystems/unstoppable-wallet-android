package io.horizontalsystems.bankwallet.modules.settings.security.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.settings.security.testnet.TestnetSettingsViewModel
import io.horizontalsystems.bankwallet.ui.compose.components.*

@Composable
fun TestnetSettingsBlock(viewModel: TestnetSettingsViewModel) {

    CellUniversalLawrenceSection(
        listOf {
            RowUniversal(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalPadding = 0.dp,
            ) {
                Column(Modifier.padding(vertical = 12.dp)) {
                    body_leah(text = stringResource(R.string.Testnet_Enable))
                    Spacer(Modifier.height(1.dp))
                    subhead2_grey(text = stringResource(R.string.Testnet_Support))
                }
                Spacer(Modifier.weight(1f))
                HsSwitch(
                    checked = viewModel.testnetEnabled,
                    onCheckedChange = { checked ->
                        viewModel.setTestnetMode(checked)
                    }
                )
            }
        })
}
