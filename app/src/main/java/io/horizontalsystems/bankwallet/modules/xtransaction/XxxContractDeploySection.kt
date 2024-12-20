package io.horizontalsystems.bankwallet.modules.xtransaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.components.cell.SectionUniversalLawrence

@Composable
fun XxxContractDeploySection(
    interfaces: List<String>,
) {
    SectionUniversalLawrence {
        XxxSectionHeaderCell(
            title = stringResource(R.string.Transactions_ContractDeploy),
            value = interfaces.joinToString(),
            painter = null
        )
    }
}