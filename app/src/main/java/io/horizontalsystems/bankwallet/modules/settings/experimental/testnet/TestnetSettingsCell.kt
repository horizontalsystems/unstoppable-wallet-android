package io.horizontalsystems.bankwallet.modules.settings.experimental.testnet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.components.*

@Composable
fun TestnetSettingsCell(viewModel: TestnetSettingsViewModel) {
    CellUniversalLawrenceSection(
        listOf {
            RowUniversal(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalPadding = 0.dp,
            ) {
                Column(Modifier.padding(vertical = 12.dp)) {
                    body_leah(text = stringResource(R.string.ExperimentalFeatures_Testnet_Enable))
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

    InfoText(
        text = stringResource(R.string.ExperimentalFeatures_Testnet_Description),
    )
}
