package com.quantum.wallet.bankwallet.modules.xtransaction.sections.ton

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.modules.xtransaction.cells.HeaderCell
import com.quantum.wallet.bankwallet.ui.compose.components.cell.SectionUniversalLawrence

@Composable
fun ContractDeploySection(
    interfaces: List<String>,
) {
    SectionUniversalLawrence {
        HeaderCell(
            title = stringResource(R.string.Transactions_ContractDeploy),
            value = interfaces.joinToString(),
            painter = null
        )
    }
}