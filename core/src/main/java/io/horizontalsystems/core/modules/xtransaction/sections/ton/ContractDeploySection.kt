package io.horizontalsystems.core.modules.xtransaction.sections.ton

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.core.R
import io.horizontalsystems.core.modules.xtransaction.cells.HeaderCell
import io.horizontalsystems.core.ui.compose.components.cell.SectionUniversalLawrence

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