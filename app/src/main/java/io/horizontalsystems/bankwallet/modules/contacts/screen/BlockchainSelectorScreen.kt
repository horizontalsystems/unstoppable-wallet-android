package io.horizontalsystems.bankwallet.modules.contacts.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.addtoken.blockchainselector.BlockchainCell
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.HSSectionRounded
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color = ComposeAppTheme.colors.tyler)
        ) {
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
                    blockchains.forEachIndexed { index, item ->
                        BlockchainCell(
                            item = item,
                            selected = selectedItem == item,
                            onCheck = {
                                selectedItem = item

                                onSelectBlockchain(it)
                            },
                            borderTop = index != 0
                        )
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
