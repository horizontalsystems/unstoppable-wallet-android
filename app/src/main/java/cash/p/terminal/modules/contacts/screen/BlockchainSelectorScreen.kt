package cash.p.terminal.modules.contacts.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.modules.addtoken.blockchainselector.BlockchainCell
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.HSSectionRounded
import cash.p.terminal.ui.compose.components.HsBackButton
import cash.p.terminal.ui.compose.components.MenuItem
import io.horizontalsystems.marketkit.models.Blockchain

@Composable
fun BlockchainSelectorScreen(
    blockchains: List<Blockchain>,
    selectedBlockchain: Blockchain,
    onSelectBlockchain: (Blockchain) -> Unit,
    onNavigateToBack: () -> Unit
) {
    val menuItems = emptyList<MenuItem>()
    var selectedItem by remember { mutableStateOf(selectedBlockchain) }

    ComposeAppTheme {
        Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
            AppBar(
                title = TranslatableString.ResString(R.string.Market_Filter_Blockchains),
                navigationIcon = {
                    HsBackButton(onNavigateToBack)
                },
                menuItems = menuItems
            )

            Column(
                Modifier.verticalScroll(rememberScrollState())
            ) {
                Spacer(Modifier.height(12.dp))
                HSSectionRounded {
                    blockchains.forEach { item ->
                        BlockchainCell(
                            item = item,
                            selected = selectedItem == item,
                            onCheck = {
                                selectedItem = item

                                onSelectBlockchain(it)
                            }
                        )
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
