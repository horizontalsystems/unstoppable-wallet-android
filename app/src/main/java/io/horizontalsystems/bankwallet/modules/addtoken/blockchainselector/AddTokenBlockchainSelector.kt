package io.horizontalsystems.bankwallet.modules.addtoken.blockchainselector

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.addtoken.AddTokenScreen
import io.horizontalsystems.bankwallet.modules.addtoken.AddTokenViewModel
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.ResultEventBus
import io.horizontalsystems.bankwallet.ui.compose.components.cell.CellBlockchainChecked
import io.horizontalsystems.bankwallet.ui.compose.components.cell.SectionUniversalLawrence
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.marketkit.models.Blockchain
import kotlinx.serialization.Serializable

@Serializable
data object AddTokenBlockchainSelectorScreen : HSScreen() {
    override fun getParentVMKey(backStack: NavBackStack<HSScreen>): String? {
        return backStack.findLast { it is AddTokenScreen }?.toString()
    }

    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>,
        resultBus: ResultEventBus
    ) {
        val viewModel = viewModel<AddTokenViewModel>()
        AddTokenBlockchainSelectorScreen(
            blockchains = viewModel.blockchains,
            selectedBlockchain = viewModel.selectedBlockchain,
            backStack = backStack,
            resultBus = resultBus,
        )
    }

    data class Result(val blockchain: Blockchain)
}

@Composable
fun AddTokenBlockchainSelectorScreen(
    blockchains: List<Blockchain>,
    selectedBlockchain: Blockchain,
    backStack: NavBackStack<HSScreen>,
    resultBus: ResultEventBus,
) {
    var selectedItem = selectedBlockchain

    HSScaffold(
        title = stringResource(R.string.Market_Filter_Blockchains),
        onBack = backStack::removeLastOrNull,
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(12.dp))
            SectionUniversalLawrence {
                blockchains.forEachIndexed { index, item ->
                    CellBlockchainChecked(
                        borderTop = index != 0,
                        blockchain = item,
                        checked = selectedItem == item,
                    ) {
                        selectedItem = item
                        resultBus.sendResult(result = AddTokenBlockchainSelectorScreen.Result(item))
                        backStack.removeLastOrNull()
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
