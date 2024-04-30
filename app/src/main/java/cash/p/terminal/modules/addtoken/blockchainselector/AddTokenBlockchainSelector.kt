package cash.p.terminal.modules.addtoken.blockchainselector

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.HsBackButton
import cash.p.terminal.ui.compose.components.cell.CellBlockchainChecked
import cash.p.terminal.ui.compose.components.cell.SectionUniversalLawrence
import io.horizontalsystems.marketkit.models.Blockchain

const val BlockchainSelectorResult = "blockchain_selector_result_key"

@Composable
fun AddTokenBlockchainSelectorScreen(
    blockchains: List<Blockchain>,
    selectedBlockchain: Blockchain,
    navController: NavController,
) {
    var selectedItem = selectedBlockchain

    Scaffold(
        topBar = {
            AppBar(
                title = stringResource(R.string.Market_Filter_Blockchains),
                navigationIcon = {
                    HsBackButton(onClick = { navController.popBackStack() })
                },
            )
        },
        backgroundColor = ComposeAppTheme.colors.tyler
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .verticalScroll(rememberScrollState())
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
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set(BlockchainSelectorResult, listOf(item))
                        navController.popBackStack()
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
